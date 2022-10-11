#!/usr/bin/env bash

# start Nexus

echo 'Start a Nexus container'
echo '--------------------------------------------------------------------------'
docker run -d \
    -p 8080:8081 \
    --name nexus \
    sonatype/nexus3


# start Artifactory

echo -e '\nStart an Artifactory container'
echo '--------------------------------------------------------------------------'
docker run -d \
    -p 8081:8081 \
    -p 8082:8082 \
    --name artifactory \
    releases-docker.jfrog.io/jfrog/artifactory-oss:latest


# wait for 20s (Nexus does take quite some time to start up)

echo -e '\nWaiting for 20 seconds before proceeding'
echo '--------------------------------------------------------------------------'
sleep 20


# loop until we get the password

echo -e '\nTry to identify Nexus password'
echo '--------------------------------------------------------------------------'
until docker exec -t nexus sh -c 'cat /nexus-data/admin.password'
do
  echo 'Wait 5s and retry ...'
  sleep 5
done


echo -e '\n\nFinished'
echo '--------------------------------------------------------------------------'
echo 'Nexus: http://localhost:8080 - initial password printed above'
echo 'Artifactory: http://localhost:8081/ui - initial password = admin:password'
