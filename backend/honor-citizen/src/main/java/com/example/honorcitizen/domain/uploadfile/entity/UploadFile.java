package com.example.honorcitizen.domain.uploadfile.entity;

import com.example.honorcitizen.common.enums.UploadFileType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "upload_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UploadFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false)
    private String storedName;

    @Column(nullable = false, length = 500)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UploadFileType fileType;

    @Column(nullable = false, length = 100)
    private String mimeType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    public static UploadFile create(String originalName, String storedName, String filePath,
            UploadFileType fileType, String mimeType, long fileSize) {
        UploadFile uploadFile = new UploadFile();
        uploadFile.originalName = originalName;
        uploadFile.storedName = storedName;
        uploadFile.filePath = filePath;
        uploadFile.fileType = fileType;
        uploadFile.mimeType = mimeType;
        uploadFile.fileSize = fileSize;
        uploadFile.uploadedAt = LocalDateTime.now();
        return uploadFile;
    }
}
