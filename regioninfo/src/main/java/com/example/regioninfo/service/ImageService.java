package com.example.regioninfo.service;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.regioninfo.model.ImageMetadata;
import com.example.regioninfo.repository.ImageMetadataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final S3Client s3Client;
    private final ImageMetadataRepository repository;
    private final SqsClient sqsClient;
    private final ImageAnalyticsService analytics;

    @Value("${app.bucketName:akmal-makhmudov-bucket}")
    private String bucketName;

    @Value("${aws.sqs.queueUrl}")
    private String sqsQueueUrl;

    @Value("${AWS_REGION:us-east-1}")
    private String awsRegion;

    public String saveImage(MultipartFile file) {
        try {
            final String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isBlank()) {
                throw new IllegalArgumentException("File name is missing");
            }
            final String key = "uploads/" + fileName;

            // 1) Upload to S3
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(Optional.ofNullable(file.getContentType()).orElse("application/octet-stream"))
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // 2) Persist metadata (public URL shown; presign for actual downloads)
            final String publicUrl = "https://" + bucketName + ".s3.amazonaws.com/" + key;

            ImageMetadata md = new ImageMetadata();
            md.setName(fileName);
            md.setSize(file.getSize());
            md.setLastModified(java.util.Date.from(Instant.now()));
            md.setContentType(file.getContentType());
            md.setS3Key(key);
            md.setDownloadUrl(publicUrl);
            repository.save(md);

            // 3) Fan out message to SQS
            Map<String, Object> msg = new HashMap<>();
            msg.put("name", fileName);
            msg.put("size", file.getSize());
            msg.put("extension", getExtension(fileName));
            msg.put("downloadUrl", publicUrl);

            String body = new ObjectMapper().writeValueAsString(msg);
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(sqsQueueUrl)
                    .messageBody(body)
                    .build());

            return "Uploaded to S3: " + key;
        } catch (Exception e) {
            throw new RuntimeException("Upload failed", e);
        }
    }

    public ResponseEntity<Void> downloadImage(String name) {
        ImageMetadata meta = getMetadata(name); // throws 404-style exception if not found
        String presigned = getPresignedDownloadUrl(meta.getS3Key(), Duration.ofMinutes(15));

        // increment download counter (don’t fail the download on analytics error)
        try {
            analytics.incrementDownload(meta.getName());
        } catch (Exception ignored) {
        }

        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, presigned)
                .build();
    }

    public ImageMetadata getMetadata(String name) {
        ImageMetadata response = repository.findAllByName(name).stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("No image found with name: " + name));
        try {
            analytics.incrementView(response.getName());
        } catch (Exception ignored) {
        }
        return response;
    }

    public ImageMetadata getRandomMetadata() {
        long count = repository.count();
        if (count == 0)
            throw new EntityNotFoundException("No images found in database.");

        int idx = ThreadLocalRandom.current().nextInt((int) count);

        ImageMetadata response = repository.findAll(PageRequest.of(idx, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("No images found in database."));

        try {
            analytics.incrementView(response.getName());
        } catch (Exception ignored) {
        }
        return response;
    }

    public String deleteImage(String name) {
        // remove DB rows first to discover S3 key(s)
        var rows = repository.findAllByName(name);
        rows.forEach(row -> {
            try {
                s3Client.deleteObject(b -> b.bucket(bucketName).key(row.getS3Key()));
            } catch (Exception ignored) {
            } // don’t fail if already gone
        });
        if (!rows.isEmpty())
            repository.deleteAll(rows);
        return rows.isEmpty() ? ("No DB records for: " + name) : ("Deleted: " + name);
    }


    private String getPresignedDownloadUrl(String key, Duration ttl) {
        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(awsRegion))
                .build()) {
            var get = GetObjectRequest.builder().bucket(bucketName).key(key).build();
            var presign = GetObjectPresignRequest.builder()
                    .signatureDuration(ttl)
                    .getObjectRequest(get)
                    .build();
            return presigner.presignGetObject(presign).url().toString();
        }
    }

    private String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return (i >= 0) ? filename.substring(i + 1).toLowerCase() : "unknown";
    }
}
