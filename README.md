## Sensor Data

Protobuf end to end



#grpcurl
https://github.com/fullstorydev/grpcurl

##Listing Services
grpcurl -plaintext localhost:3000 list

SensorDataService
grpc.reflection.v1alpha.ServerReflection

##Describing Elements
grpcurl -plaintext localhost:3000 describe SensorDataService
SensorDataService is a service:
service SensorDataService {
rpc Provide ( .SensorData ) returns ( .SensorDataReply );
rpc ProvideStreamed ( stream .SensorData ) returns ( stream .SensorDataReply );
}

##Invoking Service
grpcurl -plaintext -d '{"deviceId":"c75cb448-df0e-4692-8e06-0321b7703992","timestamp":1495545646279,"measurements":{"power":1.7,"rotorSpeed":3.9,"windSpeed":105.9}}' \
localhost:3000 SensorDataService/Provide
{
"deviceId": "c75cb448-df0e-4692-8e06-0321b7703992",
"success": true
}
