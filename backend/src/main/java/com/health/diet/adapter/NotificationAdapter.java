package com.health.diet.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationAdapter {

    private static final Logger log = LoggerFactory.getLogger(NotificationAdapter.class);

    public boolean pushAlert(String userId, String title, String message) {
        log.info("Simulating push notification to user {}: {} - {}", userId, title, message);
        return true;
    }
}
