package com.management.shop.repository;

import com.management.shop.entity.UserSettingsEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface UserSettingsRepository extends JpaRepository<UserSettingsEntity, Integer> {

    @Modifying
    @Transactional
    @Query(value="update user_settings_entity us set auto_print_invoice=?1, is_billing_page_default=?2, is_dark_mode_default=?3, updated_date=?5 where username=?4", nativeQuery = true)
    void updateUiSettings(boolean autoPrintInvoice, boolean billingPageDefault, boolean darkModeDefault, String username, LocalDateTime updatedDate);


    @Modifying
    @Transactional
    @Query(value="update user_settings_entity us set low_stock_alert=?1, auto_delete_notification=?2, auto_delete_customers=?3, auto_delete_customer_for_inactive_days=?4, auto_delete_customer_for_min_spent=?5, updated_date=?7 where username=?6", nativeQuery = true)
    void updateSchedulerSettings(boolean lowStockAlerts, int autoDeleteNotificationsDays, boolean autoDeleteCustomers, int inactiveDays, int minSpent, String username, LocalDateTime updatedDate);

    UserSettingsEntity findByUsername(String username);


    @Modifying
    @Transactional
    @Query(value="update user_settings_entity us set auto_send_invoice=?1, allow_no_stock_billing=?2, hide_no_stock_products=?3, serial_number_pattern=?4, updated_date=?6, show_partial_payment_option=?7, show_remarks_options=?8 where username=?5", nativeQuery = true)
    void updateBillingSettings(Boolean autoSendInvoice, Boolean allowNoStockBilling, Boolean hideNoStockProducts, String serialNumberPattern, String s, LocalDateTime now,  Boolean doPartialBilling, Boolean showRemarksOption);

    @Modifying
    @Transactional
    @Query(value="update user_settings_entity us set add_due_date=?1, combine_addresses=?2, show_payment_status=?3, remove_terms=?4, show_customer_gstin=?5, updated_date=?7, show_total_discount=?8, show_item_discount=?9, show_shop_pan=?10, show_support_info=?11, show_rate_column=?12, show_hsn_column=?13   where username=?6", nativeQuery = true)
    void updateInvoiceSettings(Boolean addDueDate, Boolean combineAddresses, Boolean showPaymentStatus, Boolean removeTerms, Boolean showCustomerGstin, String s, LocalDateTime now, Boolean showTotalDiscountPercentage, Boolean showIndividualDiscountPercentage, Boolean showShopPanOnInvoice, Boolean showSupportInfoOnInvoice, Boolean showRateColumn, Boolean showHsnColumn);
}
