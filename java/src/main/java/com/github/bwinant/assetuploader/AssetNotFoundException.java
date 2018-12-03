package com.github.bwinant.assetuploader;

public class AssetNotFoundException extends AssetException
{
    public AssetNotFoundException(String message)
    {
        super(message);
    }

    public AssetNotFoundException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
