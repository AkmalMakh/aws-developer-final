package com.example.regioninfo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.regioninfo.model.ImageMetadata;
import com.example.regioninfo.repository.ImageMetadataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class ImageService {

    private final S3Client s3Client;
    private final ImageMetadataRepository repository;
    private final SqsClient sqsClient;
    
    private final String bucketName = "akmal-makhmudov-bucket";
    private final Path uploadDir = Paths.get("uploads/");
    
    @Value("${aws.sqs.queueUrl}")
    private String sqsQueueUrl;

    public ImageService(ImageMetadataRepository repository, S3Client s3Client, SqsClient sqsClient) {
        this.repository = repository;
        this.s3Client = s3Client;
        this.sqsClient = sqsClient;
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    public String saveImage(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            String key = "uploads/" + fileName;

            // Upload to S3
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // Generate a public URL (if the object is public), or use pre-signed if needed
            String downloadUrl = "https://" + bucketName + ".s3.amazonaws.com/" + key;

            // Save metadata to DB
            ImageMetadata metadata = new ImageMetadata();
            metadata.setName(fileName);
            metadata.setSize(file.getSize());
            metadata.setLastModified(new Date());
            metadata.setContentType(file.getContentType());
            metadata.setS3Key(key);
            metadata.setDownloadUrl(downloadUrl);

            repository.save(metadata);

            // 🚀 Send message to SQS
            Map<String, Object> msg = new HashMap<>();
            msg.put("name", fileName);
            msg.put("size", file.getSize());
            msg.put("extension", getExtension(fileName));
            msg.put("downloadUrl", downloadUrl);

            String jsonMessage = new ObjectMapper().writeValueAsString(msg);

            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(sqsQueueUrl)
                    .messageBody(jsonMessage)
                    .build());

            return "Uploaded to S3: " + key;
        } catch (IOException e) {
            throw new RuntimeException("Upload to S3 failed!", e);
        }
    }

    public ResponseEntity<Resource> downloadImage(String name) {
        Path filePath = uploadDir.resolve(name).normalize();
        Resource resource = new FileSystemResource(filePath.toFile());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .body(resource);
    }

    public ImageMetadata getMetadata(String name) {
        List<ImageMetadata> matches = repository.findAllByName(name);
        if (matches.isEmpty()) {
            throw new RuntimeException("No image found with name: " + name);
        }
        return matches.get(0);
    }

    public ImageMetadata getRandomMetadata() {
        List<ImageMetadata> allImages = repository.findAll();
        if (allImages.isEmpty()) {
            throw new RuntimeException("No images found in database.");
        }
        return allImages.get(new Random().nextInt(allImages.size()));
    }

    public String deleteImage(String name) {
        // Delete file from filesystem
        Path filePath = uploadDir.resolve(name);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("File delete failed!", e);
        }

        // Delete all records with that name
        List<ImageMetadata> images = repository.findAllByName(name);
        if (images.isEmpty()) {
            return "File deleted (no DB records found): " + name;
        }

        repository.deleteAll(images);
        return "Deleted: " + name;
    }

    private String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return (i >= 0) ? filename.substring(i + 1).toLowerCase() : "unknown";
    }
}
