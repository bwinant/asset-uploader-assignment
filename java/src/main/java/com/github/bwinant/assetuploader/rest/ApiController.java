package com.github.bwinant.assetuploader.rest;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.bwinant.assetuploader.Asset;
import com.github.bwinant.assetuploader.AssetException;
import com.github.bwinant.assetuploader.AssetNotFoundException;
import com.github.bwinant.assetuploader.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.github.bwinant.assetuploader.Asset.Status.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping("/asset")
public class ApiController
{
    private final AssetService assetService;
    private final int downloadTimeout;
    private final int uploadTimeout;

    @Autowired
    public ApiController(AssetService assetService,
                         @Value("${download.expires.secs:60}") int downloadTimeout,
                         @Value("${upload.expires.secs:900}") int uploadTimeout)
    {
        this.assetService = assetService;
        this.downloadTimeout = downloadTimeout;
        this.uploadTimeout = uploadTimeout;
    }

    @RequestMapping(value = "", method = POST)
    public CreateResult create()
        throws AssetException
    {
        UUID assetId = assetService.createAsset();
        String uploadUrl = assetService.getUploadUrl(assetId, uploadTimeout);
        return new CreateResult(assetId, uploadUrl);
    }

    @RequestMapping(value = "/{assetId}", method = PUT)
    public void complete(@PathVariable UUID assetId,
                         @RequestBody(required = false) UpdateRequest request)
        throws AssetException
    {
        // We could define @RequestBody to be required but it would be difficult to intercept the Spring exception
        // and return a nicer error message, so let's do it ourselves
        if (request == null || request.getStatus() == null)
        {
            throw new InvalidRequestException("Invalid request");
        }

        // We could let Jackson do the String to enum conversion, but again, we want nicer error messages
        Asset.Status status = null;
        try
        {
            status = Asset.Status.valueOf(request.getStatus());
        }
        catch (IllegalArgumentException e)
        {
            throw new InvalidRequestException("Invalid request");
        }

        // How to handle a Status other than 'uploaded' is not defined in requirements
        if (status != uploaded)
        {
            throw new InvalidRequestException("Invalid request");
        }

        Asset asset = assetService.getAsset(assetId);
        assetService.completeAsset(asset);
    }

    @RequestMapping(value = "/{assetId}", method = GET)
    public GetResult get(@PathVariable UUID assetId,
                         @RequestParam(name = "timeout", required = false) String timeoutValue)
        throws AssetException
    {
        // We could define the @RequestParam to be a numeric type and Spring would happily convert the string param
        // However if a non-numeric value was supplied, it would be difficult to intercept the NumberFormatException that Spring
        // would throw and return a nicer error message, so let's do it ourselves
        int timeout = downloadTimeout;
        if (timeoutValue != null)
        {
            try
            {
                timeout = Integer.parseInt(timeoutValue);
            }
            catch (NumberFormatException e)
            {
                throw new InvalidRequestException("Invalid timeout");
            }
        }
        if (timeout <= 0)
        {
            throw new InvalidRequestException("Invalid timeout");
        }

        Asset asset = assetService.getAsset(assetId);
        if (asset.getStatus() != uploaded)
        {
            //throw new InvalidRequestException("Asset " + assetId + " has not been uploaded");
            throw new AssetNotFoundException("Asset " + assetId + " not found");
        }

        String downloadUrl = assetService.getDownloadUrl(assetId, timeout);

        return new GetResult(downloadUrl);
    }

    @RequestMapping(value = "/{assetId}", method = DELETE)
    public void delete(@PathVariable UUID assetId)
    {
        assetService.deleteAsset(assetId);
    }


    public static class CreateResult
    {
        private final UUID id;
        private final String uploadUrl;

        public CreateResult(UUID id, String uploadUrl)
        {
            this.id = id;
            this.uploadUrl = uploadUrl;
        }

        public UUID getId()
        {
            return id;
        }

        @JsonProperty("upload_url")
        public String getUploadUrl()
        {
            return uploadUrl;
        }
    }

    public static class GetResult
    {
        private final String downloadUrl;

        public GetResult(String downloadUrl)
        {
            this.downloadUrl = downloadUrl;
        }

        @JsonProperty("Download_url")
        public String getDownloadUrl()
        {
            return downloadUrl;
        }
    }

    public static class UpdateRequest
    {
        private final String status;

        @JsonCreator
        public UpdateRequest(@JsonProperty("Status") String status)
        {
            this.status = status;
        }

        public String getStatus()
        {
            return status;
        }
    }
}
