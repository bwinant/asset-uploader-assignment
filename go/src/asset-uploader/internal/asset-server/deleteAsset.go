package asset_server

import (
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/service/s3"
	"github.com/gorilla/mux"
	"log"
	"net/http"
)

type deleteHandler struct {
	*baseHandler
}

func (h deleteHandler) DeleteAsset(w http.ResponseWriter, r *http.Request) (interface{}, error) {
	vars := mux.Vars(r)
	assetId := vars[ASSET_ID]

	_, err := h.s3Client.DeleteObject(&s3.DeleteObjectInput{
		Bucket: aws.String(h.bucket),
		Key:    aws.String(assetId),
	})
	if err != nil {
		return nil, err
	}

	log.Printf("Deleted asset %s\n", assetId)
	return nil, nil
}
