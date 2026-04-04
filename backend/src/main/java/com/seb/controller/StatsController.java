package com.seb.controller;

import com.seb.common.Result;
import com.seb.dto.CollectRequest;
import com.seb.dto.StatsResponse;
import com.seb.service.StatsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class StatsController {
    
    private final StatsService statsService;
    
    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }
    
    @PostMapping("/collect")
    public Result<Void> collect(@RequestBody CollectRequest request, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        statsService.collect(request, ip);
        return Result.success();
    }
    
    @GetMapping("/stats/{websiteId}")
    public Result<StatsResponse> getStats(
            @PathVariable Long websiteId,
            @RequestParam(defaultValue = "") LocalDate start,
            @RequestParam(defaultValue = "") LocalDate end) {
        
        if (start == null) {
            start = LocalDate.now().minusDays(7);
        }
        if (end == null) {
            end = LocalDate.now();
        }
        
        return Result.success(statsService.getStats(websiteId, start, end));
    }
    
    @GetMapping("/stats/realtime/{websiteId}")
    public Result<Long> getRealtime(@PathVariable Long websiteId) {
        return Result.success(statsService.getRealtimeCount(websiteId));
    }

    @GetMapping("/stats/recent/{websiteId}")
    public Result<?> getRecent(@PathVariable Long websiteId, @RequestParam(defaultValue = "20") int limit) {
        return Result.success(statsService.getRecentVisitsWithIp(websiteId, limit));
    }

    @GetMapping("/stats/ips/{websiteId}")
    public Result<?> getTopIps(
            @PathVariable Long websiteId,
            @RequestParam(defaultValue = "") LocalDate start,
            @RequestParam(defaultValue = "") LocalDate end) {
        if (start == null) {
            start = LocalDate.now().minusDays(7);
        }
        if (end == null) {
            end = LocalDate.now();
        }
        return Result.success(statsService.getTopIps(websiteId, start, end));
    }

    @PostMapping("/stats/update-locations")
    public Result<Integer> updateLocations() {
        int updated = statsService.updateLocations();
        return Result.success(updated);
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = null;
        
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headers) {
            ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                break;
            }
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        if (ip != null && ip.startsWith("::ffff:")) {
            ip = ip.substring(7);
        }
        
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        
        return ip;
    }
}
