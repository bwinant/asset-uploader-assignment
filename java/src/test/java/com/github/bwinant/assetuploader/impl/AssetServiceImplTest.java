package com.github.bwinant.assetuploader.impl;

import java.util.UUID;

import com.amazonaws.services.s3.AmazonS3Client;
import com.github.bwinant.assetuploader.Asset;
import com.github.bwinant.assetuploader.AssetException;
import com.github.bwinant.assetuploader.AssetNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.github.bwinant.assetuploader.Asset.Status.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AssetServiceImplTest
{
    private final String bucket = "testing123";

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private AmazonS3Client s3Client;

    private AssetServiceImpl assetService;

    @Before
    public void setUp()
    {
        assetService = new AssetServiceImpl(jdbcTemplate, s3Client, bucket);
    }

    @Test
    public void completeAsset() throws AssetException
    {
        UUID assetId = UUID.randomUUID();
        Asset asset = new Asset(assetId, created);

        when(s3Client.doesObjectExist(bucket, assetId.toString())).thenReturn(true);
        when(jdbcTemplate.update(anyString(), eq(uploaded.toString()))).thenReturn(1);

        assetService.completeAsset(asset);
    }

    @Test(expected = AssetException.class)
    public void completeAsset_notInS3() throws AssetException
    {
        UUID assetId = UUID.randomUUID();
        Asset asset = new Asset(assetId, created);

        when(s3Client.doesObjectExist(bucket, assetId.toString())).thenReturn(false);

        assetService.completeAsset(asset);
    }

    @Test(expected = AssetException.class)
    public void completeAsset_alreadyCompleted() throws AssetException
    {
        UUID assetId = UUID.randomUUID();
        Asset asset = new Asset(assetId, uploaded);

        when(s3Client.doesObjectExist(bucket, assetId.toString())).thenReturn(true);

        assetService.completeAsset(asset);
    }

    @Test(expected = AssetNotFoundException.class)
    public void completeAsset_notInDb() throws AssetException
    {
        UUID assetId = UUID.randomUUID();
        Asset asset = new Asset(assetId, created);

        when(s3Client.doesObjectExist(bucket, assetId.toString())).thenReturn(true);
        when(jdbcTemplate.update(anyString(), eq(uploaded.toString()))).thenReturn(0);

        assetService.completeAsset(asset);
    }
}