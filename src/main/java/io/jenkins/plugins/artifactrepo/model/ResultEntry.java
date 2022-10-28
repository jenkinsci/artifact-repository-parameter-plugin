package io.jenkins.plugins.artifactrepo.model;

import static io.jenkins.plugins.artifactrepo.helper.Constants.ParameterValue.DOT_PLACEHOLDER;

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
    // Check the description of Constants.ParameterValue.DOT_PLACEHOLDER for more info on this
    this.key = key.replace(".", DOT_PLACEHOLDER);
    this.value = value;
  }
}
