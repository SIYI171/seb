package com.seb.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String shareToken;
    private LocalDateTime createdAt;
}
