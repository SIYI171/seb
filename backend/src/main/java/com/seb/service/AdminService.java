package com.seb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seb.config.JwtUtil;
import com.seb.entity.Admin;
import com.seb.repository.AdminRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminService {
    
    private final AdminRepository adminRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    public AdminService(AdminRepository adminRepository, JwtUtil jwtUtil) {
        this.adminRepository = adminRepository;
        this.jwtUtil = jwtUtil;
    }
    
    public String login(String username, String password) {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, username);
        Admin admin = adminRepository.selectOne(wrapper);
        
        if (admin == null) {
            LambdaQueryWrapper<Admin> countWrapper = new LambdaQueryWrapper<>();
            Long count = adminRepository.selectCount(countWrapper);
            
            if (count == 0) {
                admin = new Admin();
                admin.setUsername(username);
                admin.setPassword(passwordEncoder.encode(password));
                adminRepository.insert(admin);
                return jwtUtil.generateToken(admin.getId(), admin.getUsername());
            }
            
            throw new RuntimeException("用户不存在");
        }
        
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        
        return jwtUtil.generateToken(admin.getId(), admin.getUsername());
    }
    
    public void initAdmin(String username, String password) {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        Long count = adminRepository.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("管理员已存在");
        }
        
        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        adminRepository.insert(admin);
    }
    
    public Admin getById(Long id) {
        return adminRepository.selectById(id);
    }
}
