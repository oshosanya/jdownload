package com.oshosanya.jdownload.ui.controller;

import com.oshosanya.jdownload.entity.Download;
import com.oshosanya.jdownload.ui.model.UIDownload;
import com.oshosanya.jdownload.repository.DownloadRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

@Component
public class Root implements Initializable {
    public TableView downloadsTable;
    public TableColumn name;
    public TableColumn size;
    public TableColumn status;
    public TableColumn progress;


    private ObservableList<UIDownload> UIDownloads;
    private Map<String, DownloadPlaceHolder> downloads = new HashMap<>();

    @Autowired
    private DownloadRepository downloadRepository;

    @Override
    public void initialize(URL resource, ResourceBundle resources) {
        downloadsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        name.setCellValueFactory(new PropertyValueFactory<UIDownload,String>("fileName"));
        size.setCellValueFactory(new PropertyValueFactory<UIDownload,String>("size"));
        status.setCellValueFactory(new PropertyValueFactory<UIDownload,String>("status"));
        progress.setCellValueFactory(new PropertyValueFactory<>("progress"));
        progress.setCellFactory(ProgressBarTableCell.forTableColumn());
        buildDownloadsData();
    }

    public void buildDownloadsData() {
        this.UIDownloads = FXCollections.observableArrayList();
        Iterable<Download> downloads = downloadRepository.findAll();
        int index = 0;
        for (Download d : downloads) {
            this.downloads.put(d.getId() + d.getUrl(), new DownloadPlaceHolder(index, d.getId(), d.getUrl()));
            UIDownload uiDownload = new UIDownload();
            uiDownload.setFileName(d.getFileName());
            uiDownload.setSize(d.getContentLength());
            uiDownload.setStatus(d.getStatus().toString());
            if (d.getBytesDownloaded() == 0) {
                uiDownload.setProgress(0);
            } else {
                uiDownload.setProgress((float)d.getBytesDownloaded()/(float)d.getContentLength());
            }
            this.UIDownloads.add(index, uiDownload);
            index++;
        }
//        downloads.forEach(d -> {
//            this.downloads.put(Integer.toString(d.getId()), d);
//        });
//        downloads.forEach(d -> {
//            UIDownload uiDownload = new UIDownload();
//            uiDownload.setFileName(d.getFileName());
//            uiDownload.setSize(d.getContentLength());
//            uiDownload.setStatus(d.getStatus().toString());
//            if (d.getBytesDownloaded() == 0) {
//                uiDownload.setProgress(0);
//                System.out.println(0);
//            } else {
//                uiDownload.setProgress((float)d.getBytesDownloaded()/(float)d.getContentLength());
//                System.out.println((float)d.getBytesDownloaded()/(float)d.getContentLength());
//            }
//            this.UIDownloads.add(uiDownload);
//        });
        downloadsTable.setItems(this.UIDownloads);
    }

    //TODO Add update, add and delete functions to update table observable list;

    @FXML
    protected void handleModDownload(ActionEvent event) {
        double progress = this.UIDownloads.get(0).getProgress();
        this.UIDownloads.get(0).setProgress(progress + 0.1);
    }

    public void updateDownloadItem(Download download) {
//        System.out.printf("Updating %s \n", download.getFileName());
        DownloadPlaceHolder d = this.downloads.get(download.getId() + download.getUrl());
//        System.out.printf("Got %s \n", this.UIDownloads.get(d.getIndex()).getFileName());
        this.UIDownloads.get(d.getIndex()).setProgress((float)download.getBytesDownloaded()/(float)download.getContentLength());
//        System.out.println(download.getFileName());
//        System.out.printf("Bytes download: %d \n", download.getBytesDownloaded());
//        System.out.printf("Content length: %d \n", download.getContentLength());
        this.UIDownloads.get(d.getIndex()).setStatus(download.getStatus().toString());
    }

    private class DownloadPlaceHolder {
        int id;
        int index;
        String url;

        public DownloadPlaceHolder(int index, int id, String url) {
            this.id = id;
            this.url = url;
            this.index = index;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }
}
