package com.management.shop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.management.shop.dto.BrodcastNotificationsRequest;
import com.management.shop.entity.BroadcastedMessages;
import com.management.shop.repository.BroadcastedMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class CloudKafkaService {

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Autowired
    private FCMService fcmService;

    @Autowired
    BroadcastedMessageRepository bmrRepo;


    @Value("${kafka.event.topic.broadcastadminmsg:broadcast-admin-msg}")
    private String broadcastAdminMsgTopic;

    public void sendOrderCreatedEvent(String msg, String eventPayload) {
        kafkaTemplate.send("order-events", msg);
    }




    public String broadcastPushNotification(BrodcastNotificationsRequest request) {




            log.info("Sending broadcast notification to Kafka for user: {}", request);
           ObjectMapper objectMapper=new ObjectMapper();
            try {
                String jsonPayload=objectMapper.writeValueAsString(request);
                log.info("Sending broadcast with jsonPayload: {}", jsonPayload);

               kafkaTemplate.send("broadcast-admin-msg", jsonPayload);
               //consumeBroadcastAdminMsg( jsonPayload);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }





        return "ok";
    }

    public void consumeBroadcastAdminMsg(String message) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        BrodcastNotificationsRequest msg = objectMapper.readValue(message, BrodcastNotificationsRequest.class);
        log.info("Sending broadcast with jsonPayload from consumeBroadcastAdminMsg: {}", message);
        try {
            String response=  fcmService.sendNotification(msg.getTitle(), msg.getMsg(), msg.getUsername());
            log.info("Broadcast notification sent successfully from consumeBroadcastAdminMsg! {}",response);

            var bmr = BroadcastedMessages.builder().createdDate(LocalDateTime.now()).topic(msg.getTitle()).message(msg.getMsg()).username(msg.getUsername()).status(response).eventCode("broadcast-admin-msg").build();
            bmrRepo.save(bmr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
