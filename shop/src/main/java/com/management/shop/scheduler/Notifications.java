package com.management.shop.scheduler;


import com.management.shop.entity.MessageEntity;
import com.management.shop.entity.PaymentEntity;
import com.management.shop.entity.ProductEntity;
import com.management.shop.entity.UserInfo;
import com.management.shop.repository.NotificationsRepo;
import com.management.shop.repository.ProductRepository;
import com.management.shop.repository.SalesPaymentRepository;
import com.management.shop.repository.UserInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;


@Component
@Slf4j
public class Notifications {

    @Autowired
    private ProductRepository prodRepo;

    @Autowired
    private NotificationsRepo notiRepo;

    @Autowired
    private UserInfoRepository userinfoRepo;

    @Autowired
    SalesPaymentRepository salesPaymentRepo;


    public String extractUsername() {
        //String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // System.out.println("Current user: " + username);
        String username = "junaid1";
        return username;
    }


    @Scheduled(cron = "${scheduler.stockReminder.cron}")
    public void outOfStockNotification() {

        List<UserInfo> usersList = userinfoRepo.findAllByStatus(Boolean.TRUE);

        System.out.println("Running outOfStockNotification scheduler for users: " + usersList.size());


        usersList.stream().forEach(user -> {
            String username = user.getUsername();
            List<ProductEntity> outOfStockProducts = prodRepo.findByStock(0, username, Boolean.TRUE);


            outOfStockProducts.stream().forEach(product -> {

                MessageEntity messageEntity = MessageEntity.builder().createdDate(LocalDateTime.now()).domain("products")
                        .title("Out of Stock Alert " + product.getName())
                        .subject("Product " + product.getName() + "of " + product.getCategory() + " is out of stock.")
                        .details("Product " + product.getName() + "of " + product.getCategory() + " is out of stock. Please restock it as soon as possible by going through the Products tabs")
                        .isDeleted(false)
                        .isDone(false)
                        .isRead(false)
                        .isFlagged(false)
                        .userId(username)
                        .searchKey(product.getName() + " " + product.getCategory())
                        .updatedBy(username)
                        .searchKey(product.getName())
                        .updatedDate(LocalDateTime.now())
                        .build();

                notiRepo.save(messageEntity);


            });
        });

    }

    @Scheduled(cron = "${scheduler.paymentReminder.cron}")
    public void paymentReminders() {

        List<UserInfo> usersList = userinfoRepo.findAllByStatus(Boolean.TRUE);


        usersList.stream().forEach(user -> {
            String username = user.getUsername();
            List<PaymentEntity> paymenetList = salesPaymentRepo.findByUserId(username );


            paymenetList.stream().forEach(payment -> {
                Long daysBetween = ChronoUnit.DAYS.between(payment.getUpdatedDate(), LocalDateTime.now());
                if(daysBetween>3) {
                    MessageEntity messageEntity = MessageEntity.builder().createdDate(LocalDateTime.now()).domain("sales")
                            .title("Due Amount for Order No " + payment.getOrderNumber())
                            .subject("Payment for " + payment.getOrderNumber() + " is due for " + String.valueOf(daysBetween) + " days.")
                            .details("Payment for " + payment.getOrderNumber() + " is due for " + String.valueOf(daysBetween) + " days. Please send reminder or connect with the customer for payment")
                            .isDeleted(false)
                            .isDone(false)
                            .isRead(false)
                            .isFlagged(false)
                            .userId(username)

                            .updatedBy(username)
                            .searchKey(payment.getOrderNumber())
                            .updatedDate(LocalDateTime.now())
                            .build();

                    notiRepo.save(messageEntity);
                }

            });
        });

    }

    @Scheduled(cron = "${scheduler.messageRemover.cron}")
    public void removeOldMessages() {



       notiRepo.deleteOldUnflaggedDuplicates();
       notiRepo.deleteOldDeletedMessages();

    }
}
