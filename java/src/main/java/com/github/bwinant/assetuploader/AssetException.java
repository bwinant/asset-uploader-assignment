package com.github.bwinant.assetuploader;

public class AssetException extends Exception
{
    public AssetException(String message)
    {
        super(message);
    }

    public AssetException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
