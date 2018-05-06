package main

import (
	"fmt"
	pb "github.com/rbroggi/grpc-integrator/go-client/target/proto_model"
	"google.golang.org/grpc"
	"log"
	"testing"
)

func BenchmarkAsyncSync(b *testing.B) {

	methods := []struct {
		name string
		fun  func([]*pb.Deal, pb.DealBrokerClient) (*pb.ReceiveStatus, error)
	}{
		{"Async", sendDealsAsync},
		{"Sync", sendDealSync},
	}

	conn, err := grpc.Dial("localhost:50051", grpc.WithInsecure())
	if err != nil {
		log.Fatalf("fail to dial: %v", err)
	}
	defer conn.Close()

	client := pb.NewDealBrokerClient(conn)

	for nDeals := 2; nDeals < 5000; nDeals *= 2 {
		deals := NewRandomeDeals(nDeals, 6000, 100.0)
		for _, method := range methods {
			b.Run(fmt.Sprintf("%s - %d", method.name, nDeals), func(b *testing.B) {
				_, err := method.fun(deals, client)
				if err != nil {
					log.Fatal("Couldn't send trades")
				}
			})

		}

	}
}
