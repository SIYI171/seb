package com.seb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seb.entity.Website;
import com.seb.repository.WebsiteRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class WebsiteService {
    
    private final WebsiteRepository websiteRepository;
    
    public WebsiteService(WebsiteRepository websiteRepository) {
        this.websiteRepository = websiteRepository;
    }
    
    public List<Website> findAll() {
        return websiteRepository.selectList(null);
    }
    
    public Website findById(Long id) {
        return websiteRepository.selectById(id);
    }
    
    public Website create(Website website) {
        website.setTrackingId(UUID.randomUUID().toString().replace("-", ""));
        websiteRepository.insert(website);
        return website;
    }
    
    public void delete(Long id) {
        websiteRepository.deleteById(id);
    }
    
    public Website findByTrackingId(String trackingId) {
        LambdaQueryWrapper<Website> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Website::getTrackingId, trackingId);
        return websiteRepository.selectOne(wrapper);
    }

    public Website findByShareToken(String shareToken) {
        LambdaQueryWrapper<Website> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Website::getShareToken, shareToken);
        return websiteRepository.selectOne(wrapper);
    }

    public String generateShareToken(Long id) {
        Website website = websiteRepository.selectById(id);
        if (website == null) return null;
        
        if (website.getShareToken() == null || website.getShareToken().isEmpty()) {
            website.setShareToken(UUID.randomUUID().toString().replace("-", ""));
            websiteRepository.updateById(website);
        }
        return website.getShareToken();
    }

    public void disableShare(Long id) {
        Website website = websiteRepository.selectById(id);
        if (website != null) {
            website.setShareToken(null);
            websiteRepository.updateById(website);
        }
    }
}
