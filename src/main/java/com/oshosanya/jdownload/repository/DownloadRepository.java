package com.oshosanya.jdownload.repository;

import com.oshosanya.jdownload.constant.DownloadStatus;
import com.oshosanya.jdownload.entity.Download;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DownloadRepository extends CrudRepository<Download, Integer> {
    List<Download> findByStatusAndDone(DownloadStatus status, boolean done);

    List<Download> findByDone(boolean done);
}
