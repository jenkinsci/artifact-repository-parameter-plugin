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
MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=5005,suspend=n" \
mvn hpi:run -Djetty.port=8888
```

> This command will create a `work/` folder inside the project's folder and install
> Jenkins in there. The plugins are located in `work/plugins/`. Changes can survive
> instance restarts.

To change the Jenkins version simply change the property `jenkins.version` in the 
Maven POM. Make sure to rely on LTS versions whenever possible. 

It's possible to automatically install Jenkins plugins to the local instance for testing.
To do so list them as Maven dependencies but make sure they have a `<scope>test</scope>`
set.

## Code Styleguide

The code is formatted by the [Spotless Maven Plugin][link0] and makes use of the
[Palantir Java Format][link1] and [sortpom][link2] plugins. The execution is bound to the
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


[link0]: https://github.com/diffplug/spotless/tree/main/plugin-maven
[link1]: https://github.com/palantir/palantir-java-format
[link2]: https://github.com/Ekryd/sortpom
