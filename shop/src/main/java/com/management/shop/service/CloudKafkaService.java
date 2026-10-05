package com.management.shop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.management.shop.dto.BrodcastNotificationsRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class CloudKafkaService {

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Autowired
    private FCMService fcmService;




    @Value("${kafka.event.topic.broadcastadminmsg:broadcast-admin-msg}")
    private String broadcastAdminMsgTopic;

    public void sendOrderCreatedEvent(String msg, String eventPayload) {
        kafkaTemplate.send("order-events", msg);
    }




    public String broadcastPushNotification(BrodcastNotificationsRequest request) {

        request.getUsernames().forEach(username -> {
            var msgBody = BrodcastNotificationsRequest.builder().msg(request.getMsg()).title(request.getTitle()).username(
                    request.getUsername()
            ).build();
           ObjectMapper objectMapper=new ObjectMapper();
            try {
                String jsonPayload=objectMapper.writeValueAsString(msgBody);
                kafkaTemplate.send(broadcastAdminMsgTopic, jsonPayload);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }


        });


        return "ok";
    }
}
