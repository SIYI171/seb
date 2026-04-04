package com.seb.controller;

import com.seb.common.Result;
import com.seb.dto.LoginRequest;
import com.seb.service.AdminService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    private final AdminService adminService;
    
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }
    
    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestBody LoginRequest request) {
        try {
            String token = adminService.login(request.getUsername(), request.getPassword());
            return Result.success(Map.of("token", token));
        } catch (RuntimeException e) {
            return Result.error(401, e.getMessage());
        }
    }
    
    @PostMapping("/init")
    public Result<Void> initAdmin(@RequestBody LoginRequest request) {
        try {
            adminService.initAdmin(request.getUsername(), request.getPassword());
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/profile")
    public Result<?> getProfile(@RequestAttribute("userId") Long userId) {
        return Result.success(adminService.getById(userId));
    }
}
