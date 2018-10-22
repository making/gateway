# Kubernetes Native Spring Cloud Gateway


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
  name: hello-pks-service
  namespace: hello
  labels:
    app: hello-pks
  annotations:
    spring.cloud.gateway/port: http
    spring.cloud.gateway/routes: |
      predicates:
      - Host=hello-pks.example.com
      filters:
      - AddRequestHeader=X-Request-Foo, Bar
      - name: Retry
        args:
          retries: 3
          statuses: BAD_GATEWAY
      - name: RequestSize
        args:
          maxSize: 5000000
spec:
  type: NodePort
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
```