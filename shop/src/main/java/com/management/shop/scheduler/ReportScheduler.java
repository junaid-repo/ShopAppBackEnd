package com.management.shop.scheduler;

import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.management.shop.dto.ReportRequest;
import com.management.shop.dto.UpdateUserDTO;
import com.management.shop.entity.*;
import com.management.shop.repository.ReportRecodsRepository;
import com.management.shop.repository.UserSettingsRepository;
import com.management.shop.service.ShopService;
import com.management.shop.util.EmailSender;
import com.management.shop.util.OrderEmailTemplate;
import com.management.shop.util.Utility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
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

    @Autowired
    UserSettingsRepository settingsRepo;

    @Autowired
    Utility utils;

    @Value("${scheduler.reportGeneration.cron}")
    private String reportGenerationCron;

    @Scheduled(cron = "${scheduler.reportGeneration.cron}")
    public void sendDailyReports() {

        List<ReportsRecordEntity> reportRecordList = reportRecordsRepo.findAllByStatus(Boolean.TRUE);

        System.out.println("Report records to process: " + reportRecordList.size());
        LocalDateTime startTime = LocalDateTime.now();
        reportRecordList.stream().forEach(user -> {


                sentReports(user.getUsername());

        });
        try {
            LocalDateTime endTime = LocalDateTime.now();
            String cronExpression = "${scheduler.reportGeneration.cron}";
            long durationInSeconds = ChronoUnit.SECONDS.between(startTime, endTime);
            Boolean isCompleted = Boolean.TRUE;

            Map<String, Object> request = new HashMap<>();
            request.put("schedulerName", "Daily Report Scheduler");
            request.put("cronExpression", reportGenerationCron);
            request.put("startDateTime", startTime);
            request.put("endDateTime", endTime);
            request.put("isCompleted", isCompleted);
            request.put("durationInSeconds", durationInSeconds);
            utils.saveSchedulerDetails(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }


    public void sentReports(String username){

        UserSettingsEntity userSettings= settingsRepo.findByUsername(username);

        Boolean isEnabled=userSettings.getIsDailyReportsEnabled();
        String emailId=userSettings.getDailyReportEmailId();
        if(userSettings.getDailyReportTypes()!=null && emailId!=null && isEnabled!=null && isEnabled) {
            List<String> reportTypes = Arrays.asList(userSettings.getDailyReportTypes().split("##"));
            byte[] salesSummary = null;
            byte[] gstr_report = null;
            byte[] payment_summary_report = null;
            if (reportTypes.contains("Sales Summary")) {
                var salesSummaryReport = ReportRequest.builder().reportType("Sales Summary").fromDate(String.valueOf(LocalDate.now().minusDays(1)))
                        .username(username)
                        .toDate(String.valueOf(LocalDate.now()))
                        .format("excel")
                        .build();

                salesSummary = shopServ.generateReport(salesSummaryReport);
            }


            if (reportTypes.contains("GSTR-1 Summary")) {
                var gstr_summary = ReportRequest.builder().reportType("GSTR-1 Summary").fromDate(String.valueOf(LocalDate.now().minusDays(1)))
                        .toDate(String.valueOf(LocalDate.now()))
                        .username(username)
                        .format("excel")
                        .build();
                gstr_report = shopServ.generateReport(gstr_summary);
            }
            if (reportTypes.contains("Total Payments")) {
                var payment_summary = ReportRequest.builder().reportType("Total Payments").fromDate(String.valueOf(LocalDate.now().minusDays(1)))
                        .toDate(String.valueOf(LocalDate.now()))
                        .username(username)
                        .format("excel")
                        .build();

                payment_summary_report = shopServ.generateReport(payment_summary);

            }


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
            }
        }}


    @Scheduled(cron = "${scheduler.resetreportsentstatus.cron}")
    public void updateReportSentStatus() {

        reportRecordsRepo.updateAllRecordAfterCompletion( "SYSTEM", LocalDateTime.now(), Boolean.TRUE);


    }
}
