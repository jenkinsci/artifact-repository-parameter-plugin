package io.jenkins.plugins.artifactrepo;

import static io.jenkins.plugins.artifactrepo.helper.Constants.ParameterValue.VALUE_PREFIX;
import static java.util.function.Predicate.not;

import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import io.jenkins.plugins.artifactrepo.connectors.Connector;
import io.jenkins.plugins.artifactrepo.helper.AlphanumComparator;
import io.jenkins.plugins.artifactrepo.helper.Constants.ParameterType;
import io.jenkins.plugins.artifactrepo.helper.ParameterValueHelper;
import io.jenkins.plugins.artifactrepo.model.ArtifactRepoParamProxy;
import io.jenkins.plugins.artifactrepo.model.FormatType;
import io.jenkins.plugins.artifactrepo.model.RepoType;
import io.jenkins.plugins.artifactrepo.model.ResultEntry;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.java.Log;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

@Log
@Getter
public class ArtifactRepoParamDefinition extends ParameterDefinition {

  // properties from config.jelly
  private final String checkboxPrefix = VALUE_PREFIX;
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
  private final int resultsCount;
  private final String filterRegex;
  private final String sortOrder;
  private final String selectEntry;
  private final String selectRegex;

  private boolean exceptionThrown = false;

  /** Request data from the target instance to display as build parameter. */
  public TreeMap<String, ResultEntry> getResult() {
    exceptionThrown = false;
    Map<String, String> entries = new HashMap<>();
    try {
      entries = Connector.getInstance(this).getResults();
    } catch (Exception e) {
      exceptionThrown = true;
      log.log(Level.SEVERE, "An exception occurred while trying to get a result set", e);
    }

    TreeMap<String, ResultEntry> result =
        entries.entrySet().stream()
            .filter(distinctByValue(Entry::getValue))
            .filter(entry -> entry.getValue().matches(filterRegex))
            .sorted(getComparator())
            .limit(resultsCount)
            .map(entry -> new ResultEntry(entry.getKey(), entry.getValue()))
            .collect(
                Collectors.toMap(
                    ResultEntry::getKey, Function.identity(), (o, n) -> n, TreeMap::new));

    return markPreselectedEntries(result);
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

  /** Check if some entries must get pre-selected. */
  private TreeMap<String, ResultEntry> markPreselectedEntries(
      TreeMap<String, ResultEntry> entries) {
    if (entries.isEmpty()) {
      return entries;
    }

    switch (selectEntry) {
      case "first":
        entries.firstEntry().getValue().setSelected(true);
        break;
      case "last":
        entries.lastEntry().getValue().setSelected(true);
        break;
      case "regex":
        AtomicBoolean stopAction = new AtomicBoolean(false);
        entries.values().stream()
            .filter(not(e -> stopAction.get()))
            .filter(e -> e.getKey().matches(selectRegex))
            .forEach(
                entry -> {
                  if (List.of("dropdown", "radio").contains(displayStyle)) {
                    stopAction.set(true);
                  }
                  entry.setSelected(true);
                });
        break;
      default:
    }

    return entries;
  }

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
        null,
        null,
        null,
        null,
        "none",
        "");
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
      String selectEntry,
      String selectRegex) {

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
    this.displayStyle = Optional.ofNullable(displayStyle).map(String::trim).orElse("");
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
    this.selectRegex = Optional.ofNullable(selectRegex).map(String::trim).orElse("");
  }

  // needed since reflection and Lombok getter generation do not work well together
  public String getProxyCredentialsId() {
    return proxy.getProxyCredentialsId();
  }

  @Override
  public ParameterValue createValue(StaplerRequest request, JSONObject json) {
    return ParameterValueHelper.createValue(request, json);
  }

  @Override
  public ParameterValue createValue(StaplerRequest request) {
    return ParameterValueHelper.createValue(request);
  }

  @Extension
  public static class DescriptorImpl extends ArtifactRepoParamDescriptor {
    // check ArtifactRepoParamDescriptor for actual descriptor
  }
}
