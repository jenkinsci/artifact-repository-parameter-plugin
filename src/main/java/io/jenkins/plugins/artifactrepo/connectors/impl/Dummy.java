package io.jenkins.plugins.artifactrepo.connectors.impl;

import io.jenkins.plugins.artifactrepo.connectors.Connector;
import java.util.HashMap;
import java.util.Map;

/**
 * A dummy implementation used as a fallback in case a regular connector cannot be obtained. Returns
 * an empty result map to prevent NPE.
 */
public class Dummy implements Connector {

  @Override
  public Map<String, String> getResults() {
    return new HashMap<>();
  }
}
