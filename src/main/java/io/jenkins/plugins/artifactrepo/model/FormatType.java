package io.jenkins.plugins.artifactrepo.model;

import java.io.Serializable;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;

/** A bean that stores the selected options for format types of artifact repositories. */
@Getter
public class FormatType implements Serializable {
  public static final long serialVersionUID = -6777952034460881203L;
  public static final FormatType DEFAULT = new FormatType(null);

  private final boolean maven;
  private final boolean npm;
  private final boolean pypi;
  private final boolean docker;
  private final boolean other;

  public FormatType(String[] values) {
    if (ArrayUtils.isEmpty(values)) {
      maven = true;
      npm = true;
      pypi = true;
      docker = true;
      other = true;
      return;
    }

    maven = values.length < 1 || Boolean.parseBoolean(values[0]);
    npm = values.length >= 2 && Boolean.parseBoolean(values[1]);
    pypi = values.length >= 3 && Boolean.parseBoolean(values[2]);
    docker = values.length >= 4 && Boolean.parseBoolean(values[3]);
    other = values.length >= 5 && Boolean.parseBoolean(values[4]);
  }

  public static String[] testValue() {
    return new String[] {"true", "true", "true", "true", "true"};
  }
}
