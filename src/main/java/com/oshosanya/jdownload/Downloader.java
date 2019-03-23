package com.oshosanya.jdownload;

import com.oshosanya.jdownload.constant.DownloadStatus;
import com.oshosanya.jdownload.entity.ChildDownload;
import com.oshosanya.jdownload.entity.Download;
import com.oshosanya.jdownload.repository.ChildDownloadRepository;
import com.oshosanya.jdownload.repository.DownloadRepository;
import com.oshosanya.jdownload.ui.JdownloadUI;
import com.oshosanya.jdownload.ui.controller.Root;
import com.oshosanya.jdownload.util.IOCopier;
import javafx.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;

@Component
public class Downloader {
    @Autowired
    private DownloadRepository downloadRepository;

    @Autowired
    private ApplicationContext context;

    private ArrayBlockingQueue<Download> downloadQueue;

    Logger logger = LoggerFactory.getLogger(Downloader.class);
    final private int queueCapacity = 6;


    public Downloader() {
        this.downloadQueue = new ArrayBlockingQueue<>(this.queueCapacity);
    }

    public void start(String... args) {
        Iterable<Download> downloads = downloadRepository.findAll();
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        downloads.forEach(download -> {
            this.addDownload(download);
        });
//        Future ui = executor.submit(() -> {
//            Application.launch(JdownloadUI.class, args);
//        });
        Future populateQueue = executor.submit(() -> {
            this.populateQueue();
        });
        Future createWorkers = executor.submit(() -> {
            this.createWorkers();
        });

        try {
//            ui.get();
            //TODO perform graceful shutdown
//            System.exit(1);
            populateQueue.get();
            createWorkers.get();
        } catch (Exception e) {
            System.out.printf("Exception thrown: %s \n", e.getMessage());
        }
    }

    private void populateQueue() {
        List<Download> downloads = downloadRepository.findByStatusAndDone(DownloadStatus.RUNNING, false);
        downloads.forEach(download -> {
            //Add running downloads to queue
            try {
                this.downloadQueue.put(download);
                System.out.printf("Added %s to queue\n", download.getFileName());
            } catch (Exception e) {
//                System.out.printf("Queue threw exception: %s \n", e.getMessage());
            }
        });

        while (true) {
            List<Download> otherDownloads = downloadRepository.findByStatusAndDone(DownloadStatus.READY, false);
            otherDownloads.forEach(download -> {
                try {
                    this.downloadQueue.offer(download, 1, TimeUnit.SECONDS);
                    download.setStatus(DownloadStatus.RUNNING);
                    downloadRepository.save(download);
                    System.out.printf("Added %s to queue \n", download.getFileName());
                } catch (Exception e) {
//                    System.out.printf("Queue threw exception: %s \n", e.getMessage());
                }
            });
//            try {
//                Thread.sleep(5000);
//            } catch (Exception e) {
//                System.out.println(e.getMessage());
//            }
        }
    }

    private void createWorkers() {
        final int NUMBER_OF_WORKERS = 4;
        ThreadPoolExecutor workers = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUMBER_OF_WORKERS);
        while (true) {
            try {
                Download download = this.downloadQueue.poll(2, TimeUnit.SECONDS);
                if (download == null) {
                    continue;
                }
                System.out.println("Creating task");
                workers.submit(() -> {
                    DownloadTask downloadTask = (DownloadTask) context.getBean("downloadTaskPrototype");
                    downloadTask.setDownload(download);
                    downloadTask.startDownload();
                });
            } catch (Exception e) {
//                logger.error(e);
                e.printStackTrace();
                System.exit(0);
                System.out.printf("Queue threw exception: %s \n", e.getLocalizedMessage());
            }
        }
    }

    private void addDownload(Download download) {
        try {
            URL url = new URL(download.getUrl());
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestMethod("HEAD");
            long contentLength = httpConnection.getContentLengthLong();
            download.setContentLength((int)contentLength);
            downloadRepository.save(download);
            ArrayList<Map> limits;
            limits = getUpperAndLowerDownloadLimits(contentLength);
            this.createChildDownloads(download, limits, url);

        } catch (Exception e) {
            System.out.println("Could not connect to server");
            System.out.println(e.getMessage());
        }
    }

    private void createChildDownloads(Download download, ArrayList<Map> limits, URL url) {
        Download newDownload = downloadRepository.findById(download.getId()).orElse(new Download());
        int index = 0;
        for(Map limit : limits) {
            ChildDownload childDownload = new ChildDownload();
            childDownload.setStartLength((int)limit.get("upper-limit"));
            childDownload.setEndLength((int)limit.get("lower-limit"));
            childDownload.setFileName(FilenameUtils.getBaseName(url.getPath()) + index + "." + FilenameUtils.getExtension(url.getPath()));
            childDownload.setDownload(download);

            newDownload.getChildDownloads().add(childDownload);
            index++;
        }

        downloadRepository.save(newDownload);
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

    public static String generateFileName(String fileName) {
        File file = new File(fileName);
        if (!file.exists() && !file.isDirectory()) {
            return fileName;
        }
        File newFile = null;
        if (file.exists() && !file.isDirectory()) {
            int index = 1;
            while(true) {
                newFile = new File(FilenameUtils.getBaseName(fileName) + "-" + index + "." + FilenameUtils.getExtension(fileName));
//                System.out.printf("Checking if file %s exists \n", newFile.getPath());
                if (!newFile.exists()) {
                    break;
                }
                index++;
            }
        }
        return newFile.getPath();
    }


}
