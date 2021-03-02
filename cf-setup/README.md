## Local PersistentVolume

Login to each worker node, and create dir:

* sudo mkdir -p /mnt/disk/vol1
* sudo chmod 777 /mnt/disk/vol1

update the node names in each file (last line)

* pv1.yaml
* pv2.yaml

## Install Helm on Master

https://helm.sh/docs/intro/install/#from-apt-debianubuntu

## single replica in Cloudflow

helm install cloudflow cloudflow-helm-charts/cloudflow --namespace cloudflow \
  --set kafkaClusters.default.bootstrapServers=cloudflow-strimzi-kafka-bootstrap.cloudflow:9092 \
  --set kafkaClusters.default.replicas=1 --version "2.0.24"


helm upgrade cloudflow cloudflow-helm-charts/cloudflow --namespace cloudflow \
  --set kafkaClusters.default.bootstrapServers=cloudflow-strimzi-kafka-bootstrap.cloudflow:9092 \
  --set kafkaClusters.default.replicas=1 --version "2.0.24"

## Air Gap install of Akka Data Pipelines Console
1. docker login to bintray:
   docker login http://lightbend-docker-commercial-registry.bintray.io/ -u <username>
2. pull down images into your local docker
docker pull lightbend-docker-commercial-registry.bintray.io/lightbend/cloudflow-enterprise-operator:2.0.23
docker pull lightbend-docker-commercial-registry.bintray.io/enterprise-suite/es-console:v1.4.10
docker pull lightbend-docker-commercial-registry.bintray.io/enterprise-suite/console-api:v1.2.6
docker pull lightbend-docker-commercial-registry.bintray.io/enterprise-suite/es-grafana:v0.5.0
3. tag images
docker tag lightbend-docker-commercial-registry.bintray.io/lightbend/cloudflow-enterprise-operator:2.0.23 lightbend/cloudflow-enterprise-operator:2.0.23
docker tag lightbend-docker-commercial-registry.bintray.io/enterprise-suite/es-console:v1.4.10 enterprise-suite/es-console:v1.4.10
docker tag lightbend-docker-commercial-registry.bintray.io/enterprise-suite/console-api:v1.2.6 enterprise-suite/console-api:v1.2.6
docker tag lightbend-docker-commercial-registry.bintray.io/enterprise-suite/es-grafana:v0.5.0 enterprise-suite/es-grafana:v0.5.0
4. export images  
docker save lightbend/cloudflow-enterprise-operator:2.0.23 > enterprise.tar
docker save enterprise-suite/es-console:v1.4.10 > es-console.tar  
docker save enterprise-suite/console-api:v1.2.6 > console-api.tar
docker save enterprise-suite/es-grafana:v0.5.0 > es-grafana-tar
5. import into microk8s
microk8s ctr image import enterprise.tar
microk8s ctr image import es-console.tar
microk8s ctr image import console-api.tar
microk8s ctr image import es-grafana-tar
   
helm install cloudflow-enterprise-components cloudflow-helm-charts/cloudflow-enterprise-components \
--namespace cloudflow \
--set enterpriseOperator.version=2.0.23 \
--set enterpriseOperator.image=lightbend/cloudflow-enterprise-operator \
--set enterprise-suite.esConsoleImage=enterprise-suite/es-console \
--set enterprise-suite.esMonitorImage=enterprise-suite/console-api \
--set enterprise-suite.esGrafanaImage=enterprise-suite/es-grafana



## deploy  
cat my-dockerhub-password.txt | kubectl cloudflow deploy ../target/sensor-data-scala-proto-grpc.json -u michaelreadii --password-stdin

### With Cinnamon
cat my-dockerhub-password.txt | kubectl cloudflow deploy ../target/sensor-data-scala-proto-grpc.json -u michaelreadii --password-stdin --conf telemetry.conf

### With Cinnamon, turn down Kafka
cat my-dockerhub-password.txt | kubectl cloudflow deploy ../target/sensor-data-scala-proto-grpc.json -u michaelreadii --password-stdin --conf telemetry.conf --logback-config logback.xml

k cloudflow configure sensor-data-scala-proto-grpc --conf telemetry.conf --logback-config logback.xml
kubectl cloudflow configure sensor-data-scala-grpc --conf limit.conf

# follow logs of all pods
k -n sensor-data-scala-proto-grpc logs -f -l com.lightbend.cloudflow/app-id=sensor-data-scala-proto-grpc


## using Ray's kafka-avro-tools (https://github.com/RayRoestenburg/kafka-avro-tools)

### get broker IP
kubectl -n cloudflow get svc cloudflow-strimzi-kafka-bootstrap
There are no credentials needed. the bootstrap is normally cloudflow-strimzi-kafka-bootstrap.cloudflow:9092, like shown in the docs.

### list metadata for all topics
kafkacat -L -b 10.96.53.191:9092 

### list metadata for a topic
kafkacat -L -b 10.96.53.191:9092 -t metrics

### consume a "metrics" topic with schema
kafkacat -C -b 10.96.53.191:9092 -t metrics -q -u -D "" -f %s | java -jar /avro-tools/avro-tools-1.8.2.jar fragtojson --schema-file /schemas/Metric.avsc - | jq .