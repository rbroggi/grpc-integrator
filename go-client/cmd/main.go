package main

import (
	"context"
	"flag"
	"fmt"
	pb "github.com/rbroggi/grpc-integrator/go-client/target/proto_model"
	"google.golang.org/grpc"
	"log"
	"math/rand"
	"strconv"
	"time"
)

func main() {
	flag.Parse()
	addr := *flag.String("address", "localhost:50051", "The address in form host:port")
	nDeals := *flag.Int("num_deals", 100, "The integer number of deals to be produced")
	sSize := *flag.Int("scen_size", 3000, "The size of the random prices array produced")
	max_exp := *flag.Float64("max_exp", 100.0, "The floating point max absolute value of an exposure")
	conn, err := grpc.Dial(addr, grpc.WithInsecure())
	if err != nil {
		log.Fatalf("fail to dial: %v", err)
	}
	defer conn.Close()

	client := pb.NewDealBrokerClient(conn)
	deals := NewRandomeDeals(nDeals, sSize, max_exp)

	reply, err := sendDealsAsync(deals, client)
	if err != nil {
		log.Fatalf("Got error %v while sending async", err)
	}
	log.Printf("SendDeal summary async: %v", reply)

	replyS, err := sendDealSync(deals, client)
	if err != nil {
		log.Fatalf("Got error %v while sending sync", err)
	}
	log.Printf("SendDeal summary sync: %v", replyS)
}

//Creates an array of random Deals
func NewRandomeDeals(nDeals int, sSize int, max_exp float64) []*pb.Deal {
	var deals []*pb.Deal
	r := rand.New(rand.NewSource(time.Now().UnixNano()))
	for i := 0; i < nDeals; i++ {
		deals = append(deals, randomDeal(r, nDeals, sSize, max_exp))
	}
	return deals
}

//Sending async array of deals
func sendDealsAsync(deals []*pb.Deal, client pb.DealBrokerClient) (*pb.ReceiveStatus, error) {
	stream, err := client.SendDeals(context.Background())
	if err != nil {
		return nil, fmt.Errorf("%v.RecordRoute(_) = _, %v", client, err)
	}

	for _, deal := range deals {
		if err := stream.Send(deal); err != nil {
			return nil, fmt.Errorf("Error while sending single deal %v, %v", deal.Id, err)
		}
	}

	reply, err := stream.CloseAndRecv()
	if err != nil {
		return nil, fmt.Errorf("%v.CloseAndRecv() got error %v, want %v", stream, err, nil)
	}

	return reply, nil
}

//sending sync array of deals
func sendDealSync(deals []*pb.Deal, client pb.DealBrokerClient) (*pb.ReceiveStatus, error) {

	for _, deal := range deals {
		if _, err := client.SendDeal(context.Background(), deal); err != nil {
			return nil, fmt.Errorf("Error while sending single deal %v, %v", deal.Id, err)
		}
	}
	return &pb.ReceiveStatus{StatusOk: true, NumDeals: int64(len(deals))}, nil
}

func randomDeal(r *rand.Rand, nDeals int, sSize int, max_exp float64) *pb.Deal {
	dealNum := (r.Int31n(2) - 1) * 1e3
	contractNum := (r.Int31n(2) - 1) * 1e2
	ctpNum := (r.Int31n(2) - 1) * 1e1
	var exposures []float64
	for i := 0; i < sSize; i++ {
		exposures = append(exposures, (r.Float64()*2.0-1.0)*max_exp)
	}
	return &pb.Deal{
		Id:       "T" + strconv.Itoa(int(dealNum)),
		Contract: "C" + strconv.Itoa(int(contractNum)),
		Ctp:      "CP" + strconv.Itoa(int(ctpNum)),
		Prices:   exposures,
	}
}
