package asset_server

import (
	"encoding/json"
	"fmt"
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/service/s3"
	"github.com/gorilla/mux"
	"log"
	"net/http"
)

type updateRequest struct {
	Status string `json:"Status"`
}

type updateHandler struct {
	*baseHandler
}

func (h updateHandler) UpdateAsset(w http.ResponseWriter, r *http.Request) (interface{}, error) {
	vars := mux.Vars(r)
	assetId := vars[ASSET_ID]

	// Parse JSON body
	req := updateRequest{}
	err := json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		return nil, ServerError("Invalid request", err)
	}

	// Verify status sent
	if req.Status != UPLOADED {
		return nil, ServerError("Invalid request", nil)
	}

	// Check if asset was already completed
	metadata, err := h.getMetadata(assetId)
	if err != nil {
		return nil, err
	}

	status := metadata[S3_STATUS_HEADER]
	if status != nil && *status == UPLOADED {
		return nil, ServerError(fmt.Sprintf("Upload of asset %s is already completed", assetId), nil)
	}

	// To update object metadata of existing object requires a copy
	metadata[S3_STATUS_HEADER] = aws.String(UPLOADED)

	_, err = h.s3Client.CopyObject(&s3.CopyObjectInput{
		Bucket:            aws.String(h.bucket),
		CopySource:        aws.String(fmt.Sprintf("%s/%s", h.bucket, assetId)),
		Key:               aws.String(assetId),
		Metadata:          metadata,
		MetadataDirective: aws.String("REPLACE"),
	})
	if err != nil {
		return nil, err
	}

	log.Printf("Completed upload of asset %s\n", assetId)
	return nil, nil
}
