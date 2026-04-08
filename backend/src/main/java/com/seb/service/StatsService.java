package com.seb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seb.dto.CollectRequest;
import com.seb.dto.PagedResponse;
import com.seb.dto.StatsResponse;
import com.seb.entity.Pageview;
import com.seb.entity.Session;
import com.seb.entity.Website;
import com.seb.repository.PageviewRepository;
import com.seb.repository.SessionRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Service
public class StatsService {
    
    private final PageviewRepository pageviewRepository;
    private final SessionRepository sessionRepository;
    private final WebsiteService websiteService;
    private final GeoIpService geoIpService;
    private final LocalStatsCache localStatsCache;
    
    public StatsService(PageviewRepository pageviewRepository, 
                        SessionRepository sessionRepository,
                        WebsiteService websiteService,
                        GeoIpService geoIpService,
                        LocalStatsCache localStatsCache) {
        this.pageviewRepository = pageviewRepository;
        this.sessionRepository = sessionRepository;
        this.websiteService = websiteService;
        this.geoIpService = geoIpService;
        this.localStatsCache = localStatsCache;
    }
    
    public void collect(CollectRequest request, String ip) {
        Website website = websiteService.findByTrackingId(request.getTrackingId());
        if (website == null) {
            return;
        }
        
        String country = request.getCountry();
        if (country == null || country.isEmpty()) {
            country = geoIpService.getLocation(ip);
        }

        LocalDateTime now = LocalDateTime.now();
        syncSession(website.getId(), request, now);

        if (isSessionEndEvent(request)) {
            evictWebsiteCache(website.getId());
            return;
        }

        Pageview pageview = new Pageview();
        pageview.setWebsiteId(website.getId());
        pageview.setVisitorId(request.getVisitorId());
        pageview.setSessionId(request.getSessionId());
        pageview.setUrl(request.getUrl());
        pageview.setReferrer(request.getReferrer());
        pageview.setBrowser(request.getBrowser());
        pageview.setOs(request.getOs());
        pageview.setDevice(request.getDevice());
        pageview.setCountry(country);
        pageview.setIp(ip);
        pageviewRepository.insert(pageview);
        evictWebsiteCache(website.getId());
    }
    
    public StatsResponse getStats(Long websiteId, LocalDate start, LocalDate end) {
        String cacheKey = "stats:%d:%s:%s".formatted(websiteId, start, end);
        return localStatsCache.getOrCompute(cacheKey, Duration.ofSeconds(20), () -> buildStats(websiteId, start, end));
    }

    private StatsResponse buildStats(Long websiteId, LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
        
        StatsResponse response = new StatsResponse();
        
        LambdaQueryWrapper<Pageview> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(Pageview::getWebsiteId, websiteId)
                    .between(Pageview::getCreatedAt, startDateTime, endDateTime);
        Long pageviews = pageviewRepository.selectCount(countWrapper);
        
        Integer visitors = sessionRepository.countUniqueVisitorsInRange(websiteId, startDateTime, endDateTime);
        Long sessions = sessionRepository.countSessionsInRange(websiteId, startDateTime, endDateTime);
        Integer averageDuration = sessionRepository.averageDurationInRange(websiteId, startDateTime, endDateTime);
        
        response.setPageviews(pageviews);
        response.setVisitors(visitors != null ? visitors : 0);
        response.setSessions(sessions != null ? sessions : 0L);
        response.setAverageDuration(averageDuration != null ? averageDuration : 0);
        response.setTrend(pageviewRepository.countByDate(websiteId, startDateTime, endDateTime));
        response.setBrowsers(pageviewRepository.countByBrowser(websiteId, startDateTime, endDateTime));
        response.setOs(pageviewRepository.countByOs(websiteId, startDateTime, endDateTime));
        response.setPages(pageviewRepository.countByUrl(websiteId, startDateTime, endDateTime));
        response.setReferrers(pageviewRepository.countByReferrer(websiteId, startDateTime, endDateTime));
        response.setCountries(pageviewRepository.countByCountry(websiteId, startDateTime, endDateTime));
        response.setEntryPages(sessionRepository.countByEntryUrl(websiteId, startDateTime, endDateTime));
        response.setExitPages(sessionRepository.countByExitUrl(websiteId, startDateTime, endDateTime));
        response.setRecentSessions(sessionRepository.findRecentSessionsInRange(websiteId, startDateTime, endDateTime, 10));
        
        return response;
    }
    
    public Long getRealtimeCount(Long websiteId) {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        Integer count = sessionRepository.countActiveSessionsSince(websiteId, fiveMinutesAgo);
        return count != null ? count.longValue() : 0L;
    }

