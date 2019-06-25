#!/bin/bash
set -e

./mvnw clean package -DskipTests=true
cf local stage kubernetes-spring-cloud-gateway -p ./target/kubernetes-spring-cloud-gateway-0.0.1-SNAPSHOT.jar
cf local export kubernetes-spring-cloud-gateway -r making/kubernetes-spring-cloud-gateway
docker push making/kubernetes-spring-cloud-gateway
