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
public class DownloadTask {
    @Autowired
    private DownloadRepository downloadRepository;
    @Autowired
    private ChildDownloadRepository childDownloadRepository;

    @Autowired
    private ApplicationContext context;

    private ArrayBlockingQueue<ChildDownload> downloadQueue;
    private ArrayBlockingQueue<ChildDownload> fileAssemblyQueue;
    final private int queueCapacity = 6;
    static Semaphore semaphore = new Semaphore(1);

    public DownloadTask() {
        this.downloadQueue = new ArrayBlockingQueue<>(this.queueCapacity);
        this.fileAssemblyQueue = new ArrayBlockingQueue<>(this.queueCapacity);
    }
    public void start(String... args) {
        Iterable<Download> downloads = downloadRepository.findAll();
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
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
        Future finisedDownloadsHandler = executor.submit(() -> {
            this.handleFinishedDownloads();
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
            Collection<ChildDownload> childDownloadsRunning = childDownloadRepository.findByDoneAndDownload(false, download);
            childDownloadsRunning.forEach(childDownload -> {

                //Returns false on error, lets just move on with our lives please
                try {
                    this.downloadQueue.put(childDownload);
//                    System.out.printf("Added to queue: %s first leg \n", childDownload.getFileName());
                } catch (Exception e) {
                    System.out.printf("Queue threw exception: %s \n", e.getMessage());
                }
            });
        });

        while (true) {
            List<Download> otherDownloads = downloadRepository.findByStatusAndDone(DownloadStatus.READY, false);
            otherDownloads.forEach(download -> {
                if (this.downloadQueue.remainingCapacity() > 0) {
                    //Add ready downloads to queue and set as running
                    Collection<ChildDownload> childDownloadsRunning = childDownloadRepository.findByDoneAndDownload(false, download);
                    childDownloadsRunning.forEach(childDownload -> {
                        try {
                            this.downloadQueue.put(childDownload);
//                            System.out.printf("Added to queue: %s second leg \n", childDownload.getFileName());
                            download.setStatus(DownloadStatus.RUNNING);
                            downloadRepository.save(download);
                        } catch (Exception e) {
                            System.out.printf("Queue threw exception: %s \n", e.getMessage());
                        }
                    });
                }
            });
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void createWorkers() {
        final int NUMBER_OF_WORKERS = 4;
        ThreadPoolExecutor workers = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUMBER_OF_WORKERS);
        while (true) {
            try {
                ChildDownload childDownload = this.downloadQueue.poll(2L, TimeUnit.SECONDS);
                if (childDownload == null) {
                    continue;
                }
                childDownloadRepository.save(childDownload);
                workers.submit(() -> {
                    this.getFileFromUrl(childDownload);
                });
            } catch (Exception e) {
                System.out.printf("Queue threw exception: %s \n", e.getMessage());
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
//            System.out.printf("Content length for %s is %d", download.getFileName(), contentLength);
            ArrayList<Map> limits;
            limits = getUpperAndLowerDownloadLimits(contentLength);
            this.createChildDownloads(download, limits, url);

        } catch (Exception e) {
            System.out.println("Could not connect to server");
            System.out.println(e.getMessage());
        }
    }

    private void getFileFromUrl(ChildDownload childDownload) {
        try {
            URLConnection conn = new URL(childDownload.getDownload().getUrl()).openConnection();
            conn.setRequestProperty(
                            "Range",
                            "bytes=" + (childDownload.getStartLength() + childDownload.getBytesDownloaded()) + "-" + childDownload.getEndLength());
            BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
            FileOutputStream fileOutputStream = new FileOutputStream(childDownload.getFileName());
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                int oldBytesDownloaded = childDownload.getBytesDownloaded();
                childDownload.setBytesDownloaded(oldBytesDownloaded + bytesRead);
                childDownloadRepository.save(childDownload);
                ProgressUpdater progressUpdater = new ProgressUpdater(context, childDownload.getDownload(), downloadRepository, bytesRead);
                progressUpdater.start();
            }
            childDownload.setDone(true);
            childDownloadRepository.save(childDownload);
            try {
                this.fileAssemblyQueue.put(childDownload);
            } catch (Exception e) {
                System.out.printf("Could not queue for assembly: %s", e.getMessage());
            }
        } catch (IOException e) {
            System.out.printf("IO Error: %s \n", e.getMessage());
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

    private void mergeFiles(Download download) {
        ArrayList<File> files = new ArrayList<>();
        download.getChildDownloads().forEach(childDownload -> {
            files.add(new File(childDownload.getFileName()));
        });
        try {
            String fileName = this.generateFileName(download.getFileName());
            IOCopier.joinFiles(new File(fileName), files, true);
        } catch (Exception e) {
            System.out.printf("Unable to merge download: %s\n", e.getMessage());
        }
    }

    private String generateFileName(String fileName) {
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

    private void handleFinishedDownloads() {
        while (true) {
            //Check if siblings are done
            try {
                ChildDownload childDownload = this.fileAssemblyQueue.take();

                Collection<ChildDownload> siblings = childDownload.getDownload().getChildDownloads();
                boolean allDone = false;
                for(ChildDownload sib : siblings) {
                    if (!sib.isDone()) {
                        allDone = false;
                        break;
                    } else {
                        allDone = true;
                    }
                }

                if (allDone) {
                    Download parent = childDownload.getDownload();
                    parent.setDone(true);
                    parent.setStatus(DownloadStatus.COMPLETED);
                    downloadRepository.save(parent);
                    for(ChildDownload sib : siblings) {
                        this.fileAssemblyQueue.remove(sib);
                    }
                    this.mergeFiles(parent);
                }
            } catch (Exception e) {
                System.out.printf("Could not get download portion for assembly: %s", e.getMessage());
            }
        }
    }

    static class ProgressUpdater extends Thread {

        Download download;
        DownloadRepository downloadRepository;
        int bytesDownloaded;
        ApplicationContext ctx;

        ProgressUpdater(ApplicationContext ctx, Download download, DownloadRepository downloadRepository, int bytesDownloaded) {
            this.download = download;
            this.bytesDownloaded = bytesDownloaded;
            this.downloadRepository = downloadRepository;
            this.ctx = ctx;
        }

        public void run() {
            try {
                semaphore.acquire();
                try {
                    long start = System.nanoTime();
                    Download localDownload = downloadRepository.findById(download.getId()).orElseThrow();
                    int currentBytesDownloaded = localDownload.getBytesDownloaded();
                    localDownload.setBytesDownloaded(currentBytesDownloaded + this.bytesDownloaded);
//                    System.out.println("Updating parent");
                    downloadRepository.save(localDownload);
                    long time = System.nanoTime() - start;
//                    System.out.printf("time to update parent %d \n", time);
                    long newStart = System.nanoTime();
                    Root root = (Root)ctx.getBean("root");
                    root.updateDownloadItem(localDownload);
                    long newTime = System.nanoTime() - newStart;
//                    System.out.printf("time to update progress bar %d \n", newTime);
                } catch (Exception e) {
                    //TODO handle exception
                } finally {
                    semaphore.release();
                }
            } catch (Exception e) {
                //TODO handle exception
            }
        }
    }
}
