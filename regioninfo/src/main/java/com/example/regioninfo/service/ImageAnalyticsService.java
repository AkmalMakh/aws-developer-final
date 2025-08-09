package com.example.regioninfo.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Service
@RequiredArgsConstructor
public class ImageAnalyticsService {

  private static final String PARTITION_KEY_ATTR = "image_name";
  private static final String AGG_PK_VALUE = "ALL";
  private static final String GSI_TOP_BY_VIEWS = "topByViews";

  private final DynamoDbClient ddb;

  @Value("${DDB_TABLE_NAME:ImageAnalytics}")
  private String table;

  /* ---------- public API ---------- */

  public void incrementView(String imageName) {
    updateCounter(imageName, "view_count", "last_view_ts");
  }

  public void incrementDownload(String imageName) {
    updateCounter(imageName, "download_count", "last_download_ts");
  }

  /** Return current counters; 0/0 if item is missing. */
  public Map<String, Long> getCounts(String imageName) {
    var res = ddb.getItem(r -> r.tableName(table)
        .key(Map.of(PARTITION_KEY_ATTR, AttributeValue.builder().s(imageName).build())));
    if (!res.hasItem() || res.item().isEmpty()) {
      return Map.of("view_count", 0L, "download_count", 0L);
    }
    var item = res.item();
    long views = parseLong(item.get("view_count"));
    long downloads = parseLong(item.get("download_count"));
    return Map.of("view_count", views, "download_count", downloads);
  }

public List<Map<String,Object>> topNByViews(int n) {
  int limit = Math.min(Math.max(n, 1), 100);
  try {
    var q = ddb.query(r -> r
        .tableName(table)
        .indexName("topByViews")
        .keyConditionExpression("#pk = :all")
        .expressionAttributeNames(Map.of("#pk","pk"))
        .expressionAttributeValues(Map.of(":all", AttributeValue.builder().s("ALL").build()))
        .scanIndexForward(false)
        .limit(limit));
    return q.items().stream().map(this::toDto).toList();
  } catch (software.amazon.awssdk.services.dynamodb.model.DynamoDbException e) {
    // Fallback: scan table, filter pk="ALL", sort by view_count desc (OK for small tables)
    var scan = ddb.scan(r -> r
        .tableName(table)
        .filterExpression("#pk = :all")
        .expressionAttributeNames(Map.of("#pk","pk"))
        .expressionAttributeValues(Map.of(":all", AttributeValue.builder().s("ALL").build())));
    return scan.items().stream()
        .sorted((a,b) -> Long.compare(
            parseLong(b.get("view_count")), parseLong(a.get("view_count"))))
        .limit(limit)
        .map(this::toDto)
        .toList();
  }
}




  private void updateCounter(String imageName, String counterAttr, String tsAttr) {
    long now = Instant.now().getEpochSecond();

    var req = UpdateItemRequest.builder()
        .tableName(table)
        .key(Map.of(PARTITION_KEY_ATTR, AttributeValue.builder().s(imageName).build()))
        .updateExpression(
            "SET #pkAgg = if_not_exists(#pkAgg, :all), " +
            "#ctr = if_not_exists(#ctr, :zero) + :one, " +
            "#ts = :now")
        .expressionAttributeNames(Map.of(
            "#pkAgg", "pk",
            "#ctr", counterAttr,
            "#ts", tsAttr))
        .expressionAttributeValues(Map.of(
            ":all",  AttributeValue.builder().s(AGG_PK_VALUE).build(),
            ":zero", AttributeValue.builder().n("0").build(),
            ":one",  AttributeValue.builder().n("1").build(),
            ":now",  AttributeValue.builder().n(Long.toString(now)).build()))
        .build();

    ddb.updateItem(req);
  }

  private long parseLong(AttributeValue av) {
    if (av == null || av.n() == null) return 0L;
    try { return Long.parseLong(av.n()); } catch (NumberFormatException e) { return 0L; }
  }

  private Map<String, Object> toDto(Map<String, AttributeValue> it) {
    String imageName = it.getOrDefault(PARTITION_KEY_ATTR, AttributeValue.builder().s("").build()).s();
    long views = parseLong(it.get("view_count"));
    long downloads = parseLong(it.get("download_count"));
    return Map.of(
        "image_name", imageName,
        "view_count", views,
        "download_count", downloads
    );
  }
}
