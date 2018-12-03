package com.github.bwinant.assetuploader;

import java.util.UUID;

public class Asset
{
    public enum Status
    {
        created,
        uploaded
    }

    private final UUID id;
    private final Status status;

    public Asset(UUID id, Status status)
    {
        this.id = id;
        this.status = status;
    }

    public UUID getId()
    {
        return id;
    }

    public Status getStatus()
    {
        return status;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final Asset asset = (Asset) o;
        return id.equals(asset.getId()) && status == asset.getStatus();
    }

    @Override
    public int hashCode()
    {
        return getId().hashCode();
    }

    @Override
    public String toString()
    {
        return id.toString();
    }
}
