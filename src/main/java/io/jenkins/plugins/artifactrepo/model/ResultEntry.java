package io.jenkins.plugins.artifactrepo.model;

import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;

/** An object representing a result entry displayed on the index.jelly page. */
@AllArgsConstructor
@Data
public class ResultEntry {
  private final String key;
  private final String value;
  private boolean selected = false;

  public ResultEntry(@Nonnull String key, @Nonnull String value) {
    this.key = key;
    this.value = value;
  }
}
