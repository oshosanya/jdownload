package com.oshosanya.jdownload.entity;

import com.oshosanya.jdownload.constant.DownloadStatus;

import javax.persistence.*;
import java.util.Collection;
import java.util.Observable;

@Entity
public class Download {
    @Id
    @GeneratedValue
    private int Id;
    private String Url;
    private String FileName;
    private int BytesDownloaded;
    private int ContentLength;

    @OneToMany(mappedBy = "download", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Collection<ChildDownload> childDownloads;

    @Enumerated(EnumType.STRING)
    @Column(name="status")
    private DownloadStatus status = DownloadStatus.READY;

    private boolean done = false;

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
        return status;
    }

    public void setStatus(DownloadStatus status) {
        this.status = status;
    }

    public Collection<ChildDownload> getChildDownloads() {
        return childDownloads;
    }

    public void setChildDownloads(Collection<ChildDownload> childDownloads) {
        this.childDownloads = childDownloads;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
