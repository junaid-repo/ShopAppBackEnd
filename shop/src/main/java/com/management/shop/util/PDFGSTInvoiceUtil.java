package com.management.shop.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.management.shop.dto.InvoiceData;
import com.management.shop.dto.OrderItemInvoice;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ScreenshotType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // <-- ADDED IMPORT
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.*;

@Component
public class PDFGSTInvoiceUtil {

    private final TemplateEngine templateEngine;

    // --- ADDED: Inject the active Spring profile (defaults to 'prod' if not found) ---
    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    @Autowired
    private Environment environment;

    public PDFGSTInvoiceUtil(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generateGSTInvoice(InvoiceData data, String invoiceTemplate, String printerType)   {

        // --- Core Calculations (null-safe & rounded to nearest integer) ---
        List<OrderItemInvoice> rawProducts = data.getProducts() != null ? data.getProducts() : Collections.emptyList();

        long taxableAmount = Math.round(rawProducts.stream()
                .mapToDouble(p -> safeGetDouble(p, "getRate", "getPrice") * safeGetDouble(p, "getQuantity", "getQty"))
                .sum());

        long gstAmount = Math.round(rawProducts.stream()
                .mapToDouble(p -> safeGetDouble(p, "getTaxAmount", "getTax"))
                .sum());

        long grandTotal = taxableAmount + gstAmount;
        long currentBalance = Math.round(grandTotal + safeGetDoubleFromPrimitive(data.getPreviousBalance()) - safeGetDoubleFromPrimitive(data.getReceivedAmount()));

        String grandTotalInWords = NumberToWordsConverter.convert(grandTotal);

        // --- QR Code (UPI) ---
        // Using the rounded grandTotal for the UPI string instead of the raw data value
        String upiUrl = "upi://pay?pa="+data.getUpiId()+"&pn="+data.getShopName()+"&tn="+data.getInvoiceId()+"&am="+grandTotal+"&cu=INR";
        String qrCodeBase64="";

        if(printerType != null && printerType.contains("THERMAL")){

            qrCodeBase64 = QRCodeGenerator.generateQRCodeBase64(nullSafeString(data.getInvoiceId()), 200, 200);
        }
        else{
            qrCodeBase64 = QRCodeGenerator.generateQRCodeBase64(nullSafeString(upiUrl), 300, 300);
        }



        // --- Barcode Generation for Invoice ID (Conditional) ---
        String invoiceId = nullSafeString(data.getInvoiceId());
        String invoiceBarcodeBase64 = "";

        if (Boolean.TRUE.equals(data.getShowInvoiceBarcode()) && !invoiceId.isEmpty()) {
            int barcodeWidth = 800; // Default for A4
            int barcodeHeight = 80; // Increased default A4 height

            if (printerType != null && printerType.contains("THERMAL")) {
                // Scale width dynamically
                barcodeWidth = switch (printerType) {
                    case "THERMAL_2" -> 250; // 58mm
                    case "THERMAL_3" -> 400; // 80mm
                    case "THERMAL_4" -> 650; // 112mm
                    default          -> 250;
                };

                // Scale height dynamically so it doesn't look like a tiny sliver on wider paper
                barcodeHeight = switch (printerType) {
                    case "THERMAL_2" -> 45; // Increased by 5 units
                    case "THERMAL_3" -> 65;
                    case "THERMAL_4" -> 90;
                    default          -> 45;
                };
            }
            invoiceBarcodeBase64 = generateBarcodeBase64ForInvoice(invoiceId, barcodeWidth, barcodeHeight);
        }

        // --- Convert products to Map for template (Rounding Amounts Only) ---
        List<Map<String, Object>> productsForTemplate = new ArrayList<>();
        for (OrderItemInvoice p : rawProducts) {
            Map<String, Object> m = new HashMap<>();
            m.put("productName", nullSafeString(safeGetString(p, "getProductName", "getName")));
            m.put("description", p.getDescription());
            m.put("hsnCode", nullSafeString(safeGetString(p, "getHsnCode", "getHsn")));
            m.put("quantity", safeGetDouble(p, "getQuantity", "getQty")); // Quantity left exact
            m.put("discountPercentage", p.getDiscountPercentage()); // Percentages left exact

            // Rounding monetary amounts
            m.put("rate", getRoundedAmount(p, "getRate", "getPrice"));
            m.put("taxAmount", getRoundedAmount(p, "getTaxAmount", "getTax"));
            m.put("totalAmount", getRoundedAmount(p, "getTotalAmount", "getAmount", "getTotal"));
            m.put("igstAmount", roundDouble(p.getIgst()));
            m.put("cgstAmount", roundDouble(p.getCgst()));
            m.put("sgstAmount", roundDouble(p.getSgst()));

            m.put("igstPercentage", p.getIgstPercentage());
            m.put("cgstPercentage", p.getCgstPercentage());
            m.put("sgstPercentage", p.getSgstPercentage());

            productsForTemplate.add(m);
        }

        // --- Process GST Summary to Round Amounts ---
        List<Map<String, Object>> roundedGstSummary = new ArrayList<>();
        if (data.getGstSummary() != null) {
            for (Map<String, Object> gstMap : data.getGstSummary()) {
                Map<String, Object> roundedGst = new HashMap<>(gstMap);
                if (roundedGst.containsKey("amount")) {
                    roundedGst.put("amount", Math.round(Double.parseDouble(String.valueOf(roundedGst.get("amount")))));
                }
                roundedGstSummary.add(roundedGst);
            }
        }

        // --- Prepare Thymeleaf Context ---
        Context context = new Context();

        if (data.getShopLogoBytes() != null && data.getShopLogoBytes().length > 0) {
            String shopLogoBase64 = Base64.getEncoder().encodeToString(data.getShopLogoBytes());
            context.setVariable("shopLogoBase64", shopLogoBase64);
        }
        if(data.getShowShopSignature()!=null) {
            if (data.getShopSignatureBytes() != null && data.getShopSignatureBytes().length > 0) {
                String shopSignBase64 = Base64.getEncoder().encodeToString(data.getShopSignatureBytes());
                context.setVariable("shopSignBase64", shopSignBase64);
                context.setVariable("shopSignLabel", "Authorized Signatory");
            }
        }
        context.setVariable("shopName", nullSafeString(data.getShopName()));
        context.setVariable("shopSlogan", nullSafeString(data.getShopSlogan()));
        context.setVariable("shopLogoText", nullSafeString(data.getShopLogoText()));
        context.setVariable("shopAddress", nullSafeString(data.getShopAddress()));
        context.setVariable("shopEmail", nullSafeString(data.getShopEmail()));
        context.setVariable("shopPhone", nullSafeString(data.getShopPhone()));
        context.setVariable("gstNumber", nullSafeString(data.getGstNumber()));
        context.setVariable("panNumber", nullSafeString(data.getPanNumber()));

        context.setVariable("invoiceId", invoiceId);
        context.setVariable("invoiceBarcodeBase64", invoiceBarcodeBase64);

        context.setVariable("orderedDate", nullSafeString(data.getOrderedDate()));
        context.setVariable("dueDate", nullSafeString(data.getDueDate()));

        context.setVariable("customerName", nullSafeString(data.getCustomerName()));
        context.setVariable("customerBillingAddress", nullSafeString(data.getCustomerBillingAddress()));
        context.setVariable("customerShippingAddress", nullSafeString(data.getCustomerShippingAddress()));
        context.setVariable("customerGstNumber", nullSafeString(data.getCustomerGst()));
        context.setVariable("customerPhone", nullSafeString(data.getCustomerPhone()));
        context.setVariable("customerState", nullSafeString(data.getCustomerState()));

        context.setVariable("products", productsForTemplate);

        // Apply rounded amounts to the context
        context.setVariable("taxableAmount", taxableAmount);
        context.setVariable("grandTotal", grandTotal);
        context.setVariable("paidAmount", roundDouble(data.getPaidAmount()));
        context.setVariable("dueAmount", roundDouble(data.getDueAmount()));
        context.setVariable("totalDiscountAmount", roundDouble(data.getDiscountPercentage()));
        context.setVariable("receivedAmount", roundDouble(data.getReceivedAmount()));
        context.setVariable("previousBalance", roundDouble(data.getPreviousBalance()));
        context.setVariable("currentBalance", currentBalance);
        context.setVariable("grandTotalInWords", nullSafeString(grandTotalInWords));

        context.setVariable("gstSummary", roundedGstSummary);

        context.setVariable("bankAccountName", nullSafeString(data.getBankAccountName()));
        context.setVariable("bankAccountNumber", nullSafeString(data.getBankAccountNumber()));
        context.setVariable("bankIfscCode", nullSafeString(data.getBankIfscCode()));
        context.setVariable("bankName", nullSafeString(data.getBankName()));
        context.setVariable("upiId", nullSafeString(data.getUpiId()));
        context.setVariable("qrCodeBase64", nullSafeString(qrCodeBase64));

        context.setVariable("termsAndConditions", data.getTermsAndConditions() != null ? data.getTermsAndConditions() : Collections.emptyList());

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
        context.setVariable("gstBreakdown", data.getShowGstBreakdown() != null ? data.getShowGstBreakdown() : false);
        context.setVariable("showBankDetails", data.getShowBankDetails() != null ? data.getShowBankDetails() : false);
        context.setVariable("showUpiId", data.getShowUpiId() != null ? data.getShowUpiId() : false);
        context.setVariable("showQrCode", data.getShowQrcode() != null ? data.getShowQrcode() : false);



        context.setVariable("printerType", nullSafeString(printerType));

        // --- Generate PDF ---
        String htmlContent = templateEngine.process(invoiceTemplate, context);

        // --- ADDED: Configure Playwright options based on environment ---
        Playwright.CreateOptions createOptions = new Playwright.CreateOptions();
       if (!(Arrays.asList(environment.getActiveProfiles()).contains("prod"))) {

                 Map<String, String> env = new HashMap<>(System.getenv());
                 String userHome = System.getProperty("user.home");
                env.put("PLAYWRIGHT_BROWSERS_PATH", userHome + "/.cache/ms-playwright");
           env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");
                createOptions.setEnv(env);

        }
        // Pass the options into Playwright.create()
        try (Playwright playwright = Playwright.create(createOptions)) {

            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(Arrays.asList("--no-sandbox", "--disable-setuid-sandbox"));

            try (Browser browser = playwright.chromium().launch(launchOptions)) {

                Browser.NewContextOptions contextOptions = new Browser.NewContextOptions();

                if (printerType != null && printerType.contains("THERMAL")) {
                    int viewportHeight = 800; // Base height, expands automatically

                    int viewportWidth = switch (printerType) {
                        case "THERMAL_2" -> 384;
                        case "THERMAL_3" -> 576;
                        case "THERMAL_4" -> 832;
                        default          -> 384;
                    };

                    contextOptions.setViewportSize(viewportWidth, viewportHeight);
                    contextOptions.setDeviceScaleFactor(1.0);
                } else {
                    contextOptions.setViewportSize(794, 1123);
                    contextOptions.setDeviceScaleFactor(1.0);
                }

                try (BrowserContext context2 = browser.newContext(contextOptions);
                     Page page = context2.newPage()) {

                    page.setContent(htmlContent);

                    Page.ScreenshotOptions screenshotOptions = new Page.ScreenshotOptions()
                            .setType(ScreenshotType.PNG)
                            .setFullPage(true);

                    return page.screenshot(screenshotOptions);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generating Invoice Image", e);
        }
    }

    public byte[] getReminderImage(InvoiceData data) {

        // --- 1. Core Calculations ---
        List<OrderItemInvoice> rawProducts = data.getProducts() != null ? data.getProducts() : Collections.emptyList();

        long taxableAmount = Math.round(rawProducts.stream()
                .mapToDouble(p -> safeGetDouble(p, "getRate", "getPrice") * safeGetDouble(p, "getQuantity", "getQty"))
                .sum());

        long gstAmount = Math.round(rawProducts.stream()
                .mapToDouble(p -> safeGetDouble(p, "getTaxAmount", "getTax"))
                .sum());

        long grandTotal = taxableAmount + gstAmount;
        long dueAmount = roundDouble(data.getDueAmount());
        long paidAmount = roundDouble(data.getPaidAmount());

        // --- 2. QR Code (Generated specifically for the DUE AMOUNT) ---
        String upiUrl = "upi://pay?pa=" + data.getUpiId() +
                "&pn=" + data.getShopName() +
                "&tn=" + data.getInvoiceId() +
                "&am=" + dueAmount + "&cu=INR";

        String qrCodeBase64 = QRCodeGenerator.generateQRCodeBase64(nullSafeString(upiUrl), 400, 400);

        // --- 3. Process Products for Template ---
        List<Map<String, Object>> productsForTemplate = new ArrayList<>();
        for (OrderItemInvoice p : rawProducts) {
            Map<String, Object> m = new HashMap<>();
            m.put("productName", nullSafeString(safeGetString(p, "getProductName", "getName")));
            m.put("quantity", safeGetDouble(p, "getQuantity", "getQty"));
            m.put("totalAmount", getRoundedAmount(p, "getTotalAmount", "getAmount", "getTotal"));
            productsForTemplate.add(m);
        }

        // --- 4. Prepare Thymeleaf Context ---
        Context context = new Context();
        context.setVariable("shopName", nullSafeString(data.getShopName()));
        context.setVariable("invoiceId", nullSafeString(data.getInvoiceId()));
        context.setVariable("products", productsForTemplate);

        context.setVariable("totalAmount", grandTotal);
        context.setVariable("paidAmount", paidAmount);
        context.setVariable("dueAmount", dueAmount);

        // Ensure proper Data URI format for the img src tag
        context.setVariable("qrCodeBase64", "data:image/png;base64," + qrCodeBase64);

        // --- 5. Generate HTML ---
        String htmlContent = templateEngine.process("reminderTemplate1", context);

        // --- 6. Playwright Image Generation ---
        Playwright.CreateOptions createOptions = new Playwright.CreateOptions();
        if (!(Arrays.asList(environment.getActiveProfiles()).contains("prod"))) {
            Map<String, String> env = new HashMap<>(System.getenv());
            String userHome = System.getProperty("user.home");
            env.put("PLAYWRIGHT_BROWSERS_PATH", userHome + "/.cache/ms-playwright");
            env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");
            createOptions.setEnv(env);
        }

        try (Playwright playwright = Playwright.create(createOptions)) {

            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(Arrays.asList("--no-sandbox", "--disable-setuid-sandbox"));

            try (Browser browser = playwright.chromium().launch(launchOptions)) {

                Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                        .setViewportSize(800, 1200) // Large enough to fit the card
                        .setDeviceScaleFactor(2.0); // Retina quality for ultra-crisp WhatsApp sharing

                try (BrowserContext browserContext = browser.newContext(contextOptions);
                     Page page = browserContext.newPage()) {

                    page.setContent(htmlContent);

                    // Give the browser a tiny fraction of a second to paint the DOM
                    page.waitForTimeout(150);

                    // We use Locator screenshot to capture ONLY the card, omitting the background gradient
                    Locator.ScreenshotOptions screenshotOptions = new Locator.ScreenshotOptions()
                            .setType(ScreenshotType.PNG)
                            .setOmitBackground(true);

                    return page.locator(".card").screenshot(screenshotOptions);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generating Reminder Image", e);
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

    // --- Helper Method for Barcode ---
    private String generateBarcodeBase64ForInvoice(String text, int width, int height) {try {
        // Safely extract from n-5 to the end (the last 5 characters)
        String barcodeText = text.length() >= 5 ? text.substring(text.length() - 5) : text;

        BitMatrix bitMatrix = new MultiFormatWriter().encode(barcodeText, BarcodeFormat.CODE_128, width, height);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return Base64.getEncoder().encodeToString(pngOutputStream.toByteArray());
    } catch (Exception e) {
        System.err.println("Failed to generate barcode for text: " + text);
        e.printStackTrace();
        return "";
    }}

    // --- Rounding Helpers ---
    private long roundDouble(Double d) {
        return d == null ? 0L : Math.round(d);
    }

    private long getRoundedAmount(Object bean, String... methodNames) {
        return Math.round(safeGetDouble(bean, methodNames));
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
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}