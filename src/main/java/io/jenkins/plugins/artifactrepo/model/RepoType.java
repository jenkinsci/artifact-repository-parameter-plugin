package io.jenkins.plugins.artifactrepo.model;

import java.io.Serializable;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;

/** A bean that stores the selected options repo types of the artifact repositories. */
@Getter
public class RepoType implements Serializable {
  public static final long serialVersionUID = -7816034408993177660L;
  public static final RepoType DEFAULT = new RepoType(null);

  private final boolean local;
  private final boolean remote;
  private final boolean virtual;

  public RepoType(String[] values) {
    if (ArrayUtils.isEmpty(values)) {
      local = true;
      remote = false;
      virtual = false;
      return;
    }

    local = values.length < 1 || Boolean.parseBoolean(values[0]);
    remote = values.length >= 2 && Boolean.parseBoolean(values[1]);
    virtual = values.length >= 3 && Boolean.parseBoolean(values[2]);
  }

  public static String[] testValue() {
    return new String[] {"true", "true", "true"};
  }
}
