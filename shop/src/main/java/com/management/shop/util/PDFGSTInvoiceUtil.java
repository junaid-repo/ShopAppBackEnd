package com.management.shop.util;

import com.management.shop.dto.OrderItemInvoice;
import org.springframework.beans.factory.annotation.Autowired;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;
import java.util.Base64;
import java.io.ByteArrayOutputStream;
import java.util.List;

public class PDFGSTInvoiceUtil {


    private final TemplateEngine templateEngine;

    public PDFGSTInvoiceUtil(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generateGSTInvoice(
            // Shop Details
            String shopName, String shopSlogan, String shopLogoText, byte[] shopLogoBytes, String shopAddress,
            String shopEmail, String shopPhone, String gstNumber, String panNumber,

            // Invoice Details
            String invoiceId, String orderedDate, String dueDate,

            // Customer Details
            String customerName, String customerBillingAddress, String customerShippingAddress,
            String customerPhone, String customerState,

            // Items
            List<OrderItemInvoice> products,

            // Financial Details
            double gstPercentage, // e.g., 18.0 for the whole invoice
            double receivedAmount, double previousBalance,

            // Bank & Payment Details
            String bankAccountName, String bankAccountNumber, String bankIfscCode,
            String bankName, String upiId,

            Double igstAmount, Double igstPercentage,
            Double cgstAmount, Double cgstPercentage,
            Double sgstAmount, Double sgstPercentage,

            // Footer
            List<String> termsAndConditions
    ) throws Exception {

        // --- Calculations ---
        double taxableAmount = products.stream().mapToDouble(p -> p.getRate() * p.getQuantity()).sum();
        double gstRate = products.stream().mapToDouble(OrderItemInvoice::getTaxAmount).sum();
        double grandTotal = taxableAmount + gstRate;
        double currentBalance = grandTotal + previousBalance - receivedAmount;

        String grandTotalInWords = NumberToWordsConverter.convert((long) grandTotal);
        String qrCodeBase64 = QRCodeGenerator.generateQRCodeBase64(upiId, 200, 200);

        // --- Thymeleaf Context ---
        Context context = new Context();

        // NEW: Handle the logo byte stream
        if (shopLogoBytes != null && shopLogoBytes.length > 0) {
            String shopLogoBase64 = Base64.getEncoder().encodeToString(shopLogoBytes);
            context.setVariable("shopLogoBase64", shopLogoBase64);
        }

        // Shop Details
        context.setVariable("shopName", shopName);
        context.setVariable("shopSlogan", shopSlogan);
        context.setVariable("shopLogoText", shopLogoText);
        context.setVariable("shopAddress", shopAddress);
        context.setVariable("shopEmail", shopEmail);
        context.setVariable("shopPhone", shopPhone);
        context.setVariable("gstNumber", gstNumber);
        context.setVariable("panNumber", panNumber);

        // Invoice Details
        context.setVariable("invoiceId", invoiceId);
        context.setVariable("orderedDate", orderedDate);
        context.setVariable("dueDate", dueDate);

        // Customer Details
        context.setVariable("customerName", customerName);
        context.setVariable("customerBillingAddress", customerBillingAddress);
        context.setVariable("customerShippingAddress", customerShippingAddress);
        context.setVariable("customerPhone", customerPhone);
        context.setVariable("customerState", customerState);

        // Items
        context.setVariable("products", products);

        // Financials
        context.setVariable("taxableAmount", taxableAmount);
        context.setVariable("gstRate", gstRate);
        context.setVariable("grandTotal", grandTotal);
        context.setVariable("gstPercentage", gstPercentage);
        context.setVariable("receivedAmount", receivedAmount);
        context.setVariable("previousBalance", previousBalance);
        context.setVariable("currentBalance", currentBalance);
        context.setVariable("grandTotalInWords", grandTotalInWords);
        context.setVariable("igstAmount", igstAmount);
        context.setVariable("igstPercentage", igstPercentage);
        context.setVariable("cgstAmount", cgstAmount);
        context.setVariable("cgstPercentage", cgstPercentage);
        context.setVariable("sgstAmount", sgstAmount);
        context.setVariable("sgstPercentage", sgstPercentage);

        // Bank & Payment
        context.setVariable("bankAccountName", bankAccountName);
        context.setVariable("bankAccountNumber", bankAccountNumber);
        context.setVariable("bankIfscCode", bankIfscCode);
        context.setVariable("bankName", bankName);
        context.setVariable("upiId", upiId);
        context.setVariable("qrCodeBase64", qrCodeBase64);

        // Footer
        context.setVariable("termsAndConditions", termsAndConditions);


        // --- PDF Generation (No changes here) ---
        String htmlContent = templateEngine.process("invoice", context);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(htmlContent);
        renderer.layout();
        renderer.createPDF(baos);
        return baos.toByteArray();
    }

}
