package com.seb.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seb.entity.Pageview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface PageviewRepository extends BaseMapper<Pageview> {
    
    @Select("SELECT DATE(created_at) as date, COUNT(*) as count FROM pageview " +
            "WHERE website_id = #{websiteId} AND created_at BETWEEN #{start} AND #{end} " +
            "GROUP BY DATE(created_at) ORDER BY date")
    List<Map<String, Object>> countByDate(@Param("websiteId") Long websiteId, 
                                          @Param("start") LocalDateTime start, 
                                          @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(DISTINCT session_id) FROM pageview " +
            "WHERE website_id = #{websiteId} AND created_at BETWEEN #{start} AND #{end}")
    Integer countUniqueVisitors(@Param("websiteId") Long websiteId, 
                                @Param("start") LocalDateTime start, 
                                @Param("end") LocalDateTime end);

    @Select("SELECT browser, COUNT(*) as count FROM pageview " +
            "WHERE website_id = #{websiteId} AND created_at BETWEEN #{start} AND #{end} " +
            "GROUP BY browser ORDER BY count DESC LIMIT 10")
    List<Map<String, Object>> countByBrowser(@Param("websiteId") Long websiteId, 
                                             @Param("start") LocalDateTime start, 
                                             @Param("end") LocalDateTime end);

    @Select("SELECT os, COUNT(*) as count FROM pageview " +
            "WHERE website_id = #{websiteId} AND created_at BETWEEN #{start} AND #{end} " +
            "GROUP BY os ORDER BY count DESC LIMIT 10")
    List<Map<String, Object>> countByOs(@Param("websiteId") Long websiteId, 
                                        @Param("start") LocalDateTime start, 
                                        @Param("end") LocalDateTime end);

    @Select("SELECT url, COUNT(*) as count FROM pageview " +
            "WHERE website_id = #{websiteId} AND created_at BETWEEN #{start} AND #{end} " +
            "GROUP BY url ORDER BY count DESC LIMIT 10")
    List<Map<String, Object>> countByUrl(@Param("websiteId") Long websiteId, 
                                         @Param("start") LocalDateTime start, 
                                         @Param("end") LocalDateTime end);

    @Select("SELECT referrer, COUNT(*) as count FROM pageview " +
            "WHERE website_id = #{websiteId} AND referrer IS NOT NULL AND referrer != '' " +
            "AND created_at BETWEEN #{start} AND #{end} " +
            "GROUP BY referrer ORDER BY count DESC LIMIT 10")
    List<Map<String, Object>> countByReferrer(@Param("websiteId") Long websiteId, 
                                              @Param("start") LocalDateTime start, 
                                              @Param("end") LocalDateTime end);

    @Select("SELECT country, COUNT(*) as count FROM pageview " +
            "WHERE website_id = #{websiteId} AND created_at BETWEEN #{start} AND #{end} " +
            "GROUP BY country ORDER BY count DESC LIMIT 10")
    List<Map<String, Object>> countByCountry(@Param("websiteId") Long websiteId, 
                                             @Param("start") LocalDateTime start, 
                                             @Param("end") LocalDateTime end);

    @Select("SELECT id, url, referrer, browser, os, device, country, created_at " +
            "FROM pageview WHERE website_id = #{websiteId} ORDER BY created_at DESC LIMIT #{limit}")
    List<Map<String, Object>> findRecent(@Param("websiteId") Long websiteId, @Param("limit") int limit);

    @Select("SELECT id, url, referrer, browser, os, device, country, ip, created_at " +
            "FROM pageview WHERE website_id = #{websiteId} ORDER BY created_at DESC LIMIT #{limit}")
    List<Map<String, Object>> findRecentWithIp(@Param("websiteId") Long websiteId, @Param("limit") int limit);

    @Select("SELECT ip, COUNT(*) as count FROM pageview " +
            "WHERE website_id = #{websiteId} AND created_at BETWEEN #{start} AND #{end} " +
            "GROUP BY ip ORDER BY count DESC LIMIT 10")
    List<Map<String, Object>> countByIp(@Param("websiteId") Long websiteId, 
                                        @Param("start") LocalDateTime start, 
                                        @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(DISTINCT session_id) FROM pageview " +
            "WHERE website_id = #{websiteId} AND created_at >= #{since}")
    Integer countUniqueSessionsSince(@Param("websiteId") Long websiteId, 
                                     @Param("since") LocalDateTime since);
}
