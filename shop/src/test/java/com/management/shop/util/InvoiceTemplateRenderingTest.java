package com.management.shop.util;

import com.management.shop.dto.OrderItem;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceTemplateRenderingTest {

    private static final List<String> GST_TEMPLATES = List.of(
            "gstinvoice", "gstinvoiceBlue", "gstinvoiceCyan", "gstinvoiceGreen",
            "gstinvoiceLightGreen", "gstinvoiceOrange", "gstinvoiceSkyBlue",
            "gstInvoiceSeaGreen", "gstinvoiceThermal1", "gstinvoiceThermal2",
            "gstinvoiceThermal3", "gstinvoiceThermal4", "gstinvoiceThermal5",
            "gstinvoiceThermal6");
    private static final List<String> A4_TEMPLATES = GST_TEMPLATES.subList(0, 8);

    private final SpringTemplateEngine templateEngine = templateEngine();

    @Test
    void rendersEveryGstInvoiceTemplateWithDecimalAmounts() {
        Context context = invoiceContext();
        for (String template : GST_TEMPLATES) {
            String html = assertDoesNotThrow(() -> templateEngine.process(template, context), template);
            assertTrue(html.contains("(rounded off)"), template);
            if (A4_TEMPLATES.contains(template)) {
                assertTrue(html.contains("width: calc(100% - 28px)"), template);
                assertTrue(html.contains("background-color: #fff !important"), template);
                assertTrue(html.contains("box-shadow: none !important"), template);
                assertTrue(html.contains(".items-table .col-sr { width: 4%"), template);
                assertTrue(html.contains(".items-table .col-tax { width: 20%"), template);
            }
        }
    }

    @Test
    void rendersRoundedOffMessageForEveryIntegerInvoiceTemplate() {
        Context context = invoiceContext();
        context.setVariable("amounts", AmountDisplayFormatter.forSetting(false));
        context.setVariable("enableDecimalPlace", false);
        for (String template : GST_TEMPLATES) {
            String html = assertDoesNotThrow(() -> templateEngine.process(template, context), template);
            assertTrue(html.contains("(rounded off)"), template);
        }
    }

    @Test
    void rendersIndianNumberGroupingForEveryInvoiceTemplate() {
        Context context = invoiceContext();
        context.setVariable("taxableAmount", new BigDecimal("134553.25"));
        context.setVariable("grandTotal", new BigDecimal("134553.75"));

        for (String template : GST_TEMPLATES) {
            String html = assertDoesNotThrow(() -> templateEngine.process(template, context), template);
            assertTrue(html.contains("1,34,553.25"), template);
            assertTrue(html.contains("1,34,554"), template);
        }
    }

    @Test
    void roundsProductAndGrandTotalsWhenDecimalPlacesAreEnabled() {
        Context context = invoiceContext();
        Map<String, Object> product = product();
        product.put("totalAmount", new BigDecimal("1233.99"));
        context.setVariable("products", List.of(product));
        context.setVariable("grandTotal", new BigDecimal("1233.99"));

        for (String template : GST_TEMPLATES) {
            String html = assertDoesNotThrow(() -> templateEngine.process(template, context), template);
            assertTrue(html.contains("1,234"), template);
            assertTrue(html.indexOf("1,234") != html.lastIndexOf("1,234"), template);
            assertFalse(html.contains("1,233.99"), template);
        }
    }

    @Test
    void rendersZeroGstForEveryProductInvoiceTemplate() {
        Context context = invoiceContext();
        Map<String, Object> product = product();
        product.put("taxAmount", BigDecimal.ZERO);
        product.put("taxPercentage", BigDecimal.ZERO);
        product.put("igstAmount", BigDecimal.ZERO);
        product.put("cgstAmount", BigDecimal.ZERO);
        product.put("sgstAmount", BigDecimal.ZERO);
        product.put("zeroGst", true);
        context.setVariable("products", List.of(product));

        for (String template : GST_TEMPLATES) {
            String html = assertDoesNotThrow(() -> templateEngine.process(template, context), template);
            assertTrue(html.contains("0 (0%)"), template);
        }
    }

    @Test
    void rendersReminderAndSubscriptionTemplatesWithDecimalAmounts() {
        Context context = invoiceContext();
        context.setVariable("totalAmount", new BigDecimal("12.35"));
        context.setVariable("totalGstAmount", new BigDecimal("2.35"));
        context.setVariable("amountInWords", "Twelve Rupees Only");
        context.setVariable("appName", "Instabill");
        context.setVariable("invoiceDate", "24-08-2026");
        context.setVariable("planName", "Basic");
        context.setVariable("userName", "Customer");

        assertDoesNotThrow(() -> templateEngine.process("reminderTemplate1", context));
        context.setVariable("gstSummary", Map.of("CGST @ 9%", new BigDecimal("1.18")));
        assertDoesNotThrow(() -> templateEngine.process("thermal-subscription-receipt", context));
    }

    @Test
    void rendersLegacyInvoiceTemplateWithDecimalAmounts() {
        Context context = invoiceContext();
        context.setVariable("products", List.of(OrderItem.builder()
                .productName("Item")
                .quantity(1)
                .unitPrice(12.35)
                .gst(2.35)
                .details("Description")
                .build()));
        context.setVariable("totalAmount", new BigDecimal("10.00"));
        context.setVariable("gstRate", new BigDecimal("2.35"));
        context.setVariable("paid", true);

        assertDoesNotThrow(() -> templateEngine.process("invoice", context));
    }

    private Context invoiceContext() {
        Context context = new Context();
        Map<String, Object> values = new HashMap<>();
        values.put("amounts", AmountDisplayFormatter.INSTANCE);
        values.put("enableDecimalPlace", true);
        values.put("products", List.of(product()));
        values.put("gstSummary", List.of(gstSummary()));
        values.put("taxableAmount", new BigDecimal("10.00"));
        values.put("grandTotal", new BigDecimal("12.35"));
        values.put("paidAmount", BigDecimal.ZERO);
        values.put("dueAmount", new BigDecimal("12.35"));
        values.put("totalDiscountAmount", BigDecimal.ZERO);
        values.put("grandTotalInWords", "Twelve Rupees And Thirty Five Paise Only");
        values.put("termsAndConditions", List.of());

        for (String key : List.of(
                "shopName", "shopSlogan", "shopLogoText", "shopAddress", "shopEmail", "shopPhone",
                "gstNumber", "panNumber", "invoiceId", "invoiceBarcodeBase64", "orderedDate", "dueDate",
                "customerName", "customerEmail", "customerBillingAddress", "customerShippingAddress",
                "customerGstNumber", "customerPhone", "customerState", "bankAccountName", "shopAccountName",
                "bankAccountNumber", "bankIfscCode", "bankName", "upiId", "qrCodeBase64", "shopLogoBase64",
                "shopSignBase64", "shopSignLabel", "printerType")) {
            values.put(key, "test");
        }

        for (String key : List.of(
                "showShopPanOnInvoice", "showCustomerGst", "combineAddress", "showIndividualDiscountPercentage",
                "showHsnColumn", "showRateColumn", "showTotalDiscountPercentage", "showDueAmount", "showDueDate",
                "showSupportInfo", "removeTerms", "gstBreakdown", "showBankDetails", "showUpiId", "showQrCode",
                "showProductGst")) {
            values.put(key, true);
        }

        context.setVariables(values);
        return context;
    }

    private Map<String, Object> product() {
        Map<String, Object> product = new HashMap<>();
        product.put("productName", "Item");
        product.put("description", "Description");
        product.put("hsnCode", "1001");
        product.put("quantity", 1);
        product.put("rate", new BigDecimal("12.35"));
        product.put("taxAmount", new BigDecimal("2.35"));
        product.put("taxPercentage", new BigDecimal("18.00"));
        product.put("zeroGst", false);
        product.put("totalAmount", new BigDecimal("12.35"));
        product.put("discountPercentage", BigDecimal.ZERO);
        product.put("igstAmount", BigDecimal.ZERO);
        product.put("cgstAmount", new BigDecimal("1.18"));
        product.put("sgstAmount", new BigDecimal("1.17"));
        product.put("igstPercentage", BigDecimal.ZERO);
        product.put("cgstPercentage", new BigDecimal("9.00"));
        product.put("sgstPercentage", new BigDecimal("9.00"));
        return product;
    }

    private Map<String, Object> gstSummary() {
        Map<String, Object> gst = new HashMap<>();
        gst.put("type", "CGST");
        gst.put("percentage", new BigDecimal("9.00"));
        gst.put("amount", new BigDecimal("1.18"));
        return gst;
    }

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
