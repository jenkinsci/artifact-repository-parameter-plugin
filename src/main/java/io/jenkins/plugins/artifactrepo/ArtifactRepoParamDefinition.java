package io.jenkins.plugins.artifactrepo;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import io.jenkins.plugins.artifactrepo.connectors.Connector;
import io.jenkins.plugins.artifactrepo.helper.AlphanumComparator;
import io.jenkins.plugins.artifactrepo.helper.Constants.ParameterType;
import io.jenkins.plugins.artifactrepo.model.ArtifactRepoParamProxy;
import io.jenkins.plugins.artifactrepo.model.FormatType;
import io.jenkins.plugins.artifactrepo.model.RepoType;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.java.Log;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

@Log
@Getter
public class ArtifactRepoParamDefinition extends ParameterDefinition {

  // properties from config.jelly
  private final String uid;
  // connection options
  private final String serverType;
  private final String serverUrl;
  private final ArtifactRepoParamProxy proxy;
  private final String credentialsId;
  private final boolean ignoreCertificate;
  // api options
  private final String paramType;
  private final String artifactName;
  private final String repoName;
  private final String versionRegex;
  private final RepoType repoType;
  private final FormatType formatType;
  // display options
  private final String displayStyle;
  private final String resultsCount;
  private final String filterRegex;
  private final String sortOrder;
  private final boolean hideTextarea;

  /** Request data from the target instance to display as build parameter. */
  public Map<String, String> getResult() {
    return Connector.getInstance(this).getResults().entrySet().stream()
        .filter(distinctByValue(Entry::getValue))
        .filter(entry -> entry.getValue().matches(filterRegex))
        .sorted(getComparator())
        .limit(Integer.parseInt(resultsCount))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  /** Distinct result map by value */
  private static <T> Predicate<T> distinctByValue(Function<? super T, ?> func) {
    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(func.apply(t), Boolean.TRUE) == null;
  }

  /** Sort result map by value */
  private Comparator<Map.Entry<String, String>> getComparator() {
    Comparator<Entry<String, String>> comparator =
        Map.Entry.comparingByValue(new AlphanumComparator());

    if ("desc".equals(sortOrder)) {
      comparator = comparator.reversed();
    }
    return comparator;
  }

  /** Constructor is used during connenction validation test. */
  ArtifactRepoParamDefinition(
      String serverType,
      String serverUrl,
      String credentialsId,
      boolean ignoreCertificate,
      ArtifactRepoParamProxy proxy) {
    this(
        "Connection Validation",
        null,
        serverType,
        serverUrl,
        credentialsId,
        ignoreCertificate,
        proxy,
        ParameterType.TEST,
        null,
        null,
        null,
        RepoType.testValue(),
        FormatType.testValue(),
        null,
        null,
        null,
        null,
        false);
  }

  @DataBoundConstructor
  public ArtifactRepoParamDefinition(
      String name,
      String description,
      String serverType,
      String serverUrl,
      String credentialsId,
      boolean ignoreCertificate,
      ArtifactRepoParamProxy proxy,
      String paramType,
      String artifactName,
      String repoName,
      String versionRegex,
      String[] repoType,
      String[] formatType,
      String displayStyle,
      String resultsCount,
      String filterRegex,
      String sortOrder,
      boolean hideTextarea) {

    super(name, description);
    uid = StringUtils.substringAfterLast(UUID.randomUUID().toString(), "-");

    // connection options
    this.serverType = Optional.ofNullable(serverType).map(String::trim).orElse(null);
    this.serverUrl = Optional.ofNullable(serverUrl).map(String::trim).orElse(null);
    this.credentialsId = Optional.ofNullable(credentialsId).map(String::trim).orElse(null);
    this.ignoreCertificate = ignoreCertificate;
    this.proxy = Optional.ofNullable(proxy).orElse(ArtifactRepoParamProxy.DISABLED);
    // api options
    this.paramType = Optional.ofNullable(paramType).map(String::trim).orElse(null);
    this.artifactName = Optional.ofNullable(artifactName).map(String::trim).orElse(null);
    this.repoName = Optional.ofNullable(repoName).map(String::trim).orElse(null);
    this.versionRegex = Optional.ofNullable(versionRegex).map(String::trim).orElse(null);
    this.repoType = new RepoType(repoType);
    this.formatType = new FormatType(formatType);
    // display options
    this.displayStyle = Optional.ofNullable(displayStyle).map(String::trim).orElse(null);
    this.resultsCount =
        Optional.ofNullable(resultsCount)
            .filter(StringUtils::isNotBlank)
            .filter(s -> s.matches("\\d+"))
            .orElse("10");
    this.filterRegex = Optional.ofNullable(filterRegex).map(String::trim).orElse(null);
    this.sortOrder = Optional.ofNullable(sortOrder).map(String::trim).orElse(null);
    this.hideTextarea = hideTextarea;
  }

  // needed since reflection and Lombok getter generation do not work well together
  public String getProxyCredentialsId() {
    return proxy.getProxyCredentialsId();
  }

  @Override
  public ParameterValue createValue(StaplerRequest request, JSONObject json) {
    Object jsonValue = json.get("value");
    String name = Optional.ofNullable(json.getString("name")).orElse("MISSING");
    String value = EMPTY;

    if (jsonValue instanceof String) {
      value = (String) jsonValue;
    } else if (jsonValue instanceof JSONArray) {
      JSONArray jsonArray = (JSONArray) jsonValue;
      value = jsonArray.stream().map(Object::toString).collect(Collectors.joining("\\n"));
    }

    return new StringParameterValue(name, value, getDescription());
  }

  @Override
  public ParameterValue createValue(StaplerRequest request) {
    String[] values = request.getParameterValues(getName());
    if (ArrayUtils.isEmpty(values) || StringUtils.isBlank(values[0])) {
      return null;
    }
    String value = values[0];

    return new StringParameterValue(getName(), value, getDescription());
  }

  @Extension
  public static class DescriptorImpl extends ArtifactRepoParamDescriptor {
    // actual descriptor in base class
  }
}
