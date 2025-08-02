package com.example.regioninfo.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.regioninfo.model.ImageMetadata;
import com.example.regioninfo.service.ImageService;

@RestController
@RequestMapping("/images")
public class ImageController {

    private final ImageService service;

    public ImageController(ImageService service){
        this.service = service;
    }

    @PostMapping("/upload")
    public String uploadImage(@RequestParam("file") MultipartFile file) {
        System.out.println("Upload endpoint hit!");
        return service.saveImage(file);
    }

    @GetMapping("/metadata/{name}")
    public ImageMetadata downloadImage(@PathVariable String name){
        System.out.println(name);
        return service.getMetadata(name);
    }
    
    @GetMapping("/metadata/random")
    public ImageMetadata getRandomMetadata() {
        return service.getRandomMetadata();
    }


    @DeleteMapping("/{name}")
    public String deleteImage(@PathVariable String name) {
        return service.deleteImage(name);
    }
}
