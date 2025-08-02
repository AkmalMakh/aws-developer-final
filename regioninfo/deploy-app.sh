#!/bin/bash
./gradlew clean build -x test
aws s3 cp build/libs/regioninfo-0.0.1-SNAPSHOT.jar s3://akmal-makhmudov-bucket/image-app.jar

docker build -t regioninfo-app .wq
docker push 151182332702.dkr.ecr.us-east-1.amazonaws.com/akmal/webapp:latest
