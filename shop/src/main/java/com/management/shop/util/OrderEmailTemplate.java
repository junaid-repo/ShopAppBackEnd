package com.management.shop.util;

import com.management.shop.dto.InvoiceDetails;
import com.management.shop.dto.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderEmailTemplate {

    public String generateOrderHtml(InvoiceDetails order) {
        // Build table rows for items
        StringBuilder itemsHtml = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            double subtotal = item.getQuantity() * item.getUnitPrice();
            itemsHtml.append("<tr>")
                    .append("<td>").append(item.getProductName()).append("</td>")
                    .append("<td>").append(item.getQuantity()).append("</td>")
                    .append("<td>₹").append(String.format("%.2f", item.getUnitPrice())).append("</td>")
                    .append("<td>₹").append(String.format("%.2f", subtotal)).append("</td>")
                    .append("</tr>");
        }

        // Calculations
        double gstAmount = order.getGstRate();
        double discountAmount = order.getDiscountRate();
        double grandTotal = order.getTotalAmount() ;

        String paymentStatus = order.isPaid() ? "PAID" : "PENDING";
        String statusColor = order.isPaid() ? "green" : "red";

        // HTML template with {{placeholders}}
        String htmlTemplate = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <title>Order Confirmation</title>
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <style>
                body { margin:0; padding:0; background-color:#f5f7fa; font-family:Arial,Helvetica,sans-serif; }
                .email-container { max-width:650px; margin:auto; background:#ffffff; border-radius:8px;
                                   overflow:hidden; box-shadow:0 4px 12px rgba(0,0,0,0.1); }
                .header { background:linear-gradient(90deg,#2196F3,#1565C0); color:white; text-align:center; padding:20px; }
                .header h1 { margin:0; font-size:24px; }
                .content { padding:20px; color:#333333; }
                .content h2 { font-size:20px; margin-top:0; color:#1565C0; }
                .details { background:#f9f9f9; border:1px solid #eeeeee; border-radius:6px; padding:15px; margin-bottom:20px; }
                .details p { margin:6px 0; font-size:14px; }
                .items { width:100%; border-collapse:collapse; margin-bottom:20px; }
                .items th, .items td { border:1px solid #dddddd; padding:10px; text-align:left; font-size:14px; }
                .items th { background:#f0f0f0; }
                .summary { margin-top:20px; font-size:15px; }
                .summary p { margin:4px 0; text-align:right; }
                .summary strong { color:#1565C0; }
                .total { text-align:right; font-size:18px; font-weight:bold; margin-top:10px; color:#1565C0; }
                .status { margin-top:15px; font-size:14px; font-weight:bold; color:{{statusColor}}; }
                .footer { background:#f0f0f0; text-align:center; padding:15px; font-size:12px; color:#666666; }
              </style>
            </head>
            <body>
              <div class="email-container">
                <div class="header"><h1>Order Confirmation</h1></div>
                <div class="content">
                  <h2>Hello {{customerName}},</h2>
                  <p>Thank you for your order! Here are your order details:</p>
                  <div class="details">
                    <p><strong>Customer Name:</strong> {{customerName}}</p>
                    <p><strong>Email:</strong> {{customerEmail}}</p>
                    <p><strong>Phone:</strong> {{customerPhone}}</p>
                    <p><strong>Order No.:</strong> {{orderNumber}}</p>
                    <p><strong>Order Date.:</strong> {{orderDate}}</p>
                  </div>
                  <table class="items">
                    <thead>
                      <tr>
                        <th>Product</th>
                        <th>Qty</th>
                        <th>Unit Price</th>
                        <th>Subtotal</th>
                      </tr>
                    </thead>
                    <tbody>
                      {{orderItems}}
                    </tbody>
                  </table>
                  <div class="summary">
                    <p>Subtotal: ₹{{subtotal}}</p>
                    <p>GST amount: ₹{{gstAmount}}</p>
                    
                  </div>
                  <p class="total">Grand Total: ₹{{grandTotal}}</p>
                  <p class="status">Payment Status: {{paymentStatus}}</p>
                </div>
                <div class="footer">
                  <p>Thank you for shopping with <strong>Friends Mobile</strong>.</p>
                  <p>If you have any questions, contact us at 
                     <a href="mailto:help@friendsmobile.store">help@friendsmobile.store</a></p>
                </div>
              </div>
            </body>
            </html>
            """;

        // Replace placeholders
        return htmlTemplate
                .replace("{{customerName}}", order.getCustomerName())
                .replace("{{customerEmail}}", order.getCustomerEmail())
                .replace("{{customerPhone}}", order.getCustomerPhone())
                .replace("{{orderNumber}}", order.getInvoiceId())
                .replace("{{orderItems}}", itemsHtml.toString())
                .replace("{{subtotal}}", String.valueOf( order.getTotalAmount()-order.getGstRate()))
                //.replace("{{gstRate}}", String.valueOf( "18%"))
                .replace("{{gstAmount}}", String.valueOf(gstAmount))
                .replace("{{discountRate}}", String.format("%.0f", order.getDiscountRate() * 100))
                .replace("{{discountAmount}}", String.format("%.2f", discountAmount))
                .replace("{{grandTotal}}", String.format("%.2f", grandTotal))
                .replace("{{paymentStatus}}", paymentStatus)
                .replace("{{orderDate}}", order.getOrderedDate())
                .replace("{{statusColor}}", statusColor);
    }
}
