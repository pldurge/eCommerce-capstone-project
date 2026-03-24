package com.capstone.notificationservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/*
 * Internal HTTP client to resolve userId → email.
 * Notification events carry userId, but emails require the actual email address.
 * This calls user-service's internal endpoint (not exposed via API Gateway).
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.user-service.url}")
    private String userServiceUrl;

    /*
     * Fetch a user's email by their numeric ID.
     * Returns null on failure so notifications degrade gracefully.
     */
    public String getEmailByUserId(String userId) {
        try{
            String url = userServiceUrl + "/api/users/internal/" + userId;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if(response != null && response.get("email") != null){
                return (String) response.get("email");
            }
        } catch (Exception e) {
            log.error("Failed to fetch E-Mail for userId {} : {}", userId, e.getMessage());
        }
        return null;
    }
}
