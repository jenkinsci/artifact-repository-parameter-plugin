package io.jenkins.plugins.artifactrepo;

import hudson.model.StringParameterValue;
import org.kohsuke.stapler.DataBoundConstructor;

public class ArtifactRepoParameterValue extends StringParameterValue {

  @DataBoundConstructor
  public ArtifactRepoParameterValue(String name, String value) {
    super(name, value);
  }
}
