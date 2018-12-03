'use strict';

const uuid = require('uuid/v4');

const AWS = require("aws-sdk");
const s3 = new AWS.S3({ signatureVersion: 'v4' });
const dynamo = new AWS.DynamoDB.DocumentClient({ convertEmptyValues: true });

const BUCKET = process.env.ASSET_BUCKET;
const TABLE = process.env.ASSET_TABLE;

module.exports.create = async () => {
    // Will use UUIDs for identifying assets.
    // Reason is to prevent users from poking at API URLs: if GET /asset/1 works, there is a good chance GET asset/2 does too
    const assetId = uuid();

    // Track asset upload state in Dynamo
    const item = {
        id: assetId,
        status: 'created',
        ts: new Date().toISOString()
    };
    await dynamo.put({ TableName: TABLE, Item: item }).promise();
    console.log(`Initialized asset ${assetId}`);

    return assetId;
};

module.exports.complete = async (asset) => {
    const assetId = asset.id;

    // Check that the uploaded asset actually exists in S3
    try {
        await s3.headObject({ Bucket: BUCKET, Key: assetId }).promise();
    }
    catch (err) {
        // Will get a 403 if object does not exist
        if (err.code === 'Forbidden') {
            throw new Error(`Asset ${assetId} has not been uploaded`);
        }
    }

    // Check if asset was already completed
    if (asset.status === 'uploaded') {
        throw new Error(`Upload of asset ${assetId} is already completed`);
    }

    try {
        // Update asset state in Dynamo
        const params = {
            TableName: TABLE,
            Key: {
                id: assetId
            },
            ConditionExpression: 'id = :id',  // if asset is not in Dynamo, this will make update fail - otherwise Dynamo will upsert by default
            UpdateExpression: 'SET #status = :status, ts = :ts',
            ExpressionAttributeNames: {
                '#status': 'status'    // status is a reserved word in Dynamo, necessitating this
            },
            ExpressionAttributeValues: {
                ':id': assetId,
                ':status': 'uploaded',
                ':ts': new Date().toISOString()
            }
        };
        await dynamo.update(params).promise();
        console.log(`Completed upload of asset ${assetId}`);
    }
    catch (err) {
        if (err.code === 'ConditionalCheckFailedException') {
            throw new NotFoundError(`Asset ${assetId} not found`);
        }
        else {
            throw err;
        }
    }
};

module.exports.delete = async (assetId) => {
    // Delete from S3
    await s3.deleteObject({ Bucket: BUCKET, Key: assetId }).promise();

    // Delete from Dynamo
    await dynamo.delete({ TableName: TABLE, Key: { id: assetId } }).promise();

    console.log(`Deleted asset ${assetId}`);
};

module.exports.get = async (assetId) => {
    const result = await dynamo.get({ TableName: TABLE, Key: { id: assetId } }).promise();
    const asset = result.Item;
    if (!asset) {
        throw new NotFoundError(`Asset ${assetId} not found`);
    }
    return asset;
};

module.exports.getUploadUrl = (assetId, ttl = 900) => {
    return s3.getSignedUrl('putObject', { Bucket: BUCKET, Key: assetId, Expires: ttl });
};

module.exports.getDownloadUrl = (assetId, ttl = 60) => {
    return s3.getSignedUrl('getObject', { Bucket: BUCKET, Key: assetId, Expires: ttl });
};

class NotFoundError extends Error {
    constructor(message) {
        super(message);

        this.name = this.constructor.name;
        this.code = 'NotFound';

        Error.captureStackTrace(this, this.constructor);
    }
}