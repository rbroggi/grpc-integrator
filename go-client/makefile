# Go parameters
GOCMD=go
GOBUILD=$(GOCMD) build
GOCLEAN=$(GOCMD) clean
GOTEST=$(GOCMD) test
GOGET=$(GOCMD) get
BINARY_TARGET_FOLDER=target
BINARY_NAME=grpc_client
BINARY_UNIX=$(BINARY_NAME)_unix
TARGET_CMD=cmd
PROTO_BUF_CMD=protoc
PROTO_FILE_FOLDER=../grpc-intf/src/main/proto/model/

all: test build
build: deps
	mkdir -p $(BINARY_TARGET_FOLDER)/proto_model
	$(PROTO_BUF_CMD) -I  $(PROTO_FILE_FOLDER) $(PROTO_FILE_FOLDER)/*.proto --go_out=plugins=grpc:$(BINARY_TARGET_FOLDER)/proto_model
	$(GOBUILD) -o $(BINARY_TARGET_FOLDER)/$(BINARY_NAME) -v ./$(TARGET_CMD)
test: build
	$(GOTEST) -v ./...
clean:
	$(GOCLEAN)
	rm -rf $(BINARY_TARGET_FOLDER)
run: build
	./$(BINARY_TARGET_FOLDER)/$(BINARY_NAME)
deps:
	$(GOGET) google.golang.org/grpc


# Cross compilation
build-linux: deps
	CGO_ENABLED=0 GOOS=linux GOARCH=amd64 $(GOBUILD) -o $(BINARY_UNIX) -v
docker-build:
	docker run --rm -it -v "$(GOPATH)":/go -w /go/src/bitbucket.org/rsohlich/makepost golang:latest go build -o "$(BINARY_UNIX)" -v
