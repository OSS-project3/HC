package com.example.honorcitizen.domain.photo.repository;

import com.example.honorcitizen.domain.photo.entity.PhotoUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhotoUploadRepository extends JpaRepository<PhotoUpload, Long> {

    Optional<PhotoUpload> findByPhotoId(String photoId);
}
