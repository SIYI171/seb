package com.seb.controller;

import com.seb.common.Result;
import com.seb.dto.StatsResponse;
import com.seb.entity.Website;
import com.seb.service.StatsService;
import com.seb.service.WebsiteService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/share")
public class ShareController {
    
    private final WebsiteService websiteService;
    private final StatsService statsService;
    
    public ShareController(WebsiteService websiteService, StatsService statsService) {
        this.websiteService = websiteService;
        this.statsService = statsService;
    }
    
    @GetMapping("/{token}")
    public Result<Website> getWebsite(@PathVariable String token) {
        Website website = websiteService.findByShareToken(token);
        if (website == null) {
            return Result.error(404, "分享链接不存在或已失效");
        }
        return Result.success(website);
    }
    
    @GetMapping("/{token}/stats")
    public Result<StatsResponse> getStats(
            @PathVariable String token,
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end) {
        
        Website website = websiteService.findByShareToken(token);
        if (website == null) {
            return Result.error(404, "分享链接不存在或已失效");
        }
        
        LocalDate[] range = normalizeRange(start, end);
        return Result.success(statsService.getStats(website.getId(), range[0], range[1]));
    }
    
    @GetMapping("/{token}/realtime")
    public Result<Long> getRealtime(@PathVariable String token) {
        Website website = websiteService.findByShareToken(token);
        if (website == null) {
            return Result.error(404, "分享链接不存在或已失效");
        }
        return Result.success(statsService.getRealtimeCount(website.getId()));
    }
    
    @GetMapping("/{token}/recent")
    public Result<List<Map<String, Object>>> getRecent(
            @PathVariable String token,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end) {
        
        Website website = websiteService.findByShareToken(token);
        if (website == null) {
            return Result.error(404, "分享链接不存在或已失效");
        }
        LocalDate[] range = normalizeRange(start, end);
        return Result.success(statsService.getRecentVisits(website.getId(), limit, range[0], range[1]));
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
}
