# Artifact Repository Parameter Plugin

The goal of the plugin is to make certain information of an artifact repository available as
[Jenkins][link2] build parameter. Currently the following endpoints are supported.

* __Path__ - Display all deployed artifacts by its path.
* __Version__ - Display all available versions of an artifact.
* __Repositories__ - A list of all available repositories.

The following artifact repositories were tested during development.

* __Sonatype Nexus 3 OSS - 3.42.0-01__
* __JFrog Artifactory Cloud__
* __JFrog Artifactory 7 OSS - 7.41.13__

> When someone has access to other versions of Nexus/Artifactory it would be nice to 
> get some feedback whether it's working fine with these versions or not.

## Version Warning

Please be aware that version 2.0.0 is not backwards compatible to version 1.x. There is a
breaking change in the `Display Options` section. All results are now displayed in a select
box. The option to display the results as a dropdown, radio button or checkbox have been
removed. If multiple results must be selectable please make sure to check the
`Multiple Entries Selectable` option.

Furthermore the support for Java 8 was dropped. This plugin now requires a Java 11 or higher.

## Configuration

Detailed instructions how to configure the plugin can be found in [docs/Config.md][link0].

## Development

To build the plugin please refer to [docs/Build.md][link1].

## Known Limitations

### Blue Ocean

The current version of this plugin does not work with Blue Ocean UI.

## Similar plugins

Another plugin with similar features is
[Maven Artifact ChoiceListProvider](https://plugins.jenkins.io/maven-artifact-choicelistprovider/).
For more details please refer to the plugin's overview page.




[link0]: ./docs/Config.md
[link1]: ./docs/Build.md
[link2]: https://www.jenkins.io/
