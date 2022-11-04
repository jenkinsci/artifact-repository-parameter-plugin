package io.jenkins.plugins.artifactrepo;

import static java.util.function.Predicate.not;

import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import io.jenkins.plugins.artifactrepo.connectors.Connector;
import io.jenkins.plugins.artifactrepo.helper.AlphanumComparator;
import io.jenkins.plugins.artifactrepo.helper.Constants.ParameterType;
import io.jenkins.plugins.artifactrepo.model.ArtifactRepoParamProxy;
import io.jenkins.plugins.artifactrepo.model.FormatType;
import io.jenkins.plugins.artifactrepo.model.RepoType;
import io.jenkins.plugins.artifactrepo.model.ResultEntry;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.extern.java.Log;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

@Log
@Getter
public class ArtifactRepoParamDefinition extends ParameterDefinition {
  private static final long serialVersionUID = 1873814008801492085L;
  private static final AlphanumComparator comparator = new AlphanumComparator();
  // properties from config.jelly
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
  private final boolean multiSelection;
  private final int resultsCount;
  private final String filterRegex;
  private final String sortOrder;
  private final String selectEntry;
  private final String selectRegex;
  private final String selectRegexStyle;
  private final String submitValue;
  private boolean exceptionThrown = false;
  private transient Map<String, ResultEntry> result;

