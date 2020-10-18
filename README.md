# Artifact Repository Parameter Plugin

The goal of the plugin is to make certain information of an artifact repository available as
[Jenkins][link2] build parameter. Currently the following endpoints are supported.

* __Path__ - Display all deployed artifacts.
* __Version__ - Display all available versions of an artifact.
* __Repositories__ - A list of all available repositories.

The following artifact repositories were tested during development.

* __Sonatype Nexus 3 OSS__
* __JFrog Artifactory 6 Pro__
* __JFrog Artifactory 7 OSS__

## Configuration

Detailed instructions how to configure the plugin can be found in [docs/Config.md][link0].

## Known Limitations

### Blue Ocean

The current version of this plugin does not work with Blue Ocean.

### Internet Explorer

The current version of this plugin does not work with any version of Internet Explorer. Use Chrome,
Firefox or Edge instead.

## Similar plugins

Another plugin with similar features is
[Maven Artifact ChoiceListProvider](https://plugins.jenkins.io/maven-artifact-choicelistprovider/).
For more details please refer to the plugin's overview page.

## Development

To build the plugin please refer to [docs/Build.md][link1].



[link0]: ./docs/Config.md
[link1]: ./docs/Build.md
[link2]: https://www.jenkins.io/
