package com.rabbitMq.rabbitmqscheduler.DTO.credential;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * POJO for storing OAuth Credentials
 */
@Data
public class OAuthEndpointCredential extends EndpointCredential {
    @ToString.Exclude
    private String token;
    private boolean tokenExpires = false;
    private Date expiresAt;
    private String refreshToken;
    private boolean refreshTokenExpires = false;

}