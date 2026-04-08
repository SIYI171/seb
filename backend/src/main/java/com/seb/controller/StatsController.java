package com.seb.controller;

import com.seb.common.Result;
import com.seb.dto.PagedResponse;
import com.seb.dto.CollectRequest;
import com.seb.dto.StatsResponse;
import com.seb.service.RequestRateLimiter;
import com.seb.service.StatsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatsController {
    
    private final StatsService statsService;
    private final RequestRateLimiter requestRateLimiter;
    
    public StatsController(StatsService statsService, RequestRateLimiter requestRateLimiter) {
        this.statsService = statsService;
        this.requestRateLimiter = requestRateLimiter;
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
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end,
            HttpServletRequest request) {
        if (!allowRequest(request, websiteId, "stats", 60, Duration.ofMinutes(1))) {
            return Result.error(429, "请求过于频繁，请稍后再试");
        }
        LocalDate[] range = normalizeRange(start, end);
        return Result.success(statsService.getStats(websiteId, range[0], range[1]));
    }
    
    @GetMapping("/stats/realtime/{websiteId}")
    public Result<Long> getRealtime(@PathVariable Long websiteId, HttpServletRequest request) {
        if (!allowRequest(request, websiteId, "realtime", 120, Duration.ofMinutes(1))) {
            return Result.error(429, "请求过于频繁，请稍后再试");
        }
        return Result.success(statsService.getRealtimeCount(websiteId));
    }

    @GetMapping("/stats/recent/{websiteId}")
    public Result<PagedResponse<Map<String, Object>>> getRecent(
            @PathVariable Long websiteId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end,
            HttpServletRequest request) {
        if (!allowRequest(request, websiteId, "recent", 90, Duration.ofMinutes(1))) {
            return Result.error(429, "请求过于频繁，请稍后再试");
        }
        LocalDate[] range = normalizeRange(start, end);
        return Result.success(statsService.getRecentVisitsWithIpPage(websiteId, page, pageSize, range[0], range[1]));
    }

    @GetMapping("/stats/ips/{websiteId}")
    public Result<?> getTopIps(
            @PathVariable Long websiteId,
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end,
            HttpServletRequest request) {
        if (!allowRequest(request, websiteId, "ips", 60, Duration.ofMinutes(1))) {
            return Result.error(429, "请求过于频繁，请稍后再试");
        }
        LocalDate[] range = normalizeRange(start, end);
        return Result.success(statsService.getTopIps(websiteId, range[0], range[1]));
    }

    @GetMapping("/stats/pages/{websiteId}")
    public Result<PagedResponse<Map<String, Object>>> getPages(
            @PathVariable Long websiteId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end,
            HttpServletRequest request) {
        if (!allowRequest(request, websiteId, "pages", 60, Duration.ofMinutes(1))) {
            return Result.error(429, "请求过于频繁，请稍后再试");
        }
        LocalDate[] range = normalizeRange(start, end);
        return Result.success(statsService.getPagesPage(websiteId, page, pageSize, range[0], range[1]));
    }

    @GetMapping("/stats/sessions/{websiteId}")
    public Result<PagedResponse<Map<String, Object>>> getRecentSessions(
            @PathVariable Long websiteId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end,
            HttpServletRequest request) {
        if (!allowRequest(request, websiteId, "sessions", 60, Duration.ofMinutes(1))) {
            return Result.error(429, "请求过于频繁，请稍后再试");
        }
        LocalDate[] range = normalizeRange(start, end);
        return Result.success(statsService.getRecentSessionsPage(websiteId, page, pageSize, range[0], range[1]));
    }

    @GetMapping("/stats/export/{websiteId}")
    public ResponseEntity<ByteArrayResource> exportStats(
            @PathVariable Long websiteId,
            @RequestParam(required = false) String dataset,
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end,
            HttpServletRequest request) {
        if (!allowRequest(request, websiteId, "export", 12, Duration.ofMinutes(1))) {
            return ResponseEntity.status(429)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(new ByteArrayResource("请求过于频繁，请稍后再试".getBytes(StandardCharsets.UTF_8)));
        }

        LocalDate[] range = normalizeRange(start, end);
        byte[] content = statsService.exportCsv(websiteId, dataset, range[0], range[1]);
        String filename = statsService.buildExportFilename(websiteId, dataset, range[0], range[1]);
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .contentLength(content.length)
                .body(new ByteArrayResource(content));
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

    private LocalDate[] normalizeRange(LocalDate start, LocalDate end) {
        LocalDate normalizedStart = start != null ? start : LocalDate.now().minusDays(6);
        LocalDate normalizedEnd = end != null ? end : LocalDate.now();
        if (normalizedStart.isAfter(normalizedEnd)) {
            LocalDate temp = normalizedStart;
            normalizedStart = normalizedEnd;
            normalizedEnd = temp;
        }
        return new LocalDate[]{normalizedStart, normalizedEnd};
    }

    private boolean allowRequest(HttpServletRequest request, Long websiteId, String action, int limit, Duration window) {
        String ip = getClientIp(request);
        String key = "%s:%d:%s".formatted(action, websiteId, ip);
        return requestRateLimiter.allow(key, limit, window);
    }
}
