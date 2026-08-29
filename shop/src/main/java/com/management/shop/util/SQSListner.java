package com.management.shop.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.management.shop.dto.EmailSQSPayload;
import com.management.shop.service.ShopService;
import com.razorpay.Invoice;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!preprod")
public class SQSListner {

    @Autowired
    ShopService shopService;

    @SqsListener("email-send-queue")
    public void EmailSender(String message){

        try {
            EmailSQSPayload payload=null;
            ObjectMapper mapper = new ObjectMapper();

            // Convert JSON string to Invoice object
            payload = mapper.readValue(message, EmailSQSPayload.class);

            System.out.println(payload.getContent());
            if(payload.getEvent().equals("send-invoice-email-queue"))// Output: John
                shopService.sendInvoiceOverEmailByListner((String)payload.getContent().get("invoice_number"));
            if(payload.getEvent().equals("send-paymentReminder-email-queue"))
                shopService.sendPaymentReminder(payload.getContent());
        } catch (Exception e) {
            e.printStackTrace();
        }




    }
}
