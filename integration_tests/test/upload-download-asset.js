'use strict';

const fs = require('fs');
const url = require('url');
const expect = require('chai').expect;
const request = require('supertest');

const baseUrl = process.env.BASE_URL || 'http://localhost:8080';
const testFile = fs.readFileSync('./test/file.jpg');
const invalidId = '9784c04d-598c-4e69-87f6-0f2eae377fba';

const createAsset = () => {
    return request(baseUrl).post(`/asset`);
};

const completeAsset = (assetId, body) => {
    return request(baseUrl).put(`/asset/${assetId}`).send(body);
};

const getAsset = (assetId) => {
    return request(baseUrl).get(`/asset/${assetId}`);
};

const deleteAsset = (assetId) => {
    return request(baseUrl).delete(`/asset/${assetId}`);
};

const parseUrl = (s) => {
    const u = url.parse(s);
    return {
        host: `${u.protocol}//${u.host}`,
        path: `${u.pathname}${u.search}`
    };
};

const uploadFile = (uploadUrl, buffer) => {
    const { host, path } = parseUrl(uploadUrl);

    // Requirements say "user should be able to make a POST call to the s3 signed url to upload the asset"
    // but S3 pre-signed upload URLs require a PUT not a POST
    return request(host).put(path).send(buffer);
};

const downloadFile = (downloadUrl) => {
    const { host, path } = parseUrl(downloadUrl);

    const binaryParser = (res, cb) => {
        res.data = '';
        res.setEncoding('binary');

        res.on('data', (chunk) => {
            res.data += chunk;
        });

        res.on('end', () => {
            cb(null, new Buffer(res.data, 'binary'))
        });
    };
    
    return request(host).get(path)
            .buffer()
            .parse(binaryParser);
};


