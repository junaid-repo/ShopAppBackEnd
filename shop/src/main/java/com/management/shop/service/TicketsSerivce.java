package com.management.shop.service;


import com.management.shop.dto.SupportTicketRequest;
import com.management.shop.dto.SupportTicketResponse;
import com.management.shop.entity.TicketsEntity;
import com.management.shop.repository.SupportTicketRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TicketsSerivce {

    @Autowired
    SupportTicketRepository supportTicketRepo;

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

        TicketsEntity ticketEntity = supportTicketRepo.save(entity);

        if(ticketEntity!=null) {
            String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String sequentialPart = String.format("%04d", ticketEntity.getId());
            String ticketNumber = "TKT-" + datePart + "-" + sequentialPart;
            entity.setTicketNumber(ticketNumber);
            supportTicketRepo.save(entity);
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

}
