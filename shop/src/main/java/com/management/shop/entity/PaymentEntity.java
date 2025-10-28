package com.management.shop.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="BillingPayments")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentEntity {
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;
	
	private Integer billingId;
	private String paymentMethod;
	private String status;
	private Double tax;
	private Double subtotal;
	private Double total;
    private Double paid;
    private Double toBePaid;
	private String paymentReferenceNumber;
	private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String updatedBy;
    private String userId;
    private String orderNumber;
    private Integer reminderCount;
	
	@PostPersist
    public void generateOrderNumberOnPersist() {
        // Ensure the ID is available and orderNumber hasn't been set yet
        if (this.id != null && this.paymentReferenceNumber == null) {
            // Format the current date into YYYYMMDD (e.g., 20230818)
            String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

      
            String sequentialPart = String.format("%04d", this.id);


            this.paymentReferenceNumber = "PMT-" + datePart + "-" + sequentialPart;

        }
    }
	
}
