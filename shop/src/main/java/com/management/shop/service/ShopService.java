package com.management.shop.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.management.shop.dto.*;
import com.management.shop.entity.*;
import com.management.shop.repository.*;
import com.management.shop.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

//OpenPDF (com.lowagie.*)
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@Slf4j
public class ShopService {

    @Autowired
    private Environment environment;

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
    private ReportDetailsRepo reportDRepo;

    @Autowired
    private ShopDetailsRepo shopDetailsRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserProfilePicRepo userProfilePicRepo;

    @Autowired
    private RegisterUserRepo newUserRepo;

    @Autowired
    private UserPaymentModesRepo paymentModesRepo;

    @Autowired
    private EstimatedGoalRepository estimatedGoalsRepo;

    @Autowired
    CSVUpload util;

    @Autowired
    ReportsGenerate repogen;

    @Autowired
    PDFInvoiceUtil pdfutil;

    @Autowired
    EmailSender email;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private OTPSender otpSender;

    @Autowired
    OrderEmailTemplate emailTemplate;

    @Autowired
    SalesCacheService salesCacheService;

    @Autowired
    private NotificationsRepo notiRepo;

    private final Random random = new Random();

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public String extractUsername() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("Current user: " + username);
      //  username="junaid1";
        return username;
    }


    public boolean checkUserStatus(String username) {
        // TODO Auto-generated method stub
        return userinfoRepo.findByUsername(username).get().getIsActive();
    }


    public CustomerSuccessDTO saveCustomer(CustomerRequest request) {
        System.out.println("entered into saveCustomer with" + request.toString());

        List<CustomerEntity> existingCustomer = shopRepo.findByPhone(request.getPhone(), "ACTIVE", extractUsername());

        CustomerEntity ent = null;

        if (existingCustomer.size() > 0) {

            var customerEntity = CustomerEntity.builder().userId(extractUsername()).id(existingCustomer.get(0).getId()).name(request.getName()).email(request.getEmail())
                    .createdDate(LocalDateTime.now()).phone(request.getPhone()).status("ACTIVE").totalSpent(existingCustomer.get(0).getTotalSpent()).build();

            ent = shopRepo.save(customerEntity);

        } else {

            var customerEntity = CustomerEntity.builder().userId(extractUsername()).name(request.getName()).email(request.getEmail())
                    .createdDate(LocalDateTime.now()).phone(request.getPhone()).status("ACTIVE").totalSpent(0).build();

            ent = shopRepo.save(customerEntity);
        }


        if (ent.getId() != null) {
            try {
                salesCacheService.evictUserCustomers(extractUsername());
                salesCacheService.evictsUserAnalytics(extractUsername());

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return CustomerSuccessDTO.builder().success(true).customer(request).build();
        }

        return CustomerSuccessDTO.builder().id(ent.getId()).success(false).customer(request).build();

    }

    @CacheEvict(value = "customers", key = "#root.target.extractUsername()")
    public CustomerEntity saveCustomerForBilling(CustomerRequest request) {
        System.out.println("entered into saveCustomer with" + request.toString());
        List<CustomerEntity> existingCustomer = shopRepo.findByPhone(request.getPhone(), "ACTIVE", extractUsername());

        CustomerEntity ent = null;

        if (existingCustomer.size() > 0) {

            var customerEntity = CustomerEntity.builder().id(existingCustomer.get(0).getId()).userId(extractUsername()).name(request.getName()).email(request.getEmail())
                    .createdDate(LocalDateTime.now()).phone(request.getPhone()).status("ACTIVE").totalSpent(existingCustomer.get(0).getTotalSpent()).build();

            ent = shopRepo.save(customerEntity);

        } else {

            var customerEntity = CustomerEntity.builder().name(request.getName()).userId(extractUsername()).email(request.getEmail())
                    .createdDate(LocalDateTime.now()).phone(request.getPhone()).status("ACTIVE").totalSpent(0).build();

            ent = shopRepo.save(customerEntity);
        }

        if (ent.getId() != null) {

            try {
                salesCacheService.evictUserCustomers(extractUsername());
                salesCacheService.evictsUserAnalytics(extractUsername());

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return ent;
        }

        return ent;

    }

    @Cacheable(value = "customers", key = "#root.target.extractUsername()")
    public List<CustomerEntity> getAllCustomer() {
        System.out.println("The extracted username is " + extractUsername());

        return shopRepo.findAllActiveCustomer("ACTIVE",  extractUsername());
    }

    @Transactional
    public ProductSuccessDTO saveProduct(ProductRequest request) {

        String status = "In Stock";
        if (request.getStock() < 0)
            status = "Out of Stock";

        System.out.println("The new request" + request.getTax());

        ProductEntity productEntity = null;

        if (request.getSelectedProductId() != null && request.getSelectedProductId() != 0) {

            // prodRepo.addProductStock(request.getSelectedProductId(), request.getStock());

            productEntity = ProductEntity.builder().id(request.getSelectedProductId()).name(request.getName())
                    .category(request.getCategory()).status(status).userId(extractUsername()).stock(request.getStock()).active(true)
                    .taxPercent(request.getTax()).price(request.getPrice()).costPrice(request.getCostPrice())
                    .updatedDate(LocalDateTime.now())
                    .updatedBy(extractUsername())
                    .build();

        } else {

            productEntity = ProductEntity.builder().name(request.getName()).userId(extractUsername()).category(request.getCategory()).active(true)
                    .status(status).stock(request.getStock()).taxPercent(request.getTax()).costPrice(request.getCostPrice()).price(request.getPrice())
                    .createdDate(LocalDateTime.now())
                    .updatedDate(LocalDateTime.now())
                    .updatedBy(extractUsername())
                    .build();

        }

        ProductEntity ent = prodRepo.save(productEntity);
        if (ent.getId() != null) {

            try {
                salesCacheService.evictUserProducts(extractUsername());

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return ProductSuccessDTO.builder().success(true).product(request).build();


        }

        return ProductSuccessDTO.builder().success(false).product(request).build();

    }

    //@CacheEvict(value = "products", allEntries = true)
    public ProductSuccessDTO updateProduct(ProductRequest request) {

        String status = "In Stock";
        if (request.getStock() < 1)
            status = "Out of Stock";
        System.out.println("The updated request" + request.getTax());
        var productEntity = ProductEntity.builder().id(request.getSelectedProductId()).name(request.getName())
                .active(true).category(request.getCategory()).userId(extractUsername()).status(status).stock(request.getStock())
                .taxPercent(request.getTax()).price(request.getPrice()).costPrice(request.getCostPrice()).build();

        ProductEntity ent = prodRepo.save(productEntity);

        if (ent.getId() != null) {

            try {
                salesCacheService.evictUserProducts(extractUsername());

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return ProductSuccessDTO.builder().success(true).product(request).build();
        }

        return ProductSuccessDTO.builder().success(false).product(request).build();

    }


    public List<ProductEntity> getAllProducts() {

        return prodRepo.findAllActiveProducts(Boolean.TRUE,  extractUsername());
    }


    @Cacheable(value = "products", keyGenerator = "userScopedKeyGenerator")
    public Page<ProductEntity> getAllProducts(String search, int page, int limit, String sort, String dir) {
        // Create Sort object based on direction and sort field
        {
            String sortField = sort;

            // Map API field name to DB field
            if ("createdAt".equalsIgnoreCase(sortField)) {
                sortField = "created_date";
            }
            if ("tax".equalsIgnoreCase(sortField)) {
                sortField = "tax_percent";
            }
            if ("costPrice".equalsIgnoreCase(sortField)) {
                sortField = "cost_price";
            }

            Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;

            // ✅ Use mapped field name here
            Sort sortOrder = Sort.by(direction, sortField);

            Pageable pageable = PageRequest.of(page - 1, limit, sortOrder);

            String username = extractUsername();

            return prodRepo.findAllActiveProductsWithPagination(Boolean.TRUE, username, search, pageable);
        }

    }
    @Cacheable(value = "products", keyGenerator = "userScopedKeyGenerator")
    public Page<ProductEntity> getAllProductsForBilling(String search, int page, int limit, String sort, String dir) {
        // Create Sort object based on direction and sort field
        {
            String sortField = sort;

            // Map API field name to DB field
            if ("createdAt".equalsIgnoreCase(sortField)) {
                sortField = "created_date";
            }
            if ("tax".equalsIgnoreCase(sortField)) {
                sortField = "tax_percent";
            }
            if ("costPrice".equalsIgnoreCase(sortField)) {
                sortField = "cost_price";
            }

            Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;

            // ✅ Use mapped field name here
            Sort sortOrder = Sort.by(direction, sortField);

            Pageable pageable = PageRequest.of(page - 1, limit, sortOrder);

            String username = extractUsername();

            return prodRepo.findAllActiveProductsWithPaginationForBilling(Boolean.TRUE, username, search, pageable);
        }

    }

    @Cacheable(value = "customers", keyGenerator = "userScopedKeyGenerator")
    public Page<CustomerEntity> getCacheableCustomersList(String search, int page, int size)  {


        // Map API field name to DB field



        String    sortField = "created_date";


        Sort.Direction direction = "desc".equalsIgnoreCase("desc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        // ✅ Use mapped field name here
        Sort sortOrder = Sort.by(direction, sortField);

        Pageable pageable = PageRequest.of(page - 1, size, sortOrder);

        String username = extractUsername();
        Page<CustomerEntity> response=null;
        try{
            response=   shopRepo.findAllCustomersWithPagination(username, search, pageable);
        }
        catch(Exception e){
            e.printStackTrace();
        }

        return response;
    }

    @Transactional
    public BillingResponse doPayment(BillingRequest request) throws Exception {

        Integer unitsSold = 0;
        for (var obj : request.getCart()) {
            unitsSold += obj.getQuantity();
        }
        var billingEntity = BillingEntity.builder().customerId(request.getSelectedCustomer().getId())
                .unitsSold(unitsSold).taxAmount(request.getTax()).userId(extractUsername()).totalAmount(request.getTotal())
                .discountPercent(request.getDiscountPercentage()).remarks(request.getRemarks()).subTotalAmount(request.getTotal() - request.getTax()).createdDate(LocalDateTime.now()).build();

        BillingEntity billResponse = billRepo.save(billingEntity);
        final Double[] totalProfitOnCP = {0d};
        if (billResponse.getId() != null) {
            request.getCart().stream().forEach(obj -> {

                ProductEntity prodRes = prodRepo.findByIdAndUserId(obj.getId(), extractUsername());
                System.out.println("Product details " + prodRes);
                Integer tax = (prodRes.getTaxPercent() * obj.getQuantity() * obj.getPrice()) / 100;
                Double discountedTotal=0d;
                if (obj.getDiscountPercentage()!=0)
                      discountedTotal=obj.getPrice()- (obj.getDiscountPercentage()*obj.getPrice()) / 100;
                else
                    discountedTotal=(double)obj.getPrice();

                Integer total = obj.getQuantity() * (int)Math.round(discountedTotal);
                Integer subTotal = total- tax;
                Double profitOnCp= (discountedTotal - prodRes.getCostPrice())*obj.getQuantity();
                totalProfitOnCP[0] = totalProfitOnCP[0] +Math.round(profitOnCp);
                var productSalesEntity = ProductSalesEntity.builder().billingId(billResponse.getId())
                        .profitOnCP(profitOnCp)
                        .productId(obj.getId()).productDetails(obj.getDetails()).userId(extractUsername()).discountPercentage(obj.getDiscountPercentage()).quantity(obj.getQuantity()).tax(tax).subTotal(subTotal).total(total)
                        .build();

                ProductSalesEntity prodSalesResponse = prodSalesRepo.save(productSalesEntity);




                if (prodSalesResponse.getId() != null) {
                    prodRepo.updateProductStock(obj.getId(), obj.getQuantity(), extractUsername());

                }

            });

            billResponse.setTotalProfitOnCP(totalProfitOnCP[0]);
            Runnable rn = () ->
            {
                billRepo.save(billResponse);
            };
            rn.run();


            String paymentMethod = "CASH";
            if (request.getPaymentMethod() != null) {
                paymentMethod = request.getPaymentMethod();
            }

            var paymentEntity = PaymentEntity.builder().billingId(billResponse.getId()).createdDate(LocalDateTime.now())
                    .paymentMethod(paymentMethod).status("Paid").tax(request.getTax()).userId(extractUsername())
                    .subtotal(request.getTotal() - request.getTax()).total(request.getTotal()).build();

            salesPaymentRepo.save(paymentEntity);

            try {
                shopRepo.updateCustomerSpentAmount(request.getSelectedCustomer().getId(), request.getTotal(), extractUsername());
            } catch (Exception e) {
                // TODO Auto-generated catch block,
                e.printStackTrace();
            }




            InvoiceDetails order = getOrderDetails(billResponse.getInvoiceNumber());
            try {
                String htmlContent = emailTemplate.generateOrderHtml(order);

                if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
                    CompletableFuture<String> futureResult = email.sendEmail(order.getCustomerEmail(),
                            billResponse.getInvoiceNumber(), order.getCustomerName(),
                            generateInvoicePdf(billResponse.getInvoiceNumber()), htmlContent);
                    System.out.println(futureResult);
                }

            } catch (MailjetException | MailjetSocketTimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
              salesCacheService.evictUserSales(extractUsername());
                salesCacheService.evictUserProducts(extractUsername());
                salesCacheService.evictUserPayments(extractUsername());
                salesCacheService.evictUserCustomers(extractUsername());
                salesCacheService.evictUserDasbhoard(extractUsername());
                salesCacheService.evictsUserGoals(extractUsername());
                salesCacheService.evictsUserAnalytics(extractUsername());
                salesCacheService.evictsTopSelling(extractUsername());
                salesCacheService.evictsTopOrders(extractUsername());

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return BillingResponse.builder().paymentReferenceNumber(paymentEntity.getPaymentReferenceNumber())
                    .invoiceNumber(billResponse.getInvoiceNumber()).status("SUCCESS").build();
        }

        return BillingResponse.builder().status("FAILURE").build();
    }

    @Cacheable(value = "sales", keyGenerator = "userScopedKeyGenerator")
    public Page<SalesResponseDTO> getAllSales(int page, int size, String sort, String dir, String searchTerm) {
        String username = extractUsername();

        String sortField = sort;

        // Map API field name to DB field
        if ("date".equalsIgnoreCase(sortField)) {
            sortField = "created_date";
        }
        if ("id".equalsIgnoreCase(sortField)) {
            sortField = "invoice_number";
        }
        if ("totalAmount".equalsIgnoreCase(sortField)) {
            sortField = "total_amount";
        }
        if ("customer".equalsIgnoreCase(sortField)) {
            sortField = "customer_id";
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Sort sortOrder = Sort.by(direction, sortField);

        // Follow same paging convention as getAllProducts (1-based page param)
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, sortOrder);

        Page<BillingEntity> billingPage=null;

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            // Use a custom query to search by invoice number or customer name
            try {
                billingPage = billRepo.findByUserIdAndSearchNative(username, searchTerm.trim(), pageable);
            }
            catch(Exception e){
                e.printStackTrace();
            }
        } else {
            billingPage = billRepo.findAllByUserId(username, pageable);
        }

        List<SalesResponseDTO> dtoList = billingPage.getContent().stream()
                .map(obj -> {
                    String customerName = shopRepo.findByIdAndUserId(obj.getCustomerId(), username).getName();
                    String paymentStatus = salesPaymentRepo.findPaymentDetails(obj.getId(), username).getStatus();

                    return SalesResponseDTO.builder()
                            .customer(customerName)
                            .remarks(obj.getRemarks())
                            .date(obj.getCreatedDate().toString())
                            .id(obj.getInvoiceNumber())
                            .total(obj.getTotalAmount())
                            .status(paymentStatus)
                            .build();
                })
                .toList();

        return new PageImpl<>(dtoList, pageable, billingPage.getTotalElements());
    }

    @Cacheable(value = "sales", keyGenerator = "userScopedKeyGenerator")
    public List<SalesResponseDTO> getLastNSales(int count) {


        String username = extractUsername();


        List<BillingEntity> billingDetails = billRepo.findNNumberWithUserId(username, count);


        List<SalesResponseDTO> dtoList = billingDetails.stream()
                .map(obj -> {
                    String customerName = shopRepo.findByIdAndUserId(obj.getCustomerId(), username).getName();
                    String paymentStatus = salesPaymentRepo.findPaymentDetails(obj.getId(), username).getStatus();

                    return SalesResponseDTO.builder()
                            .customer(customerName)
                            .remarks(obj.getRemarks())
                            .date(obj.getCreatedDate().toString())
                            .id(obj.getInvoiceNumber())
                            .total(obj.getTotalAmount())
                            .status(paymentStatus)
                            .build();
                })
                .toList();

        return dtoList;

    }


    @Cacheable(value = "sales", keyGenerator = "userScopedKeyGenerator")
    public Page<SalesResponseDTO> getAllSalesWithPagination(Integer page, Integer size, String sort, String dir) {

        String sortField = sort;

        // Map API field name to DB field
        if ("createdAt".equalsIgnoreCase(sortField)) {
            sortField = "created_date";
        }
        if ("total".equalsIgnoreCase(sortField)) {
            sortField = "total_amount";
        }
        if ("invoiceNumber".equalsIgnoreCase(sortField) || "invoice".equalsIgnoreCase(sortField)) {
            sortField = "invoice_number";
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Sort sortOrder = Sort.by(direction, sortField);

        // Follow same paging convention as getAllProducts (1-based page param)
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, sortOrder);

        String username = extractUsername();

        Page<BillingEntity> billingPage = billRepo.findAllByUserId(username, pageable);

        List<SalesResponseDTO> dtoList = billingPage.getContent().stream()
                .map(obj -> {
                    String customerName = shopRepo.findByIdAndUserId(obj.getCustomerId(), username).getName();
                    String paymentStatus = salesPaymentRepo.findPaymentDetails(obj.getId(), username).getStatus();

                    return SalesResponseDTO.builder()
                            .customer(customerName)
                            .remarks(obj.getRemarks())
                            .date(obj.getCreatedDate().toString())
                            .id(obj.getInvoiceNumber())
                            .total(obj.getTotalAmount())
                            .status(paymentStatus)
                            .build();
                })
                .toList();

        return new PageImpl<>(dtoList, pageable, billingPage.getTotalElements());
    }

    @Cacheable(value = "dashboard", keyGenerator = "userScopedKeyGenerator")
    public DasbboardResponseDTO getDashBoardDetails(String range) {
        System.out.println("selected day range" + range);
        List<BillingEntity> billList = new ArrayList<>();
        List<ProductEntity> prodList = new ArrayList<>();
        Integer days = 0;
        if (!range.equals("today")) {
            if (range.equals("lastYear")) {
                days = 365;
            }
            if (range.equals("lastMonth")) {
                days = 30;
            }
            if (range.equals("lastWeek")) {
                days = 7;
            }
            billList = billRepo.findAllByDayRange(LocalDateTime.now().minusDays(days), extractUsername());

        } else if (range.equals("today")) {
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay(); // today 00:00
            LocalDateTime endOfDay = startOfDay.plusDays(1); // tomorrow 00:00
            billList = billRepo.findAllCreatedToday(startOfDay, endOfDay, extractUsername());
            // prodList = prodRepo.findAllCreatedToday(startOfDay, endOfDay);

        }
        prodList = prodRepo.findAllByStatus(Boolean.TRUE, extractUsername());
        Integer monthlyRevenue = 0;
        Integer taxCollected = 0;
        Integer totalUnitsSold = 0;
        Integer outOfStockCount = 0;

        for (BillingEntity obj : billList) {
            monthlyRevenue = monthlyRevenue + obj.getTotalAmount();
            taxCollected = taxCollected + obj.getTaxAmount();
            totalUnitsSold = totalUnitsSold + obj.getUnitsSold();
        }
        ;
        for (ProductEntity obj : prodList) {
            if (obj.getStock() < 1)
                outOfStockCount = outOfStockCount + 1;
        }
        ;

        return DasbboardResponseDTO.builder().monthlyRevenue(monthlyRevenue).outOfStockCount(outOfStockCount)
                .taxCollected(taxCollected).totalUnitsSold(totalUnitsSold).build();
    }

    @Cacheable(value = "payments", keyGenerator = "userScopedKeyGenerator")
    public List<PaymentDetails> getPaymentList(String fromDate, String toDate) {

        LocalDateTime startDate = LocalDate.parse(fromDate).atStartOfDay();
        LocalDateTime endDate = LocalDate.parse(toDate).atTime(LocalTime.MAX);
        List<BillingEntity> billList = billRepo.findAllWithUserId(extractUsername(), startDate, endDate);
        billList.sort(Comparator.comparing(BillingEntity::getCreatedDate).reversed());
        List<PaymentDetails> response = new ArrayList<>();
        billList.stream().forEach(obj -> {

            response.add(PaymentDetails.builder()
                    .id(salesPaymentRepo.findPaymentDetails(obj.getId(), extractUsername()).getPaymentReferenceNumber())
                    .amount(obj.getTotalAmount()).date(String.valueOf(obj.getCreatedDate()))
                    .saleId(obj.getInvoiceNumber())
                    .method(salesPaymentRepo.findPaymentDetails(obj.getId(), extractUsername()).getPaymentMethod()).build());
        });

        return response;
    }

    public ProductSuccessDTO uploadProduct(File request) {

        return null;
    }

    //@CacheEvict(value = "products", allEntries = true)
    public List<ProductRequest> uploadBulkProduct(MultipartFile file) {

        try {
            List<ProductRequest> prodList = util.parseCsv(file);
            System.out.println(prodList);
            prodList.stream().forEach(obj -> {
                ProductSuccessDTO prodsaveResponse = saveProduct(obj);
                System.out.println(prodsaveResponse);
            });

            try {
                salesCacheService.evictUserProducts(extractUsername());

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return prodList;

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }



    public InvoiceDetails getOrderDetails(String orderReferenceNumber) {

        BillingEntity billDetails = billRepo.findOrderByReference(orderReferenceNumber, extractUsername());

        PaymentEntity paymentEntity = salesPaymentRepo.findPaymentDetails(billDetails.getId(), extractUsername());

        boolean paid = false;
        if (paymentEntity.getStatus().equalsIgnoreCase("Paid")) {
            paid = true;
        }

        CustomerEntity customerEntity = shopRepo.findByIdAndUserId(billDetails.getCustomerId(), extractUsername());

        List<ProductSalesEntity> prodSales = prodSalesRepo.findByOrderId(billDetails.getId(), extractUsername());
        Double gst = 0d;
        for (ProductSalesEntity orders : prodSales) {
            gst = gst + orders.getTax();
        }

        List<OrderItem> items = prodSales.stream().map(obj -> {

            System.out.println("The productId is "+obj.getProductId());
            ProductEntity prodRes = prodRepo.findByIdAndUserId(obj.getProductId(), extractUsername());

            var orderItems = OrderItem.builder().productName(prodRes.getName()).unitPrice(obj.getTotal()).gst(obj.getTax())
                    .details(obj.getProductDetails())
                    .quantity(obj.getQuantity()).build();
            return orderItems;
        }).collect(Collectors.toList());
        var response = InvoiceDetails.builder().discountRate(0).invoiceId(orderReferenceNumber)
                .paymentReferenceNumber(paymentEntity.getPaymentReferenceNumber()).items(items).gstRate(gst)
                .customerPhone(customerEntity.getPhone()).customerEmail(customerEntity.getEmail()).orderedDate(String.valueOf(billDetails.getCreatedDate()).substring(0, 10))
                .totalAmount(billDetails.getTotalAmount()).customerName(customerEntity.getName()).paid(paid).build();
        return response;
    }



    public byte[] generateReport(ReportRequest request) {

        LocalDate fromDate = LocalDate.parse(request.getFromDate());

        // Combine with a time (e.g., start of day)
        LocalDateTime fromDateTime = fromDate.atStartOfDay();

        LocalDate toDate = LocalDate.parse(request.getToDate());

        // Combine with a time (e.g., start of day)
        LocalDateTime toDateTime = toDate.atStartOfDay();

        System.out.println(toDateTime);

        byte[] fileBytes = repogen.downloadReport(request.getReportType(), fromDateTime, toDateTime, extractUsername());

        return fileBytes;
    }

    public String saveReportDetails(Report request) {
        request.setStatus("READY");
        request.setUserId(extractUsername());
        reportDRepo.save(request);
        return "Success";
    }

    public List<ReportResponse> getReportsList(Integer limit) {
        List<Report> reportList = reportDRepo.findByLimit(limit, extractUsername());

        return reportList.stream().map(obj -> {

            return ReportResponse.builder().name(obj.getName()).createdAt(obj.getCreatedAt())
                    .fileName(obj.getFileName()).fromDate(obj.getFromDate()).toDate(obj.getToDate()).id(obj.getId())
                    .status(obj.getStatus()).build();

        }).collect(Collectors.toList());

    }

    public String updatePassword(UserInfo userInfo) {

        UserInfo userRes = userinfoRepo.findByUsername(userInfo.getUsername()).get();
        userRes.setPassword(passwordEncoder.encode(userInfo.getPassword()));
        userinfoRepo.save(userRes);

        return "success";
    }

    public UpdateUserDTO saveEditableUser(UpdateUserDTO request, String username) throws IOException {

        System.out.println("entered saveEditableUser with request " + request + " and username " + username);
        request.setUsername(username);
        UserInfo userinfo = userinfoRepo.findByUsername(username).get();

        userinfo.setName(request.getName());
        userinfo.setPhoneNumber(request.getPhone());
        userinfo.setEmail(request.getEmail());
        userinfoRepo.save(userinfo);

        ShopDetailsEntity shopDetails = shopDetailsRepo.findbyUsername(request.getUsername());
        if (shopDetails != null) {
            shopDetails.setAddresss(request.getAddress());
            shopDetails.setOwnerName(request.getShopOwner());
            shopDetails.setGstNumber(request.getGstNumber());
            shopDetails.setName(request.getName());
            shopDetails.setShopEmail(request.getShopEmail());
            shopDetails.setShopPhone(request.getShopPhone());
            shopDetails.setShopName(request.getShopName());
            shopDetailsRepo.save(shopDetails);
        } else {
            ShopDetailsEntity shopDetailsNew = new ShopDetailsEntity();
            shopDetailsNew.setUsername(request.getUsername());
            shopDetailsNew.setAddresss(request.getAddress());
            shopDetailsNew.setOwnerName(request.getShopOwner());
            shopDetailsNew.setName(request.getName());
            shopDetailsNew.setGstNumber(request.getGstNumber());
            shopDetailsNew.setShopEmail(request.getShopEmail());
            shopDetailsNew.setShopPhone(request.getShopPhone());
            shopDetailsNew.setShopName(request.getShopName());
            shopDetailsRepo.save(shopDetailsNew);
        }

        return request;
    }

    public String saveEditableUserProfilePic(MultipartFile profilePic, String username) throws IOException {
        System.out.println("entered saveEditableUserProfilePic with  username " + username);

        String keyName = profilePic.getOriginalFilename();

        s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(keyName)
                        .contentType(profilePic.getContentType()).build(),
                software.amazon.awssdk.core.sync.RequestBody.fromBytes(profilePic.getBytes()));

        // UserInfo userinfo = userinfoRepo.findById(Integer.parseInt(id)).get();
        UserProfilePicEntity picRes = userProfilePicRepo.findByUsername(username);
        if (picRes != null) {
            picRes.setProfilePic(keyName);
            picRes.setUpdated_date(LocalDateTime.now());
            userProfilePicRepo.save(picRes);
        } else {
            UserProfilePicEntity picResNew = new UserProfilePicEntity();
            picResNew.setUpdated_date(LocalDateTime.now());
            picResNew.setUsername(username);
            picResNew.setProfilePic(keyName);
            userProfilePicRepo.save(picResNew);
        }

        return "ok";
    }

    @Transactional
    public void deleteCustomer(Integer id) {
        shopRepo.updateStatus(id, "IN-ACTIVE", extractUsername());
        try {
            salesCacheService.evictUserCustomers(extractUsername());
            salesCacheService.evictsUserAnalytics(extractUsername());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public byte[] generateInvoicePdf(String orderId) throws Exception {
        System.out.println(orderId);
        InvoiceDetails order = getOrderDetails(orderId);
        LocalDate orderedDate = LocalDate.parse(order.getOrderedDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        UpdateUserDTO userProfile= getUserProfile(extractUsername());
        String shopEmail="";
        String gstNumber="";
        String shopAddress="";
        String shopPhone="";
        String shopName="";
        if(userProfile!=null){
             gstNumber = userProfile.getGstNumber() != null ? userProfile.getGstNumber() : "sample gst number";
             shopEmail=userProfile.getShopEmail()!=null?userProfile.getShopEmail() : "sample shop email";
             shopPhone= userProfile.getShopPhone() != null?userProfile.getShopPhone() : "sample shop phone";
            shopAddress= userProfile.getShopLocation()!=null?userProfile.getShopLocation() : "sample shop address";
            shopName=userProfile.getShopName()!=null?userProfile.getShopName() : "sample shop name";
        }

// Format to new pattern
        String formattedDate = orderedDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        byte[] response = pdfutil.generateInvoice(order.getCustomerName(), order.getCustomerEmail(),
                order.getCustomerPhone(), order.getInvoiceId(), order.getItems(), formattedDate, order.getTotalAmount(), order.isPaid(), order.getGstRate(), shopName ,shopAddress, shopEmail, shopPhone,gstNumber );

        return response;
    }

    public UpdateUserDTO getUserProfile(String username) {

        System.out.println("entered getUserProfile with request  username " + username);

        UserInfo userinfo = userinfoRepo.findByUsername(username).get();

        ShopDetailsEntity shopDetails = shopDetailsRepo.findbyUsername(username);

        if(shopDetails!=null) {

          var   response = UpdateUserDTO.builder().address(shopDetails.getAddresss()).email(userinfo.getEmail())
                    .gstNumber(shopDetails.getGstNumber()).name(userinfo.getName()).phone(userinfo.getPhoneNumber())
                  .shopEmail(shopDetails.getShopEmail())
                  .shopPhone(shopDetails.getShopPhone())
                  .shopName(shopDetails.getShopName())
                    .shopLocation(shopDetails.getAddresss()).shopOwner(shopDetails.getOwnerName()).username(username)
                  .userSource(userinfo.getSource())
                    .build();
          return response;
        }
        else{
        var     response = UpdateUserDTO.builder().address("").email(userinfo.getEmail())
                    .gstNumber("").name(userinfo.getName()).phone(userinfo.getPhoneNumber())
                    .shopLocation("").shopOwner("").username(username)
                    .userSource(userinfo.getSource())
                    .build();
        return response;
        }



    }

    public byte[] getProfilePic(String username) throws IOException {

        System.out.println("entered getProfilePic with request  username " + username);

        UserInfo res = userinfoRepo.findByUsername(username).get();


        byte[] content = null;
        if (!res.getSource().equals("google")) {
            try {
                UserProfilePicEntity picRes = userProfilePicRepo.findByUsername(username);

                GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(picRes.getProfilePic())
                        .build();
                ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
                content = s3Object.readAllBytes();
            } catch (IOException e) {
                e.printStackTrace();
                content = null; // Or handle error appropriately
            }
        } else {
            if (res.getProfilePiclink() != null) {
                    String imageUrl = res.getProfilePiclink(); // Replace with your actual URL
                try {
                    URL url = new URL(imageUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    try {
                        InputStream inputStream = connection.getInputStream();
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                        byte[] data = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                            buffer.write(data, 0, bytesRead);
                        }
                        content = buffer.toByteArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                        content = null; // Or handle error appropriately
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    content = null; // Or handle error appropriately
                }
            }
        }

        return content;
    }

    @Transactional
    public void deleteProduct(Integer id) {
        System.out.println("endtered deleteProduct with productId " + id);

        prodRepo.deActivateProduct(id, Boolean.FALSE, extractUsername());

        try {
            salesCacheService.evictUserProducts(extractUsername());

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    @Cacheable(value = "analytics", keyGenerator = "userScopedKeyGenerator")
    public AnalyticsResponse getAnalytics(AnalyticsRequest request) {

        AnalyticsResponse response = new AnalyticsResponse();
        String userId=extractUsername();

        List<String> labels = new ArrayList<>();
        List<Long> sales = new ArrayList<>();
        List<Long> stocks = new ArrayList<>();
        List<Integer> taxes = new ArrayList<>();
        List<Integer> customers = new ArrayList<>();
        List<Integer> onlinePaymentCounts = new ArrayList<>();
        List<Long> profits = new ArrayList<>();
        // Parse to LocalDate
        LocalDateTime startDate = LocalDate.parse(request.getStartDate()).atStartOfDay();
        LocalDateTime endDate = LocalDate.parse(request.getEndDate()).atTime(LocalTime.MAX);

        List<Object[]> resultsSales = billRepo.getMonthlySalesSummary(startDate, endDate, userId);

        for (Object[] row : resultsSales) {
            String month = (String) row[0];
            labels.add(month);
            Long count = ((Number) row[1]).longValue();
            sales.add(count);
        }

        try {
            List<Object[]> resultsStocks = billRepo.getMonthlyStocksSold(startDate, endDate, userId);
            for (Object[] row : resultsStocks) {

                Long count = ((Number) row[1]).longValue();
                stocks.add(count);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        List<Object[]> resultsTaxes = billRepo.getMonthlyTaxesSummary(startDate, endDate, userId);
        for (Object[] row : resultsTaxes) {
            Integer count = ((Number) row[1]).intValue();
            taxes.add(count);
        }
        try{
        List<Object[]> resultsCustomers = shopRepo.getMonthlyCustomerCount(startDate, endDate, userId);
        for (Object[] row : resultsCustomers) {
            Integer count = ((Number) row[1]).intValue();
            customers.add(count);
        }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        List<Object[]> resultsOnlinePaymentCount = salesPaymentRepo.getMonthlyPaymentCounts(startDate, endDate, userId);

        for (Object[] row : resultsOnlinePaymentCount) {
            Integer count = ((Number) row[1]).intValue();
            onlinePaymentCounts.add(count);
        }


        for (Object[] row : resultsSales) {
            double percentage = 0.08 + (0.20 - 0.08) * random.nextDouble();
            System.out.println("The profits on cp are "+((Number) row[2]).longValue());
            Long count = ((Number) row[1]).longValue();
            Long estimatedProfit = ((Number) row[1]).longValue();
            profits.add(((Number) row[2]).longValue());
        }
        response.setCustomers(customers);
        response.setLabels(labels);
        response.setProfits(profits);
        response.setSales(sales);
        response.setStocks(stocks);
        response.setTaxes(taxes);
        response.setOnlinePayments(onlinePaymentCounts);

        return response;
    }



    public Map<String, String> getUserProfileDetails() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("Current user: " + username);
        Map<String, String> response = new HashMap<>();
        response.put("username", username);
        return response;
    }


    @Cacheable(value = "notifications", keyGenerator = "userScopedKeyGenerator")
    public NotificationDTO getAllNotifications(int page, int limit, String sort, String domain, String seen, String s) {


        NotificationDTO response = new NotificationDTO();
        List<ShopNotifications> notifications = new ArrayList<>();


        String sortField = "updated_date";

        // Map API field name to DB field
        if ("createdAt".equalsIgnoreCase(sortField)) {
            sortField = "created_date";
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;

        // ✅ Use mapped field name here
        Sort sortOrder = Sort.by(direction, sortField);

        Pageable pageable = PageRequest.of(page - 1, limit, sortOrder);

        String username = extractUsername();
        Page<MessageEntity> notificationsList=null;

        if(seen!=null && !seen.isEmpty() && !seen.equals("all")) {

            Boolean isRead=false;
            if(seen.equals("seen"))
                isRead=true;
            else
                isRead=false;

            if(seen.equals("flagged")) {
               Boolean isFlagged=true;
                notificationsList = notiRepo.findAllNotificationsByFlaggedStatus(extractUsername(), domain, isFlagged, Boolean.FALSE, pageable);
            }
            else
             notificationsList = notiRepo.findAllNotificationsByReadStatus(extractUsername(), domain, isRead, Boolean.FALSE,  pageable);

        }
        else
             notificationsList = notiRepo.findAllNotifications(extractUsername(), domain, Boolean.FALSE, pageable);

        for (MessageEntity obj : notificationsList) {
            notifications.add(ShopNotifications.builder().createdAt(obj.getCreatedDate()).title(obj.getTitle()).id(String.valueOf(obj.getId())).subject(obj.getSubject()).message(obj.getDetails()).seen(obj.getIsRead()).domain(obj.getDomain()).searchKey(obj.getSearchKey()).isFlagged(obj.getIsFlagged()).build());
        }
        return NotificationDTO.builder().count(notifications.size()).notifications(notifications).build();

    }

    @Transactional
    public void updateNotificationStatus(NotificationStatusUpdateRequest request) {

        request.getNotificationIds().stream().forEach(notificationId -> {

            notiRepo.updateNotificationStatus(notificationId, extractUsername(), Boolean.TRUE);

        });
    }

    @Transactional
    public Map<String, Object> flagNotifications(Integer notificationId, Boolean flag) {

        notiRepo.updateNotificationFlaggedStatus(notificationId, extractUsername(), flag);

        Map<String, Object> response = new HashMap<>();
        response.put("id", notificationId);
        response.put("flagged", Boolean.TRUE);
        return response;
    }
    @Transactional
    public Map<String, Object> deleteNotifications(Integer notificationId) {

        notiRepo.updateNotificationDeleteStatus(notificationId, extractUsername(), Boolean.TRUE);

        Map<String, Object> response = new HashMap<>();
        response.put("id", notificationId);
        response.put("deleted", Boolean.TRUE);
        return response;
    }

    public Map<String, Boolean> getAvailablePaymentMethods() {
       UserPaymentModes paymentModes= paymentModesRepo.getUserPaymentModes(extractUsername());
        Map<String, Boolean> response= new HashMap<>();
        System.out.println("The paymentModes are "+paymentModes);
       if(paymentModes!=null){
           if(paymentModes.getCard())
               response.put("card", true);
           else
               response.put("card", false);

           if(paymentModes.getCash())
               response.put("cash", true);
           else
               response.put("cash", false);

           if(paymentModes.getUpi())
               response.put("upi", true);
           else
               response.put("upi", false);
       }
       System.out.println("the getAvailablePaymentMethods response is "+response);
        return response;
    }

    @Transactional
    public void updatePaymentReferenceNumber(String paymentRef, String orderRef){

        salesPaymentRepo.updatePaymentReferenceNumber(paymentRef, orderRef, extractUsername());

    }

    @Cacheable(value = "dashboard", keyGenerator = "userScopedKeyGenerator")
    public List<WeeklySales> getWeeklyAnalytics(String range) {


        String userId=extractUsername();

        List<WeeklySales> response= new ArrayList<>();

        List<String> labels = new ArrayList<>();
        List<Long> sales = new ArrayList<>();
        List<Long> stocks = new ArrayList<>();
        List<Integer> taxes = new ArrayList<>();
        List<Integer> customers = new ArrayList<>();
        List<Integer> onlinePaymentCounts = new ArrayList<>();
        List<Long> profits = new ArrayList<>();
        // Parse to LocalDate
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        List<Object[]> resultsSales = new ArrayList<>();
        LocalDateTime endDate = LocalDateTime.now();


        try {
            if(range.equals("today")){
                resultsSales = billRepo.getSalesAndStocksToday(endDate, userId);
            }


            if(range.equals("lastWeek")){
                resultsSales = billRepo.getWeeklySalesAndStocks(endDate, userId);
            }
            if(range.equals("lastMonth")){

               resultsSales = billRepo.getSalesAndStocksMonthly(endDate, userId);
           }
            if(range.equals("lastYear")){
               startDate = LocalDateTime.now().minusDays(365);
               resultsSales = billRepo.getSalesAndStocksYearly(endDate, userId);
           }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        for (Object[] row : resultsSales) {
            WeeklySales weeklysales = new WeeklySales();
            String day = (String) row[0];
            labels.add(day);
            Long count = ((Number) row[1]).longValue();
            Integer stocksCount = ((Number) row[3]).intValue();
            sales.add(count);
            weeklysales.setDay(day);
            weeklysales.setUnitsSold(stocksCount);
            weeklysales.setTotalSales(count);
            response.add(weeklysales);
        }




        return response;
    }
    @Cacheable(value = "dashboard", keyGenerator = "userScopedKeyGenerator")
    public List<SalesResponseDTO> getTopNSales(int count, String range) {

        {


            String username = extractUsername();


            //List<BillingEntity> billingDetails = billRepo.findNNumberWithUserId(username, count);
            List<BillingEntity> billingDetails=new ArrayList<>();

            if(range.equals("today")) {
                billingDetails = billRepo.findTopNSalesForToday(username, count);
            }
            if(range.equals("lastWeek")) {
                billingDetails = billRepo.findTopNSalesForLastWeek(username, count);
            }
            if(range.equals("lastMonth")) {
                billingDetails = billRepo.findTopNSalesForLastMonth(username, count);
            }
            if(range.equals("lastYear")) {
                billingDetails = billRepo.findTopNSalesForLastYear(username, count);
            }

            List<SalesResponseDTO> dtoList = billingDetails.stream()
                    .map(obj -> {
                        String customerName = shopRepo.findByIdAndUserId(obj.getCustomerId(), username).getName();
                        String paymentStatus = salesPaymentRepo.findPaymentDetails(obj.getId(), username).getStatus();

                        return SalesResponseDTO.builder()
                                .customer(customerName)
                                .remarks(obj.getRemarks())
                                .date(obj.getCreatedDate().toString())
                                .id(obj.getInvoiceNumber())
                                .total(obj.getTotalAmount())
                                .status(paymentStatus)
                                .build();
                    })
                    .toList();

            return dtoList;

        }


    }

    public String updateEstimatedGoals(GoalRequest request) {

        EstimatedGoalsEntity existingGoals = estimatedGoalsRepo.findByUserId(extractUsername());
        if (existingGoals != null) {
            existingGoals.setId(existingGoals.getId());
            existingGoals.setUserId(extractUsername());
            existingGoals.setSales(request.getEstimatedSales());
            existingGoals.setFromDate(request.getFromDate().atStartOfDay());
            existingGoals.setToDate(request.getToDate().atTime(LocalTime.MAX));
            existingGoals.setUpdatedBy(extractUsername());
            existingGoals.setUpdatedDate(LocalDateTime.now());
            estimatedGoalsRepo.save(existingGoals);
            salesCacheService.evictsUserGoals(extractUsername());
        } else {
            EstimatedGoalsEntity newGoals= EstimatedGoalsEntity.builder()
                    .sales(request.getEstimatedSales())
                    .userId(extractUsername())
                    .fromDate(request.getFromDate().atStartOfDay())
                    .toDate(request.getToDate().atTime(LocalTime.MAX))
                    .createdBy(extractUsername())
                    .createdDate(LocalDateTime.now())
                    .updatedDate(LocalDateTime.now())
                    .updatedBy(extractUsername())
                    .build();
            estimatedGoalsRepo.save(newGoals);
            salesCacheService.evictsUserGoals(extractUsername());
        }

        return "Success";
    }



    @Cacheable(value = "goals", keyGenerator = "userScopedKeyGenerator")
    public GoalData getTimeRangeGoalData(String range) {

        EstimatedGoalsEntity existingGoals = estimatedGoalsRepo.findByUserId(extractUsername());
        System.out.println("The existing goals are "+existingGoals);
        String username = extractUsername();
        List<BillingEntity> billingDetails = new ArrayList<>();
        if (range.equals("today")) {
            billingDetails = billRepo.findSalesNDays(username, LocalDateTime.now().minusDays(1), LocalDateTime.now());
        }
        if (range.equals("lastWeek")) {
            billingDetails = billRepo.findSalesNDays(username, LocalDateTime.now().minusDays(8), LocalDateTime.now());
        }
        if (range.equals("lastMonth")) {
            billingDetails = billRepo.findSalesNDays(username, LocalDateTime.now().minusDays(31), LocalDateTime.now());
        }
        if (range.equals("lastYear")) {
            billingDetails = billRepo.findSalesNDays(username, LocalDateTime.now().minusDays(366), LocalDateTime.now());
        }
        final Double[] actualSalesList = {0d};
        billingDetails.stream().forEach(obj -> {

            actualSalesList[0] = actualSalesList[0] + obj.getTotalAmount().doubleValue();

        });

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");

        String fromDateStr = existingGoals != null && existingGoals.getFromDate() != null
                ? existingGoals.getFromDate().format(formatter)
                : null;

        String toDateStr = existingGoals != null && existingGoals.getToDate() != null
                ? existingGoals.getToDate().format(formatter)
                : null;

        var response = GoalData.builder()
                .actualSales(actualSalesList[0])
                .estimatedSales(existingGoals != null ? existingGoals.getSales() : 0d)
                .fromDate(fromDateStr)
                .toDate(toDateStr)
                .build();
        System.out.println("response for the goals-->"+response);

        return response;
    }

    @Cacheable(value = "topSellings", keyGenerator = "userScopedKeyGenerator")
    public List<TopProductDto> getTopProducts(int count, String timeRange, String factor) {
      LocalDateTime  endDate=LocalDateTime.now();
      LocalDateTime startDate=LocalDateTime.now();

        List<ProductPerformanceProjection> topProducts = new ArrayList<>();
        List<TopProductDto> response=new ArrayList<>();
        if(timeRange.equals("lastWeek")){
            startDate=LocalDateTime.now().minusDays(7);
        }
        if(timeRange.equals("lastMonth")){
            startDate=LocalDateTime.now().minusMonths(1);
        }
        if(timeRange.equals("lastYear")){
            startDate=LocalDateTime.now().minusYears(1);
        }
        if(timeRange.equals("today")){
            startDate=LocalDateTime.now();
        }


        if(factor.equals("mostSelling")) {
            topProducts=   prodSalesRepo.findMostSellingProducts(extractUsername(), startDate, endDate, count);
        }
        if(factor.equals("topGrossing")) {
            topProducts=  prodSalesRepo.findTopGrossingProducts(extractUsername(), startDate, endDate, count);
        }

        if(topProducts.size()>0) {
            response= topProducts.stream().map(obj -> {
                return TopProductDto.builder().category(obj.getCategory()).currentStock(obj.getCurrentStock())
                        .productName(obj.getProductName()).amount(obj.getRevenue()).count(obj.getUnitsSold()).build();
            }).collect(Collectors.toList());
        }


        System.out.println("The top products are "+response);



        return response;
    }

    @Cacheable(value = "topOrders", keyGenerator = "userScopedKeyGenerator")
    public List<TopOrdersDto> getTopOrders(int count, String timeRange) {
        LocalDateTime  endDate=LocalDateTime.now();
        LocalDateTime startDate=LocalDateTime.now();

        List<ProductPerformanceProjection> topProducts = new ArrayList<>();
        List<TopOrdersDto> response=new ArrayList<>();
        if(timeRange.equals("lastWeek")){
            startDate=LocalDateTime.now().minusDays(7);
        }
        if(timeRange.equals("lastMonth")){
            startDate=LocalDateTime.now().minusMonths(1);
        }
        if(timeRange.equals("lastYear")){
            startDate=LocalDateTime.now().minusYears(1);
        }
        if(timeRange.equals("today")){
            startDate=LocalDateTime.now();
        }

        List<BillingEntity> billList= billRepo.findTopNSalesForGivenRange(extractUsername(), startDate, endDate, count);

        if(billList.size()>0) {
            response= billList.stream().map(obj -> {
                String customerName = shopRepo.findByIdAndUserId(obj.getCustomerId(), extractUsername()).getName();
                String paymentStatus = salesPaymentRepo.findPaymentDetails(obj.getId(), extractUsername()).getStatus();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
              String date=  obj.getCreatedDate().format(formatter);

                return TopOrdersDto.builder()
                        .customer(customerName)
                        .orderId(obj.getInvoiceNumber())
                        .total(obj.getTotalAmount())
                        .date(date)
                        .build();
            }).collect(Collectors.toList());
        }


        return response;
    }

    @Cacheable(value = "paymentBreakdowns", keyGenerator = "userScopedKeyGenerator")
    public Map<String, Double> getPaymentBreakdown(String timeRange) {

        LocalDateTime  endDate=LocalDateTime.now();
        LocalDateTime startDate=LocalDateTime.now();

        List<ProductPerformanceProjection> topProducts = new ArrayList<>();
        List<TopOrdersDto> response=new ArrayList<>();
        if(timeRange.equals("lastWeek")){
            startDate=LocalDateTime.now().minusDays(7);
        }
        if(timeRange.equals("lastMonth")){
            startDate=LocalDateTime.now().minusMonths(1);
        }
        if(timeRange.equals("lastYear")){
            startDate=LocalDateTime.now().minusYears(1);
        }
        if(timeRange.equals("today")){
            startDate=LocalDateTime.now();
        }
        List<Map<String, Object>> rawData= salesPaymentRepo.getPaymentBreakdown(extractUsername(), startDate, endDate);

        Map<String, Double> result = new HashMap<>();
        for (Map<String, Object> row : rawData) {
            String method = (String) row.get("paymentMethod");
            Number count = (Number) row.get("count");
            result.put(method.toLowerCase(), count.doubleValue());
        }

        return result;
    }
}
