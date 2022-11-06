# Contributing

When contributing to this repository please discuss the change you wish to make first by opening
a [Github issue](https://github.com/jenkinsci/artifact-repository-parameter-plugin/issues) before
making a change.

## Pull Request Process

1. Fork from [https://github.com/jenkinsci/artifact-repository-parameter-plugin](https://github.com/jenkinsci/artifact-repository-parameter-plugin)
   and apply your changes.
2. Ensure the project is building without any exceptions.
3. If you add unit tests make sure those unit tests run successfully.
4. If you add dependent plugins to `pom.xml` make sure to use the right version in accordance with
   the Jenkins version (see property `<jenkins.version>` in `pom.xml`).
5. Make sure the code is formatted in accordance with the coding styleguide mentioned below.
6. If all requirements are met open a PR, so we can have a look at it.

## Versioning

We try to follow [semantic versioning](https://semver.org/) whenever applicable.

## Build Plugin

For local development you can simply run the following command.
```
mvn clean install && \
  MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=5005,suspend=n" \
  mvn hpi:run -Djetty.port=8888 
```

This will start a local Jenkins instance with all required plugins available at
[http://localhost:8888/jenkins](http://localhost:8888/jenkins).

> This command will create a `work/` folder inside the project's folder and install
> Jenkins in there. The plugins are located in `work/plugins/`. Changes can survive
> instance restarts.

To change the Jenkins version simply change the property `jenkins.version` in the
Maven POM. Make sure to rely on LTS versions whenever possible.

It's possible to automatically install Jenkins plugins to the local instance for testing.
To do so list them as Maven dependencies but make sure they have a `<scope>test</scope>`
set.

### Container

It's possible to test the plugin during development with local artifact repository servers.
Following are instructions to start both Artifactory OSS and Nexus OSS in Docker.

```
# -----------------------------------------------------
# Start Artifactory
# -----------------------------------------------------
docker run -d \
    -p 8081:8081 \
    -p 8082:8082 \
    --name artifactory \
    releases-docker.jfrog.io/jfrog/artifactory-oss:latest

# -----------------------------------------------------
# Start Nexus
# -----------------------------------------------------
docker run -d \
    -p 8080:8081 \
    --name nexus \
    sonatype/nexus3
```

For Artifactory the initial password is `admin:password`. For Nexus the initial password
can be retrieved by executing the following command.

```
docker exec -t nexus sh -c 'cat /nexus-data/admin.password'
```

## Code Styleguide

The code is formatted by the [Spotless Maven Plugin](https://github.com/diffplug/spotless/tree/main/plugin-maven)
and makes use of the [Palantir Java Format](https://github.com/palantir/palantir-java-format)
and [sortpom](https://github.com/Ekryd/sortpom) plugins. The execution is bound to the
`validate` phase so basically every regular Maven command (except for explicit plugin goals)
should trigger a code format.

It's also possible to check and apply the formatting rules manually. To check if all
files follow the style guide run the following command.

```
# check if code follows rules
mvn spotless:check

# format all files
mvn spotless:apply
```

For developers using IntelliJ IDEA we recommend the use of [palantir-java-format](https://plugins.jetbrains.com/plugin/13180-palantir-java-format)
for this project.
