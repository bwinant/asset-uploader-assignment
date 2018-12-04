package asset_server

import (
	"fmt"
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/service/s3"
	"github.com/gorilla/mux"
	"net/http"
	"strconv"
	"time"
)

type getResponse struct {
	DownloadUrl string `json:"Download_url"`
}

type getHandler struct {
	*baseHandler
	downloadTimeout int // In seconds
}

func (h getHandler) getAsset(w http.ResponseWriter, r *http.Request) (interface{}, error) {
	vars := mux.Vars(r)
	assetId := vars[ASSET_ID]

	// Parse timeout query parameter
	query := r.URL.Query()
	timeout, err := getTimeout(query.Get("timeout"), h.downloadTimeout)
	if timeout <= 0 || err != nil {
		return nil, ServerError("Invalid timeout", err)
	}

	// Verify object exists and is officially uploaded
	metadata, err := h.getMetadata(assetId)
	if err != nil {
		return nil, err
	}

	status := metadata[S3_STATUS_HEADER]
	if status == nil || *status != UPLOADED {
		return nil, ServerError(fmt.Sprintf("Asset %s has not been uploaded", assetId), nil)
	}

	// Generate presigned URL
	getReq, _ := h.s3Client.GetObjectRequest(&s3.GetObjectInput{
		Bucket: aws.String(h.bucket),
		Key:    aws.String(assetId),
	})

	downloadUrl, err := getReq.Presign(time.Duration(timeout) * time.Second)
	if err != nil {
		return nil, err
	}

	return &getResponse{DownloadUrl: downloadUrl}, nil
}

func getTimeout(val string, defVal int) (int, error) {
	if val == "" {
		return defVal, nil
	}
	return strconv.Atoi(val)
}
