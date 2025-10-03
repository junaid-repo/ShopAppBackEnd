package com.management.shop.util;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.management.shop.dto.OrderItem;

@Component
public class PDFInvoiceUtil {
	

		@Autowired
		private final TemplateEngine templateEngine;

		public PDFInvoiceUtil(TemplateEngine templateEngine) {
			this.templateEngine = templateEngine;
		}
	
		public byte[] generateInvoice(String customerName, String customerEmail, String customerPhone, String invoiceId, List<OrderItem> products, String orderedDate, double totalAmount, boolean paid, double gstRate, String shopName, String shopAddress, String shopEmail, String shopPhone, String gstNumber)
				throws Exception {
			Context context = new Context();
            double grandTotal = totalAmount;
            totalAmount=totalAmount-gstRate;

			
			//String barcodeBase64 = BarcodeGenerator.generateBarcodeBase64(invoiceId);
            String barcodeBase64 = BarcodeGenerator.generateBarcodeBase64(invoiceId);


            context.setVariable("shopName", shopName);
            context.setVariable("gstNumber", gstNumber);
			context.setVariable("shopAddress", shopAddress);
            context.setVariable("shopEmail", shopEmail);
            context.setVariable("shopPhone", shopPhone);
			context.setVariable("invoiceId", invoiceId);
			context.setVariable("products", products);
			context.setVariable("grandTotal", grandTotal);
			context.setVariable("customerName", customerName);
			context.setVariable("customerEmail", customerEmail);
			context.setVariable("customerPhone", customerPhone);
            context.setVariable("orderedDate", orderedDate);
            context.setVariable("totalAmount", totalAmount);
            context.setVariable("paid", paid);
            context.setVariable("gstRate", gstRate);
			context.setVariable("barcodeBase64", barcodeBase64);

			// Render Thymeleaf template into HTML string
			String htmlContent = templateEngine.process("invoice", context);

			// Convert HTML to PDF
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ITextRenderer renderer = new ITextRenderer();
			renderer.setDocumentFromString(htmlContent);
			renderer.layout();
			renderer.createPDF(baos);

			return baos.toByteArray();
		}

		// Sample product model
		public static class Product {
	        private String name;
	        private int quantity;
	        private double price;
            private double gst;
            private String details;
	        public Product(String name, int qty, double price, double gst, String details) {
	            this.name = name; this.quantity = qty; this.price = price;
                this.gst = gst;
                this.details=details;
	        }
	        public String getName() { return name; }
	        public int getQuantity() { return quantity; }
	        public double getPrice() { return price; }
            public String getDetails(){return details;}
	    }
}


