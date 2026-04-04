package com.seb.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

@Service
public class GeoIpService {
    
    private static final Logger log = LoggerFactory.getLogger(GeoIpService.class);
    private DatabaseReader reader;
    
    @PostConstruct
    public void init() {
        try {
            File database = new File("data/GeoLite2-City.mmdb");
            if (database.exists()) {
                reader = new DatabaseReader.Builder(database).build();
                log.info("GeoIP database loaded successfully");
            } else {
                log.warn("GeoIP database not found at data/GeoLite2-City.mmdb, location will not be resolved");
            }
        } catch (IOException e) {
            log.error("Failed to load GeoIP database", e);
        }
    }
    
    public String getLocation(String ip) {
        if (reader == null || ip == null || ip.isEmpty()) {
            return null;
        }
        
        if (ip.equals("127.0.0.1") || ip.equals("::1") || ip.equals("0:0:0:0:0:0:0:1") || ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.") || ip.startsWith("::ffff:127.")) {
            return "本地测试";
        }
        
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse response = reader.city(ipAddress);
            
            String country = response.getCountry().getNames() != null ? 
                response.getCountry().getNames().get("zh-CN") : null;
            if (country == null) {
                country = response.getCountry().getName();
            }
            
            String city = response.getCity().getNames() != null ? 
                response.getCity().getNames().get("zh-CN") : null;
            if (city == null) {
                city = response.getCity().getName();
            }
            
            if (country != null && city != null && !city.isEmpty()) {
                return country + "·" + city;
            } else if (country != null) {
                return country;
            }
            
            return null;
        } catch (IOException | GeoIp2Exception e) {
            log.debug("Failed to resolve location for IP: {}", ip);
            return null;
        }
    }
}