    public List<Map<String, Object>> getRecentVisits(Long websiteId, int limit) {
        return pageviewRepository.findRecent(websiteId, limit);
    }

    public List<Map<String, Object>> getRecentVisits(Long websiteId, int limit, LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
        return pageviewRepository.findRecentInRange(websiteId, limit, startDateTime, endDateTime);
    }

    public List<Map<String, Object>> getRecentVisitsWithIp(Long websiteId, int limit) {
        return pageviewRepository.findRecentWithIp(websiteId, limit);
    }

    public List<Map<String, Object>> getRecentVisitsWithIp(Long websiteId, int limit, LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
        return pageviewRepository.findRecentWithIpInRange(websiteId, limit, startDateTime, endDateTime);
    }

    public PagedResponse<Map<String, Object>> getRecentVisitsWithIpPage(Long websiteId, int page, int pageSize, LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
        int sanitizedPage = Math.max(1, page);
        int sanitizedPageSize = Math.min(Math.max(1, pageSize), 100);
        int offset = (sanitizedPage - 1) * sanitizedPageSize;
        String cacheKey = "recent:%d:%s:%s:%d:%d".formatted(websiteId, start, end, sanitizedPage, sanitizedPageSize);

        return localStatsCache.getOrCompute(cacheKey, Duration.ofSeconds(10), () -> {
            List<Map<String, Object>> items = pageviewRepository.findRecentWithIpPageInRange(websiteId, offset, sanitizedPageSize, startDateTime, endDateTime);
            Long total = pageviewRepository.countRecentWithIpInRange(websiteId, startDateTime, endDateTime);
            return PagedResponse.of(items, total != null ? total : 0L, sanitizedPage, sanitizedPageSize);
        });
    }

    public List<Map<String, Object>> getTopIps(Long websiteId, LocalDate start, LocalDate end) {
        String cacheKey = "ips:%d:%s:%s".formatted(websiteId, start, end);
        return localStatsCache.getOrCompute(cacheKey, Duration.ofSeconds(20), () -> {
            LocalDateTime startDateTime = start.atStartOfDay();
            LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
            return pageviewRepository.countByIp(websiteId, startDateTime, endDateTime);
        });
    }

    public PagedResponse<Map<String, Object>> getPagesPage(Long websiteId, int page, int pageSize, LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
        int sanitizedPage = Math.max(1, page);
        int sanitizedPageSize = Math.min(Math.max(1, pageSize), 100);
        int offset = (sanitizedPage - 1) * sanitizedPageSize;
        String cacheKey = "pages:%d:%s:%s:%d:%d".formatted(websiteId, start, end, sanitizedPage, sanitizedPageSize);

        return localStatsCache.getOrCompute(cacheKey, Duration.ofSeconds(20), () -> {
            List<Map<String, Object>> items = pageviewRepository.countByUrlPage(websiteId, startDateTime, endDateTime, offset, sanitizedPageSize);
            Long total = pageviewRepository.countDistinctUrlsInRange(websiteId, startDateTime, endDateTime);
            return PagedResponse.of(items, total != null ? total : 0L, sanitizedPage, sanitizedPageSize);
        });
    }

    public PagedResponse<Map<String, Object>> getRecentSessionsPage(Long websiteId, int page, int pageSize, LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
        int sanitizedPage = Math.max(1, page);
        int sanitizedPageSize = Math.min(Math.max(1, pageSize), 100);
        int offset = (sanitizedPage - 1) * sanitizedPageSize;
        String cacheKey = "sessions:%d:%s:%s:%d:%d".formatted(websiteId, start, end, sanitizedPage, sanitizedPageSize);

        return localStatsCache.getOrCompute(cacheKey, Duration.ofSeconds(20), () -> {
            List<Map<String, Object>> items = sessionRepository.findRecentSessionsPageInRange(websiteId, startDateTime, endDateTime, offset, sanitizedPageSize);
            Long total = sessionRepository.countRecentSessionsInRange(websiteId, startDateTime, endDateTime);
            return PagedResponse.of(items, total != null ? total : 0L, sanitizedPage, sanitizedPageSize);
        });
    }

    public int updateLocations() {
        LambdaQueryWrapper<Pageview> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(Pageview::getCountry).or().eq(Pageview::getCountry, "");
        List<Pageview> pageviews = pageviewRepository.selectList(wrapper);
        
        int updated = 0;
        for (Pageview pageview : pageviews) {
            String location = geoIpService.getLocation(pageview.getIp());
            if (location != null && !location.isEmpty()) {
                pageview.setCountry(location);
                pageviewRepository.updateById(pageview);
                updated++;
                evictWebsiteCache(pageview.getWebsiteId());
            }
        }
        return updated;
    }

