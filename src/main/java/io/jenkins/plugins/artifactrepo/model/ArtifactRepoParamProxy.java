package io.jenkins.plugins.artifactrepo.model;

import java.io.Serializable;
import java.util.stream.Stream;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * An immutable bean storing proxy related information defined in the optional block in the config
 * view (Connection Options).
 */
@Getter
public final class ArtifactRepoParamProxy implements Serializable {
    public static final long serialVersionUID = -2264442474142821023L;

    public static final ArtifactRepoParamProxy DISABLED = new ArtifactRepoParamProxy("", "", "", "");
    private final String proxyProtocol;
    private final String proxyHost;
    private final String proxyPort;
    private final String proxyCredentialsId;

    @DataBoundConstructor
    public ArtifactRepoParamProxy(String proxyProtocol, String proxyHost, String proxyPort, String proxyCredentialsId) {
        this.proxyProtocol = proxyProtocol;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyCredentialsId = proxyCredentialsId;
    }

    public boolean isProxyActive() {
        return Stream.of(proxyProtocol, proxyHost, proxyPort).allMatch(StringUtils::isNotBlank);
    }
}
