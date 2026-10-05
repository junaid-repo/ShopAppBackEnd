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
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@Lazy(false)
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

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = "broadcast-admin-msg", groupId = "shop-backend-group")
    public void consumeBroadcastAdminMsg(String request) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        BrodcastNotificationsRequest message = objectMapper.readValue(request, BrodcastNotificationsRequest.class);
        log.info("Sending broadcast with jsonPayload: {}", message);

        List<String> usernames = message.getUsernames();
        if (usernames == null || usernames.isEmpty()) {
            if (message.getUsername() != null && !message.getUsername().isBlank()) {
                usernames = List.of(message.getUsername());
            } else {
                throw new IllegalArgumentException("No target usernames provided in broadcast request");
            }
        }

        usernames.forEach(username -> {
            log.info("just before sending notification to firebase for user {} with title {} and message {}", username, message.getTitle(), message.getMsg());
            try {
                String response = fcmService.sendNotification(message.getTitle(), message.getMsg(), username);
                log.info("Broadcast notification sent successfully! {}", response);

                var bmr = BroadcastedMessages.builder()
                        .createdDate(LocalDateTime.now())
                        .topic(message.getTitle())
                        .message(message.getMsg())
                        .username(username)
                        .status(response)
                        .eventCode("broadcast-admin-msg")
                        .build();
                bmrRepo.save(bmr);
            } catch (Exception e) {
                log.error("Failed to send notification to user {}: {}", username, e.getMessage());
                throw new RuntimeException("Failed to send notification to user: " + username, e);
            }
        });
    }

    @DltHandler
    public void consumeBroadcastAdminMsgDlt(
            String request,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage) {
        log.error("Broadcast message forwarded to DLT topic '{}'. Reason: {}. Payload: {}", topic, exceptionMessage, request);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            BrodcastNotificationsRequest message = objectMapper.readValue(request, BrodcastNotificationsRequest.class);
            List<String> usernames = message.getUsernames();
            if (usernames == null || usernames.isEmpty()) {
                if (message.getUsername() != null && !message.getUsername().isBlank()) {
                    usernames = List.of(message.getUsername());
                }
            }

            if (usernames != null && !usernames.isEmpty()) {
                usernames.forEach(username -> {
                    var bmr = BroadcastedMessages.builder()
                            .createdDate(LocalDateTime.now())
                            .topic(message.getTitle())
                            .message(message.getMsg())
                            .username(username)
                            .status("DLT_FAILED: " + (exceptionMessage != null ? exceptionMessage : "Exhausted retries"))
                            .eventCode("broadcast-admin-msg-dlt")
                            .build();
                    bmrRepo.save(bmr);
                });
            } else {
                saveDltRecord("UNKNOWN", request, "DLT_FAILED: " + (exceptionMessage != null ? exceptionMessage : "No usernames"));
            }
        } catch (Exception e) {
            log.error("Failed to parse DLT message, saving raw payload. Error: {}", e.getMessage());
            saveDltRecord("UNKNOWN", request, "DLT_PARSE_ERROR: " + (exceptionMessage != null ? exceptionMessage : e.getMessage()));
        }
    }

    private void saveDltRecord(String username, String payload, String status) {
        var bmr = BroadcastedMessages.builder()
                .createdDate(LocalDateTime.now())
                .topic("broadcast-admin-msg")
                .message(payload)
                .username(username)
                .status(status)
                .eventCode("broadcast-admin-msg-dlt")
                .build();
        bmrRepo.save(bmr);
    }
}