    public byte[] exportCsv(Long websiteId, String dataset, LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

        return switch (normalizeDataset(dataset)) {
            case "recent" -> buildCsv(
                    List.of("id", "url", "referrer", "browser", "os", "device", "country", "ip", "created_at"),
                    pageviewRepository.findRecentWithIpInRange(websiteId, 5000, startDateTime, endDateTime)
            );
            case "pages" -> buildCsv(
                    List.of("url", "count"),
                    pageviewRepository.countByUrlLimited(websiteId, startDateTime, endDateTime, 1000)
            );
            case "ips" -> buildCsv(
                    List.of("ip", "count"),
                    pageviewRepository.countByIpLimited(websiteId, startDateTime, endDateTime, 1000)
            );
            case "sessions" -> buildCsv(
                    List.of("session_id", "visitor_id", "entry_url", "exit_url", "duration", "created_at", "last_activity_at", "ended_at"),
                    sessionRepository.findRecentSessionsForExport(websiteId, startDateTime, endDateTime, 5000)
            );
            default -> throw new IllegalArgumentException("Unsupported export dataset");
        };
    }

    public String buildExportFilename(Long websiteId, String dataset, LocalDate start, LocalDate end) {
        return "website-%d-%s-%s-to-%s.csv".formatted(websiteId, normalizeDataset(dataset), start, end);
    }

    private String normalizeDataset(String dataset) {
        if (dataset == null || dataset.isBlank()) {
            return "recent";
        }
        return dataset.trim().toLowerCase();
    }

    private byte[] buildCsv(List<String> headers, List<Map<String, Object>> rows) {
        List<String> lines = new ArrayList<>();
        lines.add(String.join(",", headers));
        for (Map<String, Object> row : rows) {
            StringJoiner joiner = new StringJoiner(",");
            for (String header : headers) {
                joiner.add(escapeCsv(row.get(header)));
            }
            lines.add(joiner.toString());
        }
        byte[] body = String.join("\r\n", lines).getBytes(StandardCharsets.UTF_8);
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] result = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(body, 0, result, bom.length, body.length);
        return result;
    }

    private String escapeCsv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        String escaped = text.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private void syncSession(Long websiteId, CollectRequest request, LocalDateTime now) {
        if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
            return;
        }

        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Session::getWebsiteId, websiteId)
                .eq(Session::getSessionId, request.getSessionId())
                .last("LIMIT 1");

        Session session = sessionRepository.selectOne(wrapper);
        if (session == null) {
            session = new Session();
            session.setWebsiteId(websiteId);
            session.setSessionId(request.getSessionId());
            session.setVisitorId(resolveVisitorId(request));
            session.setEntryUrl(request.getUrl());
            session.setExitUrl(request.getUrl());
            session.setDuration(0);
            session.setCreatedAt(now);
            session.setLastActivityAt(now);
            session.setEndedAt(isSessionEndEvent(request) ? now : null);
            sessionRepository.insert(session);
            return;
        }

        if (request.getVisitorId() != null && !request.getVisitorId().isEmpty()) {
            session.setVisitorId(request.getVisitorId());
        }
        if (request.getUrl() != null && !request.getUrl().isEmpty()) {
            if (session.getEntryUrl() == null || session.getEntryUrl().isEmpty()) {
                session.setEntryUrl(request.getUrl());
            }
            session.setExitUrl(request.getUrl());
        }

        session.setLastActivityAt(now);
        LocalDateTime createdAt = session.getCreatedAt() != null ? session.getCreatedAt() : now;
        session.setDuration((int) Math.max(0, ChronoUnit.SECONDS.between(createdAt, now)));
        session.setEndedAt(isSessionEndEvent(request) ? now : null);
        sessionRepository.updateById(session);
    }

    private boolean isSessionEndEvent(CollectRequest request) {
        return "session_end".equalsIgnoreCase(request.getEventType());
    }

    private String resolveVisitorId(CollectRequest request) {
        if (request.getVisitorId() != null && !request.getVisitorId().isEmpty()) {
            return request.getVisitorId();
        }
        return request.getSessionId();
    }

    private void evictWebsiteCache(Long websiteId) {
        localStatsCache.evictByPrefix("stats:" + websiteId + ":");
        localStatsCache.evictByPrefix("recent:" + websiteId + ":");
        localStatsCache.evictByPrefix("ips:" + websiteId + ":");
        localStatsCache.evictByPrefix("pages:" + websiteId + ":");
        localStatsCache.evictByPrefix("sessions:" + websiteId + ":");
    }
}
