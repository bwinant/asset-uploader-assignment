package com.github.bwinant.assetuploader.impl;

import java.net.URL;
import java.util.Date;
import java.util.UUID;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.github.bwinant.assetuploader.Asset;
import com.github.bwinant.assetuploader.Asset.Status;
import com.github.bwinant.assetuploader.AssetException;
import com.github.bwinant.assetuploader.AssetNotFoundException;
import com.github.bwinant.assetuploader.AssetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import static com.github.bwinant.assetuploader.Asset.Status.*;

@Service
public class AssetServiceImpl implements AssetService
{
    private static final Logger log = LoggerFactory.getLogger(AssetServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final AmazonS3 s3Client;
    private final String bucketName;

    @Autowired
    public AssetServiceImpl(JdbcTemplate jdbcTemplate, AmazonS3 s3Client, @Value("${asset.bucket}") String bucketName)
    {
        this.jdbcTemplate = jdbcTemplate;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public Asset getAsset(UUID assetId) throws AssetException
    {
        try
        {
            return jdbcTemplate.queryForObject(
                "SELECT status FROM assets WHERE id = ?",
                new Object[]{assetId},
                (rs, i) -> new Asset(assetId, Status.valueOf(rs.getString("status")))
            );
        }
        catch (EmptyResultDataAccessException e)
        {
            throw new AssetNotFoundException("Asset " + assetId + " not found");
        }
    }

    @Override
    public UUID createAsset()
    {
        // Will use UUIDs for identifying assets.
        // Reason is to prevent users from poking at API URLs: if GET /asset/1 works, there is a good chance GET asset/2 does too
        UUID assetId = UUID.randomUUID();

        // Track asset upload state in PostgreSQL
        jdbcTemplate.update("INSERT INTO assets (id) VALUES(?)", assetId);
        log.debug("Initialized asset {}", assetId);
        return assetId;
    }

    @Override
    public void completeAsset(Asset asset) throws AssetException
    {
        UUID assetId = asset.getId();

        // Verify asset was actually uploaded
        boolean exists = s3Client.doesObjectExist(bucketName, assetId.toString());
        if (!exists)
        {
            throw new AssetException("Asset " + assetId + " has not been uploaded");
        }

        // Check if asset was already completed
        if (asset.getStatus() == uploaded)
        {
            throw new AssetException("Upload of asset " + assetId + " is already completed");
        }

        // Update asset state in PostgreSQL
        int count = jdbcTemplate.update("UPDATE assets SET status = ?, ts = NOW()", uploaded.toString());
        if (count == 0)
        {
            throw new AssetNotFoundException("Asset " + assetId + " not found");
        }

        log.debug("Completed uploaded of asset {}", assetId);
    }

    @Override
    public void deleteAsset(UUID assetId)
    {
        s3Client.deleteObject(bucketName, assetId.toString());
        jdbcTemplate.update("DELETE FROM assets WHERE id = ?", assetId);
        log.debug("Deleted asset {}", assetId);
    }

    @Override
    public String getUploadUrl(UUID assetId, long expires)
    {
        // Requirements say "user should be able to make a POST call to the s3 signed url to upload the asset"
        // but S3 pre-signed upload URLs require a PUT not a POST
        return getPresignedUrl(assetId, HttpMethod.PUT, expires);
    }

    @Override
    public String getDownloadUrl(UUID assetId, long expires)
    {
        return getPresignedUrl(assetId, HttpMethod.GET, expires);
    }

    private String getPresignedUrl(UUID assetId, HttpMethod method, long expires)
    {
        Date expiration = new Date(System.currentTimeMillis() + (expires * 1000));

        GeneratePresignedUrlRequest request =
            new GeneratePresignedUrlRequest(bucketName, assetId.toString())
                .withMethod(method)
                .withExpiration(expiration);

        URL url = s3Client.generatePresignedUrl(request);
        return url.toString();
    }
}
