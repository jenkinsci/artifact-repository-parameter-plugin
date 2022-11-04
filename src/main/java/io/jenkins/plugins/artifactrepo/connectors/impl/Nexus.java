package io.jenkins.plugins.artifactrepo.connectors.impl;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import io.jenkins.plugins.artifactrepo.ArtifactRepoParamDefinition;
import io.jenkins.plugins.artifactrepo.Messages;
import io.jenkins.plugins.artifactrepo.connectors.Connector;
import io.jenkins.plugins.artifactrepo.helper.Constants.ParameterType;
import io.jenkins.plugins.artifactrepo.helper.PluginHelper;
import io.jenkins.plugins.artifactrepo.model.HttpResponse;
import io.jenkins.plugins.artifactrepo.model.ResultEntry;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.apache.commons.lang.Validate;
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
public class Nexus implements Connector {

    public static final String ID = "nexus";
    private final ArtifactRepoParamDefinition definition;
    private final HttpClientBuilder httpBuilder;
    private HttpClientContext preemptiveContext;

    public Nexus(@Nonnull ArtifactRepoParamDefinition definition) {
        this.definition = definition;
        httpBuilder = PluginHelper.getBuilder(
                definition.getCredentialsId(), definition.getProxy(), definition.isIgnoreCertificate());
    }

    @Override
    public List<ResultEntry> getResults() {
        switch (definition.getParamType()) {
            case ParameterType.PATH:
                return getArtifactResult();
            case ParameterType.VERSION:
                return getVersionResult();
            case ParameterType.REPOSITORY:
            case ParameterType.TEST:
                return getRepositoryResult();
            default:
                throw new IllegalArgumentException("Invalid parameter type: " + definition.getParamType());
        }
    }

    private List<ResultEntry> getArtifactResult() {
        HttpResponse response = getArtifactsResponse(null);
        if (response.getRc() == HttpStatus.SC_OK) {
            return parseArtifactsPayload(response.getPayload());
        }

        throw new IllegalArgumentException("HTTP response code is invalid: " + response.getRc());
    }

    private List<ResultEntry> getVersionResult() {
        List<ResultEntry> result = new ArrayList<>();

        Pattern versionPattern = Pattern.compile(definition.getVersionRegex());
        for (ResultEntry entry : getArtifactResult()) {
            Optional<String> version = extractVersion(entry.getValue(), versionPattern);
            version.ifPresent(v -> result.add(new ResultEntry(v, entry.getValue())));
        }

        return result;
    }

    private List<ResultEntry> getRepositoryResult() {
        List<ResultEntry> result = new ArrayList<>();

        HttpResponse response = getRepositoriesResponse();

        Validate.isTrue(response.getRc() == HttpStatus.SC_OK, Messages.log_failedRequest(response.getRc()));

        JSONArray root = new JSONArray(response.getPayload());
        for (int i = 0; i < root.length(); i++) {
            JSONObject repo = root.getJSONObject(i);
            if (!isValidRepoType(repo.getString("type")) || !isValidFormatType(repo.getString("format"))) {
                continue;
            }

            String key = repo.getString("name");
            String value = repo.getString("url");
            if (StringUtils.isNoneBlank(value, key)) {
                result.add(new ResultEntry(key, value));
            }
        }

        return result;
    }

    private HttpResponse getArtifactsResponse(String continuationToken) {
        String url = definition.getServerUrl() + "/service/rest/v1/search?name=" + definition.getArtifactName();
        if (StringUtils.isNotBlank(definition.getRepoName())) {
            url = url + "&repository=" + definition.getRepoName();
        }
        if (StringUtils.isNotBlank(continuationToken)) {
            url = url + "&continuationToken=" + continuationToken;
        }

        return PluginHelper.get(url, httpBuilder, getPreemptiveAuthContext());
    }

    private HttpResponse getRepositoriesResponse() {
        return PluginHelper.get(
                definition.getServerUrl() + "/service/rest/v1/repositories", httpBuilder, getPreemptiveAuthContext());
    }

    private List<ResultEntry> parseArtifactsPayload(@Nonnull String jsonPayload) {
        List<ResultEntry> result = new ArrayList<>();

        JSONObject root = new JSONObject(jsonPayload);

        // first check for continuation token and request further data if required
        Object tokenObj = root.get("continuationToken");
        if (tokenObj instanceof String) {
            String token = (String) tokenObj;
            if (StringUtils.isNotBlank(token)) {
                HttpResponse response = getArtifactsResponse(token);
                Validate.isTrue(response.getRc() == HttpStatus.SC_OK, Messages.log_failedRequest(response.getRc()));

                result.addAll(parseArtifactsPayload(response.getPayload()));
            }
        }

        // next process the current json
        JSONArray items = root.getJSONArray("items");
        for (int i = 0; i < items.length(); i++) {
            JSONObject artifact = items.getJSONObject(i);
            JSONArray assets = artifact.getJSONArray("assets");
            for (int j = 0; j < assets.length(); j++) {
                JSONObject asset = assets.getJSONObject(j);
                String value = asset.getString("downloadUrl");
                String key = StringUtils.substringAfterLast(value, "/");

                if (StringUtils.isNoneBlank(value, key) && !StringUtils.endsWithAny(key, "md5", "sha1")) {
                    result.add(new ResultEntry(key, value));
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

        StandardUsernamePasswordCredentials jenkinsCreds = PluginHelper.getCredentials(definition.getCredentialsId());

        Optional.of(jenkinsCreds).ifPresent(creds -> {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(
                            creds.getUsername(), creds.getPassword().getPlainText()));

            AuthCache authCache = new BasicAuthCache();
            try {
                authCache.put(PluginHelper.getHttpHostFromUrl(definition.getServerUrl()), new BasicScheme());
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(Messages.log_invalidUrl(definition.getServerUrl()), e);
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
