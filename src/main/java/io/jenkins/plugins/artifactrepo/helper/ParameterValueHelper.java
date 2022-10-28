package io.jenkins.plugins.artifactrepo.helper;

import static io.jenkins.plugins.artifactrepo.helper.Constants.ParameterValue.DOT_PLACEHOLDER;
import static io.jenkins.plugins.artifactrepo.helper.Constants.ParameterValue.VALUE_PREFIX;

import hudson.model.ParameterValue;
import io.jenkins.plugins.artifactrepo.ArtifactRepoParameterValue;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.kohsuke.stapler.StaplerRequest;

/** A helper class to create proper parameter values that are then passed to the build script. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ParameterValueHelper {

  public static ParameterValue createValue(
      @Nonnull StaplerRequest request, @Nonnull JSONObject json) {
    String name = json.getString("name");
    Validate.notBlank(name, "No name of the parameter was provided");

    String value = hasCheckboxValues(json) ? checkboxValues(json) : nonCheckboxValues(json);

    return new ArtifactRepoParameterValue(name, value);
  }

  public static ParameterValue createValue(@Nonnull StaplerRequest request) {
    throw new UnsupportedOperationException("Currently not implemented");
  }

  private static boolean hasCheckboxValues(JSONObject json) {
    return json.names().stream()
        .filter(Objects::nonNull)
        .map(Object::toString)
        .anyMatch(ParameterValueHelper::startsWithCheckboxPrefix);
  }

  private static String checkboxValues(JSONObject json) {
    return json.names().stream()
        .filter(Objects::nonNull)
        .map(Object::toString)
        .filter(ParameterValueHelper::startsWithCheckboxPrefix)
        .map(ParameterValueHelper::removeCheckboxPrefix)
        .map(ParameterValueHelper::replaceDotPlaceholder)
        .collect(Collectors.joining("\n"));
  }

  private static String nonCheckboxValues(JSONObject json) {
    Object jsonValue = json.get("value");

    if (jsonValue instanceof String) {
      return (String) jsonValue;
    } else if (jsonValue instanceof JSONArray) {
      return ((JSONArray) jsonValue)
          .stream()
              .map(Object::toString)
              .map(ParameterValueHelper::replaceDotPlaceholder)
              .collect(Collectors.joining("\n"));
    }

    return null;
  }

  private static boolean startsWithCheckboxPrefix(String value) {
    return StringUtils.startsWith(value, VALUE_PREFIX);
  }

  private static String removeCheckboxPrefix(String value) {
    return StringUtils.remove(value, VALUE_PREFIX);
  }

  private static String replaceDotPlaceholder(String value) {
    return StringUtils.replace(value, DOT_PLACEHOLDER, ".");
  }
}
