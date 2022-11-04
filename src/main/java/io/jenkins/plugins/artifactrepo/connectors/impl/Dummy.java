package io.jenkins.plugins.artifactrepo.connectors.impl;

import io.jenkins.plugins.artifactrepo.connectors.Connector;
import io.jenkins.plugins.artifactrepo.model.ResultEntry;
import java.util.List;

/**
 * A dummy implementation used as a fallback in case a regular connector cannot be obtained. Returns
 * an empty result map to prevent NPE.
 */
public class Dummy implements Connector {

  @Override
  public List<ResultEntry> getResults() {
    return List.of();
  }
}
