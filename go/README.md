Go Asset Uploader
=========================

Go asset uploader implementation that tracks asset state in S3 metadata. 

### Requirements

- Go 1.10+

### Build
 
```
export GOPATH=$(pwd)
go get ./...
go install -i -v asset-uploader/cmd/asset-server
```

### Run

```
./bin/asset-server -region ${REGION} -profile ${PROFILE} -bucket $BUCKET_NAME
```

`$BUCKET_NAME` and `$REGION` should match previously created S3 bucket configuration. `$PROFILE` should reference an AWS profile with valid credentials and permissions.