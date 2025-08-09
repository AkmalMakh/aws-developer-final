package com.example.regioninfo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.regioninfo.service.ImageAnalyticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/analytics")
public class AnalyticsController {
  private final ImageAnalyticsService analytics;

  @GetMapping("/{name}")
  public Map<String, Long> getImageStats(@PathVariable String name) {
    return analytics.getCounts(name);
  }

  @GetMapping("/top")
  public List<Map<String,Object>> top(@RequestParam(defaultValue = "10") int limit) {
    return analytics.topNByViews(Math.min(Math.max(limit, 1), 100));
  }
}
