# Artifact Repository Parameter Plugin - Build

This page provides some information how to build the plugin.

## Local Development

To build the plugin simply execute the following command.
```
mvn clean install
```

To start a local Jenkins instance with the plugin installed run the following command
and afterwards navigate to [http://localhost:8888/jenkins](http://localhost:8888/jenkins).
```
MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n" \
mvn hpi:run -Djetty.port=8888
```

> This command will create a `work/` folder inside the project's folder and install
> Jenkins in there. The plugins are located in `work/plugins/`. Changes can survive
> instance restarts.

To change the Jenkins version this plugin depends on simply change the property 
`jenkins.version` in the Maven POM. Make sure to rely on LTS versions whenever possible. 

It's possible to automatically install Jenkins plugins to the local instance for testing.
To do so list them as Maven dependencies but make sure they have a `<scope>test</scope>`
set.

## Code Styleguide

This plugin's code follows the [Google style guide for Java][link0] in version 1.7. A
Git pre-push hook gets installed automatically to make sure the code follows this format.
For IntelliJ IDEA the use of the [google-java-format][link1] plugin is recommended.

It's also possible to check and apply the formatting rules manually. To check if all 
files follow the style guide run the following command.

```
# check if code follows rules
mvn spotless:check

# format all files
mvn spotless:apply
```

> The actual content of the hook can be seen in `pom.xml`.



[link0]: https://google.github.io/styleguide/javaguide.html
[link1]: https://plugins.jetbrains.com/plugin/8527-google-java-format
