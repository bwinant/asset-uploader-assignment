Asset Uploader Integration Tests
=========================

Asset uploader tests written in [Mocha](https://mochajs.org/)

### Requirements

- Node.js 8+
- NPM 5+
- Mocha (`npm install -g mocha`) 


### Build
 
```
npm install 
```

### Run
```
env BASE_URL=<asset uploader API URL> mocha
```

If not specified `BASE_URL` will default to `http://localhost:8080`. 
To test the serverless implementation set `BASE_URL` to the API Gateway URL: `https://<API Gateway ID>.execute-api.<region>.amazonaws.com/<stage>`, 
By default `<stage>` will be `dev` 
