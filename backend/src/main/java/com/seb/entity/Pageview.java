package com.seb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("pageview")
public class Pageview {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long websiteId;
    private String sessionId;
    private String url;
    private String referrer;
    private String browser;
    private String os;
    private String device;
    private String country;
    private String ip;
    private LocalDateTime createdAt;
}
