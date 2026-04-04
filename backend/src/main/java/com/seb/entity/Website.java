package com.seb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("website")
public class Website {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String domain;
    private String trackingId;
    private String shareToken;
    private LocalDateTime createdAt;
}
