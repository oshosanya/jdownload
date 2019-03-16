package com.oshosanya.jdownload.ui.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class UIDownload {
    public SimpleStringProperty fileName = new SimpleStringProperty();
    public SimpleIntegerProperty size = new SimpleIntegerProperty();
    public SimpleStringProperty status = new SimpleStringProperty();
    public SimpleDoubleProperty progress = new SimpleDoubleProperty();

    public String getFileName() {
        return fileName.get();
    }

    public SimpleStringProperty fileNameProperty() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName.set(fileName);
    }

    public int getSize() {
        return size.get();
    }

    public SimpleIntegerProperty sizeProperty() {
        return size;
    }

    public void setSize(int size) {
        this.size.set(size);
    }

    public String getStatus() {
        return status.get();
    }

    public SimpleStringProperty statusProperty() {
        return status;
    }

    public void setStatus(String status) {
        this.status.set(status);
    }

    public double getProgress() {
        return progress.get();
    }

    public SimpleDoubleProperty progressProperty() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress.set(progress);
    }
}
