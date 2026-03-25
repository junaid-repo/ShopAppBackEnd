package com.management.shop.scheduler;

import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.management.shop.dto.ReportRequest;
import com.management.shop.dto.UpdateUserDTO;
import com.management.shop.entity.MessageEntity;
import com.management.shop.entity.PaymentEntity;
import com.management.shop.entity.ReportsRecordEntity;
import com.management.shop.entity.UserInfo;
import com.management.shop.repository.ReportRecodsRepository;
import com.management.shop.service.ShopService;
import com.management.shop.util.EmailSender;
import com.management.shop.util.OrderEmailTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Component
public class ReportScheduler {

    @Autowired
    ReportRecodsRepository reportRecordsRepo;

    @Autowired
    ShopService shopServ;
    @Autowired
    EmailSender email;
    @Autowired
    OrderEmailTemplate emailTemplate;

    @Scheduled(cron = "${scheduler.reportGeneration.cron}")
    public void sendDailyReports() {

        List<ReportsRecordEntity> reportRecordList = reportRecordsRepo.findAllByStatus(Boolean.TRUE);

        System.out.println("Report records to process: " + reportRecordList.size());

        reportRecordList.stream().forEach(user -> {


                sentReports(user.getUsername());

        });

    }

    private void sentReports(String username){

        var salesSummaryReport= ReportRequest.builder().reportType("Sales Summary").fromDate(String.valueOf(LocalDate.now().minusDays(1)))
                .username(username)
                .toDate(String.valueOf(LocalDate.now()))
                .format("excel")
                .build();

        var gstr_summary= ReportRequest.builder().reportType("GSTR-1 Summary").fromDate(String.valueOf(LocalDate.now().minusDays(1)))
                .toDate(String.valueOf(LocalDate.now()))
                .username(username)
                .format("excel")
                .build();

        var payment_summary= ReportRequest.builder().reportType("Total Payments").fromDate(String.valueOf(LocalDate.now().minusDays(1)))
                .toDate(String.valueOf(LocalDate.now()))
                .username(username)
                .format("excel")
                .build();

        byte[] salesSummary = shopServ.generateReport(salesSummaryReport);
        byte[] gstr_report = shopServ.generateReport(gstr_summary);
        byte[] payment_summary_report = shopServ.generateReport(payment_summary);

        UpdateUserDTO userDetails = shopServ.getUserProfile(username);
        String emailId = userDetails.getShopEmail();
        String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

        if (emailId != null && Pattern.matches(EMAIL_REGEX, emailId)) {

            List<byte[]> fileStreams = new ArrayList<>();
            List<String> fileNames = new ArrayList<>();

            String dateSuffix = LocalDate.now().toString();

            // Safely add Sales Summary if it generated correctly
            if (salesSummary != null) {
                fileStreams.add(salesSummary);
                fileNames.add("Daily_Sales_Summary_" + dateSuffix + ".xlsx");
            }

            // Safely add GSTR Summary if it generated correctly
            if (gstr_report != null) {
                fileStreams.add(gstr_report);
                fileNames.add("GSTR-1_Summary_" + dateSuffix + ".xlsx");
            }
            if (payment_summary_report != null) {
                fileStreams.add(payment_summary_report);
                fileNames.add("Payment_summary_" + dateSuffix + ".xlsx");
            }

            // Only attempt to send the email if we have at least one valid report
            if (!fileStreams.isEmpty()) {
                String template = emailTemplate.getReportEmailContent("Sir", "Your Daily  Reports", "Daily");
                String result = null;

                try {
                    // Call the new multiple attachments method
                    result = email.sendEmailWithMultipleAttachments(
                            emailId,
                            "Daily Reports",
                            fileNames,
                            fileStreams,
                            template,
                            "ClearBills"
                    );

                    reportRecordsRepo.updateReportRecordAfterSending(username, "SYSTEM", LocalDateTime.now(), Boolean.TRUE);

                } catch (MailjetException | MailjetSocketTimeoutException e) {
                    throw new RuntimeException(e);
                }
                System.out.println(result);
            }
        } }


    @Scheduled(cron = "${scheduler.resetreportsentstatus.cron}")
    public void updateReportSentStatus() {

        reportRecordsRepo.updateAllRecordAfterCompletion( "SYSTEM", LocalDateTime.now(), Boolean.TRUE);


    }
}
