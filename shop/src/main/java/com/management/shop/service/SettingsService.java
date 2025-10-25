package com.management.shop.service;

import com.management.shop.dto.*;
import com.management.shop.entity.UserSettingsEntity;
import com.management.shop.repository.UserSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class SettingsService {

    @Autowired
    UserSettingsRepository settingsRepo;

    public String extractUsername() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("Current user: " + username);
        //  username="junaid1";
        return username;
    }
    public String saveUserUISettings(UiSettings request) {

        settingsRepo.updateUiSettings(request.isAutoPrintInvoice(), request.isBillingPageDefault(), request.isDarkModeDefault(), extractUsername(), LocalDateTime.now());

        return "saved";
    }
    public String saveUserSchedulerSettings(SchedulerSettings request) {
System.out.println("The scheulder settings to be saved  "+request);
        settingsRepo.updateSchedulerSettings(request.isLowStockAlerts(), request.getAutoDeleteNotificationsDays(), request.getAutoDeleteCustomers().isEnabled(), request.getAutoDeleteCustomers().getInactiveDays(), request.getAutoDeleteCustomers().getMinSpent(), extractUsername(), LocalDateTime.now());

        return "saved";
    }

    public ShopSettings getFullUserSettings() {
        System.out.println("Current user: " + extractUsername());
        UserSettingsEntity userSettings=settingsRepo.findByUsername(extractUsername());

        if(userSettings==null){
            saveUserSettings(extractUsername());
            userSettings=settingsRepo.findByUsername(extractUsername());
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
                        .serialNumberPattern(userSettings != null && userSettings.getSerialNumberPattern() != null ? userSettings.getSerialNumberPattern() : "")
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
                        .showRateColumn( userSettings != null && userSettings.getShowRateColumn() != null ? userSettings.getShowRateColumn() : false)
                        .showHsnColumn( userSettings != null && userSettings.getShowHsnColumn() != null ? userSettings.getShowHsnColumn() : false)


                        .build())
                .build();

       System.out.println("The full shop Settings are "+shopSettings);

        return shopSettings;
    }

    private void saveUserSettings(String username) {

        var userSettings=    UserSettingsEntity.builder()
                .allowNoStockBilling(Boolean.FALSE)
                .autoPrintInvoice(Boolean.FALSE)
                .autoSendInvoice(Boolean.FALSE)
                .hideNoStockProducts(Boolean.TRUE)
                .isDarkModeDefault(Boolean.FALSE)
                .isBillingPageDefault(Boolean.FALSE)
                .lowStockAlert(Boolean.TRUE)
                .serialNumberPattern("FMS")
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

        Boolean doPartialBilling =(Boolean)request.get("showPartialPaymentOption");
        Boolean showRemarksOption= (Boolean)request.get("showRemarksOnSummarySide");


        settingsRepo.updateBillingSettings(autoSendInvoice, allowNoStockBilling, hideNoStockProducts,serialNumberPattern, extractUsername(), LocalDateTime.now() ,doPartialBilling ,showRemarksOption);


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


        settingsRepo.updateInvoiceSettings(addDueDate, combineAddresses, showPaymentStatus, removeTerms, showCustomerGstin, extractUsername(), LocalDateTime.now(),
                showTotalDiscountPercentage, showIndividualDiscountPercentage, showShopPanOnInvoice, showSupportInfoOnInvoice, showRateColumn, showHsnColumn);


        return "saved";
    }
}
