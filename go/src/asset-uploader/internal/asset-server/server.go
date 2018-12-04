package asset_server

import (
	"context"
	"fmt"
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/credentials"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/s3"
	"github.com/gorilla/mux"
	"log"
	"net/http"
)

const S3_STATUS_HEADER = "Asset-Status"
const UPLOADED = "uploaded"
const ASSET_ID = "assetId"

type S3Config struct {
	Region  string
	Profile string
	Bucket  string
}

type RestServer struct {
	router     *mux.Router
	server     *http.Server
	awsSession *session.Session
}

type baseHandler struct {
	s3Client *s3.S3
	bucket   string
}

func (b baseHandler) getMetadata(assetId string) (map[string]*string, error) {
	output, err := b.s3Client.HeadObject(&s3.HeadObjectInput{
		Bucket: aws.String(b.bucket),
		Key:    aws.String(assetId),
	})

	if err != nil {
		if isNotFound(err) {
			return nil, NotFoundError(fmt.Sprintf("Asset %s not found", assetId))
		}
		return nil, err
	}

	return output.Metadata, nil
}

// Wrap all route handlers to handle all errors and sending all HTTP responses
// Probably a nicer way to do this with Mux middleware
func responseHandler(f func(w http.ResponseWriter, r *http.Request) (interface{}, error)) func(w http.ResponseWriter, r *http.Request) {
	return func(w http.ResponseWriter, r *http.Request) {
		res, err := f(w, r)

		if err != nil {
			var sc = 500
			var message = err.Error()
			var logError = err

			if restErr, ok := err.(RestError); ok {
				sc = restErr.Status()
				if restErr.Nested() != nil {
					logError = restErr.Nested()
				}
			}

			log.Println(logError)
			sendError(w, sc, message)
		} else {
			sendSuccess(w, res)
		}
	}
}

func NewServer(port int, s3Config *S3Config) (*RestServer, error) {
	// Configure AWS session
	sess, err := session.NewSession(&aws.Config{
		Region:      aws.String(s3Config.Region),
		Credentials: credentials.NewSharedCredentials("", s3Config.Profile),
	})
	if err != nil {
		return nil, err
	}

	// Create S3 service client
	s3Client := s3.New(sess)

	// Create Mux router
	router := mux.NewRouter()

	// Create base route handler struct
	baseHandler := &baseHandler{s3Client: s3Client, bucket: s3Config.Bucket}

	// Setup all routes and handlers
	createHandler := &createHandler{baseHandler: baseHandler, uploadTimeout: 15}
	router.HandleFunc("/asset", responseHandler(createHandler.CreateAsset)).Methods("POST")

	updateHandler := &updateHandler{baseHandler: baseHandler}
	router.HandleFunc("/asset/{assetId}", responseHandler(updateHandler.UpdateAsset)).Methods("PUT")

	getHandler := &getHandler{baseHandler: baseHandler, downloadTimeout: 60}
	router.HandleFunc("/asset/{assetId}", responseHandler(getHandler.getAsset)).Methods("GET")

	deleteHandler := &deleteHandler{baseHandler: baseHandler}
	router.HandleFunc("/asset/{assetId}", responseHandler(deleteHandler.DeleteAsset)).Methods("DELETE")

	// Create HTTP server
	server := &http.Server{
		Addr:    fmt.Sprintf(":%d", port),
		Handler: router,
	}

	return &RestServer{router: router, server: server, awsSession: sess}, nil
}

func (rs RestServer) Start() error {
	log.Printf("Starting server on port %s\n", rs.server.Addr)
	return rs.server.ListenAndServe()
}

func (rs RestServer) Stop(ctx context.Context) {
	log.Println("Stopping server")
	if err := rs.server.Shutdown(ctx); err != nil {
		log.Println(err)
	}
}
