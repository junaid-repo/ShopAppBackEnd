package com.management.shop.util;

import com.management.shop.dto.SubscriptionReceiptData;
import com.microsoft.playwright.options.Margin;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;

import org.thymeleaf.context.Context;
import com.microsoft.playwright.Browser; // <-- ADD
import com.microsoft.playwright.Page;      // <-- ADD
import com.microsoft.playwright.Playwright;  // <-- ADD

@Component
public class SubscriptionInvoiceUtil {


    private final TemplateEngine templateEngine;

    public SubscriptionInvoiceUtil(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generateSubscriptionReceipt(SubscriptionReceiptData data) throws Exception {

        // --- 1. Prepare Thymeleaf Context ---
        Context context = new Context();

        // App Details
        context.setVariable("appName", data.getAppName());
        context.setVariable("appAddress", data.getAppAddress());
        context.setVariable("appGstin", data.getAppGstin());
        context.setVariable("appPhone", data.getAppPhone());
        context.setVariable("appEmail", data.getAppEmail());

        // Invoice Details
        context.setVariable("invoiceId", data.getInvoiceId());
        context.setVariable("invoiceDate", data.getInvoiceDate());

        // User Details
        context.setVariable("userName", data.getUserName());
        context.setVariable("userEmail", data.getUserEmail());
        context.setVariable("userAddress", data.getUserAddress());
        context.setVariable("userGstin", data.getUserGstin());
        context.setVariable("userPhone", data.getUserPhone());

        // Line Item & Summary
        context.setVariable("planName", data.getPlanName());
        context.setVariable("taxableAmount", data.getTaxableAmount());
        context.setVariable("gstSummary", data.getGstSummary()); // Pass the Map directly
        context.setVariable("totalGstAmount", data.getTotalGstAmount());
        context.setVariable("totalAmount", data.getTotalAmount());
        context.setVariable("amountInWords", data.getAmountInWords());

        // --- 2. Process the NEW thermal template ---
        String htmlContent = templateEngine.process("thermal-subscription-receipt", context);

        // --- 3. Generate PDF using Playwright ---
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();

            page.setContent(htmlContent);

            // Set PDF options for an 80mm thermal printer
            Page.PdfOptions pdfOptions = new Page.PdfOptions()
                    .setWidth("80mm") // Set the paper width
                    .setPrintBackground(true)
                    .setMargin(new Margin() // Set very small margins
                            .setTop("2mm")
                            .setRight("2mm")
                            .setBottom("2mm")
                            .setLeft("2mm"))
                    .setScale(1); // Ensure no scaling

            // Let Playwright auto-determine the height

            byte[] pdfBytes = page.pdf(pdfOptions);

            browser.close();
            return pdfBytes;

        } catch (Exception e) {
            // Re-throw or handle as per your app's needs
            throw new Exception("Error generating subscription receipt PDF with Playwright", e);
        }
    }
}