package io.jenkins.plugins.artifactrepo.connectors.impl;

import io.jenkins.plugins.artifactrepo.ArtifactRepoParamDefinition;
import io.jenkins.plugins.artifactrepo.Messages;
import io.jenkins.plugins.artifactrepo.connectors.Connector;
import io.jenkins.plugins.artifactrepo.helper.Constants.ParameterType;
import io.jenkins.plugins.artifactrepo.helper.PluginHelper;
import io.jenkins.plugins.artifactrepo.model.HttpResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.http.HttpStatus;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

/** A connector that provides access to the supported REST endpoints of JFrog Artifactory. */
public class Artifactory implements Connector {

  public static final String ID = "artifactory";
  private final ArtifactRepoParamDefinition definition;
  private final HttpClientBuilder httpBuilder;

  public Artifactory(@Nonnull ArtifactRepoParamDefinition definition) {
    this.definition = definition;
    httpBuilder =
        PluginHelper.getBuilder(
            definition.getCredentialsId(), definition.getProxy(), definition.isIgnoreCertificate());
  }

  @Override
  public Map<String, String> getResults() {
    switch (definition.getParamType()) {
      case ParameterType.VERSION:
        return getVersionResult();
      case ParameterType.PATH:
        return getArtifactResult();
      case ParameterType.REPOSITORY:
      case ParameterType.TEST:
        return getRepositoryResult();
      default:
        throw new IllegalArgumentException(
            Messages.log_invalidParameter(definition.getParamType()));
    }
  }

  private Map<String, String> getArtifactResult() {
    Map<String, String> result = new HashMap<>();

    HttpResponse response = getArtifactsResponse();
    Validate.isTrue(
        response.getRc() == HttpStatus.SC_OK, Messages.log_failedRequest(response.getRc()));

    JSONObject root = new JSONObject(response.getPayload());
    JSONArray array = root.getJSONArray("results");
    for (int i = 0; i < array.length(); i++) {
      String key = array.getJSONObject(i).getString("uri");
      String value = StringUtils.substringAfterLast(key, "/");

      if (StringUtils.isNoneBlank(key, value)) {
        result.put(StringUtils.remove(key, "api/storage/"), value);
      }
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

  private HttpResponse getArtifactsResponse() {
    String url =
        definition.getServerUrl() + "/api/search/artifact?name=" + definition.getArtifactName();
    if (StringUtils.isNotBlank(definition.getRepoName())) {
      url = url + "&repos=" + definition.getRepoName();
    }

    return PluginHelper.get(url, httpBuilder);
  }

  private Optional<String> extractVersion(@Nonnull String path, @Nonnull Pattern pattern) {
    Matcher versionMatcher = pattern.matcher(path);
    if (versionMatcher.matches() && versionMatcher.groupCount() >= 1) {
      return Optional.of(versionMatcher.group(1));
    }
    return Optional.empty();
  }

  private Map<String, String> getRepositoryResult() {
    Map<String, String> result = new HashMap<>();

    HttpResponse response = getRepositoriesResponse();
    Validate.isTrue(
        response.getRc() == HttpStatus.SC_OK, Messages.log_failedRequest(response.getRc()));

    JSONArray root = new JSONArray(response.getPayload());
    for (int i = 0; i < root.length(); i++) {
      JSONObject repo = root.getJSONObject(i);

      if (!isValidRepoType(repo.getString("type"))
          || !isValidFormatType(repo.getString("packageType"))) {
        continue;
      }

      String key = repo.getString("url");
      String value = repo.getString("key");
      if (StringUtils.isNoneBlank(key, value)) {
        result.put(key, value);
      }
    }

    return result;
  }

  private HttpResponse getRepositoriesResponse() {
    return PluginHelper.get(definition.getServerUrl() + "/api/repositories", httpBuilder);
  }

  private boolean isValidRepoType(@Nonnull String value) {
    if (definition.getRepoType().isLocal() && "LOCAL".equals(value)) {
      return true;
    } else if (definition.getRepoType().isRemote() && "REMOTE".equals(value)) {
      return true;
    } else if (definition.getRepoType().isVirtual() && "VIRTUAL".equals(value)) {
      return true;
    } else {
      return false;
    }
  }

  private boolean isValidFormatType(@Nonnull String value) {
    if (definition.getFormatType().isMaven() && "Maven".equals(value)) {
      return true;
    } else if (definition.getFormatType().isNpm()
        && Arrays.asList("Npm", "Bower").contains(value)) {
      return true;
    } else if (definition.getFormatType().isPypi() && "Pypi".equals(value)) {
      return true;
    } else if (definition.getFormatType().isDocker() && "Docker".equals(value)) {
      return true;
    } else if (definition.getFormatType().isOther()
        && Stream.of("Maven", "Npm", "Bower", "Pypi", "Docker").noneMatch(s -> s.equals(value))) {
      return true;
    } else {
      return false;
    }
  }
}
