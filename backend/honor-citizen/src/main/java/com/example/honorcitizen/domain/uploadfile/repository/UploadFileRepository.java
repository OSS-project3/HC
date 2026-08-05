package com.example.honorcitizen.domain.uploadfile.repository;

import com.example.honorcitizen.domain.uploadfile.entity.UploadFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadFileRepository extends JpaRepository<UploadFile, Long> {
}
