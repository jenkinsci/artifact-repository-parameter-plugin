package io.jenkins.plugins.artifactrepo.helper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {

  public static final String PLUGIN_NAME = "Artifact Repository Parameter";

  /** Defines the ID of the different parameter options. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class ParameterType {
    public static final String PATH = "path";
    public static final String VERSION = "version";
    public static final String REPOSITORY = "repository";
    public static final String TEST = "test";
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class ParameterValue {
    /**
     * Values of checkboxes are passed along in the name argument. To distinguish between checkbox
     * and other input types (radio, select, dropdown) we prefix the name value when it is coming
     * from a checkbox.
     */
    public static final String VALUE_PREFIX = "_____value_____";
    /**
     * With checkboxes, we have to use the name attribute for the actual value in order for it to
     * survive the form post. Unfortunately dots in the values (e.g. with URLs) are causing issues
     * (everything before the last dot is cut off) hence we have to replace the dots with a
     * placeholder and then transform the placeholder back to dots in the ParameterValue logic.
     */
    public static final String DOT_PLACEHOLDER = "_____DOT_____";
  }
}
