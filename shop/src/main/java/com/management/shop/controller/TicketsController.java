package com.management.shop.controller;

import com.management.shop.dto.SupportTicketRequest;
import com.management.shop.dto.SupportTicketResponse;
import com.management.shop.entity.TicketsEntity;
import com.management.shop.service.TicketsSerivce;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Slf4j
public class TicketsController {

    @Autowired
    TicketsSerivce serv;

    @PostMapping("api/tickets/create")
    ResponseEntity<SupportTicketResponse> saveSupportTicket(@RequestBody SupportTicketRequest request){
        SupportTicketResponse response=serv.saveSupportTicket(request);

        if(response.getTicketNumber()==null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        return ResponseEntity.ok(response);

    }
    @GetMapping("api/tickets/my-latest")
    ResponseEntity<List<SupportTicketResponse>> getTicketsList(){
        List<SupportTicketResponse> response=serv.getTicketsList();

        return ResponseEntity.ok(response);

    }
    @PutMapping("api/tickets/update")
    ResponseEntity<SupportTicketResponse> updateSupportTicket(@RequestBody TicketsEntity request){
        SupportTicketResponse response=serv.updateSupportTicket(request);

        return ResponseEntity.ok(response);

    }
    @GetMapping("api/tickets/open")
    ResponseEntity<List<SupportTicketResponse>> getOpenTickets(){
        List<SupportTicketResponse> response=serv.getOpenTicketsList();

        return ResponseEntity.ok(response);

    }
    @PostMapping("api/support/send-email")
    public ResponseEntity<String> handleEmailRequest(
            @RequestPart("subject") String subject,
            @RequestPart("body") String body,
            // Use `required = false` because the attachment is optional
            @RequestPart(name = "attachment", required = false) MultipartFile attachment
    ) {

        String emailResponse=serv.sendSupportEmail(subject, body, attachment);

        if (attachment != null && !attachment.isEmpty()) {
            System.out.println("Received file: " + attachment.getOriginalFilename());
            System.out.println("File size: " + attachment.getSize() + " bytes");
        }

        // ... call your email service ...

        return ResponseEntity.ok("Email sent successfully.");
    }

}
