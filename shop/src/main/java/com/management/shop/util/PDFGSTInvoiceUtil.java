package com.management.shop.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.management.shop.dto.InvoiceData;
import com.management.shop.dto.OrderItemInvoice;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Margin;
import com.microsoft.playwright.options.ScreenshotType;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.*;

@Component
public class PDFGSTInvoiceUtil {

    private final TemplateEngine templateEngine;

    public PDFGSTInvoiceUtil(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generateGSTInvoice(InvoiceData data, String invoiceTemplate, String printerType)   {

        // --- Core Calculations (null-safe) ---
        List<OrderItemInvoice> rawProducts = data.getProducts() != null ? data.getProducts() : Collections.emptyList();

        double taxableAmount = rawProducts.stream()
                .mapToDouble(p -> safeGetDouble(p, "getRate", "getPrice") * safeGetDouble(p, "getQuantity", "getQty"))
                .sum();

        double gstAmount = rawProducts.stream()
                .mapToDouble(p -> safeGetDouble(p, "getTaxAmount", "getTax"))
                .sum();

        double grandTotal = taxableAmount + gstAmount;
        double currentBalance = grandTotal + safeGetDoubleFromPrimitive(data.getPreviousBalance()) - safeGetDoubleFromPrimitive(data.getReceivedAmount());

        String grandTotalInWords = NumberToWordsConverter.convert((long) Math.round(grandTotal));

        // --- QR Code (UPI) ---
        String upiUrl = "upi://pay?pa="+data.getUpiId()+"&pn="+data.getShopName()+"&tn="+data.getInvoiceId()+"&am="+data.getGrandTotal()+"&cu=INR";
        String qrCodeBase64 = QRCodeGenerator.generateQRCodeBase64(nullSafeString(upiUrl), 200, 200);

        // --- Barcode Generation for Invoice ID (Conditional) ---
        String invoiceId = nullSafeString(data.getInvoiceId());
        String invoiceBarcodeBase64 = "";

        // CHECK: Only generate if showInvoiceBarcode is TRUE and invoiceId exists
        if (Boolean.TRUE.equals(data.getShowInvoiceBarcode()) && !invoiceId.isEmpty()) {
            // Width: 300px, Height: 50px
            invoiceBarcodeBase64 = generateBarcodeBase64(invoiceId, 600, 50);
        }

        // --- Convert products to Map for template ---
        List<Map<String, Object>> productsForTemplate = new ArrayList<>();
        for (OrderItemInvoice p : rawProducts) {
            Map<String, Object> m = new HashMap<>();
            m.put("productName", nullSafeString(safeGetString(p, "getProductName", "getName")));
            m.put("description", p.getDescription());
            m.put("hsnCode", nullSafeString(safeGetString(p, "getHsnCode", "getHsn")));
            m.put("quantity", safeGetDouble(p, "getQuantity", "getQty"));
            m.put("rate", safeGetDouble(p, "getRate", "getPrice"));
            m.put("taxAmount", safeGetDouble(p, "getTaxAmount", "getTax"));
            m.put("totalAmount", safeGetDouble(p, "getTotalAmount", "getAmount", "getTotal"));
            m.put("discountPercentage", p.getDiscountPercentage());
            m.put("igstAmount", p.getIgst());
            m.put("igstPercentage", p.getIgstPercentage());
            m.put("cgstAmount", p.getCgst());
            m.put("cgstPercentage", p.getCgstPercentage());
            m.put("sgstAmount", p.getSgst());
            m.put("sgstPercentage", p.getSgstPercentage());

            productsForTemplate.add(m);
        }

        // --- Prepare Thymeleaf Context ---
        Context context = new Context();

        // Shop Logo
        if (data.getShopLogoBytes() != null && data.getShopLogoBytes().length > 0) {
            String shopLogoBase64 = Base64.getEncoder().encodeToString(data.getShopLogoBytes());
            context.setVariable("shopLogoBase64", shopLogoBase64);
        }

        // Shop Details
        context.setVariable("shopName", nullSafeString(data.getShopName()));
        context.setVariable("shopSlogan", nullSafeString(data.getShopSlogan()));
        context.setVariable("shopLogoText", nullSafeString(data.getShopLogoText()));
        context.setVariable("shopAddress", nullSafeString(data.getShopAddress()));
        context.setVariable("shopEmail", nullSafeString(data.getShopEmail()));
        context.setVariable("shopPhone", nullSafeString(data.getShopPhone()));
        context.setVariable("gstNumber", nullSafeString(data.getGstNumber()));
        context.setVariable("panNumber", nullSafeString(data.getPanNumber()));

        // Invoice Details
        context.setVariable("invoiceId", invoiceId);
        // Add the Barcode to context (will be empty string if boolean was false)
        context.setVariable("invoiceBarcodeBase64", invoiceBarcodeBase64);

        context.setVariable("orderedDate", nullSafeString(data.getOrderedDate()));
        context.setVariable("dueDate", nullSafeString(data.getDueDate()));

        // Customer Details
        context.setVariable("customerName", nullSafeString(data.getCustomerName()));
        context.setVariable("customerBillingAddress", nullSafeString(data.getCustomerBillingAddress()));
        context.setVariable("customerShippingAddress", nullSafeString(data.getCustomerShippingAddress()));
        context.setVariable("customerGstNumber", nullSafeString(data.getCustomerGst()));
        context.setVariable("customerPhone", nullSafeString(data.getCustomerPhone()));
        context.setVariable("customerState", nullSafeString(data.getCustomerState()));

        // Products
        context.setVariable("products", productsForTemplate);

        // Financials
        context.setVariable("taxableAmount", taxableAmount);
        context.setVariable("grandTotal", grandTotal);
        context.setVariable("paidAmount", safeGetDoubleFromPrimitive(data.getPaidAmount()));
        context.setVariable("dueAmount", safeGetDoubleFromPrimitive(data.getDueAmount()));
        context.setVariable("totalDiscountAmount", safeGetDoubleFromPrimitive(data.getDiscountPercentage()));
        context.setVariable("receivedAmount", safeGetDoubleFromPrimitive(data.getReceivedAmount()));
        context.setVariable("previousBalance", safeGetDoubleFromPrimitive(data.getPreviousBalance()));
        context.setVariable("currentBalance", currentBalance);
        context.setVariable("grandTotalInWords", nullSafeString(grandTotalInWords));

        // GST Summary
        context.setVariable("gstSummary", data.getGstSummary() != null ? data.getGstSummary() : Collections.emptyList());

        // Bank & Payment
        context.setVariable("bankAccountName", nullSafeString(data.getBankAccountName()));
        context.setVariable("bankAccountNumber", nullSafeString(data.getBankAccountNumber()));
        context.setVariable("bankIfscCode", nullSafeString(data.getBankIfscCode()));
        context.setVariable("bankName", nullSafeString(data.getBankName()));
        context.setVariable("upiId", nullSafeString(data.getUpiId()));
        context.setVariable("qrCodeBase64", nullSafeString(qrCodeBase64));

        // Footer
        context.setVariable("termsAndConditions", data.getTermsAndConditions() != null ? data.getTermsAndConditions() : Collections.emptyList());

        // Conditions
        context.setVariable("showShopPanOnInvoice", data.getPrintShopPan() != null ? data.getPrintShopPan() : true);
        context.setVariable("showCustomerGst", data.getPrintCustomerGst() != null ? data.getPrintCustomerGst() : true);
        context.setVariable("combineAddress", data.getCombineCustomerAddresses() != null ? data.getCombineCustomerAddresses() : false);
        context.setVariable("showIndividualDiscountPercentage", data.getItemDiscount() != null ? data.getItemDiscount() : false);
        context.setVariable("showHsnColumn", data.getShowHsnColumn() != null ? data.getShowHsnColumn() : true);
        context.setVariable("showRateColumn", data.getShowRateColumn() != null ? data.getShowRateColumn() : true);
        context.setVariable("showTotalDiscountPercentage", data.getShowTotalDiscount() != null ? data.getShowTotalDiscount() : false);
        context.setVariable("showDueAmount", data.getPrintDueAmount() != null ? data.getPrintDueAmount() : false);
        context.setVariable("showDueDate", data.getAddDueDate() != null ? data.getShowTotalDiscount() : false);
        context.setVariable("showSupportInfo", data.getShowSupportInfo() != null ? data.getShowSupportInfo() : false);
        context.setVariable("removeTerms", data.getRemoveTerms() != null ? data.getRemoveTerms() : false);

        context.setVariable("printerType", nullSafeString(printerType)); // Pass printer type to template for conditional styling

        System.out.println("The full data to render invoice " + context);

        // --- Generate PDF ---
        String htmlContent = templateEngine.process(invoiceTemplate, context);

        try (Playwright playwright = Playwright.create()) {

            // === ADDED LINUX SERVER LAUNCH OPTIONS ===
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(Arrays.asList("--no-sandbox", "--disable-setuid-sandbox"));

            // Wrapped Browser in try-with-resources to guarantee memory is freed on crash
            try (Browser browser = playwright.chromium().launch(launchOptions)) {

                // 1. Create a new Context to control the Viewport (Width/Height)
                Browser.NewContextOptions contextOptions = new Browser.NewContextOptions();

                // === EXISTING LOGIC FOR DIMENSIONS ===
                if (invoiceTemplate != null && invoiceTemplate.toLowerCase().contains("thermal")) {
                    int viewportHeight = 800; // Base height (Playwright fullPage=true captures full length)

                    // Modern switch expression to directly assign the width
                    int viewportWidth = switch (printerType) {
                        case "THERMAL_2" -> 280; // 58mm thermal (~2 inches)
                        case "THERMAL_3" -> 380; // 80mm thermal (~3.14 inches)
                        case "THERMAL_4" -> 520; // 112mm thermal (~4.4 inches)
                        default          -> 280; // Fallback default
                    };

                    // Apply the dynamic viewport size
                    contextOptions.setViewportSize(viewportWidth, viewportHeight);

                    // Use High DPI (2.0) for crisper text and barcode rendering on small prints
                    contextOptions.setDeviceScaleFactor(2.0);
                } else {
                    // Default A4-like width: A4 at 96 DPI is approx 794px wide.
                    contextOptions.setViewportSize(794, 1123);
                    contextOptions.setDeviceScaleFactor(1.0);
                }
                // ==========================

                // Wrapped Context and Page in try-with-resources
                try (BrowserContext context2 = browser.newContext(contextOptions);
                     Page page = context2.newPage()) {

                    page.setContent(htmlContent);

                    // 2. Configure Screenshot Options
                    Page.ScreenshotOptions screenshotOptions = new Page.ScreenshotOptions()
                            .setType(ScreenshotType.PNG) // Ensure output is PNG
                            .setFullPage(true); // Capture the full scrollable height (Essential for invoices)

                    byte[] imageBytes = page.screenshot(screenshotOptions);

                    // browser.close(); <--- REMOVED: try-with-resources handles this automatically now!

                    return imageBytes;
                }
            }
        } catch (Exception e) {
            // ... existing error handling ...
            throw new RuntimeException("Error generating Invoice Image", e);
        }
    }

    // --- Helper Method for Barcode ---
    private String generateBarcodeBase64(String text, int width, int height) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.CODE_128, width, height);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return Base64.getEncoder().encodeToString(pngOutputStream.toByteArray());
        } catch (Exception e) {
            System.err.println("Failed to generate barcode for text: " + text);
            e.printStackTrace();
            return "";
        }
    }

    // --- Existing Helpers ---

    private String nullSafeString(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private double safeGetDoubleFromPrimitive(Double d) {
        return d == null ? 0.0 : d;
    }

    private String safeGetString(Object bean, String... methodNames) {
        Object val = safeInvoke(bean, methodNames);
        return val == null ? "" : String.valueOf(val);
    }

    private double safeGetDouble(Object bean, String... methodNames) {
        Object val = safeInvoke(bean, methodNames);
        if (val == null) return 0.0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(val));
        } catch (Exception ex) {
            return 0.0;
        }
    }

    private Object safeInvoke(Object bean, String... methodNames) {
        if (bean == null) return null;
        for (String mName : methodNames) {
            try {
                Method m = bean.getClass().getMethod(mName);
                Object v = m.invoke(bean);
                if (v != null) return v;
            } catch (NoSuchMethodException nsme) {
                // try next possibility
            } catch (Exception ignored) {
                // any other problem - ignore and continue
            }
        }
        return null;
    }
}