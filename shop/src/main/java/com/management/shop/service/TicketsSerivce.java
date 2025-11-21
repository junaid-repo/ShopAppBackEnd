package com.management.shop.service;


import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.management.shop.dto.ChatMessage;
import com.management.shop.dto.SupportTicketRequest;
import com.management.shop.dto.SupportTicketResponse;
import com.management.shop.entity.ChatMessageEntity;
import com.management.shop.entity.TicketsEntity;
import com.management.shop.repository.ChatMessageRepository;
import com.management.shop.repository.SupportTicketRepository;
import com.management.shop.util.EmailSender;
import com.management.shop.util.OrderEmailTemplate;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class TicketsSerivce {

    @Autowired
    SupportTicketRepository supportTicketRepo;

    @Autowired
    ChatMessageRepository chatRepo;

    @Autowired
    OrderEmailTemplate emailTemplate;

    @Autowired
    private Environment environment;

    @Autowired
    EmailSender email;

    public String extractUsername() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("Current user: " + username);
        //  username="junaid1";
        return username;
    }

    public SupportTicketResponse saveSupportTicket(SupportTicketRequest request) {

        TicketsEntity entity = new TicketsEntity();

        entity.setTopic(request.getTopic());
        entity.setSummary(request.getSummary());
        entity.setStatus("open");
        entity.setUpdatedBy(extractUsername());
        entity.setClosingRemarks("");
        entity.setUpdatedDate(LocalDateTime.now());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setUsername(extractUsername());


        List<TicketsEntity> ticketList=supportTicketRepo.getOpenTicketListPerUser("open",extractUsername());

        if(ticketList.size()>2){
            var response = SupportTicketResponse.builder()
                    .createdDate(LocalDateTime.now())
                    .status(request.getStatus())
                    .topic(request.getTopic())
                    .summary("You already have "+ticketList.size()+" tickets. You can have maximum of 3 open tickets. Please close the other tickets and try")
                    .closingRemarks("")
                    .build();

            System.out.println(response);

            return response;
        }




        TicketsEntity ticketEntity = supportTicketRepo.save(entity);

        if(ticketEntity!=null) {
            String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String sequentialPart = String.format("%04d", ticketEntity.getId());
            String ticketNumber = "TKT-" + datePart + "-" + sequentialPart;
            entity.setTicketNumber(ticketNumber);
            supportTicketRepo.save(entity);
            request.setTicketNumber(entity.getTicketNumber());
            request.setUsername(entity.getUsername());
        }

        try {
            String emailContent = emailTemplate.getTicketCreationMailConent(request, extractUsername());

           // if (Arrays.asList(environment.getActiveProfiles()).contains("prod")||Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
                CompletableFuture<String> futureResult = email.sendEmailForTicketIntimation("nadanasim3001@gmail.com",
                        request.getTicketNumber(), "Support",
                        emailContent, "ClearBill");
                System.out.println(futureResult);
           // }

        } catch (MailjetException | MailjetSocketTimeoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }




        var response = SupportTicketResponse.builder().ticketNumber(String.valueOf(ticketEntity.getTicketNumber()))
                .createdDate(LocalDateTime.now())
                .status(request.getStatus())
                .topic(request.getTopic())
                .summary(request.getSummary())
                .closingRemarks("")
                .build();

        System.out.println(response);

        return response;
    }

    public List<SupportTicketResponse> getTicketsList() {

        List<TicketsEntity> ticketsList = supportTicketRepo.getTicketList(extractUsername());

        List<SupportTicketResponse> response = ticketsList.stream().map(obj -> {

            var ticket = SupportTicketResponse.builder()
                    .ticketNumber(obj.getTicketNumber())
                    .summary(obj.getSummary())
                    .topic(obj.getTopic())
                    .status(obj.getStatus())
                    .createdDate(obj.getCreatedDate())
                    .closingRemarks(obj.getClosingRemarks())
                    .build();

            System.out.println(ticket);
            return ticket;
        }).collect(Collectors.toList());
        System.out.println("The ticket list-->" + response);
        return response;
    }

    @Transactional
    public SupportTicketResponse updateSupportTicket(TicketsEntity request) {
        {
            System.out.println("Request to update the ticket: " + request);

            request.setStatus("closed");
            request.setUpdatedBy(extractUsername());
            request.setClosingRemarks(request.getClosingRemarks());
            request.setUpdatedDate(LocalDateTime.now());
            request.setTopic(request.getTopic());
            request.setSummary(request.getSummary());
            request.setUsername(extractUsername());

            supportTicketRepo.updateExistingTicket(request.getTicketNumber(),"closed", request.getClosingRemarks(),LocalDateTime.now(), extractUsername());
            try {
                chatRepo.removeClosedChatHistory(request.getTicketNumber());
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }


            var response = SupportTicketResponse.builder().ticketNumber(String.valueOf(request.getTicketNumber()))
                    .createdDate(LocalDateTime.now())
                    .status(request.getStatus())
                    .closingRemarks(request.getClosingRemarks())
                    .build();

            System.out.println(response);

            return response;
        }
    }

    public List<SupportTicketResponse> getOpenTicketsList() {


        List<TicketsEntity> ticketsList = supportTicketRepo.getOpenTicketList("open");

        List<SupportTicketResponse> response = ticketsList.stream().map(obj -> {

            var ticket = SupportTicketResponse.builder()
                    .ticketNumber(obj.getTicketNumber())
                    .summary(obj.getSummary())
                    .topic(obj.getTopic())
                    .status(obj.getStatus())
                    .createdDate(obj.getCreatedDate())
                    .closingRemarks(obj.getClosingRemarks())
                    .createdBy(obj.getUsername())
                    .build();

            System.out.println(ticket);
            return ticket;
        }).collect(Collectors.toList());
        System.out.println("The ticket list-->" + response);
        return response;
    }

    public void saveMessage(ChatMessage dto) {

        ChatMessageEntity entity = new ChatMessageEntity();

        // --- Map data from the DTO to the Entity ---

        // 1. The `chatId` from the DTO is the `ticketNumber` for the entity
        entity.setTicketNumber(dto.getChatId());

        // 2. The sender and content come directly from the DTO
        entity.setSender(dto.getSender());
        entity.setContent(dto.getContent());

        // --- Enrich the Entity with Server-Generated Data ---

        // 3. Generate the timestamp on the server to ensure accuracy
        entity.setTimestamp(Instant.now());

        // 4. Save the completed, enriched entity to the database
        chatRepo.save(entity);

    }

    public List<ChatMessage> getHistoryForTicket(String ticketNumber) {

        List<ChatMessageEntity> chatHistory=    chatRepo.findByTicketNumberOrderByTimestampAsc(ticketNumber);

        List<ChatMessage> response=chatHistory.stream().map(obj->{
            ChatMessage chat= new ChatMessage();

            chat.setChatId(obj.getTicketNumber());
            chat.setContent(obj.getContent());
            chat.setSender(obj.getSender());
            chat.setType(ChatMessage.MessageType.CHAT);


            return chat;

        }).collect(Collectors.toList());

        System.out.println("The chats for ticket-->"+ticketNumber+" is " + response);

        return response;
    }

    public String sendSupportEmail(String subject, String body, MultipartFile attachment) {

        try {
            byte[] mailAttachemnt=null;
            if(attachment!=null) {
                  mailAttachemnt = attachment.getBytes();
            }
            String emailContent = emailTemplate.generateSupportEmailHtml(extractUsername(), subject, body);

           // if (Arrays.asList(environment.getActiveProfiles()).contains("prod")||Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
                CompletableFuture<String> futureResult = email.sendSupportEmail("nadanasim3001@gmail.com",
                        subject, extractUsername(),
                        mailAttachemnt, emailContent, "Clear Bill");
                System.out.println(futureResult);
           // }

        } catch (MailjetException | MailjetSocketTimeoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "success";
    }

}
