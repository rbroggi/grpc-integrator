# grpc-integrator
java-server and go-client grpc integration for a trading aggregation application

## Dependencies

* jdk8  
* [Maven](https://maven.apache.org/) - Dependency Management 
* [Protocol Buffers](https://developers.google.com/protocol-buffers/) - Encoding/Decoding structured data multilanguage support  
* [Golang compiler](https://golang.org/) - Go language compiler

## Build

### java-server

mvn clean compile assembly:single  

### go-client

cd #PROJECT_ROOT#/go-client  
make build 

## Run

### java-server
java -jar #PROJECT_ROOT#/grpc-endpoint/target/grpc-endpoint-1.0-SNAPSHOT-jar-with-dependencies.jar
### go-client
#PROJECT_ROOT#/go-client/target/grpc_client
