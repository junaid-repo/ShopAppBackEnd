package com.management.shop.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
import com.management.shop.repository.BillingRepository;
import com.management.shop.repository.ProductRepository;
import com.management.shop.repository.SalesPaymentRepository;
import com.management.shop.repository.ShopRepository;


@Component
public class ReportsGenerate {
	
	@Autowired
	private ShopRepository shopRepo;
	
	@Autowired
	private BillingRepository billRepo;
	
	@Autowired
	private SalesPaymentRepository salesPaymentRepo;
	
	@Autowired
	private ProductRepository prodRepo;
	
	

	public byte[] downloadReport(String reportType, LocalDateTime fromDate, LocalDateTime toDate) {

		byte[] fileBytes=null;
		
		if (reportType.equals("Sales Report")) {
			try {
				fileBytes=generateSalesReport(fromDate, toDate);
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
				fileBytes=generatePaymentReport(fromDate, toDate);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (reportType.equals("Customers Report")) {
			try {
				fileBytes=generateCustomerReport(fromDate, toDate);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return fileBytes;
	}

	private byte[] generateCustomerReport(LocalDateTime fromDate, LocalDateTime toDate) throws IOException {
		
		List<CustomerEntity> customerEntity=shopRepo.findCustomerByDateRange(fromDate, toDate);
		
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

	private byte[] generatePaymentReport(LocalDateTime fromDate, LocalDateTime toDate) throws IOException {
		List<BillingEntity> billList = billRepo.findPaymentsByDateRange(fromDate, toDate);
		List<PaymentDetails> response = new ArrayList<>();
		billList.stream().forEach(obj -> {

			response.add(PaymentDetails.builder()
					.id(salesPaymentRepo.findPaymentDetails(obj.getId()).getPaymentReferenceNumber())
					.amount(obj.getTotalAmount()).date(String.valueOf(obj.getCreatedDate()))
					.saleId(obj.getInvoiceNumber())
					.method(salesPaymentRepo.findPaymentDetails(obj.getId()).getPaymentMethod()).build());
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

	private byte[] generateSalesReport(LocalDateTime fromDate, LocalDateTime toDate) throws IOException {
		List<BillingEntity> listOfBills = billRepo.findPaymentsByDateRange(fromDate, toDate);
		List<SalesResponseDTO> response = new ArrayList();
		listOfBills.stream().forEach(obj -> {
			// SalesResponseDTO salesResponse
			var salesResponse = SalesResponseDTO.builder()
					.customer(shopRepo.findById(obj.getCustomerId()).get().getName())
					.date(String.valueOf(obj.getCreatedDate())).id(obj.getInvoiceNumber()).total(obj.getTotalAmount())
					.status(salesPaymentRepo.findPaymentDetails(obj.getId()).getStatus()).build();

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
}
