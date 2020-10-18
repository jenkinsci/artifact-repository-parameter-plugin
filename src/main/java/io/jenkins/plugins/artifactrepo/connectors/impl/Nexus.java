package io.jenkins.plugins.artifactrepo.connectors.impl;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import io.jenkins.plugins.artifactrepo.ArtifactRepoParamDefinition;
import io.jenkins.plugins.artifactrepo.Messages;
import io.jenkins.plugins.artifactrepo.connectors.Connector;
import io.jenkins.plugins.artifactrepo.helper.Constants.ParameterType;
import io.jenkins.plugins.artifactrepo.helper.HttpHelper;
import io.jenkins.plugins.artifactrepo.model.HttpResponse;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

/** A class that provides access to the supported REST endpoints of Sonatype Nexus. */
@Log
public class Nexus implements Connector {

  public static final String ID = "nexus";
  private final ArtifactRepoParamDefinition definition;
  private final HttpClientBuilder httpBuilder;
  private HttpClientContext preemptiveContext;

  public Nexus(@Nonnull ArtifactRepoParamDefinition definition) {
    this.definition = definition;
    httpBuilder =
        HttpHelper.getBuilder(
            definition.getCredentialsId(), definition.getProxy(), definition.isIgnoreCertificate());
  }

  @Override
  public Map<String, String> getResults() {
    switch (definition.getParamType()) {
      case ParameterType.PATH:
        return getArtifactResult();
      case ParameterType.VERSION:
        return getVersionResult();
      case ParameterType.REPOSITORY:
      case ParameterType.TEST:
        return getRepositoryResult();
    }

    log.info(Messages.log_invalidParameter(definition.getParamType()));
    return new HashMap<>();
  }

  private Map<String, String> getArtifactResult() {
    Map<String, String> result = new HashMap<>();

    HttpResponse response = getArtifactsResponse(null);
    if (response.getRc() == HttpStatus.SC_OK) {
      result.putAll(parseArtifactsPayload(response.getPayload()));
    } else {
      log.warning(Messages.log_failedRequest(response.getRc()));
    }

    return result;
  }

  private Map<String, String> getVersionResult() {
    Map<String, String> result = new HashMap<>();

    Pattern versionPattern = Pattern.compile(definition.getVersionRegex());
    Map<String, String> artifacts = getArtifactResult();
    for (Entry<String, String> entry : artifacts.entrySet()) {
      Optional<String> version = extractVersion(entry.getKey(), versionPattern);
      version.ifPresent(v -> result.put(entry.getKey(), v));
    }

    return result;
  }

  private Map<String, String> getRepositoryResult() {
    Map<String, String> result = new HashMap<>();

    HttpResponse response = getRepositoriesResponse();
    if (response.getRc() != HttpStatus.SC_OK) {
      log.warning(Messages.log_failedRequest(response.getRc()));
      return result;
    }

    JSONArray root = new JSONArray(response.getPayload());
    for (int i = 0; i < root.length(); i++) {
      JSONObject repo = root.getJSONObject(i);
      if (!isValidRepoType(repo.getString("type"))
          || !isValidFormatType(repo.getString("format"))) {
        continue;
      }

      String key = repo.getString("url");
      String value = repo.getString("name");
      if (StringUtils.isNoneBlank(key, value)) {
        result.put(key, value);
      }
    }

    return result;
  }

  private HttpResponse getArtifactsResponse(String continuationToken) {
    String url =
        definition.getServerUrl() + "/service/rest/v1/search?name=" + definition.getArtifactName();
    if (StringUtils.isNotBlank(definition.getRepoName())) {
      url = url + "&repository=" + definition.getRepoName();
    }
    if (StringUtils.isNotBlank(continuationToken)) {
      url = url + "&continuationToken=" + continuationToken;
    }

    return HttpHelper.get(url, httpBuilder, getPreemptiveAuthContext());
  }

