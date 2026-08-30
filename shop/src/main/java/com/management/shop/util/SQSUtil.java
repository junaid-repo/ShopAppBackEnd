package com.management.shop.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.management.shop.dto.EmailSQSPayload;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@Profile("!preprod")
public class SQSUtil {

    private final SqsTemplate sqsTemplate;

    public SQSUtil(SqsTemplate sqsTemplate){
        this.sqsTemplate = sqsTemplate;
    }

    public void sendOrderDetailsJustAfterOrderCompletion(String event, String username, Map message) {



       var payload= EmailSQSPayload.builder().content(message).username(username).event(event).build();
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonPayload = mapper.writeValueAsString(payload);

            sqsTemplate.send(to->to.queue("email-send-queue").payload(jsonPayload));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }


    }



}
