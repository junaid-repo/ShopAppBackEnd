package com.management.shop.service;

import com.management.shop.dto.*;
import com.management.shop.entity.*;
import com.management.shop.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SettingsService {

    @Autowired
    UserSettingsRepository settingsRepo;

    @Autowired
    NotificationSettingsRepository notificationSettingsRepo;

    @Autowired
    ApiSaveRepository apiSaveRepo;

    @Autowired
    ReportRecodsRepository reportRecordsRepo;


    @Autowired
    SelectedInvoiceRepository invoiceRepo;

    public String extractUsername() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SecurityContextHolder.getContext().getAuthentication().getAuthorities().forEach(auth -> {
            System.out.println("Authority: " + auth.getAuthority());
        });
        System.out.println("Current user: " + username);
        //  username="junaid1";
        return username;
    }

    public String extractRole() {
        String userrole= SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().toList().get(0).getAuthority();

        System.out.println("Current user: " + userrole);
        //  username="junaid1";
        return userrole;
    }

    public String saveUserUISettings(UiSettings request) {

        settingsRepo.updateUiSettings(request.isAutoPrintInvoice(), request.isBillingPageDefault(), request.isDarkModeDefault(), extractUsername(), LocalDateTime.now());

        return "saved";
    }

    public String saveUserSchedulerSettings(SchedulerSettings request) {
        System.out.println("The scheulder settings to be saved  " + request.isDailySalesReport());
        settingsRepo.updateSchedulerSettings(request.isLowStockAlerts(), request.getAutoDeleteNotificationsDays(), request.getAutoDeleteCustomers().isEnabled(), request.getAutoDeleteCustomers().getInactiveDays(), request.getAutoDeleteCustomers().getMinSpent(), extractUsername(), LocalDateTime.now(), request.isDailySalesReport());

        if(request!=null) {

            ReportsRecordEntity reportsRecord=  reportRecordsRepo.findByUsername(extractUsername()).stream().sorted(Comparator.comparing(ReportsRecordEntity::getUpdatedDate).reversed()).findFirst().orElse(ReportsRecordEntity.builder().id(0).build());

            if(reportsRecord.getId().equals(0)) {

               var reportRecords= ReportsRecordEntity.builder()
                        .username(extractUsername())
                        .reportType("dailySalesReport")
                        .isActive(request.isDailySalesReport())
                        .isSent(Boolean.FALSE)
                        .last_report_date(LocalDateTime.now().minusDays(1))
                        .updatedBy("SYSTEM")
                        .updatedDate(LocalDateTime.now())
                        .build();

                reportRecordsRepo.save(reportRecords);
            }
            else{
                reportRecordsRepo.updateReportRecord(extractUsername(), "SYSTEM", LocalDateTime.now(), request.isDailySalesReport());
            }
        }


        return "saved";
    }

    public String saveUserReportSchedulerSettings(ReportSchedulerSettings request) {
        System.out.println("The scheulder settings to be saved  " + request.getEnabled());

        settingsRepo.updateReportSchedulerSettings(request.getEnabled(), request.getEmail(), request.getReportTypes(), extractUsername(), LocalDateTime.now());

        if(request!=null) {

            ReportsRecordEntity reportsRecord=  reportRecordsRepo.findByUsername(extractUsername()).stream().sorted(Comparator.comparing(ReportsRecordEntity::getUpdatedDate).reversed()).findFirst().orElse(ReportsRecordEntity.builder().id(0).build());

            if(reportsRecord.getId().equals(0)) {

                var reportRecords= ReportsRecordEntity.builder()
                        .username(extractUsername())
                        .reportType("dailySalesReport")
                        .isActive(request.getEnabled())
                        .isSent(Boolean.FALSE)
                        .last_report_date(LocalDateTime.now().minusDays(1))
                        .updatedBy("SYSTEM")
                        .updatedDate(LocalDateTime.now())
                        .build();

                reportRecordsRepo.save(reportRecords);
            }
            else{
                reportRecordsRepo.updateReportRecord(extractUsername(), "SYSTEM", LocalDateTime.now(), request.getEnabled());
            }
        }


        return "saved";
    }

    public ShopSettings getFullUserSettings() {
        System.out.println("Current user: " + extractUsername());
        UserSettingsEntity userSettings = settingsRepo.findByUsername(extractUsername());

        if (userSettings == null) {
            saveUserSettings(extractUsername());
            userSettings = settingsRepo.findByUsername(extractUsername());
        }

        System.out.println("Current user settings: " + userSettings);

        var shopSettings = ShopSettings.builder().ui(
                        UiSettings.builder()
                                .autoSendInvoice(userSettings != null && userSettings.getAutoSendInvoice() != null ? userSettings.getAutoSendInvoice() : false)
                                .autoPrintInvoice(userSettings != null && userSettings.getAutoPrintInvoice() != null ? userSettings.getAutoPrintInvoice() : false)
                                .darkModeDefault(userSettings != null && userSettings.getIsDarkModeDefault() != null ? userSettings.getIsDarkModeDefault() : false)
                                .billingPageDefault(userSettings != null && userSettings.getIsBillingPageDefault() != null ? userSettings.getIsBillingPageDefault() : false)
                                .build())
                .schedulers(
                        SchedulerSettings.builder()
                                .autoDeleteNotificationsDays(userSettings != null && userSettings.getAutoDeleteNotification() != null ? userSettings.getAutoDeleteNotification() : 0)
                                .dailySalesReport(userSettings != null && userSettings.getSendDailyReports() != null ? userSettings.getSendDailyReports() : false)
                                .lowStockAlerts(userSettings != null && userSettings.getLowStockAlert() != null ? userSettings.getLowStockAlert() : false)
                                .autoDeleteCustomers(
                                        AutoDeleteCustomersSettings.builder()
                                                .enabled(userSettings != null && userSettings.getAutoDeleteCustomers() != null ? userSettings.getAutoDeleteCustomers() : false)
                                                .inactiveDays(userSettings != null && userSettings.getAutoDeleteCustomerForInactiveDays() != null ? userSettings.getAutoDeleteCustomerForInactiveDays() : 0)
                                                .minSpent(userSettings != null && userSettings.getAutoDeleteCustomerForMinSpent() != null ? userSettings.getAutoDeleteCustomerForMinSpent() : 0)
                                                .build()
                                ).build()

                )
                .billing(BillingSettings.builder()
                        .allowNoStockBilling(userSettings != null && userSettings.getAllowNoStockBilling() != null ? userSettings.getAllowNoStockBilling() : false)
                        .hideNoStockProducts(userSettings != null && userSettings.getHideNoStockProducts() != null ? userSettings.getHideNoStockProducts() : false)
                        .autoSendInvoice(userSettings != null && userSettings.getAutoSendInvoice() != null ? userSettings.getAutoSendInvoice() : false)
                        .showPartialPaymentOption(userSettings != null && userSettings.getShowPartialPaymentOption() != null ? userSettings.getShowPartialPaymentOption() : false)
                        .showRemarksOnSummarySide(userSettings != null && userSettings.getShowRemarksOptions() != null ? userSettings.getShowRemarksOptions() : false)
                        .showAnonymousCustomerOption(userSettings != null && userSettings.getShowAnonymousCustomerOption() != null ? userSettings.getShowAnonymousCustomerOption() : false)
                        .serialNumberPattern(userSettings != null && userSettings.getSerialNumberPattern() != null ? userSettings.getSerialNumberPattern() : "")
                        .showBillToGstinOption(userSettings != null && userSettings.getShowBillToGstinOption() != null ? userSettings.getShowBillToGstinOption() : false)
                        .build())
                .invoice(InvoiceSettings.builder()
                        .addDueDate(userSettings != null && userSettings.getAddDueDate() != null ? userSettings.getAddDueDate() : false)
                        .combineAddresses(userSettings != null && userSettings.getCombineAddresses() != null ? userSettings.getCombineAddresses() : false)
                        .showPaymentStatus(userSettings != null && userSettings.getShowPaymentStatus() != null ? userSettings.getShowPaymentStatus() : false)
                        .removeTerms(userSettings != null && userSettings.getRemoveTerms() != null ? userSettings.getRemoveTerms() : false)
                        .showCustomerGstin(userSettings != null && userSettings.getShowCustomerGstin() != null ? userSettings.getShowCustomerGstin() : false)

                        .showTotalDiscountPercentage(userSettings != null && userSettings.getShowTotalDiscount() != null ? userSettings.getShowTotalDiscount() : false)
                        .showIndividualDiscountPercentage(userSettings != null && userSettings.getShowItemDiscount() != null ? userSettings.getShowItemDiscount() : false)
                        .showShopPanOnInvoice(userSettings != null && userSettings.getShowShopPan() != null ? userSettings.getShowShopPan() : false)
                        .showSupportInfoOnInvoice(userSettings != null && userSettings.getShowSupportInfo() != null ? userSettings.getShowSupportInfo() : false)
                        .showRateColumn(userSettings != null && userSettings.getShowRateColumn() != null ? userSettings.getShowRateColumn() : false)
                        .showHsnColumn(userSettings != null && userSettings.getShowHsnColumn() != null ? userSettings.getShowHsnColumn() : false)
                        .showInvoiceBarcode(userSettings != null && userSettings.getShowInvoiceBarcode() != null ? userSettings.getShowInvoiceBarcode() : false)
                        .showGSTBreakdown(userSettings != null && userSettings.getShowGstBreakdown() != null ? userSettings.getShowGstBreakdown() : false)


                        .build())
                .reports(ReportSchedulerSettings.builder().enabled(userSettings != null && userSettings.getIsDailyReportsEnabled() != null ? userSettings.getIsDailyReportsEnabled() : false)
                        .email(userSettings != null && userSettings.getDailyReportEmailId()!= null ? userSettings.getDailyReportEmailId() : "")
                        .reportTypes(userSettings != null && userSettings.getDailyReportTypes()!= null ? userSettings.getDailyReportTypes() : "")
                        .build())
                .build();

        System.out.println("The full shop Settings are " + shopSettings);

        return shopSettings;
    }

    private void saveUserSettings(String username) {

      var invoiceLayout=  SelectedInvoiceEntity.builder().printerType("THERMAL_2").templateName("gstinvoiceThermal1").username(username).updatedBy(username).updatedDate(LocalDateTime.now()).build();

        invoiceRepo.save(invoiceLayout);

        var userSettings = UserSettingsEntity.builder()
                .allowNoStockBilling(Boolean.FALSE)
                .autoPrintInvoice(Boolean.FALSE)
                .autoSendInvoice(Boolean.FALSE)
                .hideNoStockProducts(Boolean.TRUE)
                .isDarkModeDefault(Boolean.FALSE)
                .isBillingPageDefault(Boolean.FALSE)
                .lowStockAlert(Boolean.TRUE)
                .sendDailyReports(Boolean.FALSE)
                .serialNumberPattern("CB")
                .autoDeleteCustomers(Boolean.FALSE)
                .autoDeleteNotification(2)
                .autoDeleteCustomerForInactiveDays(30)
                .autoDeleteCustomerForMinSpent(10000)
                .combineAddresses(Boolean.FALSE)
                .addDueDate(Boolean.FALSE)
                .showPaymentStatus(Boolean.TRUE)
                .removeTerms(Boolean.FALSE)
                .showCustomerGstin(Boolean.TRUE)
                .showPaymentStatus(Boolean.TRUE)
                .showRemarksOptions(Boolean.FALSE)
                .showShopPan(Boolean.TRUE)
                .showHsnColumn(Boolean.TRUE)
                .showItemDiscount(Boolean.FALSE)
                .showRateColumn(Boolean.TRUE)
                .showTotalDiscount(Boolean.FALSE)
                .showSupportInfo(Boolean.FALSE)
                .showBillToGstinOption(Boolean.FALSE)
                .username(username)
                .updatedBy(username)
                .updatedDate(LocalDateTime.now())
                .build();

        settingsRepo.save(userSettings);
    }

    public String saveBillingSettings(Map<String, Object> request) {

        Boolean autoSendInvoice = (Boolean) request.get("autoSendInvoice");
        Boolean allowNoStockBilling = (Boolean) request.get("allowNoStockBilling");
        Boolean hideNoStockProducts = (Boolean) request.get("hideNoStockProducts");
        String serialNumberPattern = (String) request.get("serialNumberPattern");

        Boolean doPartialBilling = (Boolean) request.get("showPartialPaymentOption");
        Boolean showRemarksOption = (Boolean) request.get("showRemarksOnSummarySide");
        Boolean showAnonymousCustomer = (Boolean) request.get("showAnonymousCustomerOption");
        Boolean showBillToGstinOption = (Boolean) request.get("showBillToGstinOption");


        settingsRepo.updateBillingSettings(autoSendInvoice, allowNoStockBilling, hideNoStockProducts, serialNumberPattern, extractUsername(), LocalDateTime.now(), doPartialBilling, showRemarksOption, showAnonymousCustomer, showBillToGstinOption);


        return "saved";
    }

    public String saveInvoiceSetting(Map<String, Object> request) {

        Boolean addDueDate = (Boolean) request.get("addDueDate");
        Boolean combineAddresses = (Boolean) request.get("combineAddresses");
        Boolean showPaymentStatus = (Boolean) request.get("showPaymentStatus");
        Boolean removeTerms = (Boolean) request.get("removeTerms");
        Boolean showCustomerGstin = (Boolean) request.get("showCustomerGstin");

        Boolean showTotalDiscountPercentage = (Boolean) request.get("showTotalDiscountPercentage");
        Boolean showIndividualDiscountPercentage = (Boolean) request.get("showIndividualDiscountPercentage");
        Boolean showShopPanOnInvoice = (Boolean) request.get("showShopPanOnInvoice");
        Boolean showSupportInfoOnInvoice = (Boolean) request.get("showSupportInfoOnInvoice");
        Boolean showRateColumn = (Boolean) request.get("showRateColumn");
        Boolean showHsnColumn = (Boolean) request.get("showHsnColumn");
        Boolean showInvoiceBarcode = (Boolean) request.get("showInvoiceBarcode");
        Boolean showGstinBreakdown = (Boolean) request.get("showGSTBreakdown");


        settingsRepo.updateInvoiceSettings(addDueDate, combineAddresses, showPaymentStatus, removeTerms, showCustomerGstin, extractUsername(), LocalDateTime.now(),
                showTotalDiscountPercentage, showIndividualDiscountPercentage, showShopPanOnInvoice, showSupportInfoOnInvoice, showRateColumn, showHsnColumn, showInvoiceBarcode, showGstinBreakdown);


        return "saved";
    }

    public String updateNotificationSettings(Map<String, Object> request) {

        Boolean paymentReminders = (Boolean) request.get("receivePaymentReminders");
        Boolean lowStockAlert = (Boolean) request.get("receiveLowStockAlerts");
        Boolean systemUpdates = (Boolean) request.get("receiveSystemUpdates");

        NotificationSetting notSettings = notificationSettingsRepo.findbyUsername(extractUsername());

        if(notSettings!=null) {

            notificationSettingsRepo.updateNoficationSettings(extractUsername(), paymentReminders, lowStockAlert, systemUpdates, LocalDateTime.now());

        } else {

            var   notifiSettings = NotificationSetting.builder().paymentReminders(paymentReminders)
                    .lowStockAlert(lowStockAlert)
                    .systemUpdates(systemUpdates)
                    .username(extractUsername())
                    .updatedBy(extractUsername())
                    .updatedDate(LocalDateTime.now())
                    .build();
            notificationSettingsRepo.save(notifiSettings);

        }

        return "saved";
    }

    public Map<String, Object> getNotificationSettings() {

        Map<String, Object> response = new HashMap<>();


        NotificationSetting notSettings = notificationSettingsRepo.findbyUsername(extractUsername());

        if(notSettings==null){
            notSettings=NotificationSetting.builder().paymentReminders(true)
                    .lowStockAlert(true)
                    .systemUpdates(true)
                    .username(extractUsername())
                    .updatedBy(extractUsername())
                    .updatedDate(LocalDateTime.now())
                    .build();
            notificationSettingsRepo.save(notSettings);
        }

        response.put("receiveLowStockAlerts", notSettings != null && notSettings.getLowStockAlert() != null ? notSettings.getLowStockAlert() : false);
        response.put("receivePaymentReminders", notSettings != null && notSettings.getPaymentReminders() != null ? notSettings.getPaymentReminders() : false);
        response.put("receiveSystemUpdates", notSettings != null && notSettings.getSystemUpdates() != null ? notSettings.getSystemUpdates() : false);


        return response;
    }
    public Map<String, Object> getNotificationSettings(String username) {

        Map<String, Object> response = new HashMap<>();


        NotificationSetting notSettings = notificationSettingsRepo.findbyUsername(username);

        response.put("receiveLowStockAlerts", notSettings != null && notSettings.getLowStockAlert() != null ? notSettings.getLowStockAlert() : false);
        response.put("receivePaymentReminders", notSettings != null && notSettings.getPaymentReminders() != null ? notSettings.getPaymentReminders() : false);
        response.put("receiveSystemUpdates", notSettings != null && notSettings.getSystemUpdates() != null ? notSettings.getSystemUpdates() : false);


        return response;
    }

    public Map<String, Object> checkImportLimit() {
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
       List<GeminiTextExtract> apiLogs= apiSaveRepo.findCreatedWithinLast24Hours(last24Hours, extractUsername(), "Gemini Text Extraction API");
        Integer count=apiLogs.size();
       Map<String, Object> response = new HashMap<>();

       if(extractRole().equals("USER")){
           if(count>2) {
               response.put("count", count);
               response.put("allowed", Boolean.FALSE);
           }
           else{
               response.put("count", count);
               response.put("allowed", Boolean.TRUE);
           }
       }
       else{
           response.put("count", count);
           response.put("allowed", Boolean.TRUE);
       }

        return response;
    }
}
