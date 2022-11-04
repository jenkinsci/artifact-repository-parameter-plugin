package io.jenkins.plugins.artifactrepo;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import hudson.model.Item;
import hudson.model.ParameterDefinition.ParameterDescriptor;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.artifactrepo.connectors.Connector;
import io.jenkins.plugins.artifactrepo.connectors.impl.Artifactory;
import io.jenkins.plugins.artifactrepo.helper.Constants;
import io.jenkins.plugins.artifactrepo.model.ArtifactRepoParamProxy;
import io.jenkins.plugins.artifactrepo.model.ResultEntry;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class ArtifactRepoParamDescriptor extends ParameterDescriptor {
  private static final int MAX_RESULT = 50;

  @Override
  @Nonnull
  public String getDisplayName() {
    return Constants.PLUGIN_NAME;
  }

  // form validation

  public FormValidation doCheckName(@QueryParameter String value) {
    if (StringUtils.isBlank(value)) {
      return FormValidation.error(Messages.formError_missingName());
    }
    return FormValidation.ok();
  }

  public FormValidation doCheckServerUrl(@QueryParameter String value) {
    if (StringUtils.isBlank(value)) {
      return FormValidation.error(Messages.formError_missingServerUrl());
    }
    try {
      new URI(value).toURL();
    } catch (Exception e) {
      return FormValidation.error(Messages.formError_invalidServerUrl());
    }
    return FormValidation.ok();
  }

  public FormValidation doCheckCredentialsId(@QueryParameter String value) {
    if (StringUtils.isBlank(value)) {
      return FormValidation.error(Messages.formError_missingCreds());
    }
    return FormValidation.ok();
  }

  public FormValidation doCheckIgnoreCertificate(@QueryParameter String value) {
    if ("true".equalsIgnoreCase(value)) {
      return FormValidation.warning(Messages.formError_invalidCertIgnored());
    }
    return FormValidation.ok();
  }

  public FormValidation doCheckProxyPort(
      @QueryParameter String value, @QueryParameter String proxyHost) {
    if (StringUtils.isBlank(proxyHost)) {
      return FormValidation.ok();
    }
    if (StringUtils.isBlank(value)) {
      return FormValidation.error(Messages.formError_missingProxyPort());
    }
    if (!value.matches("\\d+")) {
      return FormValidation.error(Messages.formError_invalidProxyPort());
    }
    int port = Integer.parseInt(value);
    if (port < 1 || port > 65535) {
      return FormValidation.error(Messages.formError_invalidProxyRange());
    }
    return FormValidation.ok();
  }

  public FormValidation doCheckParamType(@QueryParameter String value) {
    if (StringUtils.isBlank(value)) {
      return FormValidation.error(Messages.formError_missingParamType());
    }
    return FormValidation.ok();
  }

  public FormValidation doCheckArtifactName(
      @QueryParameter String value, @QueryParameter String serverType) {
    if (StringUtils.isBlank(value)) {
      return FormValidation.error(Messages.formError_missingArtifactName());
    }
    if ("*".equals(value) && Artifactory.ID.equals(serverType)) {
      return FormValidation.error(Messages.formError_invalidArtifactName());
    }
    return FormValidation.ok();
  }

  public FormValidation doCheckVersionRegex(@QueryParameter String value) {
    if (StringUtils.isBlank(value)) {
      return FormValidation.error(Messages.formError_missingVersionRegex());
    }
    return FormValidation.ok();
  }

  public FormValidation doCheckResultsCount(@QueryParameter String value) {
    if (StringUtils.isBlank(value)) {
      return FormValidation.error(Messages.formError_missingResultsCount());
    }
    if (!value.matches("-?\\d+")) {
      return FormValidation.error(Messages.formError_invalidResultsCount());
    }
    int count = Integer.parseInt(value);
    if (count < 1 || count > MAX_RESULT) {
      return FormValidation.error(Messages.formError_invalidResultsCountRange(MAX_RESULT));
    }
    return FormValidation.ok();
  }

  // actions

  @POST
  public FormValidation doTestConnection(
      @QueryParameter String serverType,
      @QueryParameter String serverUrl,
      @QueryParameter String credentialsId,
      @QueryParameter boolean ignoreCertificate,
      @QueryParameter String proxyProtocol,
      @QueryParameter String proxyHost,
      @QueryParameter String proxyPort,
      @QueryParameter String proxyCredentialsId,
      @AncestorInPath Item item) {
    item.checkPermission(Item.CONFIGURE);

    if (StringUtils.isAnyBlank(serverType, serverUrl, credentialsId)) {
      return FormValidation.error(Messages.formError_invalidParameter());
    }

    ArtifactRepoParamProxy proxy =
        new ArtifactRepoParamProxy(proxyProtocol, proxyHost, proxyPort, proxyCredentialsId);
    ArtifactRepoParamDefinition dummyDefinition =
        new ArtifactRepoParamDefinition(
            serverType, serverUrl, credentialsId, ignoreCertificate, proxy);

    List<ResultEntry> result = Connector.getInstance(dummyDefinition).getResults();
    if (CollectionUtils.isNotEmpty(result)) {
      return FormValidation.okWithMarkup(
          "<span style='color:green'>" + Messages.formError_successfulConnection() + "</span>");
    } else {
      return FormValidation.error(Messages.formError_failedConnection());
    }
  }

  // fill select boxes (credentials)

  public ListBoxModel doFillCredentialsIdItems(
      @AncestorInPath Item item, @QueryParameter String credentialsId) {
    return fillCredentials(item, credentialsId);
  }

  public ListBoxModel doFillProxyCredentialsIdItems(
      @AncestorInPath Item item, @QueryParameter String credentialsId) {
    return fillCredentials(item, credentialsId);
  }

  private ListBoxModel fillCredentials(Item item, String credentialsId) {
    StandardListBoxModel result = new StandardListBoxModel();

    if (item == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER)
        || item != null
            && (!item.hasPermission(Item.EXTENDED_READ)
                || !item.hasPermission(CredentialsProvider.USE_ITEM))) {
      return result.includeCurrentValue(credentialsId);
    }

    return result
        .includeEmptyValue()
        .includeMatchingAs(
            ACL.SYSTEM,
            item,
            StandardUsernameCredentials.class,
            Collections.emptyList(),
            CredentialsMatchers.always())
        .includeCurrentValue(credentialsId);
  }

  // fill select boxes (static entries)
  public ListBoxModel doFillServerTypeItems() {
    ListBoxModel model = new ListBoxModel();
    model.add(Messages.selection_pleaseSelect(), "");
    model.add(Messages.selection_serverType_artifactory(), "artifactory");
    model.add(Messages.selection_serverType_nexus(), "nexus");
    return model;
  }

  public ListBoxModel doFillProxyProtocolItems() {
    ListBoxModel model = new ListBoxModel();
    model.add(Messages.selection_proxyProtocol_https(), "https");
    model.add(Messages.selection_proxyProtocol_http(), "http");
    return model;
  }

  public ListBoxModel doFillParamTypeItems() {
    ListBoxModel model = new ListBoxModel();
    model.add(Messages.selection_pleaseSelect(), "");
    model.add(Messages.selection_paramType_path(), "path");
    model.add(Messages.selection_paramType_version(), "version");
    model.add(Messages.selection_paramType_repo(), "repository");
    return model;
  }

  public ListBoxModel doFillSortOrderItems() {
    ListBoxModel model = new ListBoxModel();
    model.add(Messages.selection_sort_asc(), "asc");
    model.add(Messages.selection_sort_desc(), "desc");
    return model;
  }

  public ListBoxModel doFillSelectEntryItems() {
    ListBoxModel model = new ListBoxModel();
    model.add(Messages.selection_selectEntry_none(), "none");
    model.add(Messages.selection_selectEntry_first(), "first");
    model.add(Messages.selection_selectEntry_last(), "last");
    model.add(Messages.selection_selectEntry_regex(), "regex");
    return model;
  }
}