  private HttpResponse getRepositoriesResponse() {
    return HttpHelper.get(
        definition.getServerUrl() + "/service/rest/v1/repositories",
        httpBuilder,
        getPreemptiveAuthContext());
  }

  private Map<String, String> parseArtifactsPayload(@Nonnull String jsonPayload) {
    Map<String, String> result = new HashMap<>();

    JSONObject root = new JSONObject(jsonPayload);

    // first check for continuation token and request further data if required
    Object tokenObj = root.get("continuationToken");
    if (tokenObj instanceof String) {
      String token = (String) tokenObj;
      if (StringUtils.isNotBlank(token)) {
        HttpResponse response = getArtifactsResponse(token);
        if (response.getRc() == HttpStatus.SC_OK) {
          result.putAll(parseArtifactsPayload(response.getPayload()));
        } else {
          log.warning(Messages.log_failedRequest(response.getRc()));
        }
      }
    }

    // next process the current json
    JSONArray items = root.getJSONArray("items");
    for (int i = 0; i < items.length(); i++) {
      JSONObject artifact = items.getJSONObject(i);
      JSONArray assets = artifact.getJSONArray("assets");
      for (int j = 0; j < assets.length(); j++) {
        JSONObject asset = assets.getJSONObject(j);
        String key = asset.getString("downloadUrl");
        String value = StringUtils.substringAfterLast(key, "/");

        if (StringUtils.isNoneBlank(key, value) && !StringUtils.endsWithAny(value, "md5", "sha1")) {
          result.put(key, value);
        }
      }
    }

    return result;
  }

  private Optional<String> extractVersion(@Nonnull String path, @Nonnull Pattern pattern) {
    Matcher versionMatcher = pattern.matcher(path);
    if (versionMatcher.matches() && versionMatcher.groupCount() >= 1) {
      return Optional.of(versionMatcher.group(1));
    }
    return Optional.empty();
  }

  /** Preemptive authentication is required to access Nexus. */
  private HttpClientContext getPreemptiveAuthContext() {
    if (preemptiveContext != null) {
      return preemptiveContext;
    }

    HttpClientContext context = HttpClientContext.create();

    Optional<StandardUsernamePasswordCredentials> jenkinsCreds =
        HttpHelper.getCredentials(definition.getCredentialsId());
    jenkinsCreds.ifPresent(
        creds -> {
          CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
          credentialsProvider.setCredentials(
              AuthScope.ANY,
              new UsernamePasswordCredentials(
                  creds.getUsername(), creds.getPassword().getPlainText()));

          AuthCache authCache = new BasicAuthCache();
          try {
            authCache.put(
                HttpHelper.getHttpHostFromUrl(definition.getServerUrl()), new BasicScheme());
          } catch (MalformedURLException e) {
            log.log(Level.SEVERE, Messages.log_invalidUrl(definition.getServerUrl()), e);
          }

          context.setCredentialsProvider(credentialsProvider);
          context.setAuthCache(authCache);
        });

    preemptiveContext = context;
    return context;
  }

  private boolean isValidRepoType(@Nonnull String value) {
    if (definition.getRepoType().isLocal() && "hosted".equals(value)) {
      return true;
    } else if (definition.getRepoType().isRemote() && "proxy".equals(value)) {
      return true;
    } else return definition.getRepoType().isVirtual() && "group".equals(value);
  }

  private boolean isValidFormatType(@Nonnull String value) {
    if (definition.getFormatType().isMaven() && "maven2".equals(value)) {
      return true;
    } else if (definition.getFormatType().isNpm()
        && Arrays.asList("npm", "bower").contains(value)) {
      return true;
    } else if (definition.getFormatType().isPypi() && "pypi".equals(value)) {
      return true;
    } else if (definition.getFormatType().isDocker() && "docker".equals(value)) {
      return true;
    } else
      return definition.getFormatType().isOther()
          && Stream.of("maven2", "npm", "bower", "pypi", "docker").noneMatch(s -> s.equals(value));
  }
}
