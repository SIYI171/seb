package com.seb.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seb.entity.Session;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;

@Mapper
public interface SessionRepository extends BaseMapper<Session> {
    @Delete("DELETE FROM session WHERE website_id = #{websiteId}")
    int deleteByWebsiteId(@Param("websiteId") Long websiteId);

    @Select("SELECT COUNT(DISTINCT session_id) FROM session " +
            "WHERE website_id = #{websiteId} AND last_activity_at >= #{since} AND ended_at IS NULL")
    Integer countActiveSessionsSince(@Param("websiteId") Long websiteId,
                                     @Param("since") LocalDateTime since);

    @Select("SELECT COUNT(DISTINCT visitor_id) FROM session " +
            "WHERE website_id = #{websiteId} AND visitor_id IS NOT NULL AND visitor_id != '' " +
            "AND created_at <= #{end} AND COALESCE(ended_at, last_activity_at, created_at) >= #{start}")
    Integer countUniqueVisitorsInRange(@Param("websiteId") Long websiteId,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM session " +
            "WHERE website_id = #{websiteId} AND created_at BETWEEN #{start} AND #{end}")
    Long countSessionsInRange(@Param("websiteId") Long websiteId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    @Select("SELECT COALESCE(ROUND(AVG(duration)), 0) FROM session " +
            "WHERE website_id = #{websiteId} AND created_at BETWEEN #{start} AND #{end}")
    Integer averageDurationInRange(@Param("websiteId") Long websiteId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    @Select("SELECT entry_url as url, COUNT(*) as count FROM session " +
            "WHERE website_id = #{websiteId} AND entry_url IS NOT NULL AND entry_url != '' " +
            "AND created_at BETWEEN #{start} AND #{end} " +
            "GROUP BY entry_url ORDER BY count DESC LIMIT 10")
    java.util.List<java.util.Map<String, Object>> countByEntryUrl(@Param("websiteId") Long websiteId,
                                                                  @Param("start") LocalDateTime start,
                                                                  @Param("end") LocalDateTime end);

    @Select("SELECT exit_url as url, COUNT(*) as count FROM session " +
            "WHERE website_id = #{websiteId} AND exit_url IS NOT NULL AND exit_url != '' " +
            "AND created_at BETWEEN #{start} AND #{end} " +
            "GROUP BY exit_url ORDER BY count DESC LIMIT 10")
    java.util.List<java.util.Map<String, Object>> countByExitUrl(@Param("websiteId") Long websiteId,
                                                                 @Param("start") LocalDateTime start,
                                                                 @Param("end") LocalDateTime end);

    @Select("SELECT session_id, visitor_id, entry_url, exit_url, duration, created_at, last_activity_at, ended_at " +
            "FROM session WHERE website_id = #{websiteId} ORDER BY last_activity_at DESC LIMIT #{limit}")
    java.util.List<java.util.Map<String, Object>> findRecentSessions(@Param("websiteId") Long websiteId,
                                                                     @Param("limit") int limit);

    @Select("SELECT session_id, visitor_id, entry_url, exit_url, duration, created_at, last_activity_at, ended_at " +
            "FROM session WHERE website_id = #{websiteId} AND created_at BETWEEN #{start} AND #{end} " +
            "ORDER BY last_activity_at DESC LIMIT #{limit}")
    java.util.List<java.util.Map<String, Object>> findRecentSessionsInRange(@Param("websiteId") Long websiteId,
                                                                            @Param("start") LocalDateTime start,
                                                                            @Param("end") LocalDateTime end,
                                                                            @Param("limit") int limit);

    @Select("SELECT session_id, visitor_id, entry_url, exit_url, duration, created_at, last_activity_at, ended_at " +
            "FROM session WHERE website_id = #{websiteId} AND created_at BETWEEN #{start} AND #{end} " +
            "ORDER BY last_activity_at DESC LIMIT #{limit}")
    java.util.List<java.util.Map<String, Object>> findRecentSessionsForExport(@Param("websiteId") Long websiteId,
                                                                               @Param("start") LocalDateTime start,
                                                                               @Param("end") LocalDateTime end,
                                                                               @Param("limit") int limit);

    @Select("SELECT session_id, visitor_id, entry_url, exit_url, duration, created_at, last_activity_at, ended_at " +
            "FROM session WHERE website_id = #{websiteId} AND created_at BETWEEN #{start} AND #{end} " +
            "ORDER BY last_activity_at DESC LIMIT #{offset}, #{limit}")
    java.util.List<java.util.Map<String, Object>> findRecentSessionsPageInRange(@Param("websiteId") Long websiteId,
                                                                                 @Param("start") LocalDateTime start,
                                                                                 @Param("end") LocalDateTime end,
                                                                                 @Param("offset") int offset,
                                                                                 @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM session WHERE website_id = #{websiteId} AND created_at BETWEEN #{start} AND #{end}")
    Long countRecentSessionsInRange(@Param("websiteId") Long websiteId,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);
}
