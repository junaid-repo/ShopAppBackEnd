package com.management.shop.util;

import com.management.shop.dto.InvoiceData;
import com.management.shop.dto.OrderItemInvoice;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import com.microsoft.playwright.Browser; // <-- ADD
import com.microsoft.playwright.Page;      // <-- ADD
import com.microsoft.playwright.Playwright;  // <-- ADD

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.*;

@Component
public class PDFGSTInvoiceUtil {

    private final TemplateEngine templateEngine;

    public PDFGSTInvoiceUtil(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generateGSTInvoice(InvoiceData data, String invoiceTemplate) throws Exception {

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
       // upi://pay?pa=<VPA>&pn=<PayeeName>&mc=<MerchantCode>&tid=<TxnId>&tr=<RefId>&tn=<Note>&am=<Amount>&cu=INR

        String upiUrl = "upi://pay?pa="+data.getUpiId()+"&pn="+data.getShopName()+"&tn="+data.getInvoiceId()+"&am="+data.getGrandTotal()+"&cu=INR";

        String qrCodeBase64 = QRCodeGenerator.generateQRCodeBase64(nullSafeString(upiUrl), 200, 200);

        // --- Convert products to Map for template to avoid missing-getter errors ---
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
            // GST breakdown on product (if present)
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
        context.setVariable("invoiceId", nullSafeString(data.getInvoiceId()));
        context.setVariable("orderedDate", nullSafeString(data.getOrderedDate()));
        context.setVariable("dueDate", nullSafeString(data.getDueDate()));

        // Customer Details
        context.setVariable("customerName", nullSafeString(data.getCustomerName()));
        context.setVariable("customerBillingAddress", nullSafeString(data.getCustomerBillingAddress()));
        context.setVariable("customerShippingAddress", nullSafeString(data.getCustomerShippingAddress()));
        context.setVariable("customerGstNumber", nullSafeString(data.getCustomerGst()));
        context.setVariable("customerPhone", nullSafeString(data.getCustomerPhone()));
        context.setVariable("customerState", nullSafeString(data.getCustomerState()));

        // Products (maps)
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

            //Conditions to show/hide fields
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




        System.out.println("The full data to render invoice "+context);
        // --- Generate PDF using openhtmltopdf ---
        String htmlContent = templateEngine.process(invoiceTemplate, context);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(); // Launch headless Chromium
            Page page = browser.newPage();

            // Set the HTML content
            // This is perfect for your template since images are embedded (base64)
            page.setContent(htmlContent);

            // Generate the PDF
            Page.PdfOptions pdfOptions = new Page.PdfOptions()
                    .setFormat("A4")
                    .setPrintBackground(true); // Crucial for your blue header style

            byte[] pdfBytes = page.pdf(pdfOptions);

            // Cleanup
            browser.close();

            return pdfBytes;

        } catch (Exception e) {
            // Re-throw or handle as per your app's needs
            throw new Exception("Error generating PDF with Playwright", e);
        }
    }

    // --- Helpers (unchanged from your original) ---

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