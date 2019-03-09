package com.oshosanya.jdownload;

import com.oshosanya.jdownload.entity.ChildDownload;
import com.oshosanya.jdownload.entity.Download;
import com.oshosanya.jdownload.repository.DownloadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.commons.io.FilenameUtils;

import java.net.URL;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class DownloadTask {
    @Autowired
    private DownloadRepository downloadRepository;

    public void start() {
        Iterable<Download> downloads = downloadRepository.findAll();
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        downloads.forEach(download -> {
            executor.submit(() -> {
                System.out.print("Download url is: \n");
                System.out.println(download.getUrl());
                this.downloadFile(download);
            });
        });
    }

    private void downloadFile(Download download) {
        try {
            URL url = new URL(download.getUrl());
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestMethod("HEAD");
            long contentLength = httpConnection.getContentLengthLong();
            ArrayList<Map> limits;
            limits = getUpperAndLowerDownloadLimits(contentLength);
            System.out.printf("Content Length is %d \n", contentLength);
            System.out.println(limits);
            this.createChildDownloads(download, limits, url);
        } catch (Exception e) {
            System.out.println("Could not connect to server");
            System.out.println(e.getMessage());
        }
    }

    private void createChildDownloads(Download download, ArrayList<Map> limits, URL url) {
        ArrayList<ChildDownload> childDownloads = new ArrayList<>();
        limits.forEach(limit -> {
            ChildDownload childDownload = new ChildDownload();
            childDownload.setStartLength((int)limit.get("upper-limit"));
            childDownload.setEndLength((int)limit.get("lower-limit"));
            childDownload.setFileName(FilenameUtils.getName(url.getPath()));
            download.getChildDownloads().add(childDownload);
//            childDownloads.add(childDownload);
        });
//        download.setChildDownloads(childDownloads);
//        downloadRepository.save(download);
    }

    private ArrayList<Map> getUpperAndLowerDownloadLimits(long contentLength)
    {
        int numOfThreads = 4;
        int maxContentLengthPerThread = (int)contentLength/numOfThreads;

        int newUpperLimit = maxContentLengthPerThread + 1;
        ArrayList<Map> limits = new ArrayList<>();
        for (int i=0; i<numOfThreads; i++) {
            Map<String, Integer> limit = new HashMap<>();
            if (i == 0) {
                limit.put("upper-limit", 0);
                limit.put("lower-limit", maxContentLengthPerThread);
                limits.add(limit);
            } else if (i == numOfThreads - 1) {
                limit.put("upper-limit", newUpperLimit);
                limit.put("lower-limit", (int) contentLength);
                limits.add(limit);
            } else {
                limit.put("upper-limit", newUpperLimit);
                limit.put("lower-limit", newUpperLimit + maxContentLengthPerThread);
                limits.add(limit);
                newUpperLimit = newUpperLimit + maxContentLengthPerThread + 1;
            }
        }
        return limits;
    }
}
