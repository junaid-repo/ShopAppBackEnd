package com.management.shop.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.management.shop.dto.ProductSalesReport;
import com.management.shop.dto.ProductSalesReportView;
import com.management.shop.entity.ProductEntity;
import com.management.shop.repository.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.management.shop.dto.PaymentDetails;
import com.management.shop.dto.SalesResponseDTO;
import com.management.shop.entity.BillingEntity;
import com.management.shop.entity.CustomerEntity;


@Component
public class ReportsGenerate {
	
	@Autowired
	private ShopRepository shopRepo;
	
	@Autowired
	private BillingRepository billRepo;
	
	@Autowired
	private SalesPaymentRepository salesPaymentRepo;

    @Autowired
    ProductSalesRepository prodSalesRepo;
	
	@Autowired
	private ProductRepository prodRepo;
	
	

	public byte[] downloadReport(String reportType, LocalDateTime fromDate, LocalDateTime toDate, String userId) {

		byte[] fileBytes=null;
		
		if (reportType.equals("Sales Report")) {
			try {
				fileBytes=generateSalesReport(fromDate, toDate, userId);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		/*
		 * if (reportType.equals("Product Report")) {
		 * fileBytes=generateProductReport(fromDate, toDate); }
		 */
		if (reportType.equals("Payment Reports")) {
			try {
				fileBytes=generatePaymentReport(fromDate, toDate, userId);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (reportType.equals("Customers Report")) {
			try {
				fileBytes=generateCustomerReport(fromDate, toDate, userId);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
        if (reportType.equals("Product Sales Report")) {
            try {
                fileBytes=generateProductSalesReport(fromDate, toDate, userId);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (reportType.equals("Products Report")) {
            try {
                fileBytes=generateProductsReport(fromDate, toDate, userId);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

		return fileBytes;
	}

	private byte[] generateCustomerReport(LocalDateTime fromDate, LocalDateTime toDate, String userId) throws IOException {
		
		List<CustomerEntity> customerEntity=shopRepo.findCustomerByDateRange(fromDate, toDate, userId);
		
		 try (Workbook workbook = new XSSFWorkbook();
	             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

	            Sheet sheet = workbook.createSheet("Customers");

	            // Create header style
	            CellStyle headerStyle = workbook.createCellStyle();
	            Font headerFont = workbook.createFont();
	            headerFont.setBold(true);
	            headerStyle.setFont(headerFont);

	            // Header row
	            Row headerRow = sheet.createRow(0);
	            String[] columns = {"ID", "Name", "Email", "Phone", "Total Spent"};
	            for (int i = 0; i < columns.length; i++) {
	                Cell cell = headerRow.createCell(i);
	                cell.setCellValue(columns[i]);
	                cell.setCellStyle(headerStyle);
	            }

	            // Data rows
	            int rowIdx = 1;
	            for (CustomerEntity customer : customerEntity) {
	                Row row = sheet.createRow(rowIdx++);
	                row.createCell(0).setCellValue(customer.getId());
	                row.createCell(1).setCellValue(customer.getName());
	                row.createCell(2).setCellValue(customer.getEmail());
	                row.createCell(3).setCellValue(customer.getPhone());
	                row.createCell(4).setCellValue(customer.getTotalSpent());
	            }

	            // Autosize columns
	            for (int i = 0; i < columns.length; i++) {
	                sheet.autoSizeColumn(i);
	            }

	            workbook.write(out);
	            return out.toByteArray();
	        }
		
		
	}

	private byte[] generatePaymentReport(LocalDateTime fromDate, LocalDateTime toDate, String userId) throws IOException {
		List<BillingEntity> billList = billRepo.findPaymentsByDateRange(fromDate, toDate, userId);
		List<PaymentDetails> response = new ArrayList<>();
		billList.stream().forEach(obj -> {

			response.add(PaymentDetails.builder()
					.id(salesPaymentRepo.findPaymentDetails(obj.getId(), userId).getPaymentReferenceNumber())
					.amount(obj.getTotalAmount()).date(String.valueOf(obj.getCreatedDate()))
					.saleId(obj.getInvoiceNumber())
					.method(salesPaymentRepo.findPaymentDetails(obj.getId(), userId).getPaymentMethod()).build());
		});
		
		
		 try (Workbook workbook = new XSSFWorkbook();
	             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

	            Sheet sheet = workbook.createSheet("Payments");

	            // Header style
	            CellStyle headerStyle = workbook.createCellStyle();
	            Font headerFont = workbook.createFont();
	            headerFont.setBold(true);
	            headerStyle.setFont(headerFont);

	            // Header row
	            String[] columns = {"ID", "Sale ID", "Date", "Amount", "Method"};
	            Row headerRow = sheet.createRow(0);
	            for (int i = 0; i < columns.length; i++) {
	                Cell cell = headerRow.createCell(i);
	                cell.setCellValue(columns[i]);
	                cell.setCellStyle(headerStyle);
	            }

	            // Data rows
	            int rowIdx = 1;
	            double totalSaleId = 0;
	            double totalAmount = 0;

	            for (PaymentDetails payment : response) {
	                Row row = sheet.createRow(rowIdx++);
	                row.createCell(0).setCellValue(payment.getId());
	                row.createCell(1).setCellValue(payment.getSaleId());
	                row.createCell(2).setCellValue(payment.getDate());
	                
	                if (payment.getAmount() != null) {
	                    row.createCell(3).setCellValue(payment.getAmount());
	                    totalAmount += payment.getAmount();
	                }

	                row.createCell(4).setCellValue(payment.getMethod());

	                // Try parsing saleId as a number for summation
	                try {
	                    totalSaleId += Double.parseDouble(payment.getSaleId());
	                } catch (NumberFormatException e) {
	                    // Ignore if saleId is not numeric
	                }
	            }

	            // Totals row
	            Row totalRow = sheet.createRow(rowIdx);
	            CellStyle totalStyle = workbook.createCellStyle();
	            Font boldFont = workbook.createFont();
	            boldFont.setBold(true);
	            totalStyle.setFont(boldFont);

	            Cell totalLabelCell = totalRow.createCell(0);
	            totalLabelCell.setCellValue("TOTALS");
	            totalLabelCell.setCellStyle(totalStyle);

	            Cell saleIdTotalCell = totalRow.createCell(1);
	            saleIdTotalCell.setCellValue(totalSaleId);
	            saleIdTotalCell.setCellStyle(totalStyle);

	            // Leave Date column blank
	            Cell amountTotalCell = totalRow.createCell(3);
	            amountTotalCell.setCellValue(totalAmount);
	            amountTotalCell.setCellStyle(totalStyle);

	            // Autosize columns
	            for (int i = 0; i < columns.length; i++) {
	                sheet.autoSizeColumn(i);
	            }

	            workbook.write(out);
	            return out.toByteArray();
	        }
		
	}

	/*
	 * private byte[] generateProductReport(LocalDateTime fromDate, LocalDateTime
	 * toDate) { List<ProductEntity>
	 * response=prodRepo.findProductsByDateRage(fromDate, toDate);
	 * 
	 * }
	 */

	private byte[] generateSalesReport(LocalDateTime fromDate, LocalDateTime toDate, String userId) throws IOException {
		List<BillingEntity> listOfBills = billRepo.findPaymentsByDateRange(fromDate, toDate, userId);
		List<SalesResponseDTO> response = new ArrayList();
		listOfBills.stream().forEach(obj -> {
			// SalesResponseDTO salesResponse
            var salesResponse = SalesResponseDTO.builder()
                    .customer(shopRepo.findByIdAndUserId(obj.getCustomerId(), userId).getName())
                    .date(String.valueOf(obj.getCreatedDate())).id(obj.getInvoiceNumber()).total(obj.getTotalAmount())
                    .status(salesPaymentRepo.findPaymentDetails(obj.getId(), userId).getStatus()).build();

			response.add(salesResponse);

		});
		{
	        try (Workbook workbook = new XSSFWorkbook();
	             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

	            Sheet sheet = workbook.createSheet("Sales");

	            // Create header style
	            CellStyle headerStyle = workbook.createCellStyle();
	            Font headerFont = workbook.createFont();
	            headerFont.setBold(true);
	            headerStyle.setFont(headerFont);

	            // Header row
	            String[] columns = {"ID", "Customer", "Date", "Total", "Status"};
	            Row headerRow = sheet.createRow(0);
	            for (int i = 0; i < columns.length; i++) {
	                Cell cell = headerRow.createCell(i);
	                cell.setCellValue(columns[i]);
	                cell.setCellStyle(headerStyle);
	            }

	            // Data rows
	            int rowIdx = 1;
	            int idCount = 0;
	            double totalSum = 0;

	            for (SalesResponseDTO sale : response) {
	                Row row = sheet.createRow(rowIdx++);
	                row.createCell(0).setCellValue(sale.getId());
	                row.createCell(1).setCellValue(sale.getCustomer());
	                row.createCell(2).setCellValue(sale.getDate());

	                if (sale.getTotal() != null) {
	                    row.createCell(3).setCellValue(sale.getTotal());
	                    totalSum += sale.getTotal();
	                }

	                row.createCell(4).setCellValue(sale.getStatus());

	                if (sale.getId() != null && !sale.getId().trim().isEmpty()) {
	                    idCount++;
	                }
	            }

	            // Totals row
	            CellStyle totalStyle = workbook.createCellStyle();
	            Font boldFont = workbook.createFont();
	            boldFont.setBold(true);
	            totalStyle.setFont(boldFont);

	            Row totalRow = sheet.createRow(rowIdx);
	            Cell labelCell = totalRow.createCell(0);
	            labelCell.setCellValue("TOTALS");
	            labelCell.setCellStyle(totalStyle);

	            Cell countCell = totalRow.createCell(0 + 1); // Column B for count
	            countCell.setCellValue("Count of IDs: " + idCount);
	            countCell.setCellStyle(totalStyle);

	            Cell sumCell = totalRow.createCell(3); // Column D for sum
	            sumCell.setCellValue(totalSum);
	            sumCell.setCellStyle(totalStyle);

	            // Autosize columns
	            for (int i = 0; i < columns.length; i++) {
	                sheet.autoSizeColumn(i);
	            }

	            workbook.write(out);
	            return out.toByteArray();
	        }
		
	}
	}
    private byte[] generateProductSalesReport(LocalDateTime fromDate, LocalDateTime toDate, String userId) throws IOException {

        List<ProductSalesReport> response=new ArrayList<>();
        List<ProductSalesReportView> intermediateResponse=new ArrayList<>();
        try {
            intermediateResponse = prodSalesRepo.findSalesReportNative(fromDate, toDate, userId);

        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("ProductSales");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Header row
            String[] columns = {"Product Name", "Category", "Units", "Amount", "GST", "Profit on CP", "Invoice Number", "Date"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 1;
            double totalSaleId = 0;
            double totalAmount = 0;

            for (ProductSalesReportView payment : intermediateResponse) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(payment.getProductName() != null ? payment.getProductName() : "");
                row.createCell(1).setCellValue(payment.getCategory() != null ? payment.getCategory() : "");
                row.createCell(2).setCellValue(payment.getTotalSold() != null ? payment.getTotalSold() : 0);
                row.createCell(3).setCellValue(payment.getTotal() != null ? payment.getTotal() : 0.0);
                row.createCell(4).setCellValue(payment.getTax() != null ? payment.getTax() : 0.0);
                row.createCell(5).setCellValue(payment.getProfitOnCp() != null ? payment.getProfitOnCp() : 0.0);
                row.createCell(6).setCellValue(payment.getInvoiceNumber() != null ? payment.getInvoiceNumber() : "");
                row.createCell(7).setCellValue( (String.valueOf(payment.getInvoiceDate()) != null ? String.valueOf(payment.getInvoiceDate()) : ""));



            }

            // Totals row
            Row totalRow = sheet.createRow(rowIdx);
            CellStyle totalStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            totalStyle.setFont(boldFont);



            // Autosize columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }

    }

    private byte[] generateProductsReport(LocalDateTime fromDate, LocalDateTime toDate, String userId) throws IOException {

        List<ProductEntity> productList=prodRepo.getAllProductForReport(Boolean.TRUE, userId);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Products");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Header row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Name", "Category", "Cost Price", "Price", "GST%", "Stock", "Status"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 1;
            for (ProductEntity product : productList) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(product.getName());
                row.createCell(1).setCellValue(product.getCategory());
                row.createCell(2).setCellValue(product.getCostPrice());
                row.createCell(3).setCellValue(product.getPrice());
                row.createCell(4).setCellValue(product.getTaxPercent());
                row.createCell(5).setCellValue(product.getStock());
                row.createCell(6).setCellValue(product.getStatus());
            }

            // Autosize columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }


    }
}
