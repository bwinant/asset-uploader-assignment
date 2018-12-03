package com.github.bwinant.assetuploader;

import java.util.UUID;

public interface AssetService
{
    Asset getAsset(UUID assetId) throws AssetException;

    UUID createAsset() throws AssetException;

    void completeAsset(Asset asset) throws AssetException;

    /**
     * Deletes the asset.
     * If the asset does not exist, no exceptions are thrown
     *
     * @param assetId the asset to delete
     */
    void deleteAsset(UUID assetId);

    /**
     * Returns a pre-signed PUT URL to download an asset from S3
     *
     * @param assetId the asset id
     * @param expires amount of time in milliseconds before the URL expires
     *
     * @return a pre-signed upload URL
     */
    String getUploadUrl(UUID assetId, long expires);

    /**
     * Returns a pre-signed GET URL to download an asset from S3
     *
     * @param assetId the asset id
     * @param expires amount of time in milliseconds before the URL expires
     *
     * @return a pre-signed download URL
     */
    String getDownloadUrl(UUID assetId, long expires);
}
