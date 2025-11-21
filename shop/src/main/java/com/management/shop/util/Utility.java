package com.management.shop.util;

import com.management.shop.dto.InvoiceData;
import com.management.shop.dto.InvoiceDetails;
import com.management.shop.dto.OrderItem;
import com.management.shop.dto.OrderItemInvoice;
import com.management.shop.dto.UpdateUserDTO;
import com.management.shop.entity.*;
import com.management.shop.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class Utility {

    //<-- All @Autowired repositories remain the same -->
    @Autowired
    private BillingGstRepository billGstRepo;
    @Autowired
    private ShopBasicRepository shopBasicRepo;
    @Autowired
    private ShopFinanceRepository shopFinanceRepo;
    @Autowired
    private ShopBankRepository shopBankRepo;
    @Autowired
    private ShopUPIRepository salesUPIRepo;
    @Autowired
    private ShopInvoiceTermsRepository shopInvoiceTermsRepo;
    @Autowired
    private ShopDetailsRepo shopDetailsRepo;
    @Autowired
    private UserInfoRepository userinfoRepo;
    @Autowired
    private ShopRepository shopRepo;
    @Autowired
    private ProductRepository prodRepo;
    @Autowired
    private BillingRepository billRepo;
    @Autowired
    private ProductSalesRepository prodSalesRepo;
    @Autowired
    private SalesPaymentRepository salesPaymentRepo;
    @Autowired
    private UserProfilePicRepo userProfilePicRepo;
    @Autowired
    private UserSettingsRepository userSettingsRepo;

    @Autowired
    private PaymentHistoryRepository paymentHisRepo;

    @Autowired
    private ShopRepository custRepo;

    @Autowired
    SelectedInvoiceRepository invoiceRepo;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Autowired
    private S3Client s3Client;

    /**
     * Helper method to convert a null string to an empty string.
     */
    private String toEmpty(String value) {
        return value == null ? "" : value;
    }

    public String extractUsername() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // For testing purposes, you might uncomment the line below
        // username="junaid1";
        return username;
    }


    public InvoiceData getFullInvoiceDetails(String username, String orderId) {
        UpdateUserDTO userProfile = getUserProfile(username);
        InvoiceDetails order = getOrderDetails(orderId);

        // Safely parse the date, defaulting to now() if invalid/null
        LocalDate orderedDate = LocalDate.now();
        if (order.getOrderedDate() != null && !order.getOrderedDate().isEmpty()) {
            try {
                orderedDate = LocalDate.parse(order.getOrderedDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                // Log error if needed, but proceed with default date
            }
        }
        String formattedDate = orderedDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        byte[] shopLogoBytes = null;
        try {
            shopLogoBytes = getShopLogo(username);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            shopLogoBytes = null;
        }

        List<String> terms = new ArrayList<>();
        // Safely handle terms, even if the string is null
        if (userProfile != null && userProfile.getTerms1() != null) {
            Arrays.stream(userProfile.getTerms1().split("##"))
                    .filter(term -> term != null && !term.trim().isEmpty())
                    .forEach(term -> terms.add(term.trim()));
        }

        // Convert order items to invoice line items
        List<OrderItemInvoice> products = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem it : order.getItems()) {
                if (it == null) continue; // Skip null items in the list

                int qty = it.getQuantity();
                double totalForLine = it.getUnitPrice(); // Note: in existing code unitPrice is stored as line total
                double taxAmount = it.getGst(); // total tax for this line

                double rateBeforeTaxPerUnit = (qty > 0) ? (totalForLine - taxAmount) / qty : 0;
                double taxableBase = rateBeforeTaxPerUnit * Math.max(1, qty);
                double taxPercentage = (taxableBase > 0) ? (taxAmount / taxableBase) * 100.0 : 0;

                OrderItemInvoice line = OrderItemInvoice.builder()
                        .productName(toEmpty(it.getProductName()))
                        .hsnCode(it.getHsn()) // Assuming HSN is not available
                        .quantity(qty)
                        .rate(rateBeforeTaxPerUnit)
                        .taxAmount(it.getCgst() + it.getIgst() + it.getSgst())
                        .cgst(it.getCgst())
                        .igst(it.getIgst())
                        .sgst(it.getSgst())
                        .cgstPercentage(it.getCgstPercentage())
                        .igstPercentage(it.getIgstPercentage())
                        .sgstPercentage(it.getSgstPercentage())
                        .taxPercentage(taxPercentage)
                        .discountPercentage(it.getDiscount())
                        .description(it.getDetails())
                        .totalAmount(totalForLine)
                        .build();
                products.add(line);
            }
        }

        List<Map<String, Object>> gstSummary = new ArrayList<>();
        List<BillingGstEntity> billGstList = billGstRepo.findByUserIdAndOrderId(username, orderId);
        if (billGstList != null) {
            billGstList.forEach(gstEntry -> {
                if (gstEntry != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("type", toEmpty(gstEntry.getGstType()));
                    map.put("percentage", gstEntry.getGstPercentage());
                    map.put("amount", gstEntry.getGstAmount());
                    gstSummary.add(map);
                }
            });
        }

        //updateParametersAsPerSettings(order, userProfile);

        Boolean printShopPan = true;

        Boolean printCustomerGst = true;
        Boolean combineCustomerAddresses = true;

        Boolean itemDiscount=false;
        Boolean showHsnColumn=true;
        Boolean showRateColumn=true;

        Boolean showTotalDiscount=true;
        Boolean printDueAmount = true;
        Boolean addDueDate = false;

        Boolean showSupportInfo=true;
        Boolean removeTerms=false;

        try {
            UserSettingsEntity userSettingsEntity= userSettingsRepo.findByUsername(extractUsername());
            printDueAmount=   userSettingsEntity.getShowPaymentStatus();
            printCustomerGst=   userSettingsEntity.getShowCustomerGstin();
            printShopPan=userSettingsEntity.getShowShopPan();
            combineCustomerAddresses=userSettingsEntity.getCombineAddresses(); //
            itemDiscount=userSettingsEntity.getShowItemDiscount();
            showHsnColumn=userSettingsEntity.getShowHsnColumn();
            showRateColumn=userSettingsEntity.getShowRateColumn();
            showTotalDiscount=userSettingsEntity.getShowTotalDiscount();
            addDueDate=userSettingsEntity.getAddDueDate();
            showSupportInfo=userSettingsEntity.getShowSupportInfo();
            removeTerms=userSettingsEntity.getRemoveTerms();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }




        CustomerEntity custEntity = custRepo.findByIdAndUserId(order.getCustomerId(), username);

        // Using optional to avoid NullPointerException if userProfile is null
        return InvoiceData.builder()
                .shopName(Optional.ofNullable(userProfile).map(p -> toEmpty(p.getShopName())).orElse(""))
                .shopSlogan(Optional.ofNullable(userProfile).map(p -> toEmpty(p.getShopSlogan())).orElse(""))
                // Assuming not implemented
                .shopLogoText(Optional.ofNullable(userProfile).map(p -> toEmpty(p.getShopName())).orElse(""))
                .shopAddress(Optional.ofNullable(userProfile).map(p -> toEmpty(p.getShopLocation())).orElse(""))
                .shopEmail(Optional.ofNullable(userProfile).map(p -> toEmpty(p.getShopEmail())).orElse(""))
                .shopPhone(Optional.ofNullable(userProfile).map(p -> toEmpty(p.getShopPhone())).orElse(""))
                .gstNumber(Optional.ofNullable(userProfile).map(p -> toEmpty(p.getGstin())).orElse(""))
                .panNumber(Optional.ofNullable(userProfile).map(p -> toEmpty(p.getPan())).orElse(""))
                .shopLogoBytes(shopLogoBytes)
                .shopLogoText(userProfile.getShopName() == null ? "" :
                        Arrays.stream(userProfile.getShopName().trim().split("\\s+"))
                                .filter(s -> !s.isEmpty())
                                .map(s -> s.substring(0, 1).toUpperCase())
                                .collect(Collectors.joining()))
                .invoiceId(toEmpty(order.getInvoiceId()))
                .orderedDate(formattedDate)
                .dueDate(formattedDate)

                .customerName(toEmpty(order.getCustomerName()))

                .customerBillingAddress(custEntity.getCity() + ", " + custEntity.getState())
                .customerShippingAddress(custEntity.getCity() + ", " + custEntity.getState())
                .customerPhone(toEmpty(order.getCustomerPhone()))
                .customerGst(toEmpty(order.getCustomerGstNumber()))
                .customerState(custEntity.getCity() + ", " + custEntity.getState())
                .products(products)

                .receivedAmount(order.isPaid() ? order.getTotalAmount() : 0d)
                .paidAmount(order.getPaidAmount())
                .dueAmount(order.getDueAmount())
                .previousBalance(0d)
                .grandTotal(order.getTotalAmount())
                .discountPercentage(order.getDiscountRate())
                .gstSummary(gstSummary)

                .bankAccountName(Optional.ofNullable(userProfile).map(p -> toEmpty(p.getBankHolder())).orElse(""))
                .bankAccountNumber(Optional.ofNullable(userProfile).map(p -> toEmpty(p.getBankAccount())).orElse(""))
                .bankIfscCode(Optional.ofNullable(userProfile).map(p -> toEmpty(p.getBankIfsc())).orElse(""))
                .bankName(Optional.ofNullable(userProfile).map(p -> toEmpty(p.getBankName())).orElse(""))
                .upiId(Optional.ofNullable(userProfile).map(p -> toEmpty(p.getUpi())).orElse(""))

                .termsAndConditions(terms)

                .printShopPan(printShopPan)
                .printCustomerGst(printCustomerGst)
                .combineCustomerAddresses(combineCustomerAddresses)
                .itemDiscount(itemDiscount)
                .showHsnColumn(showHsnColumn)
                .showRateColumn(showRateColumn)
                .showTotalDiscount(showTotalDiscount)
                .printDueAmount(printDueAmount)
                .addDueDate(addDueDate)
                .showSupportInfo(showSupportInfo)
                .removeTerms(removeTerms)


                .build();
    }

    private void updateParametersAsPerSettings(InvoiceDetails order, UpdateUserDTO userProfile) {
        Boolean printShopPan = true;

        Boolean printCustomerGst = true;
        Boolean combineCustomerAddresses = true;

        Boolean itemDiscount=false;
        Boolean showHsnColumn=true;
        Boolean showRateColumn=true;

        Boolean showTotalDiscount=true;
        Boolean printDueAmount = true;
        Boolean addDueDate = false;

        Boolean showSupportInfo=true;
        Boolean removeTerms=false;

        try {
            UserSettingsEntity userSettingsEntity= userSettingsRepo.findByUsername(extractUsername());
            printDueAmount=   userSettingsEntity.getShowPaymentStatus();
            printCustomerGst=   userSettingsEntity.getShowCustomerGstin();
            printShopPan=userSettingsEntity.getShowShopPan();
            combineCustomerAddresses=userSettingsEntity.getCombineAddresses(); //
            itemDiscount=userSettingsEntity.getShowItemDiscount();
            showHsnColumn=userSettingsEntity.getShowHsnColumn();
            showRateColumn=userSettingsEntity.getShowRateColumn();
            showTotalDiscount=userSettingsEntity.getShowTotalDiscount();
            addDueDate=userSettingsEntity.getAddDueDate();
            showSupportInfo=userSettingsEntity.getShowSupportInfo();
            removeTerms=userSettingsEntity.getRemoveTerms();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if(!printDueAmount){
            order.setPaidAmount(0);
            order.setDueAmount(0);
        }
        if(!printCustomerGst){
            order.setCustomerGstNumber("");
        }
        if(!printShopPan){
           userProfile.setPan("");
        }


    }

    public UpdateUserDTO getUserProfile(String username) {
        username = extractUsername();

        // Use orElse(null) or orElse(new UserInfo()) for safer handling
        UserInfo userinfo = userinfoRepo.findByUsername(username).orElse(new UserInfo());

        // Fetch optional entities, which will be null if not found
        ShopDetailsEntity shopDetails = shopDetailsRepo.findbyUsername(username);
        ShopBasicEntity shopBasicEntity = shopBasicRepo.findByUserId(username);
        ShopFinanceEntity shopFinanceEntity = shopFinanceRepo.findByUserId(username);
        ShopInvoiceTermsEnity shopInvoiceTermsEntity = shopInvoiceTermsRepo.findByUserId(username);

        // Fetch nested entities only if the parent exists
        ShopBankEntity shopBankEntity = null;
        ShopUPIEntity shopUPIEntity = null;
        if (shopFinanceEntity != null) {
            shopBankEntity = shopBankRepo.findByShopFinanceId(username);
            shopUPIEntity = salesUPIRepo.findByShopFinanceId(username);
        }

        // Build the DTO safely using Optional to avoid NPEs
        return UpdateUserDTO.builder()
                .username(toEmpty(username))
                .email(toEmpty(userinfo.getEmail()))
                .name(toEmpty(userinfo.getName()))
                .phone(toEmpty(userinfo.getPhoneNumber()))
                .userSource(toEmpty(userinfo.getSource()))

                // ✅ Use correct shopBasicEntity safely
                .address(Optional.ofNullable(shopBasicEntity).map(s -> toEmpty(s.getAddress())).orElse(""))
                .shopAddress(Optional.ofNullable(shopBasicEntity).map(s -> toEmpty(s.getAddress())).orElse(""))
                .shopLocation(Optional.ofNullable(shopBasicEntity).map(s -> toEmpty(s.getAddress())).orElse(""))
                .shopEmail(Optional.ofNullable(shopBasicEntity).map(s -> toEmpty(s.getShopEmail())).orElse(""))
                .shopPhone(Optional.ofNullable(shopBasicEntity).map(s -> toEmpty(s.getShopPhone())).orElse(""))
                .shopName(Optional.ofNullable(shopBasicEntity).map(s -> toEmpty(s.getShopName())).orElse(""))
                .shopPincode(Optional.ofNullable(shopBasicEntity).map(s -> toEmpty(s.getShopPincode())).orElse(""))
                .shopCity(Optional.ofNullable(shopBasicEntity).map(s -> toEmpty(s.getShopCity())).orElse(""))
                .shopState(Optional.ofNullable(shopBasicEntity).map(s -> toEmpty(s.getShopState())).orElse(""))
                .shopSlogan(Optional.ofNullable(shopBasicEntity).map(s -> toEmpty(s.getShopSlogan())).orElse(""))

                // ✅ Finance entity
                .gstNumber(Optional.ofNullable(shopFinanceEntity).map(s -> toEmpty(s.getGstin())).orElse(""))
                .gstin(Optional.ofNullable(shopFinanceEntity).map(s -> toEmpty(s.getGstin())).orElse(""))
                .pan(Optional.ofNullable(shopFinanceEntity).map(s -> toEmpty(s.getPanNumber())).orElse(""))

                // ✅ UPI entity
                .upi(Optional.ofNullable(shopUPIEntity).map(s -> toEmpty(s.getUpiId())).orElse(""))

                // ✅ Invoice terms
                .terms1(Optional.ofNullable(shopInvoiceTermsEntity).map(s -> toEmpty(s.getTerm())).orElse(""))

                // ✅ Bank details
                .bankAccount(Optional.ofNullable(shopBankEntity).map(s -> toEmpty(s.getAccountNumber())).orElse(""))
                .bankAddress(Optional.ofNullable(shopBankEntity).map(s -> toEmpty(s.getBranchName())).orElse(""))
                .bankHolder(Optional.ofNullable(shopBankEntity).map(s -> toEmpty(s.getAccountHolderName())).orElse(""))
                .bankName(Optional.ofNullable(shopBankEntity).map(s -> toEmpty(s.getBankName())).orElse(""))
                .bankIfsc(Optional.ofNullable(shopBankEntity).map(s -> toEmpty(s.getIfscCode())).orElse(""))

                .build();

    }

    public InvoiceDetails getOrderDetails(String orderReferenceNumber) {
        String username = extractUsername();

        // If the main billing record doesn't exist, return an empty object
        BillingEntity billDetails = billRepo.findOrderByReference(orderReferenceNumber, username);
        if (billDetails == null) {
            return InvoiceDetails.builder().items(Collections.emptyList()).build(); // Return empty details
        }

        // Use default empty objects if related entities are not found
        PaymentEntity paymentEntity = salesPaymentRepo.findPaymentDetails(billDetails.getId(), username);
        if (paymentEntity == null) paymentEntity = new PaymentEntity();

        CustomerEntity customerEntity = shopRepo.findByIdAndUserId(billDetails.getCustomerId(), username);
        if (customerEntity == null) customerEntity = new CustomerEntity();

        // Check payment status safely
        boolean paid = "Paid".equalsIgnoreCase(paymentEntity.getStatus());

        List<ProductSalesEntity> prodSales = prodSalesRepo.findByOrderId(billDetails.getId(), username);
        double totalGst = prodSales.stream()
                .filter(Objects::nonNull)
                .mapToDouble(ProductSalesEntity::getTax)
                .sum();

        List<OrderItem> items = prodSales.stream()
                .filter(Objects::nonNull)
                .map(obj -> {
                    ProductEntity prodRes = prodRepo.findByIdAndUserId(obj.getProductId(), username);
                    String productName = (prodRes != null) ? toEmpty(prodRes.getName()) : "Unknown Product";

                    return OrderItem.builder()
                            .productName(productName)
                            .unitPrice(obj.getTotal())
                            .gst(obj.getTax())
                            .sgst(obj.getSgst())
                            .sgstPercentage(obj.getSgstPercentage())
                            .cgst(obj.getCgst())
                            .cgstPercentage(obj.getCgstPercentage())
                            .igst(obj.getIgst())
                            .igstPercentage(obj.getIgstPercentage())
                            .details(toEmpty(obj.getProductDetails()))
                            .discount(obj.getDiscountPercentage())
                            .quantity(obj.getQuantity())
                            .hsn(prodRes.getHsn())
                            .build();
                }).collect(Collectors.toList());

        String createdDateStr = Optional.ofNullable(billDetails.getCreatedDate())
                .map(String::valueOf)
                .map(s -> s.length() >= 10 ? s.substring(0, 10) : "")
                .orElse("");

        return InvoiceDetails.builder()
                .discountRate(billDetails.getDiscountPercent())
                .invoiceId(toEmpty(orderReferenceNumber))
                .paymentReferenceNumber(toEmpty(paymentEntity.getPaymentReferenceNumber()))
                .paidAmount(paymentEntity.getPaid())
                .customerGstNumber(toEmpty(billDetails.getGstin()))
                .dueAmount(paymentEntity.getToBePaid())
                .items(items)
                .gstRate(totalGst)
                .customerPhone(toEmpty(customerEntity.getPhone()))
                .customerEmail(toEmpty(customerEntity.getEmail()))
                .customerId(billDetails.getCustomerId())
                .orderedDate(createdDateStr)
                .totalAmount(billDetails.getTotalAmount())
                .customerName(toEmpty(customerEntity.getName()))
                .paid(paid)
                .build();
    }

    public byte[] getShopLogo(String username) {

        System.out.println("entered getProfilePic with request  username " + username);

        UserInfo res = userinfoRepo.findByUsername(username).get();


        byte[] content = null;

        try {
            UserProfilePicEntity picRes = userProfilePicRepo.findByUsername(username);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(picRes.getShopLogo())
                    .build();
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            content = s3Object.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
            content = null; // Or handle error appropriately
        }


        return content;
    }

    public Map<String, Object> gstBillingSanity() {
        Map<String, Object> response = new HashMap<>();
        UpdateUserDTO userDetails = getUserProfile(extractUsername());
        List<String> missingDetails = new ArrayList<>();
        if (userDetails != null) {
            if (userDetails.getShopState() == null) {
                response.put("success", false);
                response.put("message", "Please set your State for proper gst calculation");
                response.put("type", "error");

                return response;
            }
            // Check each essential field for null or empty values
            if (userDetails.getShopName() == null || userDetails.getShopName().trim().isEmpty()) {
                missingDetails.add("Shop Name");
            }
            if (userDetails.getShopAddress() == null || userDetails.getShopAddress().trim().isEmpty()) {
                missingDetails.add("Shop Address");
            }
            if (userDetails.getShopState() == null || userDetails.getShopState().trim().isEmpty()) {
                missingDetails.add("Shop State");
            }
            if (userDetails.getShopPincode() == null || userDetails.getShopPincode().trim().isEmpty()) {
                missingDetails.add("Shop Pincode");
            }
            // Check for GSTIN (assuming gstin and gstNumber are interchangeable)
            if ((userDetails.getGstin() == null || userDetails.getGstin().trim().isEmpty()) &&
                    (userDetails.getGstNumber() == null || userDetails.getGstNumber().trim().isEmpty())) {
                missingDetails.add("GST Number");
            }

            // If the list of missing details is not empty, construct the warning message
            if (!missingDetails.isEmpty()) {
                String missingFields = String.join(", ", missingDetails);
                String message = "These details are not present: " + missingFields + ". Please add these for proper gst invoice.";

                response.put("success", false);
                response.put("type", "warning"); // Set type to 'warning' for informational messages
                response.put("message", message);

                return response;
            }
        }

// If all checks pass, return a success response
        response.put("success", true);

        return response;

    }

    @Transactional
    public Map<String, String> saveUserInvoiceTemplate(Map<String, Object> request) {

        String selectedTemplate = request.get("selectedTemplateName").toString();

        Map<String, String> response = new HashMap<>();
        response.put("statusText", "success");

        SelectedInvoiceEntity repoEntityCheck = invoiceRepo.findByUsername(extractUsername());

        if(repoEntityCheck!=null){
            invoiceRepo.updateSelectedInvoice(selectedTemplate, extractUsername(), LocalDateTime.now());
        }
        else {
            var selectedInvoiceEntity = SelectedInvoiceEntity.builder().templateName(selectedTemplate).username(extractUsername()).updatedBy(extractUsername()).updatedDate(LocalDateTime.now()).build();

            SelectedInvoiceEntity repoEntity = invoiceRepo.save(selectedInvoiceEntity);
            if (repoEntity != null) {
                response.put("statusText", "success");
            } else {
                response.put("statusText", "some error occured");
            }

        }



        return response;
    }



    public Map<String, String> getInvoiceTemplate() {

        SelectedInvoiceEntity repoEntity = invoiceRepo.findByUsername(extractUsername());
        Map<String, String> response = new HashMap<>();
        String templateName = repoEntity.getTemplateName();
        response.put("selectedTemplateName", templateName);

        return response;
    }


   public String asyncSavePaymentHistory(Integer billingId, Integer paymentId, Double paidAmount, String orderNumber) {
        //CompletableFuture.supplyAsync()


            PaymentHistory paymentHistoryEntity= PaymentHistory.builder()
                    .billingId(billingId)
                    .paymentId(paymentId)
                    .paidAmount(paidAmount)
                    .orderNumber(orderNumber)
                    .userId(extractUsername())
                    .createdDate(LocalDateTime.now())
                    .updatedDate(LocalDateTime.now())
                    .updatedBy(extractUsername())
                    .build();

            PaymentHistory paymentHisResponse=    paymentHisRepo.save(paymentHistoryEntity);

            if(paymentHisResponse.getId()!=null){
                String tokenNumber="TKN-"+ LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +"-"+String.format("%04d",paymentHisResponse.getId());

                paymentHistoryEntity.setTokenNo(tokenNumber);

                paymentHisRepo.save(paymentHistoryEntity);
            }

            if(paymentHisResponse!=null){
                System.out.println("Payment history saved successfully for order "+paymentHisResponse.getTokenNo());
            }else {
                return "some error while saving payment history";
            }
       System.out.println("Async method invoked for saving payment history ");

            return "done";



    }

    public List<PaymentHistory> getPaymentHistory(String orderNo) {

        return paymentHisRepo.findPaymentHistoryByOrderNumber(orderNo, extractUsername());
    }
}