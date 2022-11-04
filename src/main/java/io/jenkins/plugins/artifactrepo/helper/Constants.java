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
}
