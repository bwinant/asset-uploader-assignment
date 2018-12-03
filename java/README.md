Java Asset Uploader
=========================

Java asset uploader implementation using Spring Boot, PostgresSQL for tracking asset state, and run using Docker. 

### Requirements

- Java 10+
- Maven 3+
- Docker 

### Build
 
```
mvn clean package 
docker-compose --project-name asset-uploader build --build-arg ASSET_BUCKET=$BUCKET_NAME --build-arg REGION=$REGION --build-arg AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID --build-arg AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY 
```

`$BUCKET_NAME` and `$REGION` should match previously created S3 bucket configuration. 
`$AWS_ACCESS_KEY_ID` and `$AWS_SECRET_ACCESS_KEY` are AWS credentials with s3:GetObject, s3:PutObject and s3:DeleteObject IAM permissions.

### Run
```
docker-compose --project-name asset-uploader up -d 
```
