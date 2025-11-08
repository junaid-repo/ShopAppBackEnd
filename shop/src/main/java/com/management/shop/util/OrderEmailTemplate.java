package com.management.shop.util;

import com.management.shop.dto.InvoiceDetails;
import com.management.shop.dto.OrderItem;
import com.management.shop.dto.SupportTicketRequest;
import com.management.shop.dto.UpdateUserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OrderEmailTemplate {

    @Autowired
    Utility util;

    public Map<String, Object> generateOrderHtml(InvoiceDetails order, String username) {

        Map<String, Object> response = new HashMap<>();

        // --- MODIFICATION: Use NumberFormat for locale-specific currency formatting (Indian style) ---
        NumberFormat indianCurrencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        // We will remove the ".00" for the discount percentage
        NumberFormat percentageFormat = NumberFormat.getNumberInstance();
        percentageFormat.setMaximumFractionDigits(0);

        UpdateUserDTO shopDetails = util.getUserProfile(username);
        String shopName = shopDetails.getShopName();
        String shopEmail = shopDetails.getShopEmail();

        response.put("shopName", shopName);
        response.put("shopEmail", shopEmail);


        // 1. Build the HTML for each item in the order, now using the new currency format
        String itemsHtml = order.getItems().stream().map(item -> {
            double subtotal = item.getQuantity() * item.getUnitPrice();
            return "<tr>" +
                    "<td style=\"padding: 12px 15px; text-align: left; border-bottom: 1px solid #e0e0e0;\">" + item.getProductName() + "<br><span style='font-size: 11px; color: #888;'>Unit Price: " + indianCurrencyFormat.format(item.getUnitPrice()) + "</span></td>" +
                    "<td style=\"padding: 12px 15px; text-align: center; border-bottom: 1px solid #e0e0e0;\">" + item.getQuantity() + "</td>" +
                    "<td style=\"padding: 12px 15px; text-align: right; border-bottom: 1px solid #e0e0e0;\">" + indianCurrencyFormat.format(subtotal) + "</td>" +
                    "</tr>";
        }).collect(Collectors.joining(""));

        // 2. Perform clear calculations for the summary
        double subtotalBeforeDiscount = order.getItems().stream()
                .mapToDouble(item -> item.getQuantity() * item.getUnitPrice())
                .sum();
        double discountAmount = subtotalBeforeDiscount * (order.getDiscountRate() / 100.0);
        double gstAmount = order.getGstRate();
        double grandTotal = order.getTotalAmount();

        // 3. Determine payment status and corresponding CSS class for styling
        String paymentStatus = order.isPaid() ? "PAID" : "PENDING";
        String statusClass = order.isPaid() ? "status-paid" : "status-pending";

        // --- Template remains the same ---
        String htmlTemplate = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Order Confirmation</title>
              <style>
                body { margin: 0; padding: 0; background-color: #f7f8fa; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; }
                .email-wrapper { width: 100%; background-color: #f7f8fa; padding: 25px 0; }
                .email-container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 8px 24px rgba(0,0,0,0.08); border: 1px solid #e9ecef; }
                .header { background: linear-gradient(135deg, #6a11cb 0%, #2575fc 100%); color: #ffffff; padding: 35px; text-align: center; }
                .header img { height: 40px; margin-bottom: 15px; }
                .header h1 { margin: 0; font-size: 26px; font-weight: 600; }
                .content { padding: 35px; color: #5a6474; line-height: 1.6; font-size: 14px; }
                .content h2 { font-size: 20px; color: #212529; margin-top: 0; font-weight: 600; }
                .details-table { width: 100%; margin: 25px 0; border-collapse: collapse; }
                .details-table td { padding: 6px 0; font-size: 13px; }
                .items-table { width: 100%; border-collapse: collapse; margin-top: 25px; font-size: 13px; }
                .items-table th { background-color: #f8f9fa; padding: 12px 15px; text-align: left; color: #3440; font-size: 12px; font-weight: 600; text-transform: uppercase; }
                .summary-table { width: 100%; max-width: 280px; margin-left: auto; margin-top: 25px; font-size: 14px; }
                .summary-table td { padding: 9px 0; text-align: right; }
                .summary-table .label { text-align: left; color: #5a6474; }
                .grand-total { font-size: 18px; font-weight: bold; color: #5E35B1; }
                .status-box { padding: 12px; border-radius: 8px; text-align: center; font-weight: 600; margin-top: 25px; font-size: 13px; }
                .status-paid { background-color: #e6f7f0; color: #1b8751; border: 1px solid #b3e0c8; }
                .status-pending { background-color: #fff8e1; color: #f59f0b; border: 1px solid #ffecb3; }
                .footer { background-color: #f8f9fa; padding: 25px; text-align: center; font-size: 12px; color: #868e96; }
                a { color: #5E35B1; text-decoration: none; font-weight: 600; }
                @media screen and (max-width: 600px) {
                    .content { padding: 25px; }
                    .header { padding: 30px; }
                    .header h1 { font-size: 24px; }
                }
              </style>
            </head>
            <body>
              <div class="email-wrapper">
                <div class="email-container">
                  <div class="header">
                    <h1>Thank You For Your Order!</h1>
                  </div>
                  <div class="content">
                    <h2>Hello {{customerName}},</h2>
                    <p>Your order has been successful. Here is a summary of your purchase.</p>
                    
                    <table class="details-table">
                      <tr>
                        <td><strong>Order Number:</strong></td>
                        <td style="text-align: right;">{{orderNumber}}</td>
                      </tr>
                      <tr>
                        <td><strong>Order Date:</strong></td>
                        <td style="text-align: right;">{{orderDate}}</td>
                      </tr>
                    </table>

                    <table class="items-table">
                      <thead>
                        <tr>
                          <th style="text-align: left;">Product</th>
                          <th style="text-align: center;">Qty</th>
                          <th style="text-align: right;">Total</th>
                        </tr>
                      </thead>
                      <tbody>
                        {{orderItems}}
                      </tbody>
                    </table>

                    <table class="summary-table">
                        <tr>
                          <td class="label">Subtotal:</td>
                          <td>{{subtotalBeforeDiscount}}</td>
                        </tr>
                        <tr>
                          <td class="label">Discount ({{discountPercentage}}%):</td>
                          <td>- {{discountAmount}}</td>
                        </tr>
                        <tr>
                            <td class="label">GST:</td>
                            <td>+ {{gstAmount}}</td>
                        </tr>
                        <tr><td colspan="2" style="padding-top: 10px; padding-bottom: 10px;"><hr style="border: 0; border-top: 1px solid #e9ecef;"></td></tr>
                        <tr class="grand-total">
                            <td class="label">Grand Total:</td>
                            <td>{{grandTotal}}</td>
                        </tr>
                    </table>

                    <div class="status-box {{statusClass}}">
                        Payment Status: {{paymentStatus}}
                    </div>
                  </div>
                  <div class="footer">
                    <p><strong>{{shopName}}</strong></p>
                    <p>If you have any questions, please contact us at <a href="mailto:{{shopEmail}}">{{shopEmail}}</a></p>
                  </div>
                </div>
              </div>
            </body>
            </html>
            """;

        // --- FIX: Assign the result of the replacements to a new variable ---
        String finalHtml = htmlTemplate
                .replace("{{customerName}}", order.getCustomerName())
                .replace("{{orderNumber}}", order.getInvoiceId())
                .replace("{{orderDate}}", order.getOrderedDate())
                .replace("{{orderItems}}", itemsHtml)
                .replace("{{subtotalBeforeDiscount}}", indianCurrencyFormat.format(subtotalBeforeDiscount-gstAmount))
                .replace("{{discountPercentage}}", percentageFormat.format(order.getDiscountRate()))
                .replace("{{discountAmount}}", indianCurrencyFormat.format(discountAmount))
                .replace("{{gstAmount}}", indianCurrencyFormat.format(gstAmount))
                .replace("{{grandTotal}}", indianCurrencyFormat.format(grandTotal))
                .replace("{{paymentStatus}}", paymentStatus)
                .replace("{{statusClass}}", statusClass)
                .replace("{{shopName}}", shopName)
                .replace("{{shopEmail}}", shopEmail);

        // --- FIX: Put the final, processed HTML into the map ---
        response.put("htmlTemplate", finalHtml);

        return response;
    }


    public String getTicketCreationMailConent(SupportTicketRequest ticket, String username) {

        // 1. Format the date and time for display
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        String formattedDate = LocalDateTime.now().format(formatter);

        // 2. Determine status class for styling (e.g., green for 'Open')
        String statusClass = "status-open"; // Default class for new tickets

        // 3. Define the new HTML template for the ticket notification
        String htmlTemplate = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>New Support Ticket</title>
                  <style>
                    body { margin: 0; padding: 0; background-color: #f7f8fa; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; }
                    .email-wrapper { width: 100%; background-color: #f7f8fa; padding: 25px 0; }
                    .email-container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 8px 24px rgba(0,0,0,0.08); border: 1px solid #e9ecef; }
                    .header { background: linear-gradient(135deg, #007bff 0%, #2575fc 100%); color: #ffffff; padding: 35px; text-align: center; }
                    .header h1 { margin: 0; font-size: 26px; font-weight: 600; }
                    .content { padding: 35px; color: #5a6474; line-height: 1.6; font-size: 14px; }
                    .content h2 { font-size: 20px; color: #212529; margin-top: 0; font-weight: 600; }
                    .details-table { width: 100%; margin: 25px 0; border-collapse: collapse; }
                    .details-table td { padding: 8px 0; font-size: 14px; border-bottom: 1px solid #e9ecef; }
                    .details-table td strong { color: #343a40; }
                    .summary-box { background-color: #f8f9fa; border-left: 4px solid #007bff; padding: 15px; margin: 25px 0; font-style: italic; color: #495057; }
                    .action-button { display: inline-block; background-color: #007bff; color: #ffffff; padding: 12px 25px; margin-top: 20px; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 15px; }
                    .status-box { padding: 12px; border-radius: 8px; text-align: center; font-weight: 600; margin-top: 25px; font-size: 13px; }
                    .status-open { background-color: #e6f7f0; color: #1b8751; border: 1px solid #b3e0c8; }
                    .footer { background-color: #f8f9fa; padding: 25px; text-align: center; font-size: 12px; color: #868e96; }
                    @media screen and (max-width: 600px) {
                      .content { padding: 25px; }
                      .header { padding: 30px; }
                      .header h1 { font-size: 24px; }
                    }
                  </style>
                </head>
                <body>
                  <div class="email-wrapper">
                    <div class="email-container">
                      <div class="header">
                        <h1>New Support Ticket Created</h1>
                      </div>
                      <div class="content">
                        <h2>Hello Admin,</h2>
                        <p>A new support ticket has been created and requires your attention. Please find the details below.</p>
                        
                        <table class="details-table">
                          <tr>
                            <td><strong>Ticket Number:</strong></td>
                            <td style="text-align: right;">{{ticketNumber}}</td>
                          </tr>
                          <tr>
                            <td><strong>Created By:</strong></td>
                            <td style="text-align: right;">{{username}}</td>
                          </tr>
                          <tr>
                            <td><strong>Date Created:</strong></td>
                            <td style="text-align: right;">{{createdDate}}</td>
                          </tr>
                           <tr>
                            <td><strong>Topic:</strong></td>
                            <td style="text-align: right;">{{topic}}</td>
                          </tr>
                        </table>

                        <p style="margin-top: 30px; font-weight: 600; color: #343a40;">Summary of Issue:</p>
                        <div class="summary-box">
                          {{summary}}
                        </div>

                        <div class="status-box {{statusClass}}">
                          Current Status: <strong>{{status}}</strong>
                        </div>
                        
                        <div style="text-align: center;">
                           <a href="{{adminDashboardUrl}}" class="action-button">View Ticket in Dashboard</a>
                        </div>
                      </div>
                      <div class="footer">
                        <p>This is an automated notification from your support system.</p>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """;

        // 4. Replace placeholders with actual ticket data
        String finalHtml = htmlTemplate
                .replace("{{ticketNumber}}", ticket.getTicketNumber())
                .replace("{{username}}", ticket.getUsername())
                .replace("{{createdDate}}", formattedDate)
                .replace("{{topic}}", ticket.getTopic())
                .replace("{{summary}}", ticket.getSummary())
                .replace("{{status}}", ticket.getStatus())
                .replace("{{statusClass}}", statusClass);

        return finalHtml;
    }
    public String getPaymentReminderEmailContent(String orderNo, double totalAmount, double paidAmount, double dueAmount,
                                                 String customerName, String customMessage) {

        // 1. Format the date and time for display
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        String formattedDate = LocalDateTime.now().format(formatter);

        // 2. Format currency
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        String formattedTotal = currencyFormatter.format(totalAmount);
        String formattedPaid = currencyFormatter.format(paidAmount);
        String formattedDue = currencyFormatter.format(dueAmount);

        // 3. Determine status and styling
        String status = (paidAmount > 0) ? "Partially Paid" : "Payment Due";
        String statusClass = "status-due"; // A new CSS class for outstanding payments

        // 4. Handle optional custom message
        String finalMessage = (customMessage == null || customMessage.trim().isEmpty())
                ? "This is a friendly reminder that the following invoice has an outstanding balance."
                : customMessage;

        // 5. Define the HTML template
        String htmlTemplate = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Payment Reminder</title>
                  <style>
                    body { margin: 0; padding: 0; background-color: #f7f8fa; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; }
                    .email-wrapper { width: 100%; background-color: #f7f8fa; padding: 25px 0; }
                    .email-container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 8px 24px rgba(0,0,0,0.08); border: 1px solid #e9ecef; }
                    .header { background: linear-gradient(135deg, #007bff 0%, #2575fc 100%); color: #ffffff; padding: 35px; text-align: center; }
                    .header h1 { margin: 0; font-size: 26px; font-weight: 600; }
                    .content { padding: 35px; color: #5a6474; line-height: 1.6; font-size: 14px; }
                    .content h2 { font-size: 20px; color: #212529; margin-top: 0; font-weight: 600; }
                    .details-table { width: 100%; margin: 25px 0; border-collapse: collapse; }
                    .details-table td { padding: 8px 0; font-size: 14px; border-bottom: 1px solid #e9ecef; }
                    .details-table td strong { color: #343a40; }
                    
                    /* Changed border color to red for emphasis */
                    .summary-box { background-color: #f8f9fa; border-left: 4px solid #c62828; padding: 15px; margin: 25px 0; font-style: italic; color: #495057; }
                    
                    .action-button { display: inline-block; background-color: #007bff; color: #ffffff; padding: 12px 25px; margin-top: 20px; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 15px; }
                    .status-box { padding: 12px; border-radius: 8px; text-align: center; font-weight: 600; margin-top: 25px; font-size: 13px; }
                    
                    /* New class for due/unpaid status */
                    .status-due { background-color: #fbebee; color: #c62828; border: 1px solid #f0c7c7; }
                    
                    .footer { background-color: #f8f9fa; padding: 25px; text-align: center; font-size: 12px; color: #868e96; }
                    @media screen and (max-width: 600px) {
                      .content { padding: 25px; }
                      .header { padding: 30px; }
                      .header h1 { font-size: 24px; }
                    }
                  </style>
                </head>
                <body>
                  <div class="email-wrapper">
                    <div class="email-container">
                      <div class="header">
                        <h1>Payment Reminder</h1>
                      </div>
                      <div class="content">
                        <h2>Hello {{customerName}},</h2>
                        <p>This is a friendly reminder regarding an outstanding payment for your invoice. Please find the details below.</p>
                        
                        <table class="details-table">
                          <tr>
                            <td><strong>Invoice Number:</strong></td>
                            <td style="text-align: right;">{{orderNo}}</td>
                          </tr>
                          <tr>
                            <td><strong>Reminder Date:</strong></td>
                            <td style="text-align: right;">{{reminderDate}}</td>
                          </tr>
                          <tr>
                            <td><strong>Total Amount:</strong></td>
                            <td style="text-align: right;">{{totalAmount}}</td>
                          </tr>
                          <tr>
                            <td><strong>Amount Paid:</strong></td>
                            <td style="text-align: right;">{{paidAmount}}</td>
                          </tr>
                          <tr style="font-weight: bold; font-size: 16px;">
                            <td><strong>Amount Due:</strong></td>
                            <td style="text-align: right; color: #c62828;">{{dueAmount}}</td>
                          </tr>
                        </table>
    
                        <p style="margin-top: 30px; font-weight: 600; color: #343a40;">Message from our team:</p>
                        <div class="summary-box">
                          {{customMessage}}
                        </div>
    
                        <div class="status-box {{statusClass}}">
                          Payment Status: <strong>{{status}}</strong>
                        </div>
                        
                       
                      </div>
                      <div class="footer">
                        <p>This is an automated notification. Please contact us if you have any questions.</p>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """;

        // 6. Replace placeholders with actual data
        String finalHtml = htmlTemplate
                .replace("{{customerName}}", customerName)
                .replace("{{orderNo}}", orderNo)
                .replace("{{reminderDate}}", formattedDate)
                .replace("{{totalAmount}}", formattedTotal)
                .replace("{{paidAmount}}", formattedPaid)
                .replace("{{dueAmount}}", formattedDue)
                .replace("{{customMessage}}", finalMessage)
                .replace("{{status}}", status)
                .replace("{{statusClass}}", statusClass);

        return finalHtml;
    }
    public String generateSupportEmailHtml(String username, String subject, String body) {

        // 1. Format the current date and time for display
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        String formattedDate = LocalDateTime.now().format(formatter);

        // 2. Define the HTML template for the support email notification
        String htmlTemplate = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>New Support Email</title>
                  <style>
                    body { margin: 0; padding: 0; background-color: #f7f8fa; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; }
                    .email-wrapper { width: 100%; background-color: #f7f8fa; padding: 25px 0; }
                    .email-container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 8px 24px rgba(0,0,0,0.08); border: 1px solid #e9ecef; }
                    .header { background: linear-gradient(135deg, #ff8c00 0%, #ffc107 100%); color: #ffffff; padding: 35px; text-align: center; }
                    .header h1 { margin: 0; font-size: 26px; font-weight: 600; }
                    .content { padding: 35px; color: #5a6474; line-height: 1.6; font-size: 14px; }
                    .details-table { width: 100%; margin: 25px 0; border-collapse: collapse; }
                    .details-table td { padding: 8px 0; font-size: 14px; border-bottom: 1px solid #e9ecef; }
                    .details-table td strong { color: #343a40; }
                    .message-box { background-color: #f8f9fa; border-left: 4px solid #ffc107; padding: 20px; margin: 25px 0; color: #495057; white-space: pre-wrap; word-wrap: break-word; }
                    .footer { background-color: #f8f9fa; padding: 25px; text-align: center; font-size: 12px; color: #868e96; }
                    @media screen and (max-width: 600px) {
                      .content { padding: 25px; }
                      .header { padding: 30px; }
                      .header h1 { font-size: 24px; }
                    }
                  </style>
                </head>
                <body>
                  <div class="email-wrapper">
                    <div class="email-container">
                      <div class="header">
                        <h1>New Support Email Received</h1>
                      </div>
                      <div class="content">
                        <p>A user has sent a message via the support email form. Please find the details below.</p>
                        
                        <table class="details-table">
                          <tr>
                            <td><strong>From User:</strong></td>
                            <td style="text-align: right;">{{username}}</td>
                          </tr>
                           <tr>
                            <td><strong>Date Sent:</strong></td>
                            <td style="text-align: right;">{{sentDate}}</td>
                          </tr>
                          <tr>
                            <td><strong>Subject:</strong></td>
                            <td style="text-align: right;">{{subject}}</td>
                          </tr>
                        </table>

                        <p style="margin-top: 30px; font-weight: 600; color: #343a40;">User's Message:</p>
                        <div class="message-box">
                          {{body}}
                        </div>

                      </div>
                      <div class="footer">
                        <p>This is an automated notification from your support system.</p>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """;

        // 3. Replace placeholders with the provided data
        String finalHtml = htmlTemplate
                .replace("{{username}}", username)
                .replace("{{sentDate}}", formattedDate)
                .replace("{{subject}}", subject)
                .replace("{{body}}", body);

        return finalHtml;
    }

    public String getSubscriptionSuccessEmailContent(Map<String, Object> subscriptionDetails, String username) {

        // 1. Format the data for display
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

        // Extract and cast values from the Map
        LocalDateTime startDate = (LocalDateTime) subscriptionDetails.get("startDate");
        LocalDateTime endDate = (LocalDateTime) subscriptionDetails.get("endDate");
        String formattedStartDate = startDate.format(formatter);
        String formattedEndDate = endDate.format(formatter);

        // Assuming 'price' is stored as paisa (e.g., 19900.0)
        // Cast to Number to handle BigDecimal, Double, or Long
        Number priceInPaisa = (Number) subscriptionDetails.get("price");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        String formattedPrice = currencyFormatter.format(priceInPaisa.doubleValue() / 100.0);

        String planTypeRaw = (String) subscriptionDetails.get("planType");
        String statusRaw = (String) subscriptionDetails.get("status");
        String subscriptionId = (String) subscriptionDetails.get("subscriptionId");

        // Make Plan Type and Status user-friendly (e.g., "Monthly", "Active")
        String planType = planTypeRaw.substring(0, 1).toUpperCase()
                + planTypeRaw.substring(1).toLowerCase();

        String status = statusRaw.substring(0, 1).toUpperCase()
                + statusRaw.substring(1).toLowerCase();

        // 2. Define the HTML template (this remains unchanged)
        String htmlTemplate = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-g">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Welcome to Premium!</title>
              <style>
                body { margin: 0; padding: 0; background-color: #f7f8fa; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; }
                .email-wrapper { width: 100%; background-color: #f7f8fa; padding: 25px 0; }
                .email-container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 8px 24px rgba(0,0,0,0.08); border: 1px solid #e9ecef; }
                .header { background: linear-gradient(135deg, #f7b733 0%, #f59e0b 100%); color: #333; padding: 35px; text-align: center; }
                .header img { width: 60px; height: 60px; margin-bottom: 15px; }
                .header h1 { margin: 0; font-size: 26px; font-weight: 600; }
                .content { padding: 35px; color: #5a6474; line-height: 1.6; font-size: 15px; }
                .content h2 { font-size: 22px; color: #212529; margin-top: 0; font-weight: 600; }
                .details-table { width: 100%; margin: 30px 0; border-collapse: collapse; }
                .details-table td { padding: 10px 0; font-size: 14px; border-bottom: 1px solid #e9ecef; }
                .details-table td strong { color: #343a40; }
                .status-box { padding: 10px; border-radius: 8px; font-weight: 600; font-size: 13px; display: inline-block; }
                .status-active { background-color: #e6f7f0; color: #1b8751; }
                .features-box { background-color: #f8f9fa; border-left: 4px solid #f59e0b; padding: 20px; margin-top: 30px; }
                .features-box h4 { margin-top: 0; color: #343a40; font-weight: 600; }
                .features-box ul { margin: 15px 0 0 0; padding-left: 20px; color: #495057; }
                .action-button { display: inline-block; background-color: #f59e0b; color: #ffffff; padding: 12px 25px; margin-top: 30px; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 16px; }
                .footer { background-color: #f8f9fa; padding: 25px; text-align: center; font-size: 12px; color: #868e96; }
              </style>
            </head>
            <body>
              <div class="email-wrapper">
                <div class="email-container">
                  <div class="header">
                    <img src="https://i.imgur.com/EktPEdC.png" alt="Premium Crown">
                    <h1>Welcome to Premium!</h1>
                  </div>
                  <div class="content">
                    <h2>Hello, {{username}}!</h2>
                    <p>Your upgrade is complete! You now have access to all Premium features. We're thrilled to have you on board.</p>
                    
                    <p style="margin-top: 30px; font-weight: 600; color: #343a40;">Here are your subscription details:</p>
                    <table class="details-table">
                      <tr>
                        <td><strong>Subscription ID:</strong></td>
                        <td style="text-align: right;">{{subscriptionId}}</td>
                      </tr>
                      <tr>
                        <td><strong>Plan:</strong></td>
                        <td style="text-align: right;">{{planType}}</td>
                      </tr>
                      <tr>
                        <td><strong>Amount Paid:</strong></td>
                        <td style="text-align: right; font-weight: 600;">{{amountPaid}}</td>
                      </tr>
                      <tr>
                        <td><strong>Plan Starts:</strong></td>
                        <td style="text-align: right;">{{startDate}}</td>
                      </tr>
                      <tr>
                        <td><strong>Renews On:</strong></td>
                        <td style="text-align: right;">{{endDate}}</td>
                      </tr>
                      <tr>
                        <td><strong>Status:</strong></td>
                        <td style="text-align: right;">
                          <span class="status-box status-active">{{status}}</span>
                        </td>
                      </tr>
                    </table>

                    <div class="features-box">
                      <h4>What's next?</h4>
                      <p>You can now immediately start using your new features:</p>
                      <ul>
                        <li>Create unlimited invoices</li>
                        <li>Use the Bulk CSV Upload for products</li>
                        <li>Explore Advanced Analytics & Reports</li>
                      </ul>
                    </div>
                    
                    <div style="text-align: center;">
                       <a href="/dashboard" class="action-button">Go to Your Dashboard</a>
                    </div>
                  </div>
                  <div class="footer">
                    <p>Thank you for choosing ClearBill. We're here if you need help.</p>
                  </div>
                </div>
              </div>
            </body>
            </html>
            """;

        // 3. Replace placeholders with actual data
        String finalHtml = htmlTemplate
                .replace("{{username}}", username)
                .replace("{{subscriptionId}}", subscriptionId)
                .replace("{{planType}}", planType)
                .replace("{{amountPaid}}", formattedPrice)
                .replace("{{startDate}}", formattedStartDate)
                .replace("{{endDate}}", formattedEndDate)
                .replace("{{status}}", status);

        return finalHtml;
    }
    public String getReportEmailContent(String greetingName, String reportName, String reportDateRange) {

        // 1. Format the data for display
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a");
        String formattedGeneratedDate = LocalDateTime.now().format(formatter);

        // Handle potential nulls or empty strings for a robust method
        String finalGreeting = (greetingName == null || greetingName.isBlank()) ? "Hello," : "Hello, " + greetingName + "!";
        String finalReportName = (reportName == null || reportName.isBlank()) ? "Your Report" : reportName;
        String finalReportDateRange = (reportDateRange == null || reportDateRange.isBlank()) ? "N/A" : reportDateRange;

        // 2. Define the HTML template
        String htmlTemplate = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Your Report is Ready</title>
          <style>
            body { margin: 0; padding: 0; background-color: #f7f8fa; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; }
            .email-wrapper { width: 100%; background-color: #f7f8fa; padding: 25px 0; }
            .email-container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 8px 24px rgba(0,0,0,0.08); border: 1px solid #e9ecef; }
            /* A blue/teal gradient for a more "data" feel */
            .header { background: linear-gradient(135deg, #0288d1 0%, #26a69a 100%); color: #ffffff; padding: 35px; text-align: center; }
            .header img { width: 60px; height: 60px; margin-bottom: 15px; }
            .header h1 { margin: 0; font-size: 26px; font-weight: 600; }
            .content { padding: 35px; color: #5a6474; line-height: 1.6; font-size: 15px; }
            .content h2 { font-size: 22px; color: #212529; margin-top: 0; font-weight: 600; }
            .details-table { width: 100%; margin: 30px 0; border-collapse: collapse; }
            .details-table td { padding: 12px 0; font-size: 14px; border-bottom: 1px solid #e9ecef; }
            .details-table td strong { color: #343a40; }
            .footer { background-color: #f8f9fa; padding: 25px; text-align: center; font-size: 12px; color: #868e96; }
          </style>
        </head>
        <body>
          <div class="email-wrapper">
            <div class="email-container">
              <div class="header">
                <!-- A generic report/document icon -->
                <img src="https://i.imgur.com/w1kXgT4.png" alt="Report Icon">
                <h1>Your Report is Ready</h1>
              </div>
              <div class="content">
                <h2>{{greetingName}}</h2>
                <p>Your requested report is attached to this email. Please find a summary of the report details below.</p>
                
                <table class="details-table">
                  <tr>
                    <td><strong>Report Name:</strong></td>
                    <td style="text-align: right;">{{reportName}}</td>
                  </tr>
                  <tr>
                    <td><strong>Date Range:</strong></td>
                    <td style="text-align: right;">{{reportDateRange}}</td>
                  </tr>
                  <tr>
                    <td><strong>Generated On:</strong></td>
                    <td style="text-align: right;">{{generatedDate}}</td>
                  </tr>
                </table>

                <p style="margin-top: 30px; font-size: 14px; color: #868e96;">
                  This is an automated message. The attached report was generated based on your request.
                </p>
              </div>
              <div class="footer">
                <p>Thank you for using our service.</p>
                <p>&copy; 2025 ClearBill. All rights reserved.</p>
              </div>
            </div>
          </div>
        </body>
        </html>
        """;

        // 3. Replace placeholders with actual data
        String finalHtml = htmlTemplate
                .replace("{{greetingName}}", finalGreeting)
                .replace("{{reportName}}", finalReportName)
                .replace("{{reportDateRange}}", finalReportDateRange)
                .replace("{{generatedDate}}", formattedGeneratedDate);

        return finalHtml;
    }
}

