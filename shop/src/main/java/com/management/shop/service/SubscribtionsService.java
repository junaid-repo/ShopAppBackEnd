package com.management.shop.service;

import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.management.shop.dto.BillingRequest;
import com.management.shop.dto.BillingResponse;
import com.management.shop.dto.SubscriptionReceiptData;
import com.management.shop.dto.SubsriptionRequest;
import com.management.shop.entity.UserInfo;
import com.management.shop.entity.UserSubscriptions;
import com.management.shop.repository.UserInfoRepository;
import com.management.shop.repository.UserSubscriptionsRepository;
import com.management.shop.util.EmailSender;
import com.management.shop.util.OrderEmailTemplate;
import com.management.shop.util.SubscriptionInvoiceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class SubscribtionsService {

    @Autowired
    UserSubscriptionsRepository subsRepo;

    @Autowired
    private UserInfoRepository userinfoRepo;


    @Autowired
    OrderEmailTemplate emailTemplate;

    @Autowired
    private Environment environment;

    @Autowired
    EmailSender email;

    @Autowired
    SubscriptionInvoiceUtil generateGSTInvoicePdf;

    public String extractUsername() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("Current user: " + username);
        //  username="junaid1";
        return username;
    }
    public List<String> extractRoles() {
        List<String> roles = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        System.out.println("Current user roles: " + roles);
        return roles;
    }

    public Map<String, Object> saveSubscription(SubsriptionRequest request) {
            String username=extractUsername();
            Map<String, Object> response = new HashMap<>();
             Integer numberOfDays=0;

            if (request.getPlanType().equals("YEARLY")){
                numberOfDays=365;
            }
            else{
                numberOfDays=30;
            }
        LocalDateTime startDate= LocalDateTime.now();
        LocalDateTime endDate= LocalDateTime.now().plusDays(numberOfDays);
    UserSubscriptions ent=    UserSubscriptions.builder().planType(request.getPlanType())
                .days(numberOfDays)
                .status("pending")
                .username(username)
                .startDate(startDate)
                .endDate(endDate)
            .updatedAt(LocalDateTime.now())
            .updatedBy(username)
                .price(Double.valueOf(request.getAmount())).build();

        UserSubscriptions userSub=  subsRepo.save(ent);

        if(userSub!=null){
            String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String sequentialPart = String.format("%04d", userSub.getId());
            String subscriptionId = "SUB-" + datePart + "-" + sequentialPart;
            ent.setSubscriptionId(subscriptionId);
            subsRepo.save(ent);
        }

            response.put("subscriptionId", ent.getSubscriptionId());
            response.put("amount", ent.getPrice());
        response.put("orderId", ent.getId());

        return response;
    }

    public String updateSubsription(String subscriptionId) {

        UserSubscriptions userSub= subsRepo.findBySubscriptionId(subscriptionId);

        userSub.setStatus("active");
        userSub.setUpdatedAt(LocalDateTime.now());
        userSub.setUpdatedBy(subscriptionId);
        subsRepo.save(userSub);

        userinfoRepo.updateUserRole(userSub.getUsername(),"ROLE_PREMIUM");
        sendSubscriptionInvoice();


        return "Ok";
    }

    public Map<String, Object> getSubscriptionDetails() {

        UserSubscriptions userSub= subsRepo.findByUsername(extractUsername(), "active");

        Map<String, Object> response = new HashMap<>();
        if(userSub!=null) {
            response.put("subscriptionId", userSub.getSubscriptionId());
            response.put("planType", userSub.getPlanType());
            response.put("status", userSub.getStatus());
            response.put("startDate", userSub.getStartDate());
            response.put("endDate", userSub.getEndDate());
            response.put("price", userSub.getPrice());
        }
        System.out.println("subscriptionId: " + response);
        return response;
    }

    public void sendSubscriptionInvoice() {



        UserSubscriptions userSub= subsRepo.findByUsername(extractUsername(), "active");
        UserInfo userinfo=userinfoRepo.findByUsername(extractUsername()).get();
        SubscriptionReceiptData data = new SubscriptionReceiptData();
        if(userSub!=null) {
            data.setAppName("Clear Bill");
            data.setAppAddress("123 Business Avenue, Ranchi, JH 834001");
            data.setAppGstin("20ABCDE1234F1Z5");
            data.setAppPhone("+91 98765 43210");
            data.setAppEmail("support@clearbill.store");
            data.setInvoiceId(userSub.getSubscriptionId());
            data.setInvoiceDate(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));

            data.setUserEmail(userinfo.getEmail());
            data.setUserPhone(userinfo.getPhoneNumber());
            data.setUserAddress("New Town");
            data.setUserGstin("20ABCDE1234F1Z5");

            Double price =userSub.getPrice()/100;

            Double gstRate = 18.0;              // GST percentage

// Calculate base amount and GST amount
            Double baseAmount = price / (1 + (gstRate / 100));
            Double gstAmount = price - baseAmount;

// Print the values
            System.out.println("Base Amount: " + baseAmount);
            System.out.println("GST Amount: " + gstAmount);

            Map<String, BigDecimal> gstSummary =  new HashMap<>();
            gstSummary.put("IGST @18%", BigDecimal.valueOf(gstAmount));


            data.setPlanName(userSub.getPlanType());
            data.setGstSummary(gstSummary);
            data.setTaxableAmount(BigDecimal.valueOf(baseAmount));
            data.setTotalGstAmount(BigDecimal.valueOf(gstAmount));
            data.setTotalAmount(BigDecimal.valueOf(userSub.getPrice()/100));
            data.setUserName(userSub.getUsername());


            try {
                String emailContent = emailTemplate.getSubscriptionSuccessEmailContent(getSubscriptionDetails(), extractUsername());

                if (Arrays.asList(environment.getActiveProfiles()).contains("prod")||Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
                    CompletableFuture<String> futureResult = email.sendEmail("junaidraza3002@gmail.com",
                            userSub.getSubscriptionId(),userinfo.getName(),
                            generateGSTInvoicePdf.generateSubscriptionReceipt(data), emailContent, "Clear Bill");
                    System.out.println(futureResult);
                }

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


        }

    }

}
