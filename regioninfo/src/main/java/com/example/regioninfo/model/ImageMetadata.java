package com.example.regioninfo.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "images")
public class ImageMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private long size;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModified;

    private String contentType;

    private String s3Key;

    private String downloadUrl;

    // Constructors
    public ImageMetadata() {}

    public ImageMetadata(String name, long size, Date lastModified) {
        this.name = name;
        this.size = size;
        this.lastModified = lastModified;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public String getContentType() {
        return contentType;
    }

    public String getS3Key() {
        return s3Key;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
