package com.oshosanya.jdownload.ui.controller;

import com.oshosanya.jdownload.Downloader;
import com.oshosanya.jdownload.constant.DownloadStatus;
import com.oshosanya.jdownload.entity.Download;
import com.oshosanya.jdownload.ui.JdownloadUI;
import com.oshosanya.jdownload.ui.model.UIDownload;
import com.oshosanya.jdownload.repository.DownloadRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

@Component
public class Root implements Initializable {
    public AnchorPane rootContainer;
    public TableView downloadsTable;
    public TableColumn name;
    public TableColumn size;
    public TableColumn status;
    public TableColumn progress;


    private ObservableList<UIDownload> UIDownloads;
    private Map<String, DownloadPlaceHolder> downloads = new HashMap<>();

    @Autowired
    private DownloadRepository downloadRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private JdownloadUI ui;

    @Autowired
    private Downloader downloader;

    @Override
    public void initialize(URL resource, ResourceBundle resources) {
//        rootContainer.
        downloadsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        name.setCellValueFactory(new PropertyValueFactory<UIDownload,String>("fileName"));
        size.setCellValueFactory(new PropertyValueFactory<UIDownload,String>("size"));
        status.setCellValueFactory(new PropertyValueFactory<UIDownload,String>("status"));
        progress.setCellValueFactory(new PropertyValueFactory<>("progress"));
        progress.setCellFactory(ProgressBarTableCell.forTableColumn());
        buildDownloadsData();
    }

    private void insertListItem(Download download) {
        int listIndex = this.UIDownloads.size();
        this.downloads.put(download.getId() + download.getUrl(), new DownloadPlaceHolder(listIndex, download.getId(), download.getUrl()));
        UIDownload uiDownload = new UIDownload();
        uiDownload.setFileName(download.getFileName());
        uiDownload.setSize(FileUtils.byteCountToDisplaySize(download.getContentLength()));
        uiDownload.setStatus(download.getStatus().toString());
        if (download.getBytesDownloaded() == 0) {
            uiDownload.setProgress(0);
        } else {
            uiDownload.setProgress((float)download.getBytesDownloaded()/(float)download.getContentLength());
        }
        this.UIDownloads.add(uiDownload);
    }

    public void buildDownloadsData() {
        this.UIDownloads = FXCollections.observableArrayList();
        Iterable<Download> downloads = downloadRepository.findAll();
//        int index = 0;
        for (Download d : downloads) {
            this.insertListItem(d);
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
    protected void handleAddDownload(ActionEvent event) {
//        JdownloadUI ui = (JdownloadUI)applicationContext.getBean("jdownloadui");
        TextInputDialog dialog = new TextInputDialog();
        dialog.setContentText("Enter File URL: ");
        dialog.setTitle("New Download");
        dialog.setHeaderText("New Download");

        dialog.initOwner(ui.getPrimaryStage());
//        dialog.getDialogPane().setMinHeight(30.00);
//        dialog.getDialogPane().setMinWidth(100.00);
        dialog.setResizable(true);
//        dialog.setHeight(30.00);
//        dialog.setWidth(100.00);
//        dialog.setHeaderText("Look, a Text Input Dialog");
        Optional<String> url = dialog.showAndWait();
        if (url.isPresent()){
            Download tempDownload = new Download();
            tempDownload.setUrl(url.get());
            tempDownload.setStatus(DownloadStatus.SUSPENDED);
            dialog.hide();
            try {
                Download download = downloader.addDownload(tempDownload);
                this.insertListItem(download);
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Oops");
                alert.setContentText("Unable to add download!");
                alert.initOwner(ui.getPrimaryStage());
                alert.setResizable(true);
                alert.showAndWait();
            }
        }
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
