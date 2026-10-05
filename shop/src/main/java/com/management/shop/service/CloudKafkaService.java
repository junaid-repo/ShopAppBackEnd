package com.management.shop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class CloudKafkaService {

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Autowired
    private FCMService fcmService;

    public void sendOrderCreatedEvent(String msg, String eventPayload) {
        kafkaTemplate.send("order-events", msg);
    }

    @KafkaListener(topics = "order-events", groupId = "shop-backend-group")
    public void consume(String message) {
        System.out.println("Received Kafka event: " + message);

        fcmService.sendNotification("sampleMsg", message, "junaid1");
    }


}
