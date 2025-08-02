package com.example.regioninfo.repository;

import com.example.regioninfo.model.ImageMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageMetadataRepository extends JpaRepository<ImageMetadata, Long> {
    List<ImageMetadata> findAllByName(String name);
}