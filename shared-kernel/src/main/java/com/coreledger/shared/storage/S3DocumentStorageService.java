// shared-kernel/src/main/java/com/coreledger/shared/storage/S3DocumentStorageService.java
package com.coreledger.shared.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

@Component
public class S3DocumentStorageService implements DocumentStorageService {

    private final AmazonS3 s3Client;
    private final String bucketName;

    public S3DocumentStorageService(AmazonS3 s3Client,
            @Value("${storage.s3.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public String save(byte[] content, String filename, String mimeType) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(mimeType);
        metadata.setContentLength(content.length);

        String key = UUID.randomUUID() + "-" + filename;
        s3Client.putObject(bucketName, key, new ByteArrayInputStream(content), metadata);
        return key; // storagePath = S3 key
    }

    @Override
    public byte[] load(String storagePath) {
        S3Object object = s3Client.getObject(bucketName, storagePath);
        try (InputStream in = object.getObjectContent()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load document from S3", e);
        }
    }

    @Override
    public void delete(String storagePath) {
        s3Client.deleteObject(bucketName, storagePath);
    }
}
