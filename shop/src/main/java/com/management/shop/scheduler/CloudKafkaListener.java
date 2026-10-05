package com.management.shop.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.management.shop.dto.BrodcastNotificationsRequest;
import com.management.shop.entity.BroadcastedMessages;
import com.management.shop.repository.BroadcastedMessageRepository;
import com.management.shop.service.FCMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class CloudKafkaListener {


    @Autowired
    private FCMService fcmService;

    @Autowired
    BroadcastedMessageRepository bmrRepo;

    @Value("${kafka.event.topic.broadcastadminmsg:broadcast-admin-msg}")
    private String broadcastAdminMsgTopic;

    @KafkaListener(topics = "order-events", groupId = "shop-backend-group")
    public void consume(String message) {
        System.out.println("Received Kafka event: " + message);

        fcmService.sendNotification("sampleMsg", message, "butch35");
    }

    @KafkaListener(topics = "broadcast-admin-msg", groupId = "shop-backend-group")
    public void consumeBroadcastAdminMsg(String message) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        BrodcastNotificationsRequest msg = objectMapper.readValue(message, BrodcastNotificationsRequest.class);
        log.info("Sending broadcast with jsonPayload: {}", message);
        try {
            String response=  fcmService.sendNotification(msg.getTitle(), msg.getMsg(), msg.getUsername());

            var bmr = BroadcastedMessages.builder().createdDate(LocalDateTime.now()).topic(msg.getTitle()).message(msg.getMsg()).username(msg.getUsername()).status(response).eventCode("broadcast-admin-msg").build();
            bmrRepo.save(bmr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
