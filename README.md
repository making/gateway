# Kubernetes Native Spring Cloud Gateway

```
./mvnw clean package -DskipTests=true
cf local stage kubernetes-spring-cloud-gateway -p ./target/kubernetes-spring-cloud-gateway-0.0.1-SNAPSHOT.jar
cf local export kubernetes-spring-cloud-gateway -r making/kubernetes-spring-cloud-gateway
docker push making/kubernetes-spring-cloud-gateway
```

```
kubectl apply -f k8s/gateway.yml
```

```
mkdir serviceaccount
kubectl -n gateway get secrets $(kubectl -n gateway get serviceaccount spring-cloud-gateway -o jsonpath="{.secrets[0].name}") -o jsonpath="{.data.token}" | base64 --decode > serviceaccount/token 
docker run --rm -p 8080:8080 \
  --memory=384m \
  -v $(pwd)/serviceaccount:/var/run/secrets/kubernetes.io/serviceaccount \
  making/kubernetes-spring-cloud-gateway
```
