package com.management.shop.util;

import com.management.shop.dto.*;
import com.management.shop.entity.BillingEntity;
import com.management.shop.entity.CustomerEntity;
import com.management.shop.entity.PaymentEntity;
import com.management.shop.entity.ProductEntity;
import com.management.shop.repository.*;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class ReportsGenerate {

    @Autowired
    private ShopRepository shopRepo;
    @Autowired
    private BillingRepository billRepo;
    @Autowired
    private SalesPaymentRepository salesPaymentRepo;
    @Autowired
    private ProductSalesRepository prodSalesRepo;
    @Autowired
    private ProductRepository prodRepo;

    // --- 0. PARENT DATA DTO ---
    /**
     * Internal DTO to hold prepared report data for any template.
     */
    private static class ReportData {
        String reportTitle;
        String duration;
        List<String> headers;
        List<List<String>> rows;
        List<String> footerCells;
    }

    // --- 0. PARENT METHOD ---
    /**
     * Fetches data based on reportType, prepares it, and passes it to the
     * correct template (PDF/Excel).
     */
    public byte[] downloadReport(String reportType, String format, LocalDateTime fromDate, LocalDateTime toDate, String userId) throws IOException {

        // Prepare date range string once
        DateTimeFormatter headerFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        String duration = fromDate.format(headerFormatter) + " — " + toDate.format(headerFormatter);

        // This object will be populated by the correct helper method
        ReportData reportData;

        // 1. Fetch and Prepare Data
        if ("Sales Summary".equals(reportType)) {
            reportData = prepareSalesReportData(fromDate, toDate, userId, duration);
        } else if ("Sales by Product".equals(reportType)) {
            reportData = prepareProductSalesReportData(fromDate, toDate, userId, duration);
        }else if ("Sales by Customer".equals(reportType)) {
            reportData = prepareSalesByCustomerData(fromDate, toDate, userId, duration);
        }

        else if ("Total Payments".equals(reportType)) {
            reportData = preparePaymentListReportData(fromDate, toDate, userId, duration);
        }

        else if ("Payment Status".equals(reportType)) {
            reportData = preparePaymentStatusReportData(fromDate, toDate, userId, duration);
        }

        else if ("Payment Modes".equals(reportType)) {
            reportData = preparePaymentMethodReportData(fromDate, toDate, userId, duration);
        }


        else if ("Stock Summary".equals(reportType)) {
            reportData = prepareAllProductsReportData(fromDate, toDate, userId, duration);

            // ADD this new block for the low stock report
        } else if ("Low Stock Report".equals(reportType)) {
            reportData = prepareLowStockReportData(fromDate, toDate, userId, duration);

        }

        else if ("Customer Ledger".equals(reportType)) {
            reportData = prepareCustomerListReportData(fromDate, toDate, userId, duration);

            // ADD this new block for the outstanding report
        } else if ("Customer Outstanding".equals(reportType)) {
            reportData = prepareCustomerOutstandingReportData(fromDate, toDate, userId, duration);

        }

        else if ("SateWiseGST Summary".equals(reportType)) {
            reportData = prepareGstByStateReportData(fromDate, toDate, userId, duration);
        } else if ("HSN/SAC Summary".equals(reportType)) {
            reportData = prepareGstByHsnReportData(fromDate, toDate, userId, duration);
        } else if ("GSTR-1 Summary".equals(reportType)) {
            reportData = prepareMonthlyGstReportData(fromDate, toDate, userId, duration);
        }
        else if ("CustomerWiseGST Summary".equals(reportType)) {
            reportData = prepareCustomerGstReportData(fromDate, toDate, userId, duration);
            // --- END OF NEW BLOCK ---

        }

        else if ("Payment Reports".equals(reportType)) {
            reportData = preparePaymentReportData(fromDate, toDate, userId, duration);
        } else if ("Customers Report".equals(reportType)) {
            reportData = prepareCustomerReportData(fromDate, toDate, userId, duration);
        }  else if ("Products Report".equals(reportType)) {
            reportData = prepareProductsReportData(fromDate, toDate, userId, duration);
        } else {
            // Handle unknown report type
            throw new IllegalArgumentException("Unknown report type: " + reportType);
        }

        // 2. Pass Prepared Data to the Correct Template
        if ("pdf".equalsIgnoreCase(format)) {
            return generateGenericPdfReport(reportData);
        } else {
            return generateGenericExcelReport(reportData);
        }
    }

    // --- DATA PREPARATION HELPERS ---

    private ReportData prepareSalesReportData(LocalDateTime fromDate, LocalDateTime toDate, String userId, String duration) {
        List<BillingEntity> listOfBills = billRepo.findPaymentsByDateRange(fromDate, toDate, userId);
        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

        List<String> headers = List.of("Invoice ID", "Customer", "Date", "Total (₹)", "Status");
        List<List<String>> rows = new ArrayList<>();
        double totalSum = 0;
        long idCount = 0;

        for (BillingEntity obj : listOfBills) {
            String formattedDate = (obj.getCreatedDate() != null) ? obj.getCreatedDate().format(dateFormatter) : "N/A";

            String customerName = "N/A";
            try {
                var shop = shopRepo.findByIdAndUserId(obj.getCustomerId(), userId);
                if (shop != null) customerName = shop.getName();
            } catch (Exception e) { /* ignore */ }

            String status = "N/A";
            try {
                var payment = salesPaymentRepo.findPaymentDetails(obj.getId(), userId);
                if (payment != null) status = payment.getStatus();
            } catch (Exception e) { /* ignore */ }

            Double total = obj.getTotalAmount();
            String totalStr = (total != null) ? String.format("%.2f", total) : "0.00";

            rows.add(List.of(
                    obj.getInvoiceNumber() != null ? obj.getInvoiceNumber() : "",
                    customerName,
                    formattedDate,
                    totalStr,
                    status
            ));

            if (total != null) totalSum += total;
            if (obj.getInvoiceNumber() != null && !obj.getInvoiceNumber().trim().isEmpty()) idCount++;
        }

        List<String> footerCells = List.of(
                "TOTALS",
                "Count: " + idCount,
                "", // Empty cell for Date
                String.format("%.2f", totalSum), // Cell for Total
                ""  // Empty cell for Status
        );

        ReportData data = new ReportData();
        data.reportTitle = "Sales Report";
        data.duration = duration;
        data.headers = headers;
        data.rows = rows;
        data.footerCells = footerCells;
        return data;
    }
    private ReportData prepareSalesByCustomerData(LocalDateTime fromDate, LocalDateTime toDate, String userId, String duration) {

        // 1. Call the new JPQL method
        List<CustomerSalesReportDto> customerSales = billRepo.findCustomerSalesByDateRange(fromDate, toDate, userId);

        // 2. Define Headers
        List<String> headers = List.of("Customer Name", "Email", "Phone", "Order Count", "Total Sales (₹)", "Invoice List");

        // 3. Process data
        List<List<String>> rows = new ArrayList<>();
        double grandTotalSales = 0;

        for (CustomerSalesReportDto dto : customerSales) {
            Double total = dto.getTotalSalesValue();
            String totalStr = (total != null) ? String.format("%.2f", total) : "0.00";

            rows.add(List.of(
                    dto.getName() != null ? dto.getName() : "",
                    dto.getEmail() != null ? dto.getEmail() : "",
                    dto.getPhone() != null ? dto.getPhone() : "",
                    dto.getOrderCount() != null ? String.valueOf(dto.getOrderCount()) : "0",
                    totalStr,
                    dto.getInvoiceList() != null ? dto.getInvoiceList() : ""
            ));

            if (total != null) grandTotalSales += total;
        }

        // 4. Define Footer
        List<String> footerCells = List.of(
                "TOTALS",
                "Total Customers: " + customerSales.size(),
                "", // Empty for Phone
                "", // Empty for Order Count
                String.format("%.2f", grandTotalSales), // Grand Total Sales
                ""  // Empty for Invoice List
        );

        // 5. Create ReportData object
        ReportData data = new ReportData();
        data.reportTitle = "Sales by Customer Report";
        data.duration = duration;
        data.headers = headers;
        data.rows = rows;
        data.footerCells = footerCells;
        return data;
    }

    private ReportData prepareCustomerReportData(LocalDateTime fromDate, LocalDateTime toDate, String userId, String duration) {
        List<CustomerEntity> customerEntity = shopRepo.findCustomerByDateRange(fromDate, toDate, userId);

        List<String> headers = List.of("ID", "Name", "Email", "Phone", "Total Spent (₹)");
        List<List<String>> rows = new ArrayList<>();
        double totalSpent = 0;

        for (CustomerEntity customer : customerEntity) {
            Double spent = customer.getTotalSpent();
            String spentStr = (spent != null) ? String.format("%.2f", spent) : "0.00";

            rows.add(List.of(
                    String.valueOf(customer.getId()),
                    customer.getName() != null ? customer.getName() : "",
                    customer.getEmail() != null ? customer.getEmail() : "",
                    customer.getPhone() != null ? customer.getPhone() : "",
                    spentStr
            ));

            if (spent != null) totalSpent += spent;
        }

        List<String> footerCells = List.of(
                "TOTALS",
                "Count: " + customerEntity.size(),
                "",
                "",
                String.format("%.2f", totalSpent)
        );

        ReportData data = new ReportData();
        data.reportTitle = "Customers Report";
        data.duration = duration;
        data.headers = headers;
        data.rows = rows;
        data.footerCells = footerCells;
        return data;
    }

    private ReportData preparePaymentReportData(LocalDateTime fromDate, LocalDateTime toDate, String userId, String duration) {
        List<BillingEntity> billList = billRepo.findPaymentsByDateRange(fromDate, toDate, userId);
        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

        List<String> headers = List.of("Payment ID", "Sale ID", "Date", "Amount (₹)", "Method");
        List<List<String>> rows = new ArrayList<>();
        double totalAmount = 0;

        for(BillingEntity obj : billList) {
            PaymentEntity payment = salesPaymentRepo.findPaymentDetails(obj.getId(), userId);
            if (payment == null) continue; // Skip if no payment details

            String formattedDate = (obj.getCreatedDate() != null) ? obj.getCreatedDate().format(dateFormatter) : "N/A";
            Double amount = obj.getTotalAmount();
            String amountStr = (amount != null) ? String.format("%.2f", amount) : "0.00";

            rows.add(List.of(
                    payment.getPaymentReferenceNumber() != null ? payment.getPaymentReferenceNumber() : "",
                    obj.getInvoiceNumber() != null ? obj.getInvoiceNumber() : "",
                    formattedDate,
                    amountStr,
                    payment.getPaymentMethod() != null ? payment.getPaymentMethod() : ""
            ));

            if (amount != null) totalAmount += amount;
        }

        List<String> footerCells = List.of(
                "TOTALS",
                "Count: " + rows.size(),
                "",
                String.format("%.2f", totalAmount),
                ""
        );

        ReportData data = new ReportData();
        data.reportTitle = "Payment Reports";
        data.duration = duration;
        data.headers = headers;
        data.rows = rows;
        data.footerCells = footerCells;
        return data;
    }

    private ReportData prepareProductSalesReportData(LocalDateTime fromDate, LocalDateTime toDate, String userId, String duration) {
        List<ProductSalesReportView> response = prodSalesRepo.findSalesReportNative(fromDate, toDate, userId);
        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

        List<String> headers = List.of("Product Name", "Category", "Units", "Amount", "GST", "Profit on CP", "Invoice", "Date");
        List<List<String>> rows = new ArrayList<>();

        double totalAmount = 0;
        double totalGst = 0;
        double totalProfit = 0;
        int totalUnits = 0;

        for (ProductSalesReportView item : response) {
            String dateStr = (item.getInvoiceDate() != null) ? item.getInvoiceDate().format(dateFormatter) : "";

            rows.add(List.of(
                    item.getProductName() != null ? item.getProductName() : "",
                    item.getCategory() != null ? item.getCategory() : "",
                    item.getTotalSold() != null ? String.valueOf(item.getTotalSold()) : "0",
                    item.getTotal() != null ? String.format("%.2f", item.getTotal()) : "0.00",
                    item.getTax() != null ? String.format("%.2f", item.getTax()) : "0.00",
                    item.getProfitOnCp() != null ? String.format("%.2f", item.getProfitOnCp()) : "0.00",
                    item.getInvoiceNumber() != null ? item.getInvoiceNumber() : "",
                    dateStr
            ));

            if(item.getTotal() != null) totalAmount += item.getTotal();
            if(item.getTax() != null) totalGst += item.getTax();
            if(item.getProfitOnCp() != null) totalProfit += item.getProfitOnCp();
            if(item.getTotalSold() != null) totalUnits += item.getTotalSold();
        }

        List<String> footerCells = List.of(
                "TOTALS",
                "",
                String.valueOf(totalUnits),
                String.format("%.2f", totalAmount),
                String.format("%.2f", totalGst),
                String.format("%.2f", totalProfit),
                "",
                ""
        );

        ReportData data = new ReportData();
        data.reportTitle = "Product Sales Report";
        data.duration = duration;
        data.headers = headers;
        data.rows = rows;
        data.footerCells = footerCells;
        return data;
    }

    private ReportData prepareProductsReportData(LocalDateTime fromDate, LocalDateTime toDate, String userId, String duration) {
        // Note: fromDate and toDate are not used in this query, as per original logic
        List<ProductEntity> productList = prodRepo.getAllProductForReport(Boolean.TRUE, userId);

        List<String> headers = List.of("Name", "Category", "Cost Price", "Price", "GST%", "Stock", "Status");
        List<List<String>> rows = new ArrayList<>();

        for (ProductEntity product : productList) {
            rows.add(List.of(
                    product.getName() != null ? product.getName() : "",
                    product.getCategory() != null ? product.getCategory() : "",
                    product.getCostPrice() != null ? String.valueOf(product.getCostPrice()) : "0.0",
                    product.getPrice() != null ? String.valueOf(product.getPrice()) : "0.0",
                    product.getTaxPercent() != null ? String.valueOf(product.getTaxPercent()) : "0",
                    product.getStock() != null ? String.valueOf(product.getStock()) : "0",
                    product.getStatus() != null ? String.valueOf(product.getStatus()) : ""
            ));
        }

        ReportData data = new ReportData();
        data.reportTitle = "Products Report";
        data.duration = duration; // Duration might be misleading here, but we pass it for consistency
        data.headers = headers;
        data.rows = rows;
        data.footerCells = null; // No footer for this report
        return data;
    }


    // --- 1. GENERIC TEMPLATE METHODS ---

    /**
     * NEW TEMPLATE: Generates an Excel report from any ReportData.
     */
    private byte[] generateGenericExcelReport(ReportData data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(data.reportTitle);

            // --- Header Style ---
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // --- Header Row ---
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < data.headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(data.headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // --- Data Rows ---
            int rowIdx = 1;
            for (List<String> rowData : data.rows) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < rowData.size(); i++) {
                    // We only print values if they are present in the row data
                    if (i < rowData.size()) {
                        row.createCell(i).setCellValue(rowData.get(i));
                    }
                }
            }

            // --- Footer Row ---
            if (data.footerCells != null && !data.footerCells.isEmpty()) {
                CellStyle totalStyle = workbook.createCellStyle();
                Font boldFont = workbook.createFont();
                boldFont.setBold(true);
                totalStyle.setFont(boldFont);

                Row totalRow = sheet.createRow(rowIdx);
                for (int i = 0; i < data.footerCells.size(); i++) {
                    // We only print footers that are not null or empty
                    String cellValue = data.footerCells.get(i);
                    if (cellValue != null && !cellValue.isEmpty()) {
                        Cell cell = totalRow.createCell(i);
                        cell.setCellValue(cellValue);
                        cell.setCellStyle(totalStyle);
                    }
                }
            }

            // --- Autosize Columns ---
            for (int i = 0; i < data.headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * UPDATED TEMPLATE: Generates a PDF report from any ReportData.
     */
    private byte[] generateGenericPdfReport(ReportData data) {
        String htmlContent = buildGenericPdfHtml(data);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();
            page.setContent(htmlContent);

            Page.PdfOptions pdfOptions = new Page.PdfOptions()
                    .setFormat("A4")
                    .setPrintBackground(true)
                    .setHeaderTemplate("<div/>") // Remove default header
                    .setFooterTemplate("<div/>");

            byte[] pdfBytes = page.pdf(pdfOptions);
            browser.close();
            return pdfBytes;
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0]; // Return empty byte array on failure
        }
    }

    /**
     * UPDATED TEMPLATE: Helper to build HTML string from any ReportData.
     */
    private String buildGenericPdfHtml(ReportData data) {
        StringBuilder sb = new StringBuilder();

        // --- HTML Head with CSS ---
        sb.append("<html><head><style>")
                .append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; font-size: 10pt; }")
                .append(".container { width: 100%; margin: 0 auto; }")
                .append(".header { text-align: center; border-bottom: 2px solid #007bff; padding-bottom: 10px; margin-bottom: 25px; }")
                .append(".header h1 { margin: 0; color: #007bff; font-size: 24pt; }") // App Name
                .append(".header h2 { margin: 5px 0; color: #333; font-size: 18pt; }") // Report Type
                .append(".header p { margin: 5px 0; color: #555; font-size: 10pt; }") // Duration
                .append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }")
                .append("th, td { border: 1px solid #ddd; padding: 8px 10px; text-align: left; }")
                .append("th { background-color: #f4f7fa; color: #333; font-size: 11pt; }")
                .append("tr:nth-child(even) { background-color: #f9f9f9; }")
                .append("tfoot th { background-color: #e9ecef; color: #000; font-weight: bold; }")
                .append("</style></head><body>");

        // --- HTML Body ---
        sb.append("<div class='container'>");

        // Header (Dynamic)
        sb.append("<div class='header'>")
                .append("<h1>Clear Bill</h1>") // Your App Name
                .append("<h2>").append(data.reportTitle).append("</h2>")
                .append("<p>Duration: ").append(data.duration).append("</p>")
                .append("</div>");

        // Table
        sb.append("<table>");

        // Table Header (Dynamic)
        sb.append("<thead><tr>");
        for (String header : data.headers) {
            sb.append("<th>").append(header).append("</th>");
        }
        sb.append("</tr></thead>");

        // Table Body (Dynamic)
        sb.append("<tbody>");
        if (data.rows == null || data.rows.isEmpty()) {
            sb.append("<tr><td colspan='").append(data.headers.size()).append("' style='text-align: center; color: #777;'>No data available for this period.</td></tr>");
        } else {
            for (List<String> row : data.rows) {
                sb.append("<tr>");
                // Point 2: We only iterate as far as the row data goes
                for (String cell : row) {
                    sb.append("<td>").append(cell != null ? cell : "").append("</td>");
                }
                sb.append("</tr>");
            }
        }
        sb.append("</tbody>");

        // Table Footer (Dynamic)
        if (data.footerCells != null && !data.footerCells.isEmpty()) {
            sb.append("<tfoot><tr>");
            // Point 2: We only print footer cells that are not null or empty
            for (String cell : data.footerCells) {
                sb.append("<th>").append(cell != null ? cell : "").append("</th>");
            }
            sb.append("</tr></tfoot>");
        }

        sb.append("</table>");
        sb.append("</div>");
        sb.append("</body></html>");

        return sb.toString();
    }

    private ReportData preparePaymentListReportData(LocalDateTime fromDate, LocalDateTime toDate, String userId, String duration) {

        // Call the new repository method
        List<PaymentReportDto> payments = salesPaymentRepo.findPaymentReportByDateRange(fromDate, toDate, userId);
        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

        List<String> headers = List.of("Payment ID", "Invoice ID", "Date", "Method", "Total (₹)", "Paid (₹)", "Due (₹)", "Status");
        List<List<String>> rows = new ArrayList<>();

        double grandTotal = 0;
        double grandTotalPaid = 0;
        double grandTotalDue = 0;

        for (PaymentReportDto p : payments) {
            Double total = p.getTotal();
            Double paid = p.getPaid();
            Double due = p.getToBePaid();

            rows.add(List.of(
                    p.getPaymentReferenceNumber() != null ? p.getPaymentReferenceNumber() : "",
                    p.getInvoiceNumber() != null ? p.getInvoiceNumber() : "",
                    p.getCreatedDate() != null ? p.getCreatedDate().format(dateFormatter) : "N/A",
                    p.getPaymentMethod() != null ? p.getPaymentMethod() : "",
                    (total != null) ? String.format("%.2f", total) : "0.00",
                    (paid != null) ? String.format("%.2f", paid) : "0.00",
                    (due != null) ? String.format("%.2f", due) : "0.00",
                    p.getStatus() != null ? p.getStatus() : ""
            ));

            if (total != null) grandTotal += total;
            if (paid != null) grandTotalPaid += paid;
            if (due != null) grandTotalDue += due;
        }

        List<String> footerCells = List.of(
                "TOTALS",
                "Count: " + payments.size(),
                "", // Date
                "", // Method
                String.format("%.2f", grandTotal),
                String.format("%.2f", grandTotalPaid),
                String.format("%.2f", grandTotalDue),
                ""  // Status
        );

        ReportData data = new ReportData();
        data.reportTitle = "All Payments Report";
        data.duration = duration;
        data.headers = headers;
        data.rows = rows;
        data.footerCells = footerCells;
        return data;
    }

    /**
     * 2) Prepares a summary of payments grouped by method.
     */
    private ReportData preparePaymentMethodReportData(LocalDateTime fromDate, LocalDateTime toDate, String userId, String duration) {

        // Call the new repository method
        List<PaymentSummaryDto> summary = salesPaymentRepo.findPaymentSummaryByMethod(fromDate, toDate, userId);

        List<String> headers = List.of("Payment Method", "Total Amount (₹)", "Invoice List");
        List<List<String>> rows = new ArrayList<>();
        double grandTotal = 0;

        for (PaymentSummaryDto s : summary) {
            Double total = s.getTotalAmount();
            rows.add(List.of(
                    s.getCategory() != null ? s.getCategory() : "N/A",
                    (total != null) ? String.format("%.2f", total) : "0.00",
                    s.getInvoiceList() != null ? s.getInvoiceList() : ""
            ));

            if (total != null) grandTotal += total;
        }

        List<String> footerCells = List.of(
                "GRAND TOTAL",
                String.format("%.2f", grandTotal),
                ""
        );

        ReportData data = new ReportData();
        data.reportTitle = "Payment Summary by Method";
        data.duration = duration;
        data.headers = headers;
        data.rows = rows;
        data.footerCells = footerCells;
        return data;
    }

    /**
     * 3) Prepares a summary of payments grouped by status.
     */
    private ReportData preparePaymentStatusReportData(LocalDateTime fromDate, LocalDateTime toDate, String userId, String duration) {

        // Call the new repository method
        List<PaymentSummaryDto> summary = salesPaymentRepo.findPaymentSummaryByStatus(fromDate, toDate, userId);

        List<String> headers = List.of("Payment Status", "Total Amount (₹)", "Invoice List");
        List<List<String>> rows = new ArrayList<>();
        double grandTotal = 0;

        for (PaymentSummaryDto s : summary) {
            Double total = s.getTotalAmount();
            rows.add(List.of(
                    s.getCategory() != null ? s.getCategory() : "N/A",
                    (total != null) ? String.format("%.2f", total) : "0.00",
                    s.getInvoiceList() != null ? s.getInvoiceList() : ""
            ));

            if (total != null) grandTotal += total;
        }

        List<String> footerCells = List.of(
                "GRAND TOTAL",
                String.format("%.2f", grandTotal),
                ""
        );

        ReportData data = new ReportData();
        data.reportTitle = "Payment Summary by Status";
        data.duration = duration;
        data.headers = headers;
        data.rows = rows;
        data.footerCells = footerCells;
        return data;
    }

    private ReportData prepareAllProductsReportData(LocalDateTime fromDate, LocalDateTime toDate, String userId, String duration) {
        // Note: fromDate and toDate are not used here, but we keep the signature consistent.
        List<ProductEntity> productList = prodRepo.findAllByStatus(Boolean.TRUE, userId);

        List<String> headers = List.of("Name", "Category", "HSN", "Cost Price (₹)", "Sale Price (₹)", "GST %", "Stock", "Status");
        List<List<String>> rows = new ArrayList<>();

        for (ProductEntity p : productList) {
            rows.add(List.of(
                    p.getName() != null ? p.getName() : "",
                    p.getCategory() != null ? p.getCategory() : "",
                    p.getHsn() != null ? p.getHsn() : "",
                    p.getCostPrice() != null ? String.valueOf(p.getCostPrice()) : "0",
                    p.getPrice() != null ? String.valueOf(p.getPrice()) : "0",
                    p.getTaxPercent() != null ? String.valueOf(p.getTaxPercent()) : "0",
                    p.getStock() != null ? String.valueOf(p.getStock()) : "0",
                    p.getStatus() != null ? p.getStatus() : ""
            ));
        }

        List<String> footerCells = List.of(
                "Total Active Products: " + productList.size(),
                "", "", "", "", "", "", ""
        );

        ReportData data = new ReportData();
        data.reportTitle = "All Products Report";
        // Duration doesn't apply, so we use "As of"
        data.duration = "As of " + DateTimeFormatter.ofPattern("dd-MMM-yyyy").format(LocalDateTime.now());
        data.headers = headers;
        data.rows = rows;
        data.footerCells = footerCells;
        return data;
    }

    /**
     * 2) Prepares a list of low stock products (stock < 3).
     */
    private ReportData prepareLowStockReportData(LocalDateTime fromDate, LocalDateTime toDate, String userId, String duration) {
        // Call the new repository method
        List<ProductEntity> productList = prodRepo.findLowStockProducts(userId);

        // Use the specific headers you requested
        List<String> headers = List.of("Name", "HSN", "Category", "Stock", "Status");
        List<List<String>> rows = new ArrayList<>();

        for (ProductEntity p : productList) {
            rows.add(List.of(
                    p.getName() != null ? p.getName() : "",
                    p.getHsn() != null ? p.getHsn() : "",
                    p.getCategory() != null ? p.getCategory() : "",
                    p.getStock() != null ? String.valueOf(p.getStock()) : "0",
                    p.getStatus() != null ? p.getStatus() : ""
            ));
        }

        List<String> footerCells = List.of(
                "Total Low Stock Items: " + productList.size(),
                "", "", "", ""
        );

        ReportData data = new ReportData();
        data.reportTitle = "Low Stock Report (Less than 3 units)";
        data.duration = "As of " + DateTimeFormatter.ofPattern("dd-MMM-yyyy").format(LocalDateTime.now());
        data.headers = headers;
        data.rows = rows;
        data.footerCells = footerCells;
        return data;
    }

    private ReportData prepareCustomerListReportData(LocalDateTime fromDate, LocalDateTime toDate, String userId, String duration) {

        // Using the existing method from your repository
        List<CustomerEntity> customerList = shopRepo.findCustomerByDateRange(fromDate, toDate, userId);

        List<String> headers = List.of("Name", "Email", "Phone", "GST Number", "City", "State", "Total Spent (₹)");
        List<List<String>> rows = new ArrayList<>();
        double grandTotalSpent = 0;

        for (CustomerEntity c : customerList) {
            Double totalSpent = c.getTotalSpent();
            rows.add(List.of(
                    c.getName() != null ? c.getName() : "",
                    c.getEmail() != null ? c.getEmail() : "",
                    c.getPhone() != null ? c.getPhone() : "",
                    c.getGstNumber() != null ? c.getGstNumber() : "",
                    c.getCity() != null ? c.getCity() : "",
                    c.getState() != null ? c.getState() : "",
                    (totalSpent != null) ? String.format("%.2f", totalSpent) : "0.00"
            ));

            if (totalSpent != null) grandTotalSpent += totalSpent;
        }

        List<String> footerCells = List.of(
                "Total Customers: " + customerList.size(),
                "", "", "", "", "",
                String.format("%.2f", grandTotalSpent)
        );

        ReportData data = new ReportData();
        data.reportTitle = "Customer Report";
        data.duration = duration;
        data.headers = headers;
        data.rows = rows;
        data.footerCells = footerCells;
        return data;
    }

    /**
     * 2) Prepares a list of customers with outstanding (due) amounts.
     */
    private ReportData prepareCustomerOutstandingReportData(LocalDateTime fromDate, LocalDateTime toDate, String userId, String duration) {

        // Call the new repository method. Note: It ignores dates, as "outstanding" is a current state.
        List<CustomerOutstandingDto> customerList = shopRepo.findCustomersWithOutstandingAmount(userId);

        List<String> headers = List.of("Customer Name", "Phone", "Email", "Total Due (₹)", "Outstanding Invoices");
        List<List<String>> rows = new ArrayList<>();
        double grandTotalOutstanding = 0;

        for (CustomerOutstandingDto c : customerList) {
            Double totalDue = c.getTotalOutstanding();
            rows.add(List.of(
                    c.getName() != null ? c.getName() : "",
                    c.getPhone() != null ? c.getPhone() : "",
                    c.getEmail() != null ? c.getEmail() : "",
                    (totalDue != null) ? String.format("%.2f", totalDue) : "0.00",
                    c.getInvoiceList() != null ? c.getInvoiceList() : ""
            ));

            if (totalDue != null) grandTotalOutstanding += totalDue;
        }

        List<String> footerCells = List.of(
                "Total Customers with Dues: " + customerList.size(),
                "", "",
                String.format("%.2f", grandTotalOutstanding),
                ""
        );

        ReportData data = new ReportData();
        data.reportTitle = "Customer Outstanding Report";
        // Duration is "As of" since this is a snapshot of current dues
        data.duration = "As of " + DateTimeFormatter.ofPattern("dd-MMM-yyyy").format(LocalDateTime.now());
        data.headers = headers;
        data.rows = rows;
        data.footerCells = footerCells;
        return data;
    }
    private ReportData prepareGstByStateReportData(LocalDateTime fromDate, LocalDateTime toDate, String userId, String duration) {

        // Call the new repository method from billRepo
        List<GstByStateDto> summary = billRepo.findGstByState(fromDate, toDate, userId);

        List<String> headers = List.of("State", "Taxable Value (₹)", "CGST (₹)", "SGST (₹)", "IGST (₹)", "Total GST (₹)");
        List<List<String>> rows = new ArrayList<>();

        double grandTaxable = 0, grandCgst = 0, grandSgst = 0, grandIgst = 0, grandGst = 0;

        for (GstByStateDto s : summary) {
            rows.add(List.of(
                    s.getState() != null ? s.getState() : "N/A",
                    s.getTotalTaxableValue() != null ? String.format("%.2f", s.getTotalTaxableValue()) : "0.00",
                    s.getTotalCgst() != null ? String.format("%.2f", s.getTotalCgst()) : "0.00",
                    s.getTotalSgst() != null ? String.format("%.2f", s.getTotalSgst()) : "0.00",
                    s.getTotalIgst() != null ? String.format("%.2f", s.getTotalIgst()) : "0.00",
                    s.getTotalGst() != null ? String.format("%.2f", s.getTotalGst()) : "0.00"
            ));

            if (s.getTotalTaxableValue() != null) grandTaxable += s.getTotalTaxableValue();
            if (s.getTotalCgst() != null) grandCgst += s.getTotalCgst();
            if (s.getTotalSgst() != null) grandSgst += s.getTotalSgst();
            if (s.getTotalIgst() != null) grandIgst += s.getTotalIgst();
            if (s.getTotalGst() != null) grandGst += s.getTotalGst();
        }

        List<String> footerCells = List.of(
                "GRAND TOTAL",
                String.format("%.2f", grandTaxable),
                String.format("%.2f", grandCgst),
                String.format("%.2f", grandSgst),
                String.format("%.2f", grandIgst),
                String.format("%.2f", grandGst)
        );

        ReportData data = new ReportData();
        data.reportTitle = "GST Summary by State";
        data.duration = duration;
        data.headers = headers;
        data.rows = rows;
        data.footerCells = footerCells;
        return data;
    }

    /**
     * 2) Prepares a report of GST collected, grouped by Product HSN.
     */
    private ReportData prepareGstByHsnReportData(LocalDateTime fromDate, LocalDateTime toDate, String userId, String duration) {

        // Call the new repository method from prodSalesRepo
        List<GstByHsnDto> summary = prodSalesRepo.findGstByHsn(fromDate, toDate, userId);

        List<String> headers = List.of("HSN", "Product Name", "Units Sold", "Taxable Value (₹)", "CGST (₹)", "SGST (₹)", "IGST (₹)", "Total GST (₹)");
        List<List<String>> rows = new ArrayList<>();

        double grandTaxable = 0, grandCgst = 0, grandSgst = 0, grandIgst = 0, grandGst = 0;
        int grandUnits = 0;

        for (GstByHsnDto s : summary) {
            rows.add(List.of(
                    s.getHsn() != null ? s.getHsn() : "N/A",
                    s.getProductName() != null ? s.getProductName() : "N/A",
                    s.getTotalQuantity() != null ? String.valueOf(s.getTotalQuantity()) : "0",
                    s.getTotalTaxableValue() != null ? String.format("%.2f", s.getTotalTaxableValue()) : "0.00",
                    s.getTotalCgst() != null ? String.format("%.2f", s.getTotalCgst()) : "0.00",
                    s.getTotalSgst() != null ? String.format("%.2f", s.getTotalSgst()) : "0.00",
                    s.getTotalIgst() != null ? String.format("%.2f", s.getTotalIgst()) : "0.00",
                    s.getTotalGst() != null ? String.format("%.2f", s.getTotalGst()) : "0.00"
            ));

            if (s.getTotalQuantity() != null) grandUnits += s.getTotalQuantity();
            if (s.getTotalTaxableValue() != null) grandTaxable += s.getTotalTaxableValue();
            if (s.getTotalCgst() != null) grandCgst += s.getTotalCgst();
            if (s.getTotalSgst() != null) grandSgst += s.getTotalSgst();
            if (s.getTotalIgst() != null) grandIgst += s.getTotalIgst();
            if (s.getTotalGst() != null) grandGst += s.getTotalGst();
        }

        List<String> footerCells = List.of(
                "GRAND TOTAL",
                "",
                String.valueOf(grandUnits),
                String.format("%.2f", grandTaxable),
                String.format("%.2f", grandCgst),
                String.format("%.2f", grandSgst),
                String.format("%.2f", grandIgst),
                String.format("%.2f", grandGst)
        );

        ReportData data = new ReportData();
        data.reportTitle = "GST Summary by HSN";
        data.duration = duration;
        data.headers = headers;
        data.rows = rows;
        data.footerCells = footerCells;
        return data;
    }

    /**
     * 3) Prepares a monthly summary of GST collected.
     */
    private ReportData prepareMonthlyGstReportData(LocalDateTime fromDate, LocalDateTime toDate, String userId, String duration) {

        // Call the new repository method from prodSalesRepo
        List<MonthlyGstSummaryDto> summary = prodSalesRepo.findMonthlyGstSummary(fromDate, toDate, userId);

        List<String> headers = List.of("Month", "Taxable Value (₹)", "CGST (₹)", "SGST (₹)", "IGST (₹)", "Total GST (₹)");
        List<List<String>> rows = new ArrayList<>();

        double grandTaxable = 0, grandCgst = 0, grandSgst = 0, grandIgst = 0, grandGst = 0;

        for (MonthlyGstSummaryDto s : summary) {
            rows.add(List.of(
                    s.getMonthYear() != null ? s.getMonthYear() : "N/A",
                    s.getTotalTaxableValue() != null ? String.format("%.2f", s.getTotalTaxableValue()) : "0.00",
                    s.getTotalCgst() != null ? String.format("%.2f", s.getTotalCgst()) : "0.00",
                    s.getTotalSgst() != null ? String.format("%.2f", s.getTotalSgst()) : "0.00",
                    s.getTotalIgst() != null ? String.format("%.2f", s.getTotalIgst()) : "0.00",
                    s.getTotalGst() != null ? String.format("%.2f", s.getTotalGst()) : "0.00"
            ));

            if (s.getTotalTaxableValue() != null) grandTaxable += s.getTotalTaxableValue();
            if (s.getTotalCgst() != null) grandCgst += s.getTotalCgst();
            if (s.getTotalSgst() != null) grandSgst += s.getTotalSgst();
            if (s.getTotalIgst() != null) grandIgst += s.getTotalIgst();
            if (s.getTotalGst() != null) grandGst += s.getTotalGst();
        }

        List<String> footerCells = List.of(
                "GRAND TOTAL",
                String.format("%.2f", grandTaxable),
                String.format("%.2f", grandCgst),
                String.format("%.2f", grandSgst),
                String.format("%.2f", grandIgst),
                String.format("%.2f", grandGst)
        );

        ReportData data = new ReportData();
        data.reportTitle = "Monthly GST Summary";
        data.duration = duration;
        data.headers = headers;
        data.rows = rows;
        data.footerCells = footerCells;
        return data;
    }
    private ReportData prepareCustomerGstReportData(LocalDateTime fromDate, LocalDateTime toDate, String userId, String duration) {

        // Call the new repository method
        List<GstByCustomerDto> summary = billRepo.findGstByCustomer(fromDate, toDate, userId);

        List<String> headers = List.of("Customer Name", "Phone", "GST Number", "Taxable Value (₹)", "CGST (₹)", "SGST (₹)", "IGST (₹)", "Total GST (₹)");
        List<List<String>> rows = new ArrayList<>();

        double grandTaxable = 0, grandCgst = 0, grandSgst = 0, grandIgst = 0, grandGst = 0;

        for (GstByCustomerDto s : summary) {
            rows.add(List.of(
                    s.getName() != null ? s.getName() : "N/A",
                    s.getPhone() != null ? s.getPhone() : "N/A",
                    s.getGstNumber() != null ? s.getGstNumber() : "N/A",
                    s.getTotalTaxableValue() != null ? String.format("%.2f", s.getTotalTaxableValue()) : "0.00",
                    s.getTotalCgst() != null ? String.format("%.2f", s.getTotalCgst()) : "0.00",
                    s.getTotalSgst() != null ? String.format("%.2f", s.getTotalSgst()) : "0.00",
                    s.getTotalIgst() != null ? String.format("%.2f", s.getTotalIgst()) : "0.00",
                    s.getTotalGst() != null ? String.format("%.2f", s.getTotalGst()) : "0.00"
            ));

            if (s.getTotalTaxableValue() != null) grandTaxable += s.getTotalTaxableValue();
            if (s.getTotalCgst() != null) grandCgst += s.getTotalCgst();
            if (s.getTotalSgst() != null) grandSgst += s.getTotalSgst();
            if (s.getTotalIgst() != null) grandIgst += s.getTotalIgst();
            if (s.getTotalGst() != null) grandGst += s.getTotalGst();
        }

        List<String> footerCells = List.of(
                "GRAND TOTAL",
                "", // Phone
                "", // GST Number
                String.format("%.2f", grandTaxable),
                String.format("%.2f", grandCgst),
                String.format("%.2f", grandSgst),
                String.format("%.2f", grandIgst),
                String.format("%.2f", grandGst)
        );

        ReportData data = new ReportData();
        data.reportTitle = "GST Summary by Customer";
        data.duration = duration;
        data.headers = headers;
        data.rows = rows;
        data.footerCells = footerCells;
        return data;
    }
}