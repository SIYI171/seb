package com.seb.controller;

import com.seb.common.Result;
import com.seb.dto.WebsiteRequest;
import com.seb.entity.Website;
import com.seb.service.WebsiteService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/websites")
public class WebsiteController {
    
    private final WebsiteService websiteService;
    
    public WebsiteController(WebsiteService websiteService) {
        this.websiteService = websiteService;
    }
    
    @GetMapping
    public Result<List<Website>> findAll() {
        return Result.success(websiteService.findAll());
    }
    
    @GetMapping("/{id}")
    public Result<Website> findById(@PathVariable Long id) {
        return Result.success(websiteService.findById(id));
    }
    
    @PostMapping
    public Result<Website> create(@RequestBody WebsiteRequest request) {
        Website website = new Website();
        website.setName(request.getName());
        website.setDomain(request.getDomain());
        return Result.success(websiteService.create(website));
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        websiteService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/share")
    public Result<String> enableShare(@PathVariable Long id) {
        String token = websiteService.generateShareToken(id);
        return Result.success(token);
    }

    @DeleteMapping("/{id}/share")
    public Result<Void> disableShare(@PathVariable Long id) {
        websiteService.disableShare(id);
        return Result.success();
    }
}
