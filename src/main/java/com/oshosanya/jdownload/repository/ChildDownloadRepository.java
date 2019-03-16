package com.oshosanya.jdownload.repository;

import com.oshosanya.jdownload.constant.DownloadStatus;
import com.oshosanya.jdownload.entity.ChildDownload;
import com.oshosanya.jdownload.entity.Download;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChildDownloadRepository extends CrudRepository<ChildDownload, Integer> {
    List<ChildDownload> findByDoneAndDownload(boolean done, Download download);

    List<ChildDownload> findByDone(boolean done);
}
