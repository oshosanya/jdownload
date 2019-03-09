package com.oshosanya.jdownload.entity;

import javax.persistence.*;

@Entity

public class ChildDownload {
    @Id
    @GeneratedValue
    private int Id;
    private int StartLength;
    private int EndLength;
    private int BytesDownloaded = 0;
    private String FileName;

    @ManyToOne
    @JoinColumn(name="download_id")
    private Download download;

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public int getStartLength() {
        return StartLength;
    }

    public void setStartLength(int startLength) {
        StartLength = startLength;
    }

    public int getEndLength() {
        return EndLength;
    }

    public void setEndLength(int endLength) {
        EndLength = endLength;
    }

    public int getBytesDownloaded() {
        return BytesDownloaded;
    }

    public void setBytesDownloaded(int bytesDownloaded) {
        BytesDownloaded = bytesDownloaded;
    }

    public String getFileName() {
        return FileName;
    }

    public void setFileName(String fileName) {
        FileName = fileName;
    }

    public Download getDownload() {
        return download;
    }

    public void setDownload(Download download) {
        this.download = download;
    }
}