describe('Asset Uploader Service Upload/Download Tests', function() {
    let assetId, uploadUrl, downloadUrl;

    it('Create new asset', async function() {
        const res = await createAsset()
                .expect(200)
                .then(res => res.body);
        expect(res).to.be.an('object');
        expect(res).to.have.property('id').and.to.be.a('string');
        expect(res).to.have.property('upload_url').and.to.be.a('string');
        assetId = res.id;
        uploadUrl = res.upload_url;
    });

    it('Verify pre-signed upload URL', async function() {
        const res = await uploadFile(uploadUrl, testFile)
                .expect(200)
                .then(res => res.body);
    });

    it('Mark asset upload as completed', async function() {
        const res = await completeAsset(assetId, { Status: 'uploaded' })
                .expect(200)
                .then(res => res.body);
    });

    it('Retrieve asset pre-signed download URL', async function() {
        const res = await getAsset(assetId)
                .expect(200)
                .then(res => res.body);
        expect(res).to.be.an('object');
        expect(res).to.have.property('Download_url').and.to.be.a('string');
        downloadUrl = res.Download_url
    });

    it('Verify pre-signed download URL', async function() {
        const res = await downloadFile(downloadUrl)
                .expect(200)
                .then(res => res.body);
        expect(testFile.equals(res)); // testFile and res.body are Node.js Buffers
    });

    it('Delete asset', async function() {
        const res = await deleteAsset(assetId)
                .expect(200)
                .then(res => res.body);
    });

    it('Verify deleted asset no longer retrievable', async function() {
        let res;

        res = await getAsset(assetId)
                .expect(404)
                .then(res => res.body);
        expect(res).to.be.an('object');
        expect(res).to.have.property('error', `Asset ${assetId} not found`);

        // Depending on AWS credentials used, this could return a 403 or 404 - so let's just check for not 200
        res = await downloadFile(downloadUrl)
                .then(res => res.status);
        expect(res).to.not.equal(200);
    });

    describe('Upload Completed Error Cases', function() {
        before(async function() {
            const res = await createAsset().expect(200).then(res => res.body);
            assetId = res.id;
            uploadUrl = res.upload_url;
        });

        after(async function() {
            await deleteAsset(assetId).expect(200);
        });

        it('Attempt to mark non-existent asset upload as completed', async function() {
            const res = await completeAsset(invalidId, { Status: 'uploaded' })
                    .expect(404)
                    .then(res => res.body);
            expect(res).to.be.an('object');
            expect(res).to.have.property('error', `Asset ${invalidId} not found`);
        });

        it('Attempt to mark asset upload as completed with invalid request', async function() {
             let res;

             res = await completeAsset(assetId)
                     .expect(500)
                     .then(res => res.body);
            expect(res).to.be.an('object');
            expect(res).to.have.property('error', 'Invalid request');

            res = await completeAsset(assetId, { Status: 'something other than uploaded' })
                    .expect(500)
                    .then(res => res.body);
            expect(res).to.be.an('object');
            expect(res).to.have.property('error', 'Invalid request');
        });

        it('Attempt to mark asset upload as completed even though no upload was performed', async function() {
            const res = await completeAsset(assetId, { Status: 'uploaded' })
                    .expect(404)
                    .then(res => res.body);
            expect(res).to.be.an('object');
            expect(res).to.have.property('error', `Asset ${assetId} not found`);
        });

        it('Attempt to mark asset upload as completed more than once', async function() {
            let res;

            res = await uploadFile(uploadUrl, testFile)
                    .expect(200)
                    .then(res => res.body);

            res = await completeAsset(assetId, { Status: 'uploaded' })
                    .expect(200)
                    .then(res => res.body);

            res = await completeAsset(assetId, { Status: 'uploaded' })
                    .expect(500)
                    .then(res => res.body);
            expect(res).to.be.an('object');
            expect(res).to.have.property('error', `Upload of asset ${assetId} is already completed`);
        });
    });

    describe('Retrieve Asset Error Cases', function() {
        before(async function() {
            const res = await createAsset().expect(200).then(res => res.body);
            assetId = res.id;
        });

        after(async function() {
            await deleteAsset(assetId).expect(200);
        });

        it('Attempt to retrieve pre-signed download URL for non-existent asset', async function() {
            const res = await getAsset(invalidId)
                    .expect(404)
                    .then(res => res.body);
            expect(res).to.be.an('object');
            expect(res).to.have.property('error', `Asset ${invalidId} not found`);
        });

        it('Attempt to retrieve pre-signed download URL for asset that does not have a completed upload', async function() {
            const res = await getAsset(assetId)
                    .expect(404)
                    .then(res => res.body);
            expect(res).to.be.an('object');
            expect(res).to.have.property('error', `Asset ${assetId} not found`);
        });

        it('Attempt to retrieve pre-signed download URL with invalid timeout', async function() {
            let res;

            res = await getAsset(assetId)
                    .query({ timeout: 'not a number'})
                    .expect(500)
                    .then(res => res.body);
            expect(res).to.be.an('object');
            expect(res).to.have.property('error', 'Invalid timeout');

            res = await getAsset(assetId)
                    .query({ timeout: 0})
                    .expect(500)
                    .then(res => res.body);
            expect(res).to.be.an('object');
            expect(res).to.have.property('error', 'Invalid timeout');

            res = await getAsset(assetId)
                    .query({ timeout: -1})
                    .expect(500)
                    .then(res => res.body);
            expect(res).to.be.an('object');
            expect(res).to.have.property('error', 'Invalid timeout');
        });
    });

    describe('Delete Asset Edge Cases', function() {
        before(async function() {
            const res = await createAsset().expect(200).then(res => res.body);
            assetId = res.id;
        });

        it('Deleting non-existent asset does not fail', async function() {
            const res = await deleteAsset(invalidId)
                    .expect(200)
                    .then(res => res.body);
        });

        it('Delete asset that does not have a completed upload does not fail', async function() {
            const res = await deleteAsset(assetId)
                .expect(200)
                .then(res => res.body);
        });
    });
});