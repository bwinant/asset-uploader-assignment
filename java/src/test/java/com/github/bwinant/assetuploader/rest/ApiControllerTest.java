package com.github.bwinant.assetuploader.rest;

import java.util.UUID;

import com.github.bwinant.assetuploader.Asset;
import com.github.bwinant.assetuploader.AssetException;
import com.github.bwinant.assetuploader.AssetNotFoundException;
import com.github.bwinant.assetuploader.AssetService;
import com.github.bwinant.assetuploader.rest.ApiController.CreateResult;
import com.github.bwinant.assetuploader.rest.ApiController.GetResult;
import com.github.bwinant.assetuploader.rest.ApiController.UpdateRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.github.bwinant.assetuploader.Asset.Status.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ApiControllerTest
{
    private final int timeout = 60;

    @Mock
    private AssetService assetService;

    private ApiController apiController;

    @Before
    public void setUp()
    {
        apiController = new ApiController(assetService, timeout, timeout);
    }

    @Test
    public void create() throws AssetException
    {
        UUID assetId = UUID.randomUUID();
        String uploadUrl = "https://example.com/something";

        when(assetService.createAsset()).thenReturn(assetId);
        when(assetService.getUploadUrl(assetId, timeout)).thenReturn(uploadUrl);

        CreateResult result = apiController.create();
        assertNotNull(result);
        assertEquals(assetId, result.getId());
        assertEquals(uploadUrl, result.getUploadUrl());
    }

    @Test(expected = InvalidRequestException.class)
    public void complete_noPostBody() throws AssetException
    {
        UUID assetId = UUID.randomUUID();
        UpdateRequest request = null;

        apiController.complete(assetId, request);
    }

    @Test(expected = InvalidRequestException.class)
    public void complete_nullStatus() throws AssetException
    {
        UUID assetId = UUID.randomUUID();
        UpdateRequest request = new UpdateRequest(null);

        apiController.complete(assetId, request);
    }

    @Test(expected = InvalidRequestException.class)
    public void complete_invalidStatus() throws AssetException
    {
        UUID assetId = UUID.randomUUID();
        UpdateRequest request = new UpdateRequest("fail!");

        apiController.complete(assetId, request);
    }

    @Test
    public void complete() throws AssetException
    {
        UUID assetId = UUID.randomUUID();
        UpdateRequest request = new UpdateRequest("uploaded");

        when(assetService.getAsset(assetId)).thenReturn(new Asset(assetId, created));

        apiController.complete(assetId, request);
    }

    @Test(expected = InvalidRequestException.class)
    public void get_nonNumericTimeout() throws AssetException
    {
        UUID assetId = UUID.randomUUID();

        apiController.get(assetId, "abc123");
    }

    @Test(expected = InvalidRequestException.class)
    public void get_zeroTimeout() throws AssetException
    {
        UUID assetId = UUID.randomUUID();

        apiController.get(assetId, "0");
    }

    @Test(expected = InvalidRequestException.class)
    public void get_negativeTimeout() throws AssetException
    {
        UUID assetId = UUID.randomUUID();

        apiController.get(assetId, "-2");
    }

    @Test(expected = InvalidRequestException.class)
    public void get_nonIntegerTimeout() throws AssetException
    {
        UUID assetId = UUID.randomUUID();

        apiController.get(assetId, "5.2");
    }

    //@Test(expected = InvalidRequestException.class)
    @Test(expected = AssetNotFoundException.class)
    public void get_assetNotUploaded() throws AssetException
    {
        UUID assetId = UUID.randomUUID();

        when(assetService.getAsset(assetId)).thenReturn(new Asset(assetId, created));

        apiController.get(assetId, null);
    }

    @Test
    public void get() throws AssetException
    {
        UUID assetId = UUID.randomUUID();
        String downloadUrl = "https://example.com/something";

        when(assetService.getAsset(assetId)).thenReturn(new Asset(assetId, uploaded));
        when(assetService.getDownloadUrl(assetId, timeout)).thenReturn(downloadUrl);

        GetResult result = apiController.get(assetId, null);
        assertNotNull(result);
        assertEquals(downloadUrl, result.getDownloadUrl());
    }

    @Test
    public void get_withTimeout() throws AssetException
    {
        UUID assetId = UUID.randomUUID();
        int timeout = 120;
        String downloadUrl = "https://example.com/something";

        when(assetService.getAsset(assetId)).thenReturn(new Asset(assetId, uploaded));
        when(assetService.getDownloadUrl(assetId, timeout)).thenReturn(downloadUrl);

        GetResult result = apiController.get(assetId, String.valueOf(timeout));
        assertNotNull(result);
        assertEquals(downloadUrl, result.getDownloadUrl());
    }
}