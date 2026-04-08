package com.seb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("session")
public class Session {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long websiteId;
    private String sessionId;
    private String visitorId;
    private String entryUrl;
    private String exitUrl;
    private Integer duration;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;
    private LocalDateTime endedAt;
}
