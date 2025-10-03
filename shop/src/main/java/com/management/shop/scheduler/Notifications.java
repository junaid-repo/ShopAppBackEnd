package com.management.shop.scheduler;


import com.management.shop.entity.MessageEntity;
import com.management.shop.entity.ProductEntity;
import com.management.shop.repository.NotificationsRepo;
import com.management.shop.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;


@Component
@Slf4j
public class Notifications {

    @Autowired
    private ProductRepository prodRepo;

    @Autowired
    private NotificationsRepo notiRepo;



    public String extractUsername() {
        //String username = SecurityContextHolder.getContext().getAuthentication().getName();
       // System.out.println("Current user: " + username);
        String   username="junaid1";
        return username;
    }


    @Scheduled(cron = "${scheduler.stockReminder.cron}")
    public void outOfStockNotification() {

        List<ProductEntity> outOfStockProducts = prodRepo.findByStock(0, extractUsername());


        outOfStockProducts.stream().forEach(product -> {
            log.info("The out of stock product is --> {}", product.getName());

            MessageEntity     messageEntity= MessageEntity.builder().createdDate(LocalDateTime.now()).domain("products")
                    .title("Out of Stock Alert " + product.getName())
                    .subject("Product " + product.getName() + "of " + product.getCategory() + " is out of stock.")
                    .details("Product " + product.getName() + "of " + product.getCategory() + " is out of stock. Please restock it as soon as possible by going through the Products tabs")
                    .isDeleted(false)
                    .isDone(false)
                    .isRead(false)
                    .isFlagged(false)
                    .userId(product.getUserId())
                    .searchKey(product.getName() + " " + product.getCategory())
                    .updatedBy(extractUsername())
                    .searchKey(product.getName())
                    .updatedDate(LocalDateTime.now())
                    .build();

            notiRepo.save(messageEntity);


        });

    }
    @Scheduled(cron = "${scheduler.messageRemover.cron}")
    public void removeOldMessages() {
        log.info("Removing old messages older than 1 days");
        notiRepo.deleteAllConditional();
        notiRepo.deleteDeletedMessages();

    }
}
