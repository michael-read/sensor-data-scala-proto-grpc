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
  --set kafkaClusters.default.replicas=1 --version "2.0.23"


helm upgrade cloudflow cloudflow-helm-charts/cloudflow --namespace cloudflow \
  --set kafkaClusters.default.bootstrapServers=cloudflow-strimzi-kafka-bootstrap.cloudflow:9092 \
  --set kafkaClusters.default.replicas=1

## Air Gap install of Akka Data Pipelines Console
helm install cloudflow-enterprise-components cloudflow-helm-charts/cloudflow-enterprise-components \
--namespace cloudflow \
--set enterpriseOperator.version=2.0.23 \
--set enterpriseOperator.image=lightbend/cloudflow-enterprise-operator:2.0.23
--set enterprise-suite.esConsoleImage=enterprise-suite/es-console:v1.4.10
--set enterprise-suite.esMonitorImage=enterprise-suite/console-api:v1.2.6
--set enterprise-suite.esGrafanaImage=enterprise-suite/es-grafana:v0.5.0

docker login lightbend-docker-commercial-registry.bintray.io -u <lb user>

## deploy  
cat my-dockerhub-password.txt | kubectl cloudflow deploy ../target/sensor-data-scala-proto-grpc.json -u michaelreadii --password-stdin

### With Cinnamon
cat my-dockerhub-password.txt | kubectl cloudflow deploy ../target/sensor-data-scala-proto-grpc.json -u michaelreadii --password-stdin --conf telemetry.conf

k cloudflow configure sensor-data-scala-proto-grpc --conf telemetry.conf
kubectl cloudflow configure sensor-data-scala-grpc --conf limit.conf




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