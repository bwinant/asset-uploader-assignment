package asset_server

import (
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/service/s3"
	"github.com/google/uuid"
	"log"
	"net/http"
	"time"
)

type createResponse struct {
	ID        string `json:"id"`
	UploadUrl string `json:"upload_url"`
}

type createHandler struct {
	*baseHandler
	uploadTimeout int // In minutes
}

func (h createHandler) CreateAsset(w http.ResponseWriter, r *http.Request) (interface{}, error) {
	// Generate id
	assetId, err := uuid.NewRandom()
	if err != nil {
		return nil, err
	}

	// Generate presigned URL
	putReq, _ := h.s3Client.PutObjectRequest(&s3.PutObjectInput{
		Bucket: aws.String(h.bucket),
		Key:    aws.String(assetId.String()),
	})
	uploadUrl, err := putReq.Presign(time.Duration(h.uploadTimeout) * time.Minute)
	if err != nil {
		return nil, err
	}

	log.Printf("Created asset %s", assetId)
	return &createResponse{ID: assetId.String(), UploadUrl: uploadUrl}, nil
}
