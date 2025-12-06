package com.management.shop.scheduler;


import com.management.shop.entity.UserInfo;
import com.management.shop.entity.UserSubscriptions;
import com.management.shop.repository.UserInfoRepository;
import com.management.shop.repository.UserSubscriptionsRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class SubscriptionsUpdate {

    @Autowired
    UserSubscriptionsRepository subsRepo;

    @Autowired
    private UserInfoRepository userinfoRepo;

    @Scheduled(cron = "${scheduler.subsriptionUpdate.cron}")
    @Transactional
    public void updateExpiredSubscription() {


        List<UserInfo> usersList = userinfoRepo.findAllByStatusAndRole(Boolean.TRUE, "ROLE_PREMIUM");

        System.out.println("Running updateExpiredSubscription scheduler for users: " + usersList);

        usersList.stream().forEach(user -> {
            String username = user.getUsername();

           List<UserSubscriptions> subsList= subsRepo.findSubListByUsername(username, "pending");

              subsList.stream().forEach(subs->{
                LocalDateTime now = LocalDateTime.now();
                if((subs.getEndDate().isBefore(now) || subs.getEndDate().isEqual(now))&& !subs.getStatus().equals("expired")){

                    System.out.println("Updating subscription to expired for user: " + username + " Subscription ID: " + subs.getId());
                    try {
                        subsRepo.updateById("expired", subs.getId());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    log.info("Subscription expired for user: " + username + " Subscription ID: " + subs.getId());
                }

                  if((subs.getStartDate().isBefore(now) || subs.getStartDate().isEqual(now))&& !subs.getStatus().equals("active")){

                      System.out.println("Updating subscription to active for user: " + username + " Subscription ID: " + subs.getId());

                      try {
                          subsRepo.updateById("active", subs.getId());
                      } catch (Exception e) {
                          throw new RuntimeException(e);
                      }
                      log.info("Subscription activated for user: " + username + " Subscription ID: " + subs.getId());
                  }
              });



        });




    }

    @Scheduled(cron = "${scheduler.subsriptionUpdate.cron}")
    @Transactional
    public void updateExpiredSubscriptionRoles() {


        List<UserInfo> usersList = userinfoRepo.findAllByStatusAndRole(Boolean.TRUE, "ROLE_PREMIUM");
        System.out.println("Running updateExpiredSubscriptionRoles scheduler for users: " + usersList);


        usersList.stream().forEach(user -> {
            String username = user.getUsername();

            UserSubscriptions latestSub  = subsRepo.findLatestActiveOrUpcomingByUsername(username);



                LocalDateTime now = LocalDateTime.now();
                if(latestSub.getEndDate().isBefore(now) || latestSub.getEndDate().isEqual(now)){

                     userinfoRepo.save(user);
                     log.info("User role downgraded to ROLE_USER for user: " + username + " Subscription ID: " + latestSub.getId());
                }

        });




    }



}


