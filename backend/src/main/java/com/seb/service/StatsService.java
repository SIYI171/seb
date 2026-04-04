package com.seb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seb.dto.CollectRequest;
import com.seb.dto.StatsResponse;
import com.seb.entity.Pageview;
import com.seb.entity.Session;
import com.seb.entity.Website;
import com.seb.repository.PageviewRepository;
import com.seb.repository.SessionRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
public class StatsService {
    
    private final PageviewRepository pageviewRepository;
    private final SessionRepository sessionRepository;
    private final WebsiteService websiteService;
    private final GeoIpService geoIpService;
    
    public StatsService(PageviewRepository pageviewRepository, 
                        SessionRepository sessionRepository,
                        WebsiteService websiteService,
                        GeoIpService geoIpService) {
        this.pageviewRepository = pageviewRepository;
        this.sessionRepository = sessionRepository;
        this.websiteService = websiteService;
        this.geoIpService = geoIpService;
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
        
        Pageview pageview = new Pageview();
        pageview.setWebsiteId(website.getId());
        pageview.setSessionId(request.getSessionId());
        pageview.setUrl(request.getUrl());
        pageview.setReferrer(request.getReferrer());
        pageview.setBrowser(request.getBrowser());
        pageview.setOs(request.getOs());
        pageview.setDevice(request.getDevice());
        pageview.setCountry(country);
        pageview.setIp(ip);
        pageviewRepository.insert(pageview);
    }
    
    public StatsResponse getStats(Long websiteId, LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
        
        StatsResponse response = new StatsResponse();
        
        LambdaQueryWrapper<Pageview> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(Pageview::getWebsiteId, websiteId)
                    .between(Pageview::getCreatedAt, startDateTime, endDateTime);
        Long pageviews = pageviewRepository.selectCount(countWrapper);
        
        Integer visitors = pageviewRepository.countUniqueVisitors(websiteId, startDateTime, endDateTime);
        
        response.setPageviews(pageviews);
        response.setVisitors(visitors != null ? visitors : 0);
        response.setTrend(pageviewRepository.countByDate(websiteId, startDateTime, endDateTime));
        response.setBrowsers(pageviewRepository.countByBrowser(websiteId, startDateTime, endDateTime));
        response.setOs(pageviewRepository.countByOs(websiteId, startDateTime, endDateTime));
        response.setPages(pageviewRepository.countByUrl(websiteId, startDateTime, endDateTime));
        response.setReferrers(pageviewRepository.countByReferrer(websiteId, startDateTime, endDateTime));
        response.setCountries(pageviewRepository.countByCountry(websiteId, startDateTime, endDateTime));
        
        return response;
    }
    
    public Long getRealtimeCount(Long websiteId) {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        Integer count = pageviewRepository.countUniqueSessionsSince(websiteId, fiveMinutesAgo);
        return count != null ? count.longValue() : 0L;
    }

    public List<Map<String, Object>> getRecentVisits(Long websiteId, int limit) {
        return pageviewRepository.findRecent(websiteId, limit);
    }

    public List<Map<String, Object>> getRecentVisitsWithIp(Long websiteId, int limit) {
        return pageviewRepository.findRecentWithIp(websiteId, limit);
    }

    public List<Map<String, Object>> getTopIps(Long websiteId, LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
        return pageviewRepository.countByIp(websiteId, startDateTime, endDateTime);
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
            }
        }
        return updated;
    }
}
