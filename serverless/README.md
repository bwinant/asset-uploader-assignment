Serverless Asset Uploader
=========================

Serverless asset uploader implementation using API Gateway, AWS Lambda and DynamoDB 
### Requirements

- An AWS profile with proper permissions to create resources.
- Node.js 8+
- NPM 5+
- [Serverless Framework](https://serverless.com) (`npm install -g serverless`) 
- It would be helpful to have some knowledge of the Serverless Framework. 


### Build
 
```
npm install 
```

### Deploy
```
 sls deploy --asset-bucket $BUCKET_NAME --aws-profile $PROFILE --region $REGION
```

`$BUCKET_NAME` and `$REGION` should match previously created S3 bucket configuration. `$PROFILE` should reference an AWS profile with valid credentials and permissions.
