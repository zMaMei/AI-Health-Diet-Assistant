package com.health.diet.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/* 消息推送适配器 */
@Component
public class NotificationAdapter {

    private static final Logger log = LoggerFactory.getLogger(NotificationAdapter.class);

    /* 推送预警通知（模拟实现） */
    public boolean pushAlert(String userId, String title, String message) {
        log.info("Simulating push notification to user {}: {} - {}", userId, title, message);
        return true;
    }
}
