package io.jenkins.plugins.artifactrepo.connectors;

import io.jenkins.plugins.artifactrepo.ArtifactRepoParamDefinition;
import io.jenkins.plugins.artifactrepo.connectors.impl.Artifactory;
import io.jenkins.plugins.artifactrepo.connectors.impl.Dummy;
import io.jenkins.plugins.artifactrepo.connectors.impl.Nexus;
import java.util.Map;
import javax.annotation.Nonnull;

/** A connector interface for each artifact repository connector to implement. */
public interface Connector {

  /** Return a connector based on the server type configured in the given build definition. */
  static Connector getInstance(@Nonnull ArtifactRepoParamDefinition definition) {
    switch (definition.getServerType()) {
      case Nexus.ID:
        return new Nexus(definition);
      case Artifactory.ID:
        return new Artifactory(definition);
      default:
        return new Dummy();
    }
  }

  /**
   * Get the results from the repository instance defined in the build parameters. The results take
   * the API options into consideration but not any of the display options. No modifications are
   * done and the results are returned as provided by the target server.
   */
  Map<String, String> getResults();
}
