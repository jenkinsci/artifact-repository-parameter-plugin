package io.jenkins.plugins.artifactrepo.helper;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.ProxyConfiguration;
import io.jenkins.plugins.artifactrepo.Messages;
import io.jenkins.plugins.artifactrepo.model.ArtifactRepoParamProxy;
import io.jenkins.plugins.artifactrepo.model.HttpResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Optional;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import jenkins.model.Jenkins;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

/**
 * A simple utility class to help create the HTTP connection from the plugin to the target
 * repository instances.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Log
public class HttpHelper {

  private static final int SOCKET_TIMEOUT = 60;
  private static final int CONN_TIMEOUT = 60;
  private static final String AGENT = "Jenkins Plugin - Artifact Repository Parameter";

  /**
   * A generic implementation to do GET requests will automatic resource clean up.
   *
   * @param url The URL to call
   * @param builder The builder object used to create the {@link org.apache.http.client.HttpClient}.
   * @param context A possible context object to add to the request. Can be used to perform
   *     pre-emptive authentication (required by Nexus).
   * @return An instance of {@link HttpResponse} with both return code and response payload.
   */
  public static HttpResponse get(
      @Nonnull String url, @Nonnull HttpClientBuilder builder, @Nonnull HttpClientContext context) {
    if (StringUtils.isBlank(url)) {
      log.info(Messages.log_blankUrl());
      return HttpResponse.EXCEPTION;
    }

    try (CloseableHttpClient httpClient = builder.build()) {
      HttpGet get = new HttpGet(url);
      try (CloseableHttpResponse response = httpClient.execute(get, context)) {
        String payload =
            IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        int rc = response.getStatusLine().getStatusCode();
        return new HttpResponse(rc, payload);
      }
    } catch (Exception e) {
      log.log(Level.SEVERE, "", e);
      return HttpResponse.EXCEPTION;
    }
  }

  /**
   * Convenience method of {@link HttpHelper#get(String, HttpClientBuilder, HttpClientContext)} with
   * a default client context.
   */
  public static HttpResponse get(@Nonnull String url, @Nonnull HttpClientBuilder builder) {
    return get(url, builder, HttpClientContext.create());
  }

  /**
   * Returns an opinionated and preconfigured HttpClient builder object.
   *
   * @param repoCredId The ID of the credentials object that should get used to authenticate at the
   *     target repository instance.
   * @param proxy A proxy object with all the proxy information in it.
   * @param ignoreSSL Whether or not to ignore invalid SSL certificates (e. g. self-signed).
   * @return An {@link HttpClientBuilder} object with some pre-defined configuraitons.
   */
  public static HttpClientBuilder getBuilder(
      String repoCredId, ArtifactRepoParamProxy proxy, boolean ignoreSSL) {
    return Optional.of(HttpClients.custom())
        .map(HttpHelper::addDefaultConfig)
        .map(builder -> addBasicAuth(builder, repoCredId, proxy))
        .map(builder -> addProxy(builder, proxy))
        .map(builder -> addSslHandling(builder, ignoreSSL))
        .orElse(HttpClients.custom());
  }

  /**
   * Get the Jenkins credentials object of type StandardUsernamePasswordCredentials identified by
   * the provided ID string.
   */
  public static Optional<StandardUsernamePasswordCredentials> getCredentials(String credId) {
    if (StringUtils.isBlank(credId)) {
      log.info(Messages.log_missingCreds());
      return Optional.empty();
    }

    return com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
            StandardUsernamePasswordCredentials.class, Jenkins.get(), null, Collections.emptyList())
        .stream()
        .filter(cred -> StringUtils.equals(cred.getId(), credId))
        .findFirst();
  }

  /** Takes a URL string and creates a {@link HttpHost} object of it. */
  public static HttpHost getHttpHostFromUrl(@Nonnull String urlString)
      throws MalformedURLException {
    URL url = new URL(urlString);
    return new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
  }

  /**
   * Adds a default request configuration that includes user agent, redirect and timeout
   * information.
   */
  private static HttpClientBuilder addDefaultConfig(HttpClientBuilder builder) {
    RequestConfig.Builder configBuilder =
        RequestConfig.copy(RequestConfig.DEFAULT)
            .setSocketTimeout(SOCKET_TIMEOUT * 1000)
            .setConnectionRequestTimeout(CONN_TIMEOUT * 1000)
            .setRedirectsEnabled(true)
            .setMaxRedirects(10);

    return builder.setUserAgent(AGENT).setDefaultRequestConfig(configBuilder.build());
  }

  /**
   * Retrieve Jenkins credentials identified by the credential IDs and add them to the HTTP
   * credentials provider.
   */
  private static HttpClientBuilder addBasicAuth(
      HttpClientBuilder builder, String repoCredId, ArtifactRepoParamProxy proxy) {
    CredentialsProvider httpProvider = new BasicCredentialsProvider();

    Optional<StandardUsernamePasswordCredentials> repoCred = getCredentials(repoCredId);
    if (repoCred.isPresent()) {
      httpProvider.setCredentials(
          AuthScope.ANY,
          new UsernamePasswordCredentials(
              repoCred.get().getUsername(), repoCred.get().getPassword().getPlainText()));
    } else {
      log.info(Messages.log_invalidCreds(repoCredId));
    }

    if (proxy == null || !proxy.isProxyActive()) {
      return builder.setDefaultCredentialsProvider(httpProvider);
    }

    Optional<StandardUsernamePasswordCredentials> proxyCredId =
        getCredentials(proxy.getProxyCredentialsId());
    if (proxyCredId.isPresent()) {
      httpProvider.setCredentials(
          new AuthScope(proxy.getProxyHost(), Integer.parseInt(proxy.getProxyPort())),
          new UsernamePasswordCredentials(
              proxyCredId.get().getUsername(), proxyCredId.get().getPassword().getPlainText()));
    } else {
      log.info(Messages.log_invalidCreds(proxyCredId));
    }

    return builder.setDefaultCredentialsProvider(httpProvider);
  }

  /**
   * Check if either in the build config or globally a proxy is configured and if so add it to the
   * builder.
   */
  private static HttpClientBuilder addProxy(
      HttpClientBuilder builder, ArtifactRepoParamProxy proxy) {
    ProxyConfiguration jenkinsProxy = Jenkins.get().proxy;
    HttpHost proxyHost = null;

    if (proxy != null && StringUtils.isNoneBlank(proxy.getProxyHost(), proxy.getProxyPort())) {
      proxyHost =
          new HttpHost(
              proxy.getProxyHost(),
              Integer.parseInt(proxy.getProxyPort()),
              proxy.getProxyProtocol());
    } else if (jenkinsProxy != null && StringUtils.isNotBlank(jenkinsProxy.name)) {
      // TODO add noProxyHost check
      proxyHost = new HttpHost(jenkinsProxy.name, jenkinsProxy.port);
    } else {
      log.info(Messages.log_disabledProxy());
    }

    return builder.setProxy(proxyHost);
  }

  /**
   * Depending on what was configured in the Jenkins build config the builder may accept invalid SSL
   * certificates.
   */
  private static HttpClientBuilder addSslHandling(HttpClientBuilder builder, boolean ignoreSSL) {
    if (!ignoreSSL) {
      return builder;
    }

    try {
      SSLContext sslContext =
          new SSLContextBuilder().loadTrustMaterial(null, (x509Certificates, s) -> true).build();
      builder.setSSLContext(sslContext);

      HostnameVerifier verifier = NoopHostnameVerifier.INSTANCE;
      builder.setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext, verifier));
    } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
      log.log(Level.SEVERE, Messages.log_errorCertsProcessing(), e);
    }

    return builder;
  }
}
