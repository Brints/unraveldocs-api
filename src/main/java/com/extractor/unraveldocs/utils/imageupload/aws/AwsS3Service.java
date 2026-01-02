package com.extractor.unraveldocs.utils.imageupload.aws;

import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.utils.imageupload.FileFolder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Getter
    private static final String PROFILE_PICTURE_FOLDER = FileFolder.PROFILE_PICTURE.getFolder();

    @Getter
    private static final String DOCUMENT_PICTURE_FOLDER = FileFolder.DOCUMENT_PICTURE.getFolder();

    public String uploadFile(MultipartFile file, String fileName) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(fileName)).toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    public String generateFileName(String originalFileName, String folderName) {
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new BadRequestException("Original file name cannot be null or empty");
        }
        return folderName + UUID.randomUUID() + "-" + originalFileName.split("\\.")[0].replaceAll("[^a-zA-Z0-9]",
                "_").trim() + "." + originalFileName.split("\\.")[1];
    }

    public String generateRandomPublicId(String originalFileName) {
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new BadRequestException("Original file name cannot be null or empty");
        }
        return UUID.randomUUID() + "-" + originalFileName.replaceAll("[^a-zA-Z0-9]", "_");
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            int bucketIndex = fileUrl.indexOf(bucketName);
            if (bucketIndex == -1) {
                log.error("Bucket name not found in file URL: {}", fileUrl);
                throw new RuntimeException("Invalid file URL: Bucket name not found");
            }
            String key = fileUrl.substring(bucketIndex + bucketName.length() + 1);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

        } catch (S3Exception ex) {
            log.error("Error deleting file from S3: {}", ex.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to delete file from S3", ex);
        }
    }

}
