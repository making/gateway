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
kubectl -n spring-cloud-gateway get secrets $(kubectl -n spring-cloud-gateway get serviceaccount spring-cloud-gateway -o jsonpath="{.secrets[0].name}") -o jsonpath="{.data.token}" | base64 --decode > serviceaccount/token 
docker run --rm -p 8080:8080 \
  --memory=384m \
  -v $(pwd)/serviceaccount:/var/run/secrets/kubernetes.io/serviceaccount \
  making/kubernetes-spring-cloud-gateway
```

sample

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: hello
---
kind: Service
apiVersion: v1
metadata:
  name: hello-pks
  namespace: hello
  labels:
    app: hello-pks
  annotations:
    spring.cloud.gateway/routes: |
      predicates:
      - Host=hello-pks.example.com
      filters:
      - Hystrix=hello/hello-pks
      - name: Retry
        args:
          retries: 3
          statuses: BAD_GATEWAY
spec:
  selector:
    app: hello-pks
  ports:
  - protocol: TCP
    port: 8080
    name: http
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: hello-pks
  namespace: hello
spec:
  replicas: 1
  selector:
    matchLabels:
      app: hello-pks
  template:
    metadata:
      labels:
        app: hello-pks
    spec:
      containers:
      - image: making/hello-pks:0.0.2
        name: hello-pks
        ports:
        - containerPort: 8080
---
apiVersion: gateway.ik.am/v1beta1
kind: RouteDefinition
metadata:
  name: hello-pks
  namespace: hello
spec:
  route:
    predicates:
    - Host=hello-pks.example.com
    filters:
    - Hystrix=hello/hello-pks
    - name: Retry
      args:
        retries: 3
        statuses: BAD_GATEWAY
```