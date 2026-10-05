package com.management.shop.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.management.shop.dto.BrodcastNotificationsRequest;
import com.management.shop.service.FCMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CloudKafkaListener {


    @Autowired
    private FCMService fcmService;

    @Value("${kafka.event.topic.broadcastadminmsg:broadcast-admin-msg}")
    private String broadcastAdminMsgTopic;

    @KafkaListener(topics = "order-events", groupId = "shop-backend-group")
    public void consume(String message) {
        System.out.println("Received Kafka event: " + message);

        fcmService.sendNotification("sampleMsg", message, "junaid1");
    }

    @KafkaListener(topics="broadcastAdminMsgTopic", groupId = "shop-backend-group")
    public void consumeBroadcastAdminMsg(String message) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        BrodcastNotificationsRequest msg= objectMapper.readValue(message, BrodcastNotificationsRequest.class);
        log.info("Sending broadcast with jsonPayload: {}", message);
        fcmService.sendNotification(msg.getMsg(), msg.getUsername());

    }
}
