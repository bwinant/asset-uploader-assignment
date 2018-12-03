Asset Uploader Assignment
=========================

This repository contains multiple implementations of the S3 asset uploader service programming assignment:

**serverless** directory contains a Serverless implementation

**java** directory contains a Java implementation

**integration_tests** contains an integration test suite that can be run against all implementations 

Each subdirectory contains a README file with further instructions on how to build and run the respective implementations.

### Prerequisites

An S3 bucket:

```
aws s3api create-bucket --bucket $BUCKET_NAME --acl private --region $REGION --create-bucket-configuration LocationConstraint=$REGION 
```

For simplicity, all implementations will share this bucket. Implementations will need to be configured with the chosen values of `$BUCKET_NAME` and `$REGION`