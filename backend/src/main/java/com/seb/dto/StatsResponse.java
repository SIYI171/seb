package com.seb.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class StatsResponse {
    private Long pageviews;
    private Integer visitors;
    private Long sessions;
    private Integer averageDuration;
    private List<Map<String, Object>> trend;
    private List<Map<String, Object>> browsers;
    private List<Map<String, Object>> os;
    private List<Map<String, Object>> pages;
    private List<Map<String, Object>> referrers;
    private List<Map<String, Object>> countries;
    private List<Map<String, Object>> entryPages;
    private List<Map<String, Object>> exitPages;
    private List<Map<String, Object>> recentSessions;
}
