package com.seb.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class StatsResponse {
    private Long pageviews;
    private Integer visitors;
    private List<Map<String, Object>> trend;
    private List<Map<String, Object>> browsers;
    private List<Map<String, Object>> os;
    private List<Map<String, Object>> pages;
    private List<Map<String, Object>> referrers;
    private List<Map<String, Object>> countries;
}
