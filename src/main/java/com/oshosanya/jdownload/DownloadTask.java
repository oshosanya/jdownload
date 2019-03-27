package com.oshosanya.jdownload;

import com.oshosanya.jdownload.constant.DownloadStatus;
import com.oshosanya.jdownload.entity.ChildDownload;
import com.oshosanya.jdownload.entity.Download;
import com.oshosanya.jdownload.repository.ChildDownloadRepository;
import com.oshosanya.jdownload.repository.DownloadRepository;
import com.oshosanya.jdownload.ui.controller.Root;
import com.oshosanya.jdownload.util.IOCopier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;


public class DownloadTask {
    private Download download;
    private Collection<ChildDownload> childDownloads;
    private ArrayBlockingQueue<ChildDownload> downloadQueue;
    private List<Future> workers;
    final private int NUMBER_OF_WORKERS = 4;
    final private int queueCapacity = 6;

    @Autowired
    private ChildDownloadRepository childDownloadRepository;

    @Autowired
    private DownloadRepository downloadRepository;

    @Autowired
    private ApplicationContext context;

    private static Semaphore semaphore = new Semaphore(1);
    private ArrayBlockingQueue<ChildDownload> fileAssemblyQueue;

    public DownloadTask() {
        System.out.println("Creating download task");
        this.downloadQueue = new ArrayBlockingQueue<>(this.queueCapacity);
        this.fileAssemblyQueue = new ArrayBlockingQueue<>(this.queueCapacity);
    }

    public void setDownload(Download download) {
        this.download = download;
        this.childDownloads = download.getChildDownloads();
        System.out.println("Child download size");
        System.out.println(this.childDownloads.size());
    }

    public void startDownload() {
        System.out.printf("Download %s started\n", download.getUrl());
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
        Future populateQueue = executor.submit(() -> {
            this.populateQueue();
        });

        Future handleFinishedDownloads = executor.submit(() -> {
            this.handleFinishedDownloads();
        });
        this.createWorkers();
    }

    private void populateQueue() {
        this.childDownloads.forEach(childDownload -> {
            try {
                this.downloadQueue.put(childDownload);
                System.out.println("Added child to queue");
            } catch (Exception e) {
                System.out.printf("Queue threw exception: %s \n", e.getMessage());
            }
        });
    }

    private void createWorkers() {
        ThreadPoolExecutor workers = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUMBER_OF_WORKERS);
        while (true) {
            try {
//                System.out.println("Polling");
//                System.out.println(this.downloadQueue.remainingCapacity());
                ChildDownload childDownload = this.downloadQueue.poll(2L, TimeUnit.SECONDS);
                if (childDownload == null) {
                    continue;
                }
                childDownloadRepository.save(childDownload);
                System.out.println("Polling queue");
                Future worker = workers.submit(() -> {
                    System.out.println("Getting child from url");
                    this.getFileFromUrl(childDownload);
                });
                this.workers.add(worker);
                Thread.sleep(5000);
            } catch (Exception e) {
//                System.out.printf("Queue threw exception: %s \n", e.getMessage());
            }
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
                this.updateProgress(bytesRead, childDownload.getDownload());
//                try {
//                    semaphore.acquire();
//                    ProgressUpdater progressUpdater = new ProgressUpdater(context, childDownload.getDownload(), downloadRepository, bytesRead);
//                    progressUpdater.start();
//                } catch (Exception e) {
//                    //
//                }
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

    private void updateProgress(int bytesDownloaded, Download download) {
        try {
            semaphore.acquire();
            Download parent = downloadRepository.findById(download.getId()).orElseThrow();
            int newBytesDownloaded = parent.getBytesDownloaded() + bytesDownloaded;
            parent.setBytesDownloaded(newBytesDownloaded);
            downloadRepository.save(parent);
            Root root = (Root)context.getBean("root");
            root.updateDownloadItem(parent);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            semaphore.release();
        }
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
                    Download parent = downloadRepository.findById(childDownload.getDownload().getId()).orElseThrow();
                    parent.setDone(true);
                    parent.setStatus(DownloadStatus.COMPLETED);
                    downloadRepository.save(parent);
                    System.out.printf("Setting %s as completed \n", parent.getFileName());
                    for(ChildDownload sib : siblings) {
                        this.fileAssemblyQueue.remove(sib);
                    }
                    this.mergeFiles(parent);
                    Root root = (Root)context.getBean("root");
                    root.updateDownloadItem(parent);
                }
            } catch (Exception e) {
                System.out.printf("Could not get download portion for assembly: %s", e.getMessage());
            }
        }
    }

    private void mergeFiles(Download download) {
        ArrayList<File> files = new ArrayList<>();
        download.getChildDownloads().forEach(childDownload -> {
            files.add(new File(childDownload.getFileName()));
        });
        try {
            String fileName = Downloader.generateFileName(download.getFileName());
            IOCopier.joinFiles(new File(fileName), files, true);
        } catch (Exception e) {
            System.out.printf("Unable to merge download: %s\n", e.getMessage());
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
                long start = System.nanoTime();
                Download localDownload = downloadRepository.findById(download.getId()).orElseThrow();
                int currentBytesDownloaded = localDownload.getBytesDownloaded();
                localDownload.setBytesDownloaded(currentBytesDownloaded + this.bytesDownloaded);
//                    System.out.println("Updating parent");
                downloadRepository.save(localDownload);
                long time = System.nanoTime() - start;
//                    System.out.printf("time to update parent %d \n", time);
//                long newStart = System.nanoTime();
//                Root root = (Root)ctx.getBean("root");
//                root.updateDownloadItem(localDownload);
//                long newTime = System.nanoTime() - newStart;
//                    System.out.printf("time to update progress bar %d \n", newTime);
            } catch (Exception e) {
                //TODO handle exception
            } finally {
                semaphore.release();
            }
        }
    }
}
