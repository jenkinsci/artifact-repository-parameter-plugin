# Container

This page contains some information how to work with containers for local development
and testing.

## Sonatype Nexus OSS

### Start container
```
docker run -d \
    -p 8080:8081 \
    --name nexus \
    sonatype/nexus3
```

### Retrieve password
`docker exec -t nexus sh -c 'cat /nexus-data/admin.password'`

### Stop container & remove
```
docker stop --time=30 nexus
docker rm nexus
```


## JFrog Artifactory

docker pull 

### Start container
```
docker run -d \
    -p 8081:8081 \
    -p 8082:8082 \
    --name artifactory \
    releases-docker.jfrog.io/jfrog/artifactory-oss:latest
```

### Initial password
admin:password

### Stop container
``` 
docker stop --time=30 artifactory
docker rm artifactory
```
