package com.oshosanya.jdownload.repository;

import com.oshosanya.jdownload.entity.Download;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DownloadRepository extends CrudRepository<Download, Integer> {

}
