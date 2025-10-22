package com.management.shop.repository;

import com.management.shop.entity.TicketsEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface SupportTicketRepository extends JpaRepository<TicketsEntity, Integer> {

    @Query(value="select * from support_tickets where username=?1 order by created_date desc limit 5", nativeQuery=true)
    List<TicketsEntity> getTicketList(String s);


    @Transactional
    @Modifying
    @Query(value="update support_tickets set status=?2, updated_date=?4, closing_remarks=?3 where username=?5 and ticket_number=?1", nativeQuery=true)
    public void  updateExistingTicket(String ticketNumber, String closed, String closingRemarks, LocalDateTime now, String s);

    @Query(value="select * from support_tickets where status=?1 order by created_date desc", nativeQuery=true)
    List<TicketsEntity> getOpenTicketList(String status);



    @Query(value="select * from support_tickets where status=?1 and username=?2 order by created_date desc", nativeQuery=true)
    List<TicketsEntity> getOpenTicketListPerUser(String status, String username);

}
