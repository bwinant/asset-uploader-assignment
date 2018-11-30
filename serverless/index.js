'use strict';

const assets = require('./assets');

module.exports.createAsset = async (event) => {
    try {
        const assetId = await assets.create();
        const uploadUrl = assets.getUploadUrl(assetId);
        return success({ id: assetId, upload_url: uploadUrl });
    }
    catch (err) {
        return error(err);
    }
};

module.exports.completeAsset = async (event) => {
    const assetId = event.pathParameters.assetId;
    const body = JSON.parse(event.body || '{}');

    // How to handle a Status other than 'uploaded' is not defined in requirements
    if (body.Status !== 'uploaded') {
        return error('Invalid request');
    }

    try {
        await assets.complete(assetId);
        return success(); // Response for this is not defined in requirements
    }
    catch (err) {
        return error(err);
    }
};

module.exports.getAsset = async (event) => {
    const assetId = event.pathParameters.assetId;
    const queryParams = event.queryStringParameters || {};
    const timeout = queryParams.timeout;

    if (timeout !== undefined && (!Number.isInteger(timeout) || timeout <= 0)) {
        return error('Invalid timeout');
    }

    try {
        const asset = await assets.get(assetId);
        if (asset.status !== 'uploaded') {
            return error(`Asset ${assetId} has not been uploaded`);  // Error message for this response is not defined in requirements
        }

        const downloadUrl = assets.getDownloadUrl(assetId, timeout);
        return success({ Download_url: downloadUrl });
    }
    catch (err) {
        return error(err);
    }
};

// Added for completeness
module.exports.deleteAsset = async (event) => {
    const assetId = event.pathParameters.assetId;
    try {
        await assets.delete(assetId);
        return success();
    }
    catch (err) {
        return error(err);
    }
};


const success = (obj) => {
    return apigResponse(obj, 200);
};

const error = (err) => {
    let message;
    let sc = 500;

    if (err instanceof Error)  {
        console.log(err.stack);
        message = err.message;
    }
    else if (err.message) {
        console.log(err.message);
        message = err.message;
    }
    else {
        console.log(err);
        message = err;
    }

    if (err.code === 'NotFound') {
        sc = 404;
    }

    return apigResponse(message, sc);
};

const apigResponse = (obj, sc) => {
    // In real world scenario would lock down CORS headers
    return {
        statusCode: sc,
        headers: {
            'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify(obj)
    };
};