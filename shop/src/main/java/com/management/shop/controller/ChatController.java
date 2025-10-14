package com.management.shop.controller;

import com.management.shop.dto.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        // When a message is received, broadcast it to everyone subscribed to that specific chat room
        String destination = "/topic/chat/" + chatMessage.getChatId();
        messagingTemplate.convertAndSend(destination, chatMessage);
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        // Add username and chatId to the WebSocket session
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        headerAccessor.getSessionAttributes().put("chatId", chatMessage.getChatId());

        // Broadcast the "user joined" message to the specific chat room
        String destination = "/topic/chat/" + chatMessage.getChatId();
        messagingTemplate.convertAndSend(destination, chatMessage);
    }
    @MessageMapping("/chat.notifyAdmin")
    public void notifyAdminOfNewChat(@Payload ChatMessage chatMessage) {
        // Broadcast the new chat session info to all listening admins
        messagingTemplate.convertAndSend("/topic/admin/new-chats", chatMessage);
    }
}
