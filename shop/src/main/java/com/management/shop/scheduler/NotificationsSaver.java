package com.management.shop.scheduler;


import com.management.shop.entity.MessageEntity;
import com.management.shop.entity.PaymentEntity;
import com.management.shop.entity.ProductEntity;
import com.management.shop.entity.UserInfo;
import com.management.shop.repository.NotificationsRepo;
import com.management.shop.repository.ProductRepository;
import com.management.shop.repository.SalesPaymentRepository;
import com.management.shop.repository.UserInfoRepository;
import com.management.shop.service.FCMService;
import com.management.shop.service.SettingsService;
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
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Component
@Slf4j
public class NotificationsSaver {

    @Autowired
    private ProductRepository prodRepo;

    @Autowired
    private NotificationsRepo notiRepo;

    @Autowired
    private UserInfoRepository userinfoRepo;

    @Autowired
    SalesPaymentRepository salesPaymentRepo;

    @Autowired
    SettingsService setServ;

    @Autowired
    FCMService fcmService;

    @Autowired
    NotificationScheduler notificationScheduler;


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
           // if(username.equals("gadae40")){
            Boolean stockNotificationEnabled = (Boolean) getNotificationSettings(username).getOrDefault("receiveLowStockAlerts", Boolean.FALSE);

            if(stockNotificationEnabled) {
            List<ProductEntity> outOfStockProducts = prodRepo.findByStock(0, username, Boolean.TRUE);


            outOfStockProducts.stream().forEach(product -> {

                // Check if category exists and is NOT just "Product"
                String categoryText = (product.getCategory() != null && !product.getCategory().equalsIgnoreCase("Product"))
                        ? " of " + product.getCategory() + " category"
                        : "";

                String title = "📦 Product " + product.getName() + categoryText + " is out of stock.";

                String details = "Product " + product.getName() + categoryText + " is out of stock. Please restock it as soon as possible by going through the Products tab.";

                MessageEntity messageEntity = MessageEntity.builder().createdDate(LocalDateTime.now()).domain("products")
                        .title("Out of Stock Alert " + product.getName())
                        .subject(title)
                        .details(details)
                        .isDeleted(false)
                        .isDone(false)
                        .isRead(false)
                        .isFlagged(false)
                        .userId(username)
                        .searchKey(product.getName() + " " + product.getCategory())
                        .updatedBy(username)
                        .searchKey(product.getName())
                        .updatedDate(LocalDateTime.now())
                        .isSent(Boolean.FALSE)
                        .cronEx(generanteRandomCornEx())
                        .build();
                ;
                notificationScheduler.scheduleNewTask(notiRepo.save(messageEntity));



            });

        }
        //}
        });

    }

    @Scheduled(cron = "${scheduler.paymentReminder.cron}")
    public void paymentReminders() {

        List<UserInfo> usersList = userinfoRepo.findAllByStatus(Boolean.TRUE);


        usersList.stream().forEach(user -> {
            String username = user.getUsername();

            Boolean paymentReminder = (Boolean) getNotificationSettings(username).getOrDefault("receivePaymentReminders", Boolean.FALSE);

            if(paymentReminder) {

                List<PaymentEntity> paymenetList = salesPaymentRepo.findByUserId(username);


                paymenetList.stream().forEach(payment -> {
                    Long daysBetween = Optional.ofNullable(payment.getUpdatedDate())
                            .map(updatedDate -> ChronoUnit.DAYS.between(updatedDate, LocalDateTime.now()))
                            .orElse(0l);
                    if (daysBetween > 0) {

                        String title = "Due Amount for Order No " + payment.getOrderNumber();
                        String details = "\uD83D\uDCB5 Payment for " + payment.getOrderNumber() + " is due for " + String.valueOf(daysBetween) + " days. Please send reminder or connect with the customer for payment";

                        MessageEntity messageEntity = MessageEntity.builder().createdDate(LocalDateTime.now()).domain("sales")
                                .title(title)
                                .subject("Payment for " + payment.getOrderNumber() + " is due for " + String.valueOf(daysBetween) + " days.")
                                .details(details)
                                .isDeleted(false)
                                .isDone(false)
                                .isRead(false)
                                .isFlagged(false)
                                .userId(username)

                                .updatedBy(username)
                                .searchKey(payment.getOrderNumber())
                                .updatedDate(LocalDateTime.now())
                                .isSent(Boolean.FALSE)
                                .cronEx(generanteRandomCornEx())
                                .build();

                        notificationScheduler.scheduleNewTask(notiRepo.save(messageEntity));


                       // fcmService.sendNotification(title, details, username);
                    }

                });
            }
        });

    }

    @Scheduled(cron = "${scheduler.messageRemover.cron}")
    public void removeOldMessages() {



       notiRepo.deleteOldUnflaggedDuplicates();
       notiRepo.deleteOldDeletedMessages();

    }

    private Map<String, Object> getNotificationSettings(String username){
        return setServ.getNotificationSettings(username);
    }

    private static String generanteRandomCornEx(){
        Random random = new Random();

        int second = random.nextInt(60);   // 0-59
        int minute = random.nextInt(60);   // 0-59
        int hour = random.nextInt(24);     // 0-23

        return String.format("%d %d %d * * ?", second, minute, hour);
    }
}