  /** Constructor is used during connection validation test. */
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
        false,
        null,
        null,
        null,
        "none",
        "",
        "both");
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
      boolean multiSelection,
      String resultsCount,
      String filterRegex,
      String sortOrder,
      String selectEntry,
      String selectRegex,
      String submitValue) {

    super(name);
    setDescription(description);

    // connection options
    this.serverType = Optional.ofNullable(serverType).map(String::trim).orElse("");
    this.serverUrl = Optional.ofNullable(serverUrl).map(String::trim).orElse("");
    this.credentialsId = Optional.ofNullable(credentialsId).map(String::trim).orElse("");
    this.ignoreCertificate = ignoreCertificate;
    this.proxy = Optional.ofNullable(proxy).orElse(ArtifactRepoParamProxy.DISABLED);
    // api options
    this.paramType = Optional.ofNullable(paramType).map(String::trim).orElse("");
    this.artifactName = Optional.ofNullable(artifactName).map(String::trim).orElse("");
    this.repoName = Optional.ofNullable(repoName).map(String::trim).orElse("");
    this.versionRegex = Optional.ofNullable(versionRegex).map(String::trim).orElse("");
    this.repoType = new RepoType(repoType);
    this.formatType = new FormatType(formatType);
    // display options
    this.multiSelection = multiSelection;
    this.resultsCount =
        Optional.ofNullable(resultsCount)
            .filter(s -> s.matches("\\d+"))
            .map(Integer::valueOf)
            .orElse(10);
    this.filterRegex =
        Optional.ofNullable(filterRegex)
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .orElse(".+");
    this.sortOrder = Optional.ofNullable(sortOrder).map(String::trim).orElse("");
    this.selectEntry = Optional.ofNullable(selectEntry).map(String::trim).orElse("none");
    this.selectRegexStyle = "regex".equals(this.selectEntry) ? "block" : "none";
    this.selectRegex = Optional.ofNullable(selectRegex).map(String::trim).orElse("");
    this.submitValue = Optional.ofNullable(submitValue).map(String::trim).orElse("both");
  }

  /** Request data from the target instance to display as build parameter. */
  public Map<String, ResultEntry> getResult() {
    if (MapUtils.isNotEmpty(result)) {
      return result;
    }

    exceptionThrown = false;
    List<ResultEntry> repoEntries;
    try {
      repoEntries = Connector.getInstance(this).getResults();
    } catch (Exception e) {
      exceptionThrown = true;
      log.log(Level.SEVERE, "An exception occurred while trying to get a result set", e);
      return new HashMap<>();
    }

    Map<String, ResultEntry> resultEntries = new LinkedHashMap<>();
    repoEntries.stream()
        .map(this::checkSubmitValue)
        .filter(this::filterRegex)
        .sorted(this::sortResult)
        .limit(resultsCount)
        .forEach(entry -> resultEntries.put(entry.getKey(), entry));
    result = markPreselectedEntries(resultEntries);
    return result;
  }

  /**
   * Checks if only the label value should get send to the build pipeline and if so will change the
   * value of the given entry to the same value as the key.
   */
  private ResultEntry checkSubmitValue(ResultEntry entry) {
    if ("label".equals(submitValue)) {
      entry.setSubmitValue(entry.getKey());
    } else if ("path".equals(submitValue)) {
      entry.setSubmitValue(entry.getValue());
    }

    return entry;
  }

  /**
   * Filter search results by a given regex. It will check both key and value for a match with the
   * regex and if neither matches the regex it will get removed.
   */
  private boolean filterRegex(@Nonnull ResultEntry entry) {
    if (StringUtils.isBlank(filterRegex)) {
      return true;
    }

    return entry.getKey().matches(filterRegex) || entry.getValue().matches(filterRegex);
  }

  /**
   * Sort the result list by key value. Depending on config it will be sorted ascending or
   * descending.
   */
  private int sortResult(@Nonnull ResultEntry entry1, @Nonnull ResultEntry entry2) {
    if ("desc".equals(sortOrder)) {
      return comparator.compare(entry2.getKey(), entry1.getKey());
    } else {
      return comparator.compare(entry1.getKey(), entry2.getKey());
    }
  }

  /**
   * Allows to mark some result entries to be preselected upon opening the start build view. Due to
   * the possibility to mark first or last entry to be selected it cannot be part of the stream and
   * must be done afterwards.
   */
  private Map<String, ResultEntry> markPreselectedEntries(Map<String, ResultEntry> entries) {
    if (entries.isEmpty()) {
      return entries;
    }

    switch (selectEntry) {
      case "first":
        entries.entrySet().stream().findFirst().ifPresent(e -> e.getValue().setSelected(true));
        break;
      case "last":
        entries.entrySet().stream()
            .reduce((first, second) -> second)
            .ifPresent(e -> e.getValue().setSelected(true));
        break;
      case "regex":
        AtomicBoolean stopAction = new AtomicBoolean(false);
        entries.entrySet().stream()
            .filter(not(e -> stopAction.get()))
            .filter(this::selectedRegex)
            .forEach(
                entry -> {
                  if (!multiSelection) {
                    stopAction.set(true);
                  }
                  entry.getValue().setSelected(true);
                });
        break;
      default:
    }

    return entries;
  }

  /**
   * If the selection option is set to regex it will find entries matching the given regex (both key
   * and value are checked) and any entry that matches the regex will be marked to be pre-selected.
   */
  private boolean selectedRegex(@Nonnull Map.Entry<String, ResultEntry> entry) {
    if (StringUtils.isBlank(selectRegex)) {
      return false;
    }

    return entry.getValue().getKey().matches(selectRegex)
        || entry.getValue().getValue().matches(selectRegex);
  }

  // needed since reflection and Lombok getter generation do not seem to work well together
  public String getProxyCredentialsId() {
    return proxy.getProxyCredentialsId();
  }

  @Override
  public ParameterValue createValue(StaplerRequest request, JSONObject json) {
    String name = json.getString("name");
    Validate.notBlank(name, "No name of the parameter was provided");

    String value = getValuesFromJson(json);

    return new ArtifactRepoParameterValue(name, value);
  }

  @Override
  public ParameterValue createValue(StaplerRequest request) {
    throw new UnsupportedOperationException("Currently not implemented");
  }

  private String getValuesFromJson(JSONObject json) {
    Object jsonValue = json.get("value");

    if (jsonValue instanceof String) {
      return (String) jsonValue;
    } else if (jsonValue instanceof JSONArray) {
      return ((JSONArray) jsonValue)
          .stream().map(Object::toString).collect(Collectors.joining("\n"));
    }

    return null;
  }

  @Extension
  public static class DescriptorImpl extends ArtifactRepoParamDescriptor {
    // check ArtifactRepoParamDescriptor for actual descriptor
  }
}
