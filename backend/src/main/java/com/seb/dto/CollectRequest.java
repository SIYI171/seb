package com.seb.dto;

import lombok.Data;

@Data
public class CollectRequest {
    private String trackingId;
    private String sessionId;
    private String url;
    private String referrer;
    private String browser;
    private String os;
    private String device;
    private String country;
}
