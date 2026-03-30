package com.management.shop.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.management.shop.dto.*;
import com.management.shop.entity.*;
import com.management.shop.enums.CustomerEnum;
import com.management.shop.repository.*;
import com.management.shop.scheduler.BillingProcess;
import com.management.shop.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

//OpenPDF (com.lowagie.*)
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
    private BillingGstRepository billGstRepo;

    @Autowired
    private UserSettingsRepository userSettingsRepo;

    @Autowired
    private ReminderCounterRepo reminderCounterRepo;

    @Autowired
    CSVUpload util;

    @Autowired
    ReportsGenerate repogen;

    @Autowired
    PDFInvoiceUtil pdfutil;

    @Autowired
    PDFGSTInvoiceUtil pdfgstutil;

    @Autowired
    EmailSender email;

 /*   @Autowired
    private S3Client s3Client;*/

    @Autowired
    private OTPSender otpSender;

    @Autowired
    OrderEmailTemplate emailTemplate;

    @Autowired
    SalesCacheService salesCacheService;

    @Autowired
    private NotificationsRepo notiRepo;

    @Autowired
    BillingProcess billingProcess;

    @Autowired
    SelectedInvoiceRepository invoiceRepo;

    @Autowired
    Utility utils;

    @Autowired
    GlobalSearchIndexRepository globalSearchRepo;

    @Autowired
    SQSUtil sqsUtil;

    @Autowired
    CSVUtil csvutil;

    @Autowired
    EmailRecordRepo emailRecordRepo;

    @Autowired
    GeminiApiCalls geminiCalls;

    @Autowired
    ProductCategoryRepository prodCatRepo;

    @Autowired
    ApiSaveRepository apiSaveRepo;

    @Autowired
    SettingsService setServ;

    private final Random random = new Random();


    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private final String UPLOAD_DIR = "/home/ubuntu/clearbills/uploads/profiles/";
    private final String LOGO_UPLOAD_DIR = "/home/ubuntu/clearbills/uploads/logos/";

    public String extractUsername(String orderReferenceNumber) {
        String username = "";
        try {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            BillingEntity billDetails = billRepo.findOrderByJustReference(orderReferenceNumber);
            username = billDetails.getUserId();
        }
        // For testing purposes, you might uncomment the line below
        // username="junaid1";
        return username;
    }

    public String extractRole() {
        String userrole = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().toList().get(0).getAuthority();

        log.info("Current user: " + userrole);
        //  username="junaid1";
        return userrole;
    }


    public String extractUsername() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SecurityContextHolder.getContext().getAuthentication().getAuthorities().forEach(auth -> {
            log.info("Authority: " + auth.getAuthority());
        });
        return username;
    }

    public List<String> extractRoles() {
        List<String> roles = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        log.info("Current user roles: " + roles);
        return roles;
    }

    private static <T, R> R getIfNotNull(T source, Function<T, R> getter) {
        return (source != null) ? getter.apply(source) : null;
    }

    public boolean checkUserStatus(String username) {
        // TODO Auto-generated method stub
        return userinfoRepo.findByUsername(username).get().getIsActive();
    }


    public CustomerSuccessDTO saveCustomer(CustomerRequest request) {
        log.info("entered into saveCustomer with" + request.toString());
        List<CustomerEntity> existingCustomer = new ArrayList<>();

        if (!request.getPhone().equals("") && !(request.getPhone() == null) && !request.getPhone().equals("0000000000")) {
            existingCustomer = shopRepo.findByPhone(request.getPhone(), "ACTIVE", extractUsername());
        }
        CustomerEntity ent = null;

        if (existingCustomer.size() > 0) {

            var customerEntity = CustomerEntity.builder().userId(extractUsername()).id(existingCustomer.get(0).getId()).name(request.getName()).email(request.getEmail())
                    .createdDate(LocalDateTime.now())
                    .gstNumber(request.getGstNumber())
                    .isActive(Boolean.TRUE)
                    .state(request.getCustomerState())
                    .city(request.getCity())
                    .phone(request.getPhone()).status("ACTIVE").totalSpent(existingCustomer.get(0).getTotalSpent()).build();

            ent = shopRepo.save(customerEntity);

        } else {

            var customerEntity = CustomerEntity.builder().userId(extractUsername()).name(request.getName()).email(request.getEmail())
                    .createdDate(LocalDateTime.now())
                    .state(request.getCustomerState())
                    .gstNumber(request.getGstNumber())
                    .city(request.getCity())
                    .isActive(Boolean.TRUE)
                    .phone(request.getPhone()).status("ACTIVE").totalSpent(0d).build();

            ent = shopRepo.save(customerEntity);
        }

        salesCacheService.evictUserCustomers(extractUsername());
        salesCacheService.evictsUserAnalytics(extractUsername());
        if (ent.getId() != null) {
            try {
                salesCacheService.evictUserCustomers(extractUsername());
                salesCacheService.evictsUserAnalytics(extractUsername());
                salesCacheService.evictsReportsCache(extractUsername());

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
        log.info("entered into saveCustomer with" + request.toString());
        List<CustomerEntity> existingCustomer = new ArrayList<>();

        if (!request.getPhone().equals("") && !(request.getPhone() == null) && !request.getPhone().equals("0000000000")) {
            existingCustomer = shopRepo.findByPhone(request.getPhone(), "ACTIVE", extractUsername());
        }
        CustomerEntity ent = null;

        if (existingCustomer.size() > 0) {

            var customerEntity = CustomerEntity.builder().id(existingCustomer.get(0).getId()).userId(extractUsername()).name(request.getName()).email(request.getEmail())
                    .state(request.getCustomerState())
                    .gstNumber(request.getGstNumber())
                    .city(request.getCity())
                    .createdDate(LocalDateTime.now()).phone(request.getPhone()).status("ACTIVE").isActive(Boolean.TRUE).totalSpent(existingCustomer.get(0).getTotalSpent()).build();

            ent = shopRepo.save(customerEntity);

        } else {

            var customerEntity = CustomerEntity.builder().name(request.getName()).userId(extractUsername()).email(request.getEmail())
                    .state(request.getCustomerState())
                    .gstNumber(request.getGstNumber())
                    .city(request.getCity())
                    .createdDate(LocalDateTime.now()).phone(request.getPhone()).status("ACTIVE").isActive(Boolean.TRUE).totalSpent(0d).build();

            ent = shopRepo.save(customerEntity);
        }

        if (ent.getId() != null) {

            try {
                salesCacheService.evictUserCustomers(extractUsername());
                salesCacheService.evictsUserAnalytics(extractUsername());
                salesCacheService.evictsReportsCache(extractUsername());
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
        log.info("The extracted username is " + extractUsername());

        return shopRepo.findAllActiveCustomer("ACTIVE", extractUsername());
    }

    @Transactional
    public ProductSuccessDTO saveProduct(ProductRequest request) {

        String status = "In Stock";
        if (request.getStock() < 0)
            status = "Out of Stock";

        log.info("The new request" + request.getTax());

        ProductEntity productEntity = null;

        if (request.getSelectedProductId() != null && request.getSelectedProductId() != 0) {
// prodRepo.addProductStock(request.getSelectedProductId(), request.getStock());

            productEntity = ProductEntity.builder()
                    .id(request.getSelectedProductId())
                    .name(request.getName() == null ? "" : request.getName())
                    .category(request.getCategory() == null ? "" : request.getCategory())
                    .status(status)
                    .userId(extractUsername())
                    .stock(request.getStock())
                    .active(true)
                    .taxPercent(request.getTax())
                    .price(request.getPrice())
                    .costPrice(request.getCostPrice())
                    .hsn(request.getHsn() == null ? "" : request.getHsn())
                    .updatedDate(LocalDateTime.now())
                    .updatedBy(extractUsername())
                    .build();

        } else {
            productEntity = ProductEntity.builder()
                    .name(request.getName() == null ? "" : request.getName())
                    .userId(extractUsername())
                    .category(request.getCategory() == null ? "" : request.getCategory())
                    .active(true)
                    .status(status)
                    .stock(request.getStock())
                    .taxPercent(request.getTax())
                    .costPrice(request.getCostPrice())
                    .price(request.getPrice())
                    .hsn(request.getHsn() == null ? "" : request.getHsn())
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

    @Transactional
    public ProductSuccessDTO saveProductFromImage(ProductRequest request) {

        String status = "In Stock";
        if (request.getStock() < 0)
            status = "Out of Stock";

        log.info("The new request" + request.getTax());

        ProductEntity productEntity = null;

        String productName = request.getName().toLowerCase().replaceAll("//s", "");

        ProductEntity prodEntity = prodRepo.findByNameAndUserId(productName, extractUsername());
        Integer updatedStock = request.getStock();
        Integer updatedPrice = request.getPrice();
        if (prodEntity != null) {
            request.setSelectedProductId(prodEntity.getId());
            updatedStock = updatedStock + prodEntity.getStock();

            if (updatedPrice == 0) {
                request.setPrice(prodEntity.getPrice());
            }
        }


        if (request.getSelectedProductId() != null && request.getSelectedProductId() != 0) {
// prodRepo.addProductStock(request.getSelectedProductId(), request.getStock());

            productEntity = ProductEntity.builder()
                    .id(request.getSelectedProductId())
                    .name(request.getName() == null ? "" : request.getName())
                    .category(request.getCategory() == null ? "" : request.getCategory())
                    .status(status)
                    .userId(extractUsername())
                    .stock(updatedStock)
                    .active(true)
                    .taxPercent(request.getTax())
                    .price(request.getPrice())
                    .costPrice(request.getCostPrice())
                    .hsn(request.getHsn() == null ? "" : request.getHsn())
                    .updatedDate(LocalDateTime.now())
                    .updatedBy(extractUsername())
                    .build();

        } else {
            productEntity = ProductEntity.builder()
                    .name(request.getName() == null ? "" : request.getName())
                    .userId(extractUsername())
                    .category(request.getCategory() == null ? "" : request.getCategory())
                    .active(true)
                    .status(status)
                    .stock(request.getStock())
                    .taxPercent(request.getTax())
                    .costPrice(request.getCostPrice())
                    .price(request.getPrice())
                    .hsn(request.getHsn() == null ? "" : request.getHsn())
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
        log.info("The updated request" + request.getTax());
        var productEntity = ProductEntity.builder().id(request.getSelectedProductId()).name(request.getName())
                .active(true).category(request.getCategory()).userId(extractUsername()).status(status).stock(request.getStock())
                .updatedDate(LocalDateTime.now())
                .updatedBy(extractUsername())
                .hsn(request.getHsn()).taxPercent(request.getTax()).price(request.getPrice()).costPrice(request.getCostPrice()).build();

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

        return prodRepo.findAllActiveProducts(Boolean.TRUE, extractUsername());
    }

    public byte[] exportAllProductAsCSV() {

        List<ProductEntity> productList = prodRepo.findAllActiveProducts(Boolean.TRUE, extractUsername());

        try {
            return csvutil.exportAllProductAsCSV(productList);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        return null;
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
            if ("tax".equalsIgnoreCase(sortField)) {
                sortField = "taxPercent";
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
    public Page<CustomerEntity> getCacheableCustomersList(String search, int page, int size, String sort, String dir) {

        String sortField = sort;

        if ("createdAt".equalsIgnoreCase(sortField)) {
            sortField = "created_date";
        }
        if ("createdDate".equalsIgnoreCase(sortField)) {
            sortField = "created_date";
        }
        if ("totalSpent".equalsIgnoreCase(sortField)) {
            sortField = "total_spent";
        }
        if ("totalOrders".equalsIgnoreCase(sortField)) {
            sortField = "total_orders";
        }


        Sort.Direction direction = dir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;

        // ✅ Use mapped field name here
        Sort sortOrder = Sort.by(direction, sortField);

        Pageable pageable = PageRequest.of(page - 1, size, sortOrder);

        String username = extractUsername();
        Page<CustomerEntity> response = null;
        try {
            response = shopRepo.findAllCustomersWithPagination(username, search, pageable, Boolean.TRUE);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    public Page<CustomerEntity> getBillingCustomersList(String search, int page, int size, String sort) {

        String sortField = sort;

        if ("createdAt".equalsIgnoreCase(sortField)) {
            sortField = "created_date";
        }
        if ("createdDate".equalsIgnoreCase(sortField)) {
            sortField = "created_date";
        }
        if ("totalSpent".equalsIgnoreCase(sortField)) {
            sortField = "total_spent";
        }


        Sort.Direction direction = Sort.Direction.DESC;

        // ✅ Use mapped field name here
        Sort sortOrder = Sort.by(direction, sortField);

        Pageable pageable = PageRequest.of(page - 1, size, sortOrder);

        String username = extractUsername();
        Page<CustomerEntity> response = null;
        try {
            response = shopRepo.findAllCustomersWithPagination(username, search, pageable, Boolean.TRUE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    @Transactional
    public BillingResponse doPayment(BillingRequest request) throws Exception {


        checkAnonymousCustomer(request);

        Integer unitsSold = 0;
        for (var obj : request.getCart()) {
            unitsSold += obj.getQuantity();
        }

        UserSettingsEntity userSettingsEntity = userSettingsRepo.findByUsername(extractUsername());
        Boolean sendInvoice = true;

        var billingEntity = BillingEntity.builder().customerId(request.getSelectedCustomer().getId())
                .unitsSold(unitsSold).taxAmount(request.getTax()).userId(extractUsername()).totalAmount(request.getTotal())
                .payingAmount(request.getPayingAmount())
                .gstin(request.getGstin())
                .dueReminderCount(0)
                .remainingAmount(request.getRemainingAmount())
                .discountPercent(request.getDiscountPercentage()).remarks(request.getRemarks()).subTotalAmount(request.getTotal() - request.getTax()).createdDate(LocalDateTime.now()).build();


        BillingEntity billResponse = billRepo.save(billingEntity);

        if (userSettingsEntity != null) {
            sendInvoice = userSettingsEntity.getAutoSendInvoice();
            String orderPrefix = userSettingsEntity.getSerialNumberPattern();

            String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));

            String sequentialPart = String.format("%04d", billResponse.getId());
            String invoiceNumber = "FMS-" + datePart + "-" + sequentialPart;
            if (orderPrefix != null) {
                invoiceNumber = orderPrefix + "-" + datePart + "-" + sequentialPart;

                billResponse.setInvoiceNumber(invoiceNumber);
                billRepo.save(billResponse);
            } else {
                billResponse.setInvoiceNumber(invoiceNumber);
                billRepo.save(billResponse);
            }
        }

        final Double[] totalProfitOnCP = {0d};
        if (billResponse.getId() != null) {
            request.getCart().stream().forEach(obj -> {

                ProductEntity prodRes = prodRepo.findByIdAndUserId(obj.getId(), extractUsername());
                log.info("Product details " + prodRes);
                Double tax = (prodRes.getTaxPercent() * obj.getQuantity() * obj.getPrice()) / 100;
                Double discountedTotal = 0d;

                if (obj.getDiscountPercentage() != 0) {
                    discountedTotal = obj.getPrice() - (obj.getDiscountPercentage() * obj.getPrice()) / 100;
                    obj.setPrice(discountedTotal);
                } else
                    discountedTotal = (double) obj.getPrice();

                Double total = (double) (obj.getQuantity() * Math.round(discountedTotal));
                //Integer subTotal = total- tax;
                Double profitOnCp = (discountedTotal - prodRes.getCostPrice()) * obj.getQuantity();
                totalProfitOnCP[0] = totalProfitOnCP[0] + Math.round(profitOnCp);

                ProductSalesEntity gstCalc = getGSTBreakDown(request.getSelectedCustomer(), obj, prodRes, extractUsername());

                var productSalesEntity = ProductSalesEntity.builder().billingId(billResponse.getId())
                        .profitOnCP(profitOnCp)
                        .sgstPercentage(gstCalc.getSgstPercentage())
                        .sgst(gstCalc.getSgst())
                        .cgstPercentage(gstCalc.getCgstPercentage())
                        .cgst(gstCalc.getCgst())
                        .igstPercentage(gstCalc.getIgstPercentage())
                        .igst(gstCalc.getIgst())
                        .productId(obj.getId())
                        .productDetails(obj.getDetails())
                        .userId(extractUsername())
                        .discountPercentage(obj.getDiscountPercentage())
                        .quantity(obj.getQuantity())
                        .tax(gstCalc.getTax())
                        .subTotal(gstCalc.getSubTotal())
                        .total(total)
                        .updatedAt(LocalDateTime.now())
                        .build();

                ProductSalesEntity prodSalesResponse = prodSalesRepo.save(productSalesEntity);


                try {
                    String saveGSTListing = billingProcess.saveGstListing(billResponse.getInvoiceNumber(), extractUsername());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (!userSettingsEntity.getAllowNoStockBilling()) {
                    if (prodSalesResponse.getId() != null) {
                        prodRepo.updateProductStock(obj.getId(), obj.getQuantity(), extractUsername(), LocalDateTime.now());

                    }
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
            String payingStatus = "Paid";

            if (request.getTotal() > request.getPayingAmount()) {
                payingStatus = "SemiPaid";
            }
            if (request.getPayingAmount() == 0) {
                payingStatus = "UnPaid";
            }


            var paymentEntity = PaymentEntity.builder().billingId(billResponse.getId()).createdDate(LocalDateTime.now())
                    .paymentMethod(paymentMethod).status(payingStatus).tax(request.getTax()).userId(extractUsername())
                    .orderNumber(billResponse.getInvoiceNumber())
                    .paid(request.getPayingAmount())
                    .toBePaid(request.getRemainingAmount())
                    .reminderCount(0)
                    .updatedBy(extractUsername())
                    .updatedDate(LocalDateTime.now())
                    .subtotal(request.getTotal() - request.getTax()).total(request.getTotal()).build();


            salesPaymentRepo.save(paymentEntity);

            try {
                String savePaymentHistory = utils.asyncSavePaymentHistory(billResponse.getId(), paymentEntity.getId(), request.getPayingAmount(), billResponse.getInvoiceNumber());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            try {
                //shopRepo.updateCustomerSpentAmount(request.getSelectedCustomer().getId(), request.getTotal(), extractUsername());
                shopRepo.updateCustomerSpentAmountAndOrdersCount(request.getSelectedCustomer().getId(), request.getTotal(), extractUsername());
            } catch (Exception e) {
                // TODO Auto-generated catch block,
                e.printStackTrace();
            }


            if (sendInvoice && !(request.getSelectedCustomer().getName().equals("Anonymous"))) {

                try {
                    sendInvoiceOverEmail(billResponse);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

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
                salesCacheService.evictsReportsCache(extractUsername());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return BillingResponse.builder().paymentReferenceNumber(paymentEntity.getPaymentReferenceNumber())
                    .invoiceNumber(billResponse.getInvoiceNumber()).status("SUCCESS").build();
        }

        return BillingResponse.builder().status("FAILURE").build();
    }

    @Transactional
    public BillingResponse doPayment2(BillingRequest request) throws Exception {
        checkAnonymousCustomer(request);
        String username = extractUsername();

        Map<String, Object> validateMap = validateBillingRequest(request);

        // 1. Setup & Initial Billing Entity
        if((boolean) validateMap.get("validated")) {
            UserSettingsEntity userSettings = userSettingsRepo.findByUsername(username);
            int unitsSold = calculateTotalUnits(request.getCart());
            BillingEntity billResponse = createInitialBill(request, unitsSold, username);

            // 2. Generate Invoice Number
            if (userSettings != null) {
                assignInvoiceNumber(billResponse, userSettings);
            }

            if (billResponse.getId() == null) {
                return BillingResponse.builder().status("FAILURE").build();
            }

            // 3. Process Cart Items (Calculates taxes, profits, stock)
            double totalProfitOnCP = processCartItems(request, billResponse, userSettings, username);

            // 4. Update Bill with final profit
            billResponse.setTotalProfitOnCP(totalProfitOnCP);
            billRepo.save(billResponse);

            // 5. Process Payment
            PaymentEntity payment = processPayment(request, billResponse, username);

            // 6. Post-Payment Actions
            savePaymentHistorySafe(billResponse, payment, request);
            updateCustomerMetricsSafe(request, username);
            handleInvoiceEmail(request, billResponse, userSettings);
            clearSalesCachesSafe(username);

            return BillingResponse.builder()
                    .paymentReferenceNumber(payment.getPaymentReferenceNumber())
                    .invoiceNumber(billResponse.getInvoiceNumber())
                    .status("SUCCESS")
                    .build();
        }
        else{
            return (BillingResponse) validateMap.get("validateResponse");
        }
    }

    private Map<String, Object> validateBillingRequest(BillingRequest request) {
        Map<String, Object> res=new HashMap<>();
        if(request.getPayingAmount()>request.getTotal()) {
          var validateResponse=  BillingResponse.builder()
                    .errorCode("401")
                    .errorMessage("Paying amount cannnot be more than total amount")
                    .status("VALIDATED")
                    .build();
            res.put("validated", Boolean.FALSE);
            res.put("validateResponse", validateResponse);
        }
        if(request.getSelectedCustomer()==null) {
            var validateResponse=  BillingResponse.builder()
                    .errorCode("401")
                    .errorMessage("Please select valid customer")
                    .status("VALIDATED")
                    .build();
            res.put("validated", Boolean.FALSE);
            res.put("validateResponse", validateResponse);
        }
        if(request.getCart().size()<1) {
            var validateResponse=  BillingResponse.builder()
                    .errorCode("401")
                    .errorMessage("No item in cart")
                    .status("VALIDATED")
                    .build();
            res.put("validated", Boolean.FALSE);
            res.put("validateResponse", validateResponse);
        }
        else{
            res.put("validated", Boolean.TRUE);
        }

        return res;
    }

    private int calculateTotalUnits(List<ProductBillDTO> cart) { // Note: Replace CartItemDto with your actual class name
        return cart.stream().mapToInt(obj -> obj.getQuantity()).sum();
    }

    private BillingEntity createInitialBill(BillingRequest request, int unitsSold, String username) {
        BillingEntity billingEntity = BillingEntity.builder()
                .customerId(request.getSelectedCustomer().getId())
                .unitsSold(unitsSold)
                .taxAmount(request.getTax())
                .userId(username)
                .totalAmount(request.getTotal())
                .payingAmount(request.getPayingAmount())
                .gstin(request.getGstin())
                .dueReminderCount(0)
                .remainingAmount(request.getRemainingAmount())
                .discountPercent(request.getDiscountPercentage())
                .remarks(request.getRemarks())
                .subTotalAmount(request.getTotal() - request.getTax())
                .createdDate(LocalDateTime.now())
                .build();

        return billRepo.save(billingEntity);
    }

    private void assignInvoiceNumber(BillingEntity billResponse, UserSettingsEntity userSettings) {
        String orderPrefix = userSettings.getSerialNumberPattern();
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String sequentialPart = String.format("%04d", billResponse.getId());

        String invoiceNumber = (orderPrefix != null ? orderPrefix : "FMS") + "-" + datePart + "-" + sequentialPart;

        billResponse.setInvoiceNumber(invoiceNumber);
        billRepo.save(billResponse);
    }

    private double processCartItems(BillingRequest request, BillingEntity billResponse, UserSettingsEntity userSettings, String username) {
        double totalProfit = 0d;
        boolean allowNoStockBilling = userSettings != null && userSettings.getAllowNoStockBilling();

        // Replaced stream with standard loop for cleaner exception handling and primitive math tracking
        for (var obj : request.getCart()) {
            ProductEntity prodRes = prodRepo.findByIdAndUserId(obj.getId(), username);

            double discountedTotal = obj.getPrice();
            if (obj.getDiscountPercentage() != 0) {
                discountedTotal = obj.getPrice() - (obj.getDiscountPercentage() * obj.getPrice()) / 100;
                obj.setPrice(discountedTotal);
            }

            double total = obj.getQuantity() * Math.round(discountedTotal);
            double profitOnCp = (discountedTotal - prodRes.getCostPrice()) * obj.getQuantity();
            totalProfit += Math.round(profitOnCp);

            ProductSalesEntity gstCalc = getGSTBreakDown(request.getSelectedCustomer(), obj, prodRes, username);

            ProductSalesEntity productSalesEntity = ProductSalesEntity.builder()
                    .billingId(billResponse.getId())
                    .profitOnCP(profitOnCp)
                    .sgstPercentage(gstCalc.getSgstPercentage())
                    .sgst(gstCalc.getSgst())
                    .cgstPercentage(gstCalc.getCgstPercentage())
                    .cgst(gstCalc.getCgst())
                    .igstPercentage(gstCalc.getIgstPercentage())
                    .igst(gstCalc.getIgst())
                    .productId(obj.getId())
                    .productDetails(obj.getDetails())
                    .userId(username)
                    .discountPercentage(obj.getDiscountPercentage())
                    .quantity(obj.getQuantity())
                    .tax(gstCalc.getTax())
                    .subTotal(gstCalc.getSubTotal())
                    .total(total)
                    .updatedAt(LocalDateTime.now())
                    .build();

            ProductSalesEntity prodSalesResponse = prodSalesRepo.save(productSalesEntity);

            try {
                billingProcess.saveGstListing(billResponse.getInvoiceNumber(), username);
            } catch (Exception e) {
                throw new RuntimeException("Failed to save GST listing", e);
            }

            if (!allowNoStockBilling && prodSalesResponse.getId() != null) {
                prodRepo.updateProductStock(obj.getId(), obj.getQuantity(), username, LocalDateTime.now());
            }
        }
        return totalProfit;
    }

    private PaymentEntity processPayment(BillingRequest request, BillingEntity billResponse, String username) {
        String paymentMethod = request.getPaymentMethod() != null ? request.getPaymentMethod() : "CASH";
        String payingStatus = "Paid";

        if (request.getPayingAmount() == 0) {
            payingStatus = "UnPaid";
        } else if (request.getTotal() > request.getPayingAmount()) {
            payingStatus = "SemiPaid";
        }

        PaymentEntity paymentEntity = PaymentEntity.builder()
                .billingId(billResponse.getId())
                .createdDate(LocalDateTime.now())
                .paymentMethod(paymentMethod)
                .status(payingStatus)
                .tax(request.getTax())
                .userId(username)
                .orderNumber(billResponse.getInvoiceNumber())
                .paid(request.getPayingAmount())
                .toBePaid(request.getRemainingAmount())
                .reminderCount(0)
                .updatedBy(username)
                .updatedDate(LocalDateTime.now())
                .subtotal(request.getTotal() - request.getTax())
                .total(request.getTotal())
                .build();

        return salesPaymentRepo.save(paymentEntity);
    }

    private void savePaymentHistorySafe(BillingEntity bill, PaymentEntity payment, BillingRequest request) {
        try {
            utils.asyncSavePaymentHistory(bill.getId(), payment.getId(), request.getPayingAmount(), bill.getInvoiceNumber());
        } catch (Exception e) {
            throw new RuntimeException("Failed to save payment history", e);
        }
    }

    private void updateCustomerMetricsSafe(BillingRequest request, String username) {
        try {
            shopRepo.updateCustomerSpentAmountAndOrdersCount(request.getSelectedCustomer().getId(), request.getTotal(), username);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleInvoiceEmail(BillingRequest request, BillingEntity billResponse, UserSettingsEntity userSettings) {
        boolean sendInvoice = userSettings != null && userSettings.getAutoSendInvoice() != null ? userSettings.getAutoSendInvoice() : true;

        if (sendInvoice && !("Anonymous".equals(request.getSelectedCustomer().getName()))) {
            try {
                sendInvoiceOverEmail(billResponse);
            } catch (Exception e) {
                throw new RuntimeException("Failed to send invoice over email", e);
            }
        }
    }

    private void clearSalesCachesSafe(String username) {
        try {
            salesCacheService.evictUserSales(username);
            salesCacheService.evictUserProducts(username);
            salesCacheService.evictUserPayments(username);
            salesCacheService.evictUserCustomers(username);
            salesCacheService.evictUserDasbhoard(username);
            salesCacheService.evictsUserGoals(username);
            salesCacheService.evictsUserAnalytics(username);
            salesCacheService.evictsTopSelling(username);
            salesCacheService.evictsTopOrders(username);
            salesCacheService.evictsReportsCache(username);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendInvoiceOverEmail(BillingEntity billResponse) {

        InvoiceDetails order = getOrderDetails(billResponse.getInvoiceNumber());
        try {
            Map<String, Object> emailContent = emailTemplate.generateOrderHtml(order, extractUsername());

            //if (Arrays.asList(environment.getActiveProfiles()).contains("prod") || Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
            String customerEmail = order.getCustomerEmail();
            String invoiceNumber = billResponse.getInvoiceNumber();
            String username = extractUsername();
            CompletableFuture<String> emailResult = CompletableFuture.supplyAsync(() -> {


                return email.sendEmail(customerEmail,
                        invoiceNumber, order.getCustomerName(),
                        generateGSTInvoicePdf(invoiceNumber, username), (String) emailContent.get("htmlTemplate"), (String) emailContent.get("shopName"));


            }).thenApply(futureResult -> {

                EmailRecord emailRecord = null;
                try {
                    emailRecord = EmailRecord.builder().emailId(customerEmail)
                            .identifier(invoiceNumber)
                            .username(username)
                            .emailApiResponse(futureResult.get())
                            .event("ORD-CREATED")
                            .createdDate(LocalDateTime.now())
                            .build();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }

                EmailRecord emailRecordSave = emailRecordRepo.save(emailRecord);

                return String.valueOf(futureResult);

            });

         /*           CompletableFuture<String> futureResult = email.sendEmail(order.getCustomerEmail(),
                            billResponse.getInvoiceNumber(), order.getCustomerName(),
                            generateGSTInvoicePdf(billResponse.getInvoiceNumber()), (String) emailContent.get("htmlTemplate"), (String) emailContent.get("shopName"));
                    log.info(futureResult);*/
            // }


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void checkAnonymousCustomer(BillingRequest request) {

        if (request.getSelectedCustomer().getName().equals("Anonymous")) {
            CustomerEntity existingCustomer = shopRepo.findByNameAndId(extractUsername(), request.getSelectedCustomer().getName());

            if (existingCustomer == null) {
                var customerEntity = CustomerEntity.builder().name(request.getSelectedCustomer().getName()).email(request.getSelectedCustomer().getEmail())
                        .createdDate(LocalDateTime.now())
                        .isActive(Boolean.TRUE)
                        .state(request.getSelectedCustomer().getState())
                        .city("na")
                        .phone(request.getSelectedCustomer().getPhone())
                        .status("ACTIVE").userId(extractUsername()).totalSpent(0d).build();

                CustomerEntity ent = shopRepo.save(customerEntity);
                log.info("The saved customer details is " + ent);
                request.getSelectedCustomer().setId(ent.getId());
            } else {
                request.getSelectedCustomer().setId(existingCustomer.getId());
            }
        }
    }

    private ProductSalesEntity getGSTBreakDown(CustomerEntity selectedCustomer, ProductBillDTO obj, ProductEntity prodRes, String username) {
        String customerState = selectedCustomer.getState();
        String shopState = "";
        try {
            shopState = shopBasicRepo.findByUserId(username).getShopState();
        } catch (Exception e) {
            shopState = "West Bengal";
        }

        double taxPercent = prodRes.getTaxPercent(); // e.g., 18
        double qty = obj.getQuantity();
        double price = obj.getPrice(); // MRP (tax inclusive)

        // Extract base price (tax exclusive)
        double basePrice = price / (1 + taxPercent / 100.0);
        double totalTax = price - basePrice;

        double cgst = 0, sgst = 0, igst = 0;
        double cgstPercent = 0, sgstPercent = 0, igstPercent = 0;

        if (customerState.equals(shopState)) {
            // Intra-state: CGST + SGST
            cgst = totalTax / 2;
            sgst = totalTax / 2;
            cgstPercent = taxPercent / 2;
            sgstPercent = taxPercent / 2;
        } else {
            // Inter-state: IGST only
            igst = totalTax;
            igstPercent = taxPercent;
        }

        // Multiply by quantity
        basePrice *= qty;
        cgst *= qty;
        sgst *= qty;
        igst *= qty;
        totalTax *= qty;

        return ProductSalesEntity.builder()
                .cgstPercentage((int) Math.round(cgstPercent))
                .cgst(round2(cgst))
                .sgstPercentage((int) Math.round(sgstPercent))
                .sgst(round2(sgst))
                .igstPercentage((int) Math.round(igstPercent))
                .igst(round2(igst))
                .tax(round2(totalTax))
                .subTotal(round2(basePrice))
                .build();
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
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
        if ("total".equalsIgnoreCase(sortField)) {
            sortField = "total_amount";
        }
        if ("customer".equalsIgnoreCase(sortField)) {
            sortField = "customer_id";
        }
        if ("paid".equalsIgnoreCase(sortField)) {
            sortField = "paying_amount";
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Sort sortOrder = Sort.by(direction, sortField);

        // Follow same paging convention as getAllProducts (1-based page param)
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, sortOrder);

        Page<BillingEntity> billingPage = null;

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            // Use a custom query to search by invoice number or customer name
            try {
                billingPage = billRepo.findByUserIdAndSearchNative(username, searchTerm.trim(), pageable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            billingPage = billRepo.findAllByUserId(username, pageable);
        }

        List<SalesResponseDTO> dtoList = billingPage.getContent().stream()
                .map(obj -> {
                    String customerName = null;
                    String paymentStatus = null;
                    try {
                        customerName = shopRepo.findByIdAndUserId(obj.getCustomerId(), username).getName();
                        paymentStatus = salesPaymentRepo.findPaymentDetails(obj.getId(), username).getStatus();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    return SalesResponseDTO.builder()
                            .customer(customerName)
                            .remarks(obj.getRemarks())
                            .date(obj.getCreatedDate().toString())
                            .id(obj.getInvoiceNumber())
                            .total(obj.getTotalAmount())
                            .paid(obj.getPayingAmount())
                            .status(paymentStatus)
                            .count(obj.getUnitsSold())
                            .gstin(obj.getGstin())
                            .reminderCount(obj.getDueReminderCount())
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
                            .gstin(obj.getGstin())
                            .status(paymentStatus)
                            .build();
                })
                .toList();

        return new PageImpl<>(dtoList, pageable, billingPage.getTotalElements());
    }

    @Cacheable(value = "dashboard", keyGenerator = "userScopedKeyGenerator")
    public DasbboardResponseDTO getDashBoardDetails(String range) {
        log.info("selected day range" + range);
        List<BillingEntity> billList = new ArrayList<>();
        List<ProductEntity> prodList = new ArrayList<>();

        List<String> roles = extractRoles();
        log.info("The user roles" + roles);
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
        Integer countOfOrders = 0;

        for (BillingEntity obj : billList) {
            monthlyRevenue = (int) (monthlyRevenue + obj.getTotalAmount());
            taxCollected = (int) (taxCollected + obj.getTaxAmount());
            totalUnitsSold = totalUnitsSold + obj.getUnitsSold();
            countOfOrders = countOfOrders + 1;
        }
        ;
        for (ProductEntity obj : prodList) {
            if (obj.getStock() < 1)
                outOfStockCount = outOfStockCount + 1;
        }
        ;

        return DasbboardResponseDTO.builder().monthlyRevenue(monthlyRevenue).outOfStockCount(outOfStockCount)
                .taxCollected(taxCollected).totalUnitsSold(totalUnitsSold).countOfSales(countOfOrders).build();
    }

    @Cacheable(value = "payments", keyGenerator = "userScopedKeyGenerator")
    public List<PaymentDetails> getPaymentList(String fromDate, String toDate) {

        LocalDateTime startDate = LocalDate.parse(fromDate).atStartOfDay();
        LocalDateTime endDate = LocalDate.parse(toDate).atTime(LocalTime.MAX);
        List<PaymentEntity> paymentList = salesPaymentRepo.getPaymentList(startDate, endDate, extractUsername());
        paymentList.sort(Comparator.comparing(PaymentEntity::getCreatedDate).reversed());
        List<PaymentDetails> response = new ArrayList<>();
        paymentList.stream().forEach(obj -> {
            //userProfile.getGstNumber() != null ? userProfile.getGstNumber() : "sample gst number"
            response.add(PaymentDetails.builder()
                    .id(obj.getPaymentReferenceNumber())
                    .amount(obj.getTotal())
                    .date(String.valueOf(obj.getCreatedDate()))
                    .saleId(obj.getOrderNumber())
                    .reminderCount(obj.getReminderCount())
                    .method(obj.getPaymentMethod())
                    .paid(obj.getPaid() != null ? obj.getPaid() : 0d)
                    .due(obj.getToBePaid() != null ? obj.getToBePaid() : 0d)
                    .status(obj.getStatus())
                    .build());
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
            log.info(prodList.toString());
            prodList.stream().forEach(obj -> {
                ProductSuccessDTO prodsaveResponse = saveProduct(obj);
                log.info(String.valueOf(prodsaveResponse));
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

    public List<ProductRequest> uploadBulkProductFromImage(MultipartFile file) {

        Map<String, Object> importCheck = setServ.checkImportLimit();

        if ((boolean) importCheck.get("allowed")) {
            try {
                List<ProductRequest> prodList = util.validateDataFromImage(file);
                log.info(prodList.toString());
                prodList.stream().forEach(obj -> {
                    ProductSuccessDTO prodsaveResponse = saveProductFromImage(obj);
                    log.info(String.valueOf(prodsaveResponse));
                });

                try {
                    salesCacheService.evictUserProducts(extractUsername());

                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return prodList;

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            log.info("Import limit exceeded for today");
        }

        return null;
    }


    public InvoiceDetails getOrderDetails(String orderReferenceNumber) {

        String username = "";

        if (orderReferenceNumber != null) {
            BillingEntity billDetails = billRepo.findOrderByJustReference(orderReferenceNumber);
            username = billDetails.getUserId();
        }

        BillingEntity billDetails = billRepo.findOrderByReference(orderReferenceNumber, username);

        Double paidAmount = 0d;
        Double dueAmount = 0d;

        PaymentEntity paymentEntity = salesPaymentRepo.findPaymentDetails(billDetails.getId(), username);

        boolean paid = false;
        if (paymentEntity.getStatus().equalsIgnoreCase("Paid")) {
            paid = true;
        }
        paidAmount = paymentEntity.getPaid() != null ? paymentEntity.getPaid() : 0d;
        dueAmount = paymentEntity.getPaid() != null ? paymentEntity.getToBePaid() : 0d;
        CustomerEntity customerEntity = new CustomerEntity();
        if (billDetails.getCustomerId() == 0) {
            customerEntity.setId(billDetails.getCustomerId());
            customerEntity.setName("Anonymous");
            customerEntity.setEmail("na@na.com");
            customerEntity.setPhone("0000000000");
        } else {
            customerEntity = shopRepo.findByIdAndUserId(billDetails.getCustomerId(), username);
        }


        List<ProductSalesEntity> prodSales = prodSalesRepo.findByOrderId(billDetails.getId(), username);
        Double gst = 0d;
        for (ProductSalesEntity orders : prodSales) {
            gst = gst + orders.getTax();
        }

        List<OrderItem> items = prodSales.stream().map(obj -> {
            String username2 = "";
            if (orderReferenceNumber != null) {
                BillingEntity billDetails2 = billRepo.findOrderByJustReference(orderReferenceNumber);
                username2 = billDetails.getUserId();
            }

            log.info("The productId is " + obj.getProductId());
            ProductEntity prodRes = prodRepo.findByIdAndUserId(obj.getProductId(), username2);

            var orderItems = OrderItem.builder().productName(prodRes.getName()).unitPrice(obj.getTotal()).gst(obj.getTax())
                    .sgst(obj.getSgst())
                    .sgstPercentage(obj.getSgstPercentage())
                    .cgst(obj.getCgst())
                    .cgstPercentage(obj.getCgstPercentage())
                    .igst(obj.getIgst())
                    .igstPercentage(obj.getIgstPercentage())
                    .details(obj.getProductDetails())
                    .discount(obj.getDiscountPercentage())
                    .quantity(obj.getQuantity()).build();
            return orderItems;
        }).collect(Collectors.toList());
        var response = InvoiceDetails.builder().discountRate(billDetails.getDiscountPercent()).invoiceId(orderReferenceNumber)
                .paymentReferenceNumber(paymentEntity.getPaymentReferenceNumber()).items(items).gstRate(gst)
                .gstNumber(billDetails.getGstin())
                .reminderCount(billDetails.getDueReminderCount())
                .customerPhone(customerEntity.getPhone()).customerEmail(customerEntity.getEmail()).orderedDate(String.valueOf(billDetails.getCreatedDate()).substring(0, 10))
                .totalAmount(billDetails.getTotalAmount()).customerName(customerEntity.getName())
                .paidAmount(paidAmount)
                .dueAmount(dueAmount)
                .paid(paid).build();
        return response;
    }

    public InvoiceDetails getOrderDetailsNew(String orderReferenceNumber) {

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

            log.info("The productId is " + obj.getProductId());
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


    @Cacheable(value = "reports", keyGenerator = "userScopedKeyGenerator")
    public byte[] generateReport(ReportRequest request) {

        if (request.getUsername() == null) {
            request.setUsername(extractUsername());
        }

        LocalDate fromDate = LocalDate.parse(request.getFromDate());

        // Combine with a time (e.g., start of day)
        LocalDateTime fromDateTime = fromDate.atStartOfDay();

        LocalDate toDate = LocalDate.parse(request.getToDate());

        // Combine with a time (e.g., start of day)
        LocalDateTime toDateTime = toDate.atTime(LocalTime.MAX);

        log.info(String.valueOf(toDateTime));

        byte[] fileBytes = null;
        try {
            fileBytes = repogen.downloadReport(request.getReportType(), request.getFormat(), fromDateTime, toDateTime, request.getUsername());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            if (fileBytes != null) {
                var reportData = Report.builder().name(request.getReportType())
                        .fileName(request.getReportId() + "." + request.getFormat())
                        .reportType(request.getReportType())
                        .fromDate(LocalDate.parse(request.getFromDate()))
                        .toDate(LocalDate.parse(request.getToDate()))
                        .fileFormat(request.getFormat())
                        .status("READY")
                        .userId(request.getUsername())
                        .createdAt(OffsetDateTime.now())
                        .build();

                reportDRepo.save(reportData);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
                    .type(obj.getReportType())
                    .format(obj.getFileFormat())
                    .fileName(obj.getFileName()).fromDate(obj.getFromDate()).toDate(obj.getToDate()).id(obj.getId())
                    .status(obj.getStatus()).build();

        }).collect(Collectors.toList());

    }

    public String updatePassword(UserInfo userInfo) {
        log.info("entered updatePassword with request " + userInfo);

        if (userInfo.getUsername() == null) {
            userInfo.setUsername(extractUsername());
        }
        log.info("updated updatePassword with request " + userInfo);

        UserInfo userRes = userinfoRepo.findByUsername(userInfo.getUsername()).get();
        userRes.setPassword(passwordEncoder.encode(userInfo.getPassword()));
        userRes.setUpdatedAt(LocalDateTime.now());
        userinfoRepo.save(userRes);

        return "success";
    }

    public UpdateUserDTO saveEditableUser(UpdateUserDTO request, String username) throws IOException {

        log.info("entered saveEditableUser with request " + request + " and username " + username);
        request.setUsername(username);
        UserInfo userinfo = userinfoRepo.findByUsername(username).get();

        if (!(userinfo.getSource().equalsIgnoreCase("google"))) {
            userinfo.setEmail(request.getEmail());
        }

        userinfo.setName(request.getName());
        //userinfo.setPhoneNumber(request.getPhone());

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

  /*  public String saveEditableUserProfilePic(MultipartFile profilePic, String username) throws IOException {
        log.info("entered saveEditableUserProfilePic with  username " + username);

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
    }*/


// ... inside your service class ...

    // Define the absolute path on your Ubuntu server where images will live


    public String saveEditableUserProfilePicInOracleCloud(MultipartFile profilePic, String username) throws Exception {
        log.info("entered saveEditableUserProfilePic with username " + username);
        UserInfo userinfo = userinfoRepo.findByUsername(username).get();

        if (userinfo.getSource().equalsIgnoreCase("google")) {
            throw new RuntimeException("Profile picture cannot be updated for Google authenticated users");
        }

        // 1. Create a unique filename (e.g., "junaid_profile.jpg")
        String originalFilename = profilePic.getOriginalFilename();
        String safeFilename = username + "_" + System.currentTimeMillis() + "_" + originalFilename;

        // 2. Ensure the directory exists on the server
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 3. Save the file to the Ubuntu hard drive
        Path filePath = uploadPath.resolve(safeFilename);
        Files.copy(profilePic.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 4. Save the filename to your Database (Exactly as you did before)
        UserProfilePicEntity picRes = userProfilePicRepo.findByUsername(username);
        if (picRes != null) {
            picRes.setProfilePic(safeFilename); // Save the new unique filename
            picRes.setUpdated_date(LocalDateTime.now());
            userProfilePicRepo.save(picRes);
        } else {
            UserProfilePicEntity picResNew = new UserProfilePicEntity();
            picResNew.setUpdated_date(LocalDateTime.now());
            picResNew.setUsername(username);
            picResNew.setProfilePic(safeFilename);
            userProfilePicRepo.save(picResNew);
        }

        return "ok";
    }

    @Transactional
    public void deleteCustomer(Integer id) {
        shopRepo.updateStatus(id, "IN-ACTIVE", extractUsername(), Boolean.FALSE);
        try {
            salesCacheService.evictUserCustomers(extractUsername());
            salesCacheService.evictsUserAnalytics(extractUsername());
            salesCacheService.evictsReportsCache(extractUsername());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public byte[] generateInvoicePdf(String orderId) throws Exception {
        log.info(orderId);
        InvoiceDetails order = getOrderDetails(orderId);
        LocalDate orderedDate = LocalDate.parse(order.getOrderedDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        UpdateUserDTO userProfile = getUserProfile(extractUsername());
        String shopEmail = "";
        String gstNumber = "";
        String shopAddress = "";
        String shopPhone = "";
        String shopName = "";
        if (userProfile != null) {
            gstNumber = userProfile.getGstNumber() != null ? userProfile.getGstNumber() : "sample gst number";
            shopEmail = userProfile.getShopEmail() != null ? userProfile.getShopEmail() : "sample shop email";
            shopPhone = userProfile.getShopPhone() != null ? userProfile.getShopPhone() : "sample shop phone";
            shopAddress = userProfile.getShopLocation() != null ? userProfile.getShopLocation() : "sample shop address";
            shopName = userProfile.getShopName() != null ? userProfile.getShopName() : "sample shop name";
        }

// Format to new pattern
        String formattedDate = orderedDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        byte[] response = pdfutil.generateInvoice(order.getCustomerName(), order.getCustomerEmail(),
                order.getCustomerPhone(), order.getInvoiceId(), order.getItems(), formattedDate, order.getTotalAmount(), order.isPaid(), order.getGstRate(), shopName, shopAddress, shopEmail, shopPhone, gstNumber);

        return response;
    }

    public byte[] generateGSTInvoicePdf(String orderId) throws Exception {
        log.info("Generating invoice for orderNumber-->" + orderId);

        String username = "";
        if (orderId != null) {
            BillingEntity billDetails = billRepo.findOrderByJustReference(orderId);
            username = billDetails.getUserId();
        }

        InvoiceData invoiceData = utils.getFullInvoiceDetails(username, orderId);

        String invoiceTemplateName = "gstinvoice";
        String invoicePrinter = "THERMAL_2";

        try {
            SelectedInvoiceEntity repoEntity = invoiceRepo.findByUsername(username);
            if (repoEntity != null) {
                invoiceTemplateName = repoEntity.getTemplateName();
                invoicePrinter = repoEntity.getPrinterType();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        byte[] response = pdfgstutil.generateGSTInvoice(invoiceData, invoiceTemplateName, invoicePrinter);
        log.info("The full invoice Data is " + invoiceData);


        return response;
    }

    public byte[] generateGSTInvoicePdf(String orderId, String username) {
        log.info("Generating invoice for orderNumber-->" + orderId);

      /*  String username = "";
        if (orderId != null) {
            BillingEntity billDetails = billRepo.findOrderByJustReference(orderId);
            username = billDetails.getUserId();
        }*/

        InvoiceData invoiceData = utils.getFullInvoiceDetails(username, orderId);

        String invoiceTemplateName = "gstinvoice";
        String invoicePrinter = "THERMAL_2";

        try {
            SelectedInvoiceEntity repoEntity = invoiceRepo.findByUsername(username);
            if (repoEntity != null) {
                invoiceTemplateName = repoEntity.getTemplateName();
                invoicePrinter = repoEntity.getPrinterType();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        byte[] response = pdfgstutil.generateGSTInvoice(invoiceData, invoiceTemplateName, invoicePrinter);
        log.info("The full invoice Data is " + invoiceData);


        return response;
    }

    public UpdateUserDTO getUserProfile(String username) {

        if (username == null||username == "null"||username.equals("null")) {
            username = extractUsername();
        }

        // ShopDetailsEntity shopDetails = shopDetailsRepo.findbyUsername(username);

        ShopBasicEntity shopBasicEntity = shopBasicRepo.findByUserId(username);

        ShopFinanceEntity shopFinanceEntity = shopFinanceRepo.findByUserId(username);
        ShopBankEntity shopBankEntity = new ShopBankEntity();
        ShopUPIEntity shopUPIEntity = new ShopUPIEntity();
        if (shopFinanceEntity != null) {
            shopBankEntity = shopBankRepo.findByShopFinanceId(username);
            shopUPIEntity = salesUPIRepo.findByShopFinanceId(username);
        }

        ShopInvoiceTermsEnity shopInvoiceTermsEntity = shopInvoiceTermsRepo.findByUserId(username);


        log.info("entered getUserProfile with request  username " + username);

        UserInfo userinfo = userinfoRepo.findByUsername(username).get();


        if (shopBasicEntity != null) {

            var response = UpdateUserDTO.builder()
                    .username(username)
                    .email(getIfNotNull(userinfo, UserInfo::getEmail))
                    .name(getIfNotNull(userinfo, UserInfo::getName))
                    .phone(getIfNotNull(userinfo, UserInfo::getPhoneNumber))
                    .userSource(getIfNotNull(userinfo, UserInfo::getSource))

                    // Fields from shopBasicEntity
                    .address(getIfNotNull(shopBasicEntity, ShopBasicEntity::getAddress))
                    .shopLocation(getIfNotNull(shopBasicEntity, ShopBasicEntity::getAddress))
                    .shopAddress(getIfNotNull(shopBasicEntity, ShopBasicEntity::getAddress))
                    .shopEmail(getIfNotNull(shopBasicEntity, ShopBasicEntity::getShopEmail))
                    .shopPhone(getIfNotNull(shopBasicEntity, ShopBasicEntity::getShopPhone))
                    .shopName(getIfNotNull(shopBasicEntity, ShopBasicEntity::getShopName))
                    .shopPincode(getIfNotNull(shopBasicEntity, ShopBasicEntity::getShopPincode))
                    .shopCity(getIfNotNull(shopBasicEntity, ShopBasicEntity::getShopCity))
                    .shopState(getIfNotNull(shopBasicEntity, ShopBasicEntity::getShopState))
                    .shopSlogan(getIfNotNull(shopBasicEntity, ShopBasicEntity::getShopSlogan))

                    // Fields from shopFinanceEntity
                    .gstNumber(getIfNotNull(shopFinanceEntity, ShopFinanceEntity::getGstin))
                    .pan(getIfNotNull(shopFinanceEntity, ShopFinanceEntity::getPanNumber))
                    .gstin(getIfNotNull(shopFinanceEntity, ShopFinanceEntity::getGstin))

                    // Field from shopUPIEntity
                    .upi(getIfNotNull(shopUPIEntity, ShopUPIEntity::getUpiId))

                    // Field from shopInvoiceTermsEntity
                    .terms1(getIfNotNull(shopInvoiceTermsEntity, ShopInvoiceTermsEnity::getTerm))

                    // Fields from shopBankEntity
                    .bankAccount(getIfNotNull(shopBankEntity, ShopBankEntity::getAccountNumber))
                    .bankAddress(getIfNotNull(shopBankEntity, ShopBankEntity::getBranchName))
                    .bankHolder(getIfNotNull(shopBankEntity, ShopBankEntity::getAccountHolderName))
                    .bankName(getIfNotNull(shopBankEntity, ShopBankEntity::getBankName))
                    .bankIfsc(getIfNotNull(shopBankEntity, ShopBankEntity::getIfscCode))

                    .build();
            return response;
        } else {
            var response = UpdateUserDTO.builder().address("").email(userinfo.getEmail())
                    .gstNumber("").name(userinfo.getName()).phone(userinfo.getPhoneNumber())
                    .shopLocation("").shopOwner("").username(username)
                    .userSource(userinfo.getSource())
                    .build();
            return response;
        }


    }

    /*public byte[] getProfilePic(String username) throws IOException {

        log.info("entered getProfilePic with request  username " + username);

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
    }*/

    public byte[] getProfilePicOracle(String username) throws IOException {

        log.info("entered getProfilePic with request username " + username);

        UserInfo res = userinfoRepo.findByUsername(username).orElseThrow(() ->
                new RuntimeException("User not found: " + username));

        byte[] content = null;

        if (!"google".equals(res.getSource())) {
            // === 1. LOCAL UBUNTU SERVER RETRIEVAL (Replaces AWS S3) ===
            try {
                UserProfilePicEntity picRes = userProfilePicRepo.findByUsername(username);

                if (picRes != null && picRes.getProfilePic() != null) {
                    // Combine the directory path with the saved filename
                    Path imagePath = Paths.get(UPLOAD_DIR, picRes.getProfilePic());

                    // Only try to read if the file actually exists on the hard drive
                    if (Files.exists(imagePath)) {
                        content = Files.readAllBytes(imagePath);
                    } else {
                        log.info("File not found on server: " + imagePath.toString());
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading local profile picture: " + e.getMessage());
                e.printStackTrace();
            }

        } else {
            // === 2. GOOGLE URL RETRIEVAL ===
            if (res.getProfilePiclink() != null) {
                try {
                    URL url = new URL(res.getProfilePiclink());
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    // Using try-with-resources ensures the network connection closes automatically
                    try (InputStream inputStream = connection.getInputStream()) {
                        // Java 17 magic: readAllBytes() replaces your entire buffer loop!
                        content = inputStream.readAllBytes();
                    }
                } catch (IOException e) {
                    System.err.println("Error fetching Google profile picture: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        return content;
    }

    @Transactional
    public void deleteProduct(Integer id) {
        log.info("endtered deleteProduct with productId " + id);

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
        String userId = extractUsername();

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
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<Object[]> resultsTaxes = billRepo.getMonthlyTaxesSummary(startDate, endDate, userId);
        for (Object[] row : resultsTaxes) {
            Integer count = ((Number) row[1]).intValue();
            taxes.add(count);
        }
        try {
            List<Object[]> resultsCustomers = shopRepo.getMonthlyCustomerCount(startDate, endDate, userId);
            for (Object[] row : resultsCustomers) {
                Integer count = ((Number) row[1]).intValue();
                customers.add(count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<Object[]> resultsOnlinePaymentCount = salesPaymentRepo.getMonthlyPaymentCounts(startDate, endDate, userId);

        for (Object[] row : resultsOnlinePaymentCount) {
            Integer count = ((Number) row[1]).intValue();
            onlinePaymentCounts.add(count);
        }


        for (Object[] row : resultsSales) {
            double percentage = 0.08 + (0.20 - 0.08) * random.nextDouble();
            log.info("The profits on cp are " + ((Number) row[2]).longValue());
            Long count = ((Number) row[1]).longValue();
            Long estimatedProfit = ((Number) row[1]).longValue();
            profits.add(((Number) row[2]).longValue());
        }
        response.setCustomers(customers);
        response.setLabels(labels);
        response.setProfits(profits);
        response.setSales(onlinePaymentCounts);
        response.setStocks(stocks);
        response.setTaxes(taxes);
        response.setRevenues(sales);

        return response;
    }


    public Map<String, String> getUserProfileDetails() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Current user: " + username);
        Map<String, String> response = new HashMap<>();
        response.put("username", username);
        return response;
    }

    public UserProfileDto getUserProfileWithRoles() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserInfo res = userinfoRepo.findByUsername(username).orElseThrow(() ->
                new RuntimeException("User not found: " + username));

        UserProfileDto response = UserProfileDto.builder().username(username).phoneNumber(res.getPhoneNumber()).name(res.getName()).roles(extractRoles()).build();

        log.info("getUserProfileWithRoles: " + response);
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
        Page<MessageEntity> notificationsList = null;

        if (seen != null && !seen.isEmpty() && !seen.equals("all")) {

            Boolean isRead = false;
            if (seen.equals("seen"))
                isRead = true;
            else
                isRead = false;

            if (seen.equals("flagged")) {
                Boolean isFlagged = true;
                notificationsList = notiRepo.findAllNotificationsByFlaggedStatus(extractUsername(), domain, isFlagged, Boolean.FALSE, Boolean.TRUE, pageable);
            } else
                notificationsList = notiRepo.findAllNotificationsByReadStatus(extractUsername(), domain, isRead, Boolean.FALSE, Boolean.TRUE, pageable);

        } else
            notificationsList = notiRepo.findAllNotifications(extractUsername(), domain, Boolean.FALSE, Boolean.TRUE, pageable);

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
        UserPaymentModes paymentModes = paymentModesRepo.getUserPaymentModes(extractUsername());
        Map<String, Boolean> response = new HashMap<>();
        log.info("The paymentModes are " + paymentModes);
        if (paymentModes != null) {
            if (paymentModes.getCard())
                response.put("card", true);
            else
                response.put("card", false);

            if (paymentModes.getCash())
                response.put("cash", true);
            else
                response.put("cash", false);

            if (paymentModes.getUpi())
                response.put("upi", true);
            else
                response.put("upi", false);
        }
        log.info("the getAvailablePaymentMethods response is " + response);
        return response;
    }

    @Transactional
    public void updatePaymentReferenceNumber(String paymentRef, String orderRef) {

        salesPaymentRepo.updatePaymentReferenceNumber(paymentRef, orderRef, extractUsername());

    }

    @Cacheable(value = "dashboard", keyGenerator = "userScopedKeyGenerator")
    public List<WeeklySales> getWeeklyAnalytics(String range) {


        String userId = extractUsername();

        List<WeeklySales> response = new ArrayList<>();

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
            if (range.equals("today")) {
                resultsSales = billRepo.getSalesAndStocksToday(endDate, userId);
            }

            if (range.equals("lastWeek")) {
                resultsSales = billRepo.getWeeklySalesAndStocks(endDate, userId);
            }
            if (range.equals("lastMonth")) {

                resultsSales = billRepo.getSalesAndStocksMonthly(endDate, userId);
            }
            if (range.equals("lastYear")) {
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
            List<BillingEntity> billingDetails = new ArrayList<>();

            if (range.equals("today")) {
                billingDetails = billRepo.findTopNSalesForToday(username, count);
            }
            if (range.equals("lastWeek")) {
                billingDetails = billRepo.findTopNSalesForLastWeek(username, count);
            }
            if (range.equals("lastMonth")) {
                billingDetails = billRepo.findTopNSalesForLastMonth(username, count);
            }
            if (range.equals("lastYear")) {
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
            EstimatedGoalsEntity newGoals = EstimatedGoalsEntity.builder()
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
        log.info("The existing goals are " + existingGoals);
        String username = extractUsername();
        List<BillingEntity> billingDetails = new ArrayList<>();
        if (range.equals("today")) {
            billingDetails = billRepo.findSalesNDays(username, LocalDateTime.now().toLocalDate().atTime(LocalTime.MIN), LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX));
        }
        if (range.equals("lastWeek")) {
            billingDetails = billRepo.findSalesNDays(username, LocalDateTime.now().minusWeeks(1), LocalDateTime.now());
        }
        if (range.equals("lastMonth")) {
            billingDetails = billRepo.findSalesNDays(username, LocalDateTime.now().minusMonths(1), LocalDateTime.now());
        }
        if (range.equals("lastYear")) {
            billingDetails = billRepo.findSalesNDays(username, LocalDateTime.now().minusYears(1), LocalDateTime.now());
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
        log.info("response for the goals-->" + response);

        return response;
    }

    @Cacheable(value = "topSellings", keyGenerator = "userScopedKeyGenerator")
    public List<TopProductDto> getTopProducts(int count, String timeRange, String factor) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = LocalDateTime.now();

        List<ProductPerformanceProjection> topProducts = new ArrayList<>();
        List<TopProductDto> response = new ArrayList<>();
        if (timeRange.equals("lastWeek")) {
            startDate = LocalDateTime.now().minusDays(7);
        }
        if (timeRange.equals("lastMonth")) {
            startDate = LocalDateTime.now().minusMonths(1);
        }
        if (timeRange.equals("lastYear")) {
            startDate = LocalDateTime.now().minusYears(1);
        }
        if (timeRange.equals("today")) {
            startDate = LocalDateTime.now().toLocalDate().atTime(LocalTime.MIN);
            endDate = LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX);

        }


        if (factor.equals("mostSelling")) {
            topProducts = prodSalesRepo.findMostSellingProducts(extractUsername(), startDate, endDate, count);
        }
        if (factor.equals("topGrossing")) {
            topProducts = prodSalesRepo.findTopGrossingProducts(extractUsername(), startDate, endDate, count);
        }

        if (topProducts.size() > 0) {
            response = topProducts.stream().map(obj -> {
                return TopProductDto.builder().category(obj.getCategory()).currentStock(obj.getCurrentStock())
                        .productName(obj.getProductName()).amount(obj.getRevenue()).count(obj.getUnitsSold()).build();
            }).collect(Collectors.toList());
        }


        log.info("The top products are " + response);


        return response;
    }

    @Cacheable(value = "topOrders", keyGenerator = "userScopedKeyGenerator")
    public List<TopOrdersDto> getTopOrders(int count, String timeRange) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = LocalDateTime.now();

        List<ProductPerformanceProjection> topProducts = new ArrayList<>();
        List<TopOrdersDto> response = new ArrayList<>();
        if (timeRange.equals("lastWeek")) {
            startDate = LocalDateTime.now().minusDays(7);
        }
        if (timeRange.equals("lastMonth")) {
            startDate = LocalDateTime.now().minusMonths(1);
        }
        if (timeRange.equals("lastYear")) {
            startDate = LocalDateTime.now().minusYears(1);
        }
        if (timeRange.equals("today")) {
            startDate = LocalDateTime.now().toLocalDate().atStartOfDay();
        }

        List<BillingEntity> billList = billRepo.findTopNSalesForGivenRange(extractUsername(), startDate, endDate, count);

        if (billList.size() > 0) {
            response = billList.stream().map(obj -> {
                String customerName = shopRepo.findByIdAndUserId(obj.getCustomerId(), extractUsername()).getName();
                String paymentStatus = salesPaymentRepo.findPaymentDetails(obj.getId(), extractUsername()).getStatus();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                String date = obj.getCreatedDate().format(formatter);

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
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = LocalDateTime.now();

        List<ProductPerformanceProjection> topProducts = new ArrayList<>();
        List<TopOrdersDto> response = new ArrayList<>();
        if (timeRange.equals("lastWeek")) {
            startDate = LocalDateTime.now().minusDays(7);
        }
        if (timeRange.equals("lastMonth")) {
            startDate = LocalDateTime.now().minusMonths(1);
        }
        if (timeRange.equals("lastYear")) {
            startDate = LocalDateTime.now().minusYears(1);
        }
        if (timeRange.equals("today")) {
            startDate = LocalDateTime.now().toLocalDate().atStartOfDay();
        }
        List<Map<String, Object>> rawData = salesPaymentRepo.getPaymentBreakdown(extractUsername(), startDate, endDate);


        Map<String, Double> result = new HashMap<>();
        for (Map<String, Object> row : rawData) {
            String method = (String) row.get("paymentMethod");
            Number count = (Number) row.get("count");
            result.put(method.toLowerCase(), count.doubleValue());


        }

        return result;
    }

    @Cacheable(value = "paymentBreakdowns", keyGenerator = "userScopedKeyGenerator")
    public Map<String, Double> getPaymentStatusBreakdown(String timeRange) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = LocalDateTime.now();

        List<ProductPerformanceProjection> topProducts = new ArrayList<>();
        List<TopOrdersDto> response = new ArrayList<>();
        if (timeRange.equals("lastWeek")) {
            startDate = LocalDateTime.now().minusDays(7);
        }
        if (timeRange.equals("lastMonth")) {
            startDate = LocalDateTime.now().minusMonths(1);
        }
        if (timeRange.equals("lastYear")) {
            startDate = LocalDateTime.now().minusYears(1);
        }
        if (timeRange.equals("today")) {
            startDate = LocalDateTime.now().toLocalDate().atStartOfDay();
        }
        List<Map<String, Object>> rawData = salesPaymentRepo.getPaymentStatusBreakdown(extractUsername(), startDate, endDate);

        log.info("The raw payment status breakdown data is " + rawData);

        Map<String, Double> result = new HashMap<>();
        for (Map<String, Object> row : rawData) {
            Number totalPaid = (Number) row.get("totalPaid");
            Number totalDue = (Number) row.get("totalDue");
            result.put("Paid", totalPaid.doubleValue());
            result.put("Due", totalDue.doubleValue());
        }

        return result;
    }


    /*public String updateShopLogo(MultipartFile shopLogo) throws IOException {

        String username = extractUsername();
        log.info("entered saveEditableUserProfilePic with  username " + username);

        String keyName = shopLogo.getOriginalFilename();

        s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(keyName)
                        .contentType(shopLogo.getContentType()).build(),
                software.amazon.awssdk.core.sync.RequestBody.fromBytes(shopLogo.getBytes()));

        // UserInfo userinfo = userinfoRepo.findById(Integer.parseInt(id)).get();
        UserProfilePicEntity picRes = userProfilePicRepo.findByUsername(username);
        if (picRes != null) {
            picRes.setProfilePic(picRes.getProfilePic());
            picRes.setShopLogo(keyName);
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
    }*/

    public String updateShopLogoOracle(MultipartFile shopLogo) throws IOException {

        String username = extractUsername();
        log.info("entered updateShopLogo with username " + username);

        // 1. Create a unique filename (e.g., "junaid_logo_16789..._logo.png")
        String originalFilename = shopLogo.getOriginalFilename();
        String safeFilename = username + "_logo_" + System.currentTimeMillis() + "_" + originalFilename;

        // 2. Ensure the directory exists on the server
        Path uploadPath = Paths.get(LOGO_UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 3. Save the file to the Ubuntu hard drive
        Path filePath = uploadPath.resolve(safeFilename);
        Files.copy(shopLogo.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 4. Update the Database
        UserProfilePicEntity picRes = userProfilePicRepo.findByUsername(username);
        if (picRes != null) {
            picRes.setShopLogo(safeFilename);
            picRes.setUpdated_date(LocalDateTime.now());
            userProfilePicRepo.save(picRes);
        } else {
            UserProfilePicEntity picResNew = new UserProfilePicEntity();
            picResNew.setUsername(username);

            // BUG FIX: Your original code accidentally saved this to setProfilePic!
            // Changed it to setShopLogo to ensure it goes into the correct column.
            picResNew.setShopLogo(safeFilename);

            picResNew.setUpdated_date(LocalDateTime.now());
            userProfilePicRepo.save(picResNew);
        }

        return "ok";
    }

    private String safe(String value) {
        return value != null ? value.trim() : "";
    }

    @Transactional
    public String updateBasicDetails(ShopBasicDetailsRequest request) {

        log.info("ShopBasicDetailsRequest with request->" + request);

        shopBasicRepo.removeExistingBasicDetails(extractUsername());

        var shopEntity = ShopBasicEntity.builder()
                .shopName(safe(request.getShopName()))
                .shopSlogan(safe(request.getShopSlogan()))
                .shopPhone(safe(request.getShopPhone()))
                .shopEmail(safe(request.getShopEmail()))
                .address(safe(request.getShopAddress()))
                .shopPincode(safe(request.getShopPincode()))
                .shopCity(safe(request.getShopCity()))
                .shopState(safe(request.getShopState()))
                .userId(extractUsername())
                .updatedBy(extractUsername())
                .updatedAt(LocalDateTime.now())
                .build();


        ShopBasicEntity res = shopBasicRepo.save(shopEntity);
        if (res != null) {
            var shopFinanceEntity = ShopFinanceEntity.builder()
                    .gstin(safe(request.getGstin()))
                    .panNumber(safe(request.getPanNumber()))
                    .userId(extractUsername())
                    .updatedBy(extractUsername())
                    .updatedAt(LocalDateTime.now())
                    .build();
            shopFinanceRepo.removeShopFinanceEntities(extractUsername());
            ShopFinanceEntity finRes = shopFinanceRepo.save(shopFinanceEntity);
        }

        return res != null ? "Success" : "Not Successful";
    }


    @Transactional
    public String updateFinanceDetails(ShopFinanceDetailsRequest request) {
        var shopFinanceEntity = ShopFinanceEntity.builder()
                .gstin(safe(request.getGstin()))
                .panNumber(safe(request.getPan()))
                .userId(extractUsername())
                .updatedBy(extractUsername())
                .updatedAt(LocalDateTime.now())
                .build();

        shopFinanceRepo.removeShopFinanceEntities(extractUsername());
        ShopFinanceEntity finRes = shopFinanceRepo.save(shopFinanceEntity);

        if (finRes != null) {
            // Bank Details
            ShopBankEntity shopBankEntity = ShopBankEntity.builder()
                    .accountHolderName(safe(request.getBankHolder()))
                    .accountNumber(safe(request.getBankAccount()))
                    .ifscCode(safe(request.getBankIfsc()))
                    .bankName(safe(request.getBankName()))
                    .branchName(safe(request.getBankAddress()))
                    .shopFinanceId(finRes.getId())
                    .userId(extractUsername())
                    .updatedBy(extractUsername())
                    .updatedAt(LocalDateTime.now())
                    .build();
            shopBankRepo.removeBankDetails(extractUsername());
            shopBankRepo.save(shopBankEntity);

            // UPI Details
            ShopUPIEntity shopUPIEntity = ShopUPIEntity.builder()
                    .upiId(safe(request.getUpi()))
                    .upiProvider("google")
                    .shopFinanceId(finRes.getId())
                    .userId(extractUsername())
                    .updatedBy(extractUsername())
                    .updatedAt(LocalDateTime.now())
                    .build();
            salesUPIRepo.removeUpiId(extractUsername());
            salesUPIRepo.save(shopUPIEntity);

            return "Success";
        }

        return "Not Successful";
    }


    public String updateOtherDetails(ShopInvoiceTerms request) {
        var shopInvoiceTermsEntity = ShopInvoiceTermsEnity.builder()
                .term(safe(request.getTerms1()))
                .userId(extractUsername())
                .updatedBy(extractUsername())
                .updatedAt(LocalDateTime.now())
                .build();

        ShopInvoiceTermsEnity res = shopInvoiceTermsRepo.save(shopInvoiceTermsEntity);
        return res != null ? "Success" : "Not Successful";
    }


   /* public byte[] getShopLogo(String username) throws IOException {

        log.info("entered getProfilePic with request  username " + username);

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
    }*/


    public byte[] getShopLogoOracle(String username) throws IOException {

        // Fixed the print statement typo (was saying getProfilePic)
        log.info("Entered getShopLogo with request username " + username);

        // Safer retrieval to prevent server crashes on bad usernames
        UserInfo res = userinfoRepo.findByUsername(username).orElseThrow(() ->
                new RuntimeException("User not found: " + username));

        byte[] content = null;

        try {
            UserProfilePicEntity picRes = userProfilePicRepo.findByUsername(username);

            // Crucial null checks before trying to read from the hard drive
            if (picRes != null && picRes.getShopLogo() != null) {

                // Combine the directory path with the saved logo filename
                Path logoPath = Paths.get(LOGO_UPLOAD_DIR, picRes.getShopLogo());

                // Only attempt to read if the file physically exists on the server
                if (Files.exists(logoPath)) {
                    content = Files.readAllBytes(logoPath);
                } else {
                    log.info("Shop logo file not found on server: " + logoPath.toString());
                }
            } else {
                log.info("No shop logo assigned for user: " + username);
            }

        } catch (IOException e) {
            System.err.println("Error reading local shop logo: " + e.getMessage());
            e.printStackTrace();
        }

        return content;
    }

    public List<ProductSearchDto> findProductsByQuery(String query, int limit) {

        // Create Sort object based on direction and sort field

        List<ProductSearchDto> response = new ArrayList<>();

        String username = extractUsername();

        UserSettingsEntity userSettingsEntity = userSettingsRepo.findByUsername(extractUsername());
        Integer stockCount = 0;
        if (userSettingsEntity.getAllowNoStockBilling())
            stockCount = -99999;


        List<ProductEntity> productList = prodRepo.findAllActiveProductsForGSTBilling(Boolean.TRUE, username, query, limit, stockCount);


        productList.stream().forEach(obj -> {
            var prodSearch = ProductSearchDto.builder()
                    .id(Long.valueOf(obj.getId()))
                    .name(obj.getName())
                    .hsn(obj.getHsn())
                    .price(BigDecimal.valueOf(obj.getPrice()))
                    .costPrice(BigDecimal.valueOf(obj.getCostPrice()))
                    .tax(obj.getTaxPercent())
                    .stock(obj.getStock())
                    .build();
            response.add(prodSearch);

        });

        log.info("The result list for the query " + query + " is " + response);


        return response;
    }

    public Map<String, String> sendPaymentReminder(Map<String, Object> request) {

        String orderNo = (String) request.get("orderId");

        BillingEntity billDetails = billRepo.findByInvoiceNumber(orderNo);

        CustomerEntity customer = shopRepo.findByIdAndUserId(billDetails.getCustomerId(), extractUsername(orderNo));

        Double totalAmount = billDetails.getTotalAmount();
        Double paidAmount = billDetails.getPayingAmount();
        Double dueAmout = billDetails.getRemainingAmount();

        String customerName = customer.getName();
        String customerEmail = customer.getEmail();
        String message = (String) request.get("message");

        String htmlTemplate = emailTemplate.getPaymentReminderEmailContent(orderNo, totalAmount, paidAmount, dueAmout, customerName, message);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");

        ShopBasicEntity shopBasic = shopBasicRepo.findByUserId(extractUsername(orderNo));

        try {
            CompletableFuture<String> futureResult = email.sendEmailForPaymentReminder(customerEmail, orderNo, customerName, htmlTemplate, shopBasic.getShopName());

            billRepo.updateReminderCount(orderNo, extractUsername(orderNo), LocalDateTime.now());
            salesPaymentRepo.updateReminderCount(orderNo, extractUsername(orderNo), LocalDateTime.now());

            var reminderCounter = ReminderCounter.builder().createdBy(extractUsername())
                    .method((String) request.get("method"))
                    .username(extractUsername())
                    .invoiceId((String) request.get("orderId"))
                    .message((String) request.get("message"))
                    .createdDate(LocalDateTime.now())
                    .build();

            ReminderCounter savedCounter = reminderCounterRepo.save(reminderCounter);
            salesCacheService.evictUserSales(extractUsername(orderNo));
            salesCacheService.evictUserPayments(extractUsername(orderNo));

        } catch (MailjetException e) {
            throw new RuntimeException(e);
        } catch (MailjetSocketTimeoutException e) {
            throw new RuntimeException(e);
        }

        return null;
    }


    public List<ReminderCounter> getPaymentReminderLists(String invoiceId) {
        List<ReminderCounter> paymentReminders = reminderCounterRepo.findByInvoiceIdAndUsername(invoiceId, extractUsername());
        return paymentReminders;
    }


    @Transactional
    public Map<String, Object> updateDuePayments(Map<String, Object> request) {

        String orderNo = (String) request.get("invoiceId");
        Double amount = Double.parseDouble((String) request.get("amount"));


        try {
            billRepo.updateDuePayment(orderNo, extractUsername(), LocalDateTime.now(), amount);
            salesPaymentRepo.updateDueAmount(orderNo, extractUsername(), LocalDateTime.now(), amount);
            salesCacheService.evictUserSales(extractUsername());
            salesCacheService.evictUserPayments(extractUsername());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        PaymentEntity paymentDetails = salesPaymentRepo.findByOrderNumber(orderNo);

        try {
            String status = "SemiPaid";
            if (paymentDetails.getToBePaid() <= 0) {
                status = "Paid";
            }
            salesPaymentRepo.updatePaymentStatus(orderNo, extractUsername(), status);

            utils.asyncSavePaymentHistory(paymentDetails.getBillingId(), paymentDetails.getId(), amount, orderNo);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Map<String, Object> response = new HashMap<>();

        response.put("paid", paymentDetails.getPaid());
        response.put("due", paymentDetails.getToBePaid());
        response.put("status", paymentDetails.getStatus());

        log.info("The response updateDuePayments is " + response);


        return response;
    }

    public List<Map<String, Object>> getPaymentHistory(Map<String, Object> request) {

        String orderNo = (String) request.get("orderNumber");
        String paymentRefereceNumber = (String) request.get("PaymentReferenceNumber");

        List<Map<String, Object>> response = new ArrayList<>();

        List<PaymentHistory> historyList = utils.getPaymentHistory(orderNo);


        historyList.stream().forEach(obj -> {
            Map<String, Object> historyMap = new HashMap<>();
            historyMap.put("date", obj.getUpdatedDate());
            historyMap.put("amount", obj.getPaidAmount());
            historyMap.put("tokenNumber", obj.getTokenNo());
            response.add(historyMap);
        });

        log.info("The response getPaymentHistory is " + response);
        return response;
    }


    public Map<String, Object> sendInvoiceOverEmailByListner(String invoiceNumber) {
        InvoiceDetails order = getOrderDetails(invoiceNumber);
        Map<String, Object> response = new HashMap<>();
        try {

            Map<String, Object> body = new HashMap<>();
            body.put("invoice_number", invoiceNumber);

            sqsUtil.sendOrderDetailsJustAfterOrderCompletion("send-invoice-email-queue", "SQS", body);

            // }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        response.put("errorData", "success");
        return response;
    }

    public Map<String, Object> sendInvoiceOverEmail(String invoiceNumber) {
        log.info("Entered sending email by listner with refrenece Number " + invoiceNumber);
        InvoiceDetails order = getOrderDetails(invoiceNumber);
        Map<String, Object> response = new HashMap<>();
        try {
            String username = "";
            if (invoiceNumber != null) {
                BillingEntity billDetails = billRepo.findOrderByJustReference(invoiceNumber);
                username = billDetails.getUserId();

                sendInvoiceOverEmail(BillingEntity.builder().invoiceNumber(invoiceNumber).build());
                response.put("errorData", "success");
                return response;
            }


            Map<String, Object> emailContent = emailTemplate.generateOrderHtml(order, username);

            //if (Arrays.asList(environment.getActiveProfiles()).contains("prod")||Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
            CompletableFuture<String> futureResult = email.sendEmail(order.getCustomerEmail(),
                    invoiceNumber, order.getCustomerName(),
                    generateGSTInvoicePdf(invoiceNumber, extractUsername()), (String) emailContent.get("htmlTemplate"), (String) emailContent.get("shopName"));
            log.info(String.valueOf(futureResult));


            // }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        response.put("errorData", "success");
        return response;
    }

    public List<Map<String, Object>> globalSearch(String globalSearchTerms, Integer limit) {


        List<GlobalSearchIndex> searchList = globalSearchRepo.findActiveEntities(extractUsername(), globalSearchTerms, Boolean.TRUE);


        List<Map<String, Object>> response = searchList.stream().map(obj -> {
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("id", obj.getSourceId());
            responseMap.put("displayName", obj.getDisplayName());
            responseMap.put("sourceType", obj.getSourceType());
            return responseMap;

        }).collect(Collectors.toList());

        log.info("The global search response is " + response);

        return response;
    }

    public String clearServerSideCache() {
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
            salesCacheService.evictsReportsCache(extractUsername());
            return "success";
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();


        }
        return "not success";
    }

    public Map<String, Integer> getOrderCountForDay() {

        Map<String, Integer> response = new HashMap<>();
        String username = extractUsername();
        try {
            Integer totalOrders = billRepo.countOrdersForToday(username, LocalDateTime.now().toLocalDate().atStartOfDay(), LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX));

            response.put("count", totalOrders);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return response;
    }


    public Map<String, Integer> addSubscriptions() {

        userinfoRepo.updateUserRole(extractUsername(), "ROLE_PREMIUM");

        return null;
    }

    @Cacheable(value = "analytics", keyGenerator = "userScopedKeyGenerator")
    public AnalyticsRes getSuperAnalytics(AnalyticsRequest request) {
        String userId = extractUsername();
        YearMonth startYm = YearMonth.parse(request.getStartDate());
        YearMonth endYm = YearMonth.parse(request.getEndDate());

        LocalDateTime startDate = startYm.atDay(1).atStartOfDay();
        LocalDateTime endDate = endYm.atEndOfMonth().atTime(LocalTime.MAX);

        AnalyticsRes superResponse = new AnalyticsRes();

        // --- 1 & 2. Combined Payment and Invoice Status ---
        List<Object[]> combinedResults = salesPaymentRepo.getCombinedPaymentSummary(startDate, endDate, userId);
        List<PieAnalyticsMap> paymentStatusList = new ArrayList<>();
        List<PieAnalyticsMap> invoiceStatusList = new ArrayList<>();

        if (combinedResults != null) {
            for (Object[] row : combinedResults) {
                if (row == null || row[0] == null) continue;
                String statusName = row[0].toString();

                double paymentValue = (row[1] != null) ? ((Number) row[1]).doubleValue() : 0.0;
                PieAnalyticsMap paymentMap = new PieAnalyticsMap();
                paymentMap.setName(statusName);
                paymentMap.setValue(paymentValue);
                paymentStatusList.add(paymentMap);

                double invoiceCount = (row[2] != null) ? ((Number) row[2]).doubleValue() : 0.0;
                PieAnalyticsMap invoiceMap = new PieAnalyticsMap();
                invoiceMap.setName(statusName);
                invoiceMap.setValue(invoiceCount);
                invoiceStatusList.add(invoiceMap);
            }
        }
        superResponse.setPaymentStatus(paymentStatusList);
        superResponse.setInvoiceStatus(invoiceStatusList);

        // --- 3. Monthly Profits, Revenue, Stock, Sales AND AOV ---
        List<Object[]> billingResults = billRepo.getMonthlyBillingSummary(startDate, endDate, userId);
        List<PieAnalyticsMap> monthlyProfitsList = new ArrayList<>();
        List<PieAnalyticsMap> monthlyRevenueList = new ArrayList<>();
        List<PieAnalyticsMap> monthlyStockList = new ArrayList<>();
        List<PieAnalyticsMap> monthlySalesList = new ArrayList<>();

        // 🟢 NEW: List to hold AOV Data
        List<Map<String, Object>> aovDataList = new ArrayList<>();

        double totalProfit = 0.0, totalRevenue = 0.0;
        Double totalStockSold = 0d, totalSales = 0d;

        if (billingResults != null) {
            for (Object[] row : billingResults) {
                if (row == null || row[0] == null) continue;
                String month = row[0].toString();

                double profitValue = (row[1] != null) ? ((Number) row[1]).doubleValue() : 0.0;
                PieAnalyticsMap profitMap = new PieAnalyticsMap();
                profitMap.setName(month);
                profitMap.setValue(profitValue);
                monthlyProfitsList.add(profitMap);
                totalProfit += profitValue;

                double revenueValue = (row[2] != null) ? ((Number) row[2]).doubleValue() : 0.0;
                PieAnalyticsMap revenueMap = new PieAnalyticsMap();
                revenueMap.setName(month);
                revenueMap.setValue(revenueValue);
                monthlyRevenueList.add(revenueMap);
                totalRevenue += revenueValue;

                double stockValue = (row[3] != null) ? ((Number) row[3]).doubleValue() : 0.0;
                PieAnalyticsMap stockMap = new PieAnalyticsMap();
                stockMap.setName(month);
                stockMap.setValue(stockValue);
                monthlyStockList.add(stockMap);
                totalStockSold += stockValue;

                double salesCountValue = (row[4] != null) ? ((Number) row[4]).doubleValue() : 0.0;
                PieAnalyticsMap salesMap = new PieAnalyticsMap();
                salesMap.setName(month);
                salesMap.setValue(salesCountValue);
                monthlySalesList.add(salesMap);
                totalSales += salesCountValue;

                // 🟢 NEW: Calculate Average Order Value
                double aov = salesCountValue > 0 ? Math.round(revenueValue / salesCountValue) : 0.0;
                Map<String, Object> aovMap = new HashMap<>();
                aovMap.put("name", month);
                aovMap.put("orders", salesCountValue);
                aovMap.put("aov", aov);
                aovDataList.add(aovMap);
            }
        }
        superResponse.setMonthlyProfits(monthlyProfitsList);
        superResponse.setTotalProfit(totalProfit);
        superResponse.setSalesAndRevenue(monthlyRevenueList);
        superResponse.setTotalRevenue(totalRevenue);
        superResponse.setMonthlyStockSold(monthlyStockList);
        superResponse.setTotalStockSold(totalStockSold);
        superResponse.setMonthlySales(monthlySalesList);
        superResponse.setTotalSales(totalSales);

        // 🟢 Set AOV Data
        superResponse.setAovData(aovDataList);

        // --- 4. Monthly New Customers ---
        List<Object[]> newCustomerResults = shopRepo.getMonthlyCustomerCount(startDate, endDate, userId);
        List<PieAnalyticsMap> monthlyNewCustomers = new ArrayList<>();
        if (newCustomerResults != null) {
            for (Object[] row : newCustomerResults) {
                if (row == null || row[0] == null) continue;
                PieAnalyticsMap map = new PieAnalyticsMap();
                map.setName(row[0].toString());
                map.setValue(((Number) row[1]).doubleValue());
                monthlyNewCustomers.add(map);
            }
        }
        superResponse.setMonthlyNewCustomers(monthlyNewCustomers);

        // --- 5. Peak Hours ---
        List<Object[]> peakHourResults = billRepo.getPeakPurchaseHours(startDate, endDate, userId);
        List<PieAnalyticsMap> peakHours = new ArrayList<>();
        if (peakHourResults != null) {
            for (Object[] row : peakHourResults) {
                if (row == null || row[0] == null) continue;
                PieAnalyticsMap map = new PieAnalyticsMap();
                map.setName(row[0].toString() + ":00");
                map.setValue(((Number) row[1]).doubleValue());
                peakHours.add(map);
            }
        }
        superResponse.setPeakHours(peakHours);

        // --- 6. Category Revenue ---
        List<Object[]> categoryResults = prodSalesRepo.getRevenueByCategory(startDate, endDate, userId);
        List<PieAnalyticsMap> categoryRevenue = new ArrayList<>();
        if (categoryResults != null) {
            for (Object[] row : categoryResults) {
                if (row == null || row[0] == null) continue;
                PieAnalyticsMap map = new PieAnalyticsMap();
                map.setName(row[0].toString());
                map.setValue(((Number) row[1]).doubleValue());
                categoryRevenue.add(map);
            }
        }
        superResponse.setCategoryRevenue(categoryRevenue);

        // --- 7. Payment Methods ---
        List<Object[]> paymentMethodResults = salesPaymentRepo.getPaymentMethodSummary(startDate, endDate, userId);
        List<PieAnalyticsMap> paymentMethods = new ArrayList<>();
        if (paymentMethodResults != null) {
            for (Object[] row : paymentMethodResults) {
                if (row == null || row[0] == null) continue;
                PieAnalyticsMap map = new PieAnalyticsMap();
                map.setName(row[0].toString());
                map.setValue(((Number) row[1]).doubleValue());
                paymentMethods.add(map);
            }
        }
        superResponse.setPaymentMethods(paymentMethods);

        // --- 8. Top Sold Products ---
        Integer n = 6;
        List<Object[]> topProductsResults = prodSalesRepo.getTopSoldProducts(startDate, endDate, userId, n);
        List<PieAnalyticsMap> topProductsList = new ArrayList<>();
        if (topProductsResults != null) {
            for (Object[] row : topProductsResults) {
                if (row == null || row[0] == null) continue;
                PieAnalyticsMap pieMap = new PieAnalyticsMap();
                pieMap.setName(row[0].toString());
                pieMap.setValue(((Number) row[1]).doubleValue());
                topProductsList.add(pieMap);
            }
        }
        superResponse.setTopProducts(topProductsList);

        // --- 9. Customer GST Summary ---
        List<Object[]> gstResults = shopRepo.getCustomerGstSummary(startDate, endDate, userId);
        List<PieAnalyticsMap> customerGstList = new ArrayList<>();
        if (gstResults != null) {
            for (Object[] row : gstResults) {
                if (row == null || row[0] == null) continue;
                PieAnalyticsMap pieMap = new PieAnalyticsMap();
                pieMap.setName(row[0].toString());
                pieMap.setValue(((Number) row[1]).doubleValue());
                customerGstList.add(pieMap);
            }
        }
        superResponse.setCustomerGst(customerGstList);

        // --- 10. 🟢 NEW: Customer Retention Logic ---
        List<Object[]> retentionResults = shopRepo.getCustomerRetentionSummary(startDate, endDate, userId);
        List<Map<String, Object>> retentionList = new ArrayList<>();

        // 10a. Add the transparent outer target ring required by Recharts
        Map<String, Object> targetMap = new HashMap<>();
        targetMap.put("name", "Total Target");
        targetMap.put("value", 100.0);
        targetMap.put("fill", "transparent");
        retentionList.add(targetMap);

        double totalCustomers = 0;
        double returningCount = 0;
        double newCount = 0;

        if (retentionResults != null) {
            for (Object[] row : retentionResults) {
                if (row == null || row[0] == null) continue;
                String type = row[0].toString();
                double count = ((Number) row[1]).doubleValue();

                totalCustomers += count;
                if ("Returning".equalsIgnoreCase(type)) {
                    returningCount = count;
                } else {
                    newCount = count;
                }
            }
        }

        // 10b. Calculate percentages and format the Maps
        double returningPct = totalCustomers > 0 ? Math.round((returningCount / totalCustomers) * 100.0) : 0.0;
        double newPct = totalCustomers > 0 ? Math.round((newCount / totalCustomers) * 100.0) : 0.0;

        Map<String, Object> returningMap = new HashMap<>();
        returningMap.put("name", "Returning");
        returningMap.put("value", returningPct);
        returningMap.put("fill", "var(--primary-color)");
        retentionList.add(returningMap);

        Map<String, Object> newMap = new HashMap<>();
        newMap.put("name", "New");
        newMap.put("value", newPct);
        newMap.put("fill", "#71a894"); // Matches the warning color in your frontend Theme
        retentionList.add(newMap);

        superResponse.setRetentionData(retentionList);

        log.info("SuperAnalytics processing complete for user: {}", userId);
        return superResponse;
    }

    public void sendReportEmail(MultipartFile file, String subject, List<String> emailList) {


        try {

            byte[] fileBytes = file.getBytes();


            emailList.stream().forEach(emailObj -> {
                String template = emailTemplate.getReportEmailContent("Sir", file.getOriginalFilename(), "monthly");
                CompletableFuture<String> futureResult = null;
                try {
                    futureResult = email.sendEmailReportWithAttachment(emailObj,
                            subject, file.getOriginalFilename(),
                            fileBytes, template, "Clear Bill");
                } catch (MailjetException e) {
                    throw new RuntimeException(e);
                } catch (MailjetSocketTimeoutException e) {
                    throw new RuntimeException(e);
                }
                log.info(String.valueOf(futureResult));

            });

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }
    }

    public InvoiceData getFullInvoiceDetails(String invoiceId) {

        return utils.getFullInvoiceDetails(extractUsername(), invoiceId);
    }

    // @Async("geminiAsync")
    public String extractTextFromImage(MultipartFile file) throws IOException {

        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        List<GeminiTextExtract> apiLogs = apiSaveRepo.findCreatedWithinLast24Hours(last24Hours, extractUsername(), "Gemini Text Extraction API");
        Integer count = apiLogs.size();


        if (extractRole().equals("USER")) {
            if (count > 2) {

                return "Sorry, you have exceeded the free usage limit for text extraction. Please upgrade to Premium for unlimited access.";
            }

        }

        String base64Image = Base64.getEncoder().encodeToString(file.getBytes());

        String mimeType = file.getContentType(); // e.g., "image/jpeg" or "image/png"

        if (mimeType == null || !mimeType.startsWith("image/")) {
            mimeType = "image/jpeg"; // Fallback
        }

        String response = geminiCalls.geminiApiCall(base64Image, mimeType);

        log.info("The response of textExtraction is " + response);


        return response;
    }

    public List<String> getCategories() {

        List<String> response = new ArrayList<>();

        List<ProductCategory> productCategoryList = prodCatRepo.getCategoryNamesList(extractUsername());

        if (productCategoryList != null && productCategoryList.size() != 0) {
            response = productCategoryList.stream().map(i -> i.getCategoryName()).collect(Collectors.toList());
            return response;
        }
        response.add("Products");
        response.add("Services");

        return response;
    }

    public SaveCategoryDto saveCategories(Map<String, List> request) {

        List<ProductCategory> productCategoryList = prodCatRepo.getCategoryNamesList(extractUsername());

        List<String> existingList = productCategoryList.stream().map(i -> i.getCategoryName().replaceAll("[^a-zA-Z0-9]", "")).collect(Collectors.toList());

        log.info("The existing category list is " + existingList);

        List<String> newCategories = request.get("categories");

        List<String> categoriesToAdd = newCategories.stream().filter(i -> !(existingList.contains(i.replaceAll("[^a-zA-Z0-9]", "")))).collect(Collectors.toList());

        log.info("The categoriesToAdd  list is " + categoriesToAdd);

        List<ProductCategory> categoryEntitiesToAdd = categoriesToAdd.stream().map(i -> ProductCategory.builder().categoryName(i).type("product").username(extractUsername()).updatedBy(extractUsername()).updateDate(LocalDateTime.now()).build()).collect(Collectors.toList());

        try {
            categoryEntitiesToAdd.stream().forEach(i -> {
                prodCatRepo.save(i);

            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<String> listCategories = getCategories();

        Boolean success = Boolean.TRUE;
        String message = "Categories added successfully";

        var response = SaveCategoryDto.builder().categories(listCategories).success(success).message(message).build();

        return response;
    }
}
