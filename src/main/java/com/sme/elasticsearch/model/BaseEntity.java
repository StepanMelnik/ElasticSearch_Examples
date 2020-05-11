package com.sme.elasticsearch.model;

import java.io.Serializable;

/**
 * Base entity.
 */
public class BaseEntity implements Serializable
{
    protected int id;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }
}
