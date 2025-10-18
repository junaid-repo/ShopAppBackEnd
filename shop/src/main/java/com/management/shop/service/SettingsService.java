package com.management.shop.service;

import com.management.shop.dto.AutoDeleteCustomersSettings;
import com.management.shop.dto.SchedulerSettings;
import com.management.shop.dto.ShopSettings;
import com.management.shop.dto.UiSettings;
import com.management.shop.entity.UserSettingsEntity;
import com.management.shop.repository.UserSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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

        System.out.println("Current user settings: " + userSettings);

     var shopSettigns=   ShopSettings.builder().ui(
                UiSettings.builder()
                        .autoSendInvoice(userSettings.getAutoSendInvoice())
                        .autoPrintInvoice(userSettings.getAutoPrintInvoice())
                        .darkModeDefault(userSettings.getIsDarkModeDefault())
                        .billingPageDefault(userSettings.getIsBillingPageDefault())
                        .build())
                .schedulers(
                    SchedulerSettings.builder()
                            .autoDeleteNotificationsDays(userSettings.getAutoDeleteNotification())
                            .lowStockAlerts(userSettings.getLowStockAlert())
                            .autoDeleteCustomers(
                                    AutoDeleteCustomersSettings.builder()
                                            .enabled(userSettings.getAutoDeleteCustomers())
                                            .inactiveDays(userSettings.getAutoDeleteCustomerForInactiveDays())
                                            .minSpent(userSettings.getAutoDeleteCustomerForMinSpent())
                                            .build()
                            ).build()
                ).build();

       System.out.println("The full shop Settings are "+shopSettigns);

        return shopSettigns;
    }
}
