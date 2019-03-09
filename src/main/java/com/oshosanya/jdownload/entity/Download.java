package com.oshosanya.jdownload.entity;

import javax.persistence.*;
import java.util.List;

enum DownloadStatus
{
    READY, RUNNING, COMPLETED;
}

@Entity
public class Download {
    @Id
    @GeneratedValue
    private int Id;
    private String Url;
    private String FileName;
    private int BytesDownloaded;
    private int ContentLength;

    @OneToMany(mappedBy = "download", fetch = FetchType.EAGER)
    private List<ChildDownload> childDownloads;

    @Enumerated(EnumType.STRING)
    private DownloadStatus Status;

    public Download() {}

    public Download(String Url, String FileName, int ContentLength) {
        this.Url = Url;
        this.FileName = FileName;
        this.BytesDownloaded = 0;
        this.ContentLength = ContentLength;
    }
    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getUrl() {
        return Url;
    }

    public void setUrl(String url) {
        Url = url;
    }

    public String getFileName() {
        return FileName;
    }

    public void setFileName(String fileName) {
        FileName = fileName;
    }

    public int getBytesDownloaded() {
        return BytesDownloaded;
    }

    public void setBytesDownloaded(int bytesDownloaded) {
        BytesDownloaded = bytesDownloaded;
    }

    public int getContentLength() {
        return ContentLength;
    }

    public void setContentLength(int contentLength) {
        ContentLength = contentLength;
    }

    public DownloadStatus getStatus() {
        return Status;
    }

    public void setStatus(DownloadStatus status) {
        Status = status;
    }

    public List<ChildDownload> getChildDownloads() {
        return childDownloads;
    }

    public void setChildDownloads(List<ChildDownload> childDownloads) {
        this.childDownloads = childDownloads;
    }
}
