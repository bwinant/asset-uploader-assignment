package main

import (
	assets "asset-uploader/internal/asset-server"
	"context"
	"flag"
	"fmt"
	"log"
	"os"
	"os/signal"
)

func main() {
	flag.Usage = func() {
		fmt.Printf("Usage: asset-uploader [options]\n")
		flag.PrintDefaults()
	}

	// Parse CLI arguments
	var region, profile, bucket string
	var port int
	flag.StringVar(&region, "region", "", "AWS region")
	flag.StringVar(&profile, "profile", "", "Name of AWS profile with required S3 permissions")
	flag.StringVar(&bucket, "bucket", "", "Name of S3 bucket")
	flag.IntVar(&port, "port", 8080, "Port to listen on")
	flag.Parse()

	if region == "" || profile == "" || bucket == "" {
		flag.Usage()
		os.Exit(1)
	}

	// Configure server
	config := &assets.S3Config{Region: region, Profile: profile, Bucket: bucket}
	server, err := assets.NewServer(port, config)
	if err != nil {
		log.Fatalln(err)
	}

	// Start server in goroutine
	go func() {
		if err := server.Start(); err != nil {
			log.Println(err)
		}
	}()

	// Signal server shutdown
	c := make(chan os.Signal, 1)
	signal.Notify(c, os.Interrupt)

	// Block until shutdown signal received
	<-c

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	server.Stop(ctx)
	os.Exit(0)
}
