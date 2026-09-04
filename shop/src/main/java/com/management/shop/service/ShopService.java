package com.management.shop.service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
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
import org.springframework.http.MediaType;
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

import javax.imageio.ImageIO;

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
    private ProductImageRepository productImageRepo;

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
    private Optional<SQSUtil> sqsUtil;

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

    @Autowired
    InvoiceSequenceRepository invoiceSeqRepo;

    private final Random random = new Random();


    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${upload.profile.dir}")
    private  String UPLOAD_DIR;

    @Value("${upload.logo.dir}")
    private  String LOGO_UPLOAD_DIR;

    @Value("${upload.logo.dir}")
    private  String SIGN_UPLOAD_DIR;

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
                    .phone(request.getPhone()).status("ACTIVE").totalSpent(existingCustomer.get(0).getTotalSpent()).totalOrders(existingCustomer.get(0).getTotalOrders()).build();

            ent = shopRepo.save(customerEntity);

        } else {

            var customerEntity = CustomerEntity.builder().userId(extractUsername()).name(request.getName()).email(request.getEmail())
                    .createdDate(LocalDateTime.now())
                    .state(request.getCustomerState())
                    .gstNumber(request.getGstNumber())
                    .city(request.getCity())
                    .isActive(Boolean.TRUE)
                    .phone(request.getPhone()).status("ACTIVE").totalOrders(0).totalSpent(0d).build();

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

    public CustomerSuccessDTO saveCustomerInternal(CustomerRequest request) {
        log.info("entered into saveCustomer with" + request.toString());


        CustomerEntity ent = null;


        var customerEntity = CustomerEntity.builder().userId(request.getUsername()).name(request.getName()).email(request.getEmail())
                .createdDate(LocalDateTime.now())
                .state(request.getCustomerState())
                .gstNumber(request.getGstNumber())
                .city(request.getCity())
                .isActive(Boolean.TRUE)
                .phone(request.getPhone()).status("ACTIVE").totalSpent(0d).build();

        ent = shopRepo.save(customerEntity);


        salesCacheService.evictUserCustomers(request.getUsername());
        salesCacheService.evictsUserAnalytics(request.getUsername());
        if (ent.getId() != null) {
            try {
                salesCacheService.evictUserCustomers(request.getUsername());
                salesCacheService.evictsUserAnalytics(request.getUsername());
                salesCacheService.evictsReportsCache(request.getUsername());

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
                    .createdDate(LocalDateTime.now()).phone(request.getPhone()).status("ACTIVE").isActive(Boolean.TRUE).totalSpent(existingCustomer.get(0).getTotalSpent()).totalOrders(existingCustomer.get(0).getTotalOrders()).build();

            ent = shopRepo.save(customerEntity);

        } else {

            var customerEntity = CustomerEntity.builder().name(request.getName()).userId(extractUsername()).email(request.getEmail())
                    .state(request.getCustomerState())
                    .gstNumber(request.getGstNumber())
                    .city(request.getCity())
                    .createdDate(LocalDateTime.now()).phone(request.getPhone()).status("ACTIVE").isActive(Boolean.TRUE).totalOrders(0).totalSpent(0d).build();

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
        resolveExistingProductId(request);
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
                    .location(request.getLocation() == null ? "" : request.getLocation())
                    .status(status)
                    .userId(extractUsername())
                    .stock(request.getStock())
                    .active(true)
                    .taxPercent(request.getTax())
                    .price(request.getPrice())
                    .costPrice((request.getCostPrice()==null||request.getCostPrice()==0?request.getPrice():request.getCostPrice()))
                    .hsn(request.getHsn() == null ? "" : request.getHsn())
                    .updatedDate(LocalDateTime.now())
                    .updatedBy(extractUsername())
                    .build();

        } else {
            productEntity = ProductEntity.builder()
                    .name(request.getName() == null ? "" : request.getName())
                    .userId(extractUsername())
                    .category(request.getCategory() == null ? "" : request.getCategory())
                    .location(request.getLocation() == null ? "" : request.getLocation())
                    .active(true)
                    .status(status)
                    .stock(request.getStock())
                    .taxPercent(request.getTax())
                    .costPrice((request.getCostPrice()==null||request.getCostPrice()==0?request.getPrice():request.getCostPrice()))
                    .price(request.getPrice())
                    .hsn(request.getHsn() == null ? "" : request.getHsn())
                    .createdDate(LocalDateTime.now())
                    .updatedDate(LocalDateTime.now())
                    .updatedBy(extractUsername())
                    .build();
        }


        ProductEntity ent = prodRepo.save(productEntity);
        if (ent.getId() != null) {

            request.setSelectedProductId(ent.getId());

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
    public ProductSuccessDTO saveProductInternal(ProductRequest request) {

        String status = "In Stock";
        if (request.getStock() < 0)
            status = "Out of Stock";

        log.info("The new request" + request.getTax());

        ProductEntity productEntity = null;


            productEntity = ProductEntity.builder()
                    .name(request.getName() == null ? "" : request.getName())
                    .userId(request.getUsername())
                    .category(request.getCategory() == null ? "" : request.getCategory())
                    .location(request.getLocation() == null ? "" : request.getLocation())
                    .active(true)
                    .status(status)
                    .stock(request.getStock())
                    .taxPercent(request.getTax())
                    .costPrice(request.getCostPrice())
                    .price(request.getPrice())
                    .hsn(request.getHsn() == null ? "" : request.getHsn())
                    .createdDate(LocalDateTime.now())
                    .updatedDate(LocalDateTime.now())
                    .updatedBy("SYSTEM")
                    .build();



        ProductEntity ent = prodRepo.save(productEntity);
        if (ent.getId() != null) {

            try {
                salesCacheService.evictUserProducts(request.getUsername());

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return ProductSuccessDTO.builder().success(true).product(request).build();


        }

        return ProductSuccessDTO.builder().success(false).product(request).build();

    }
    private void resolveExistingProductId(ProductRequest request) {
        // Only attempt to find an existing product if the ID is null or 0
        if (request.getSelectedProductId() == null || request.getSelectedProductId() == 0) {

            String username = extractUsername();
            String rawName = request.getName() == null ? "" : request.getName();
            String normalizedName = rawName.toLowerCase().replaceAll("\\s+", "");

            String status = "In Stock";
            if (request.getStock() < 0) {
                status = "Out of Stock";
            }

            // Query the database using the custom normalized query
            List<Optional<ProductEntity>> existingProductOpt = prodRepo.findByNormalizedNameAndUserIdAndStatus(
                    normalizedName, username, Boolean.TRUE
            );

            // If a match is found, attach the existing ID to the request object
            if (!existingProductOpt.isEmpty()) {
                request.setSelectedProductId(existingProductOpt.get(0).get().getId());
            }
        }
    }

    @Transactional
    public ProductSuccessDTO saveProductFromImage(ProductRequest request) {
        String username = extractUsername();
        String productName = request.getName().toLowerCase().replaceAll("\\s", ""); // Fixed regex: \\s instead of //s

        // 1. Try to find existing product by Name or ID
        ProductEntity productEntity = prodRepo.findByNameAndUserId(productName, username);

        // If not found by name, try by ID if provided
        if (productEntity == null && request.getSelectedProductId() != null && request.getSelectedProductId() != 0) {
            productEntity = prodRepo.findById(request.getSelectedProductId()).orElse(null);
        }

        if (productEntity != null) {
            // UPDATE CASE: Update the existing managed entity
            Integer newStock = productEntity.getStock() + request.getStock();
            productEntity.setStock(newStock);

            // Only update price if request price is not 0
            if (request.getPrice() != 0) {
                productEntity.setPrice(request.getPrice());
            }

            productEntity.setName(request.getName());
            productEntity.setCategory(request.getCategory());
            productEntity.setLocation(request.getLocation());
            productEntity.setTaxPercent(request.getTax());
            productEntity.setCostPrice(request.getCostPrice());
            productEntity.setActive(true);
            productEntity.setHsn(request.getHsn());
            productEntity.setStatus(newStock < 0 ? "Out of Stock" : "In Stock");
            productEntity.setUpdatedDate(LocalDateTime.now());
            productEntity.setUpdatedBy(username);
            // Note: No need to call builder. All changes to 'productEntity' are tracked.
        } else {
            // INSERT CASE: Create new entity
            productEntity = ProductEntity.builder()
                    .name(request.getName())
                    .userId(username)
                    .category(request.getCategory())
                    .location(request.getLocation())
                    .active(true)
                    .status(request.getStock() < 0 ? "Out of Stock" : "In Stock")
                    .stock(request.getStock())
                    .taxPercent(request.getTax())
                    .costPrice(request.getCostPrice())
                    .price(request.getPrice())
                    .hsn(request.getHsn())
                    .createdDate(LocalDateTime.now())
                    .updatedDate(LocalDateTime.now())
                    .updatedBy(username)
                    .build();
        }

        ProductEntity saved = prodRepo.save(productEntity);

        // Evict cache only after successful save
        salesCacheService.evictUserProducts(username);

        return ProductSuccessDTO.builder().success(true).product(request).build();
    }

    //@CacheEvict(value = "products", allEntries = true)
    public ProductSuccessDTO updateProduct(ProductRequest request) {

        String status = "In Stock";
        if (request.getStock() < 1)
            status = "Out of Stock";
        log.info("The updated request" + request.getTax());
        var productEntity = ProductEntity.builder().id(request.getSelectedProductId()).name(request.getName())
                .active(true).category(request.getCategory()).userId(extractUsername()).status(status).stock(request.getStock())
                .location(request.getLocation())
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

    public ProductImageEntity saveProductImage(Integer productId, MultipartFile image) throws IOException {
        final long maxProductImageBytes = 5L * 1024L;
        String username = extractUsername();
        ProductEntity product = prodRepo.findByIdAndUserId(productId, username);

        if (product == null || !Boolean.TRUE.equals(product.getActive())) {
            throw new IllegalArgumentException("Product not found");
        }
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Choose a product image");
        }
        if (image.getSize() > maxProductImageBytes) {
            throw new IllegalArgumentException("Product image must be 5 KB or smaller");
        }

        String contentType = Optional.ofNullable(image.getContentType()).orElse("").toLowerCase(Locale.ROOT);
        if (!Set.of(MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, "image/webp").contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG or WebP product images are supported");
        }

        ProductImageEntity productImage = productImageRepo.findByProductIdAndUserId(productId, username)
                .orElseGet(() -> ProductImageEntity.builder().productId(productId).userId(username).build());
        productImage.setContentType(contentType);
        productImage.setImageData(image.getBytes());
        return productImageRepo.save(productImage);
    }

    public Optional<ProductImageEntity> getProductImage(Integer productId) {
        return productImageRepo.findByProductIdAndUserId(productId, extractUsername());
    }

    @Transactional
    public void deleteProductImage(Integer productId) {
        productImageRepo.deleteByProductIdAndUserId(productId, extractUsername());
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
    public BillingResponse doPayment2(BillingRequest request) throws Exception {
        String username = extractUsername();
        Map<String, Object> validateMap = validateBillingRequest(request);
        if (!(boolean) validateMap.get("validated")) {
            return (BillingResponse) validateMap.get("validateResponse");
        }

        // 1. Setup & Initial Billing Entity
        UserSettingsEntity userSettings = userSettingsRepo.findByUsername(username);
        CartProcessingResult cartResult = calculateCartItems(request, username);

        BillingResponse amountValidation = validateCalculatedPayment(request, cartResult.totalAmount());
        if (amountValidation != null) {
            return amountValidation;
        }

        checkAnonymousCustomer(request);
        int unitsSold = calculateTotalUnits(request.getCart());
        BillingEntity billResponse = createInitialBill(request, unitsSold, username, cartResult);

        // 2. Generate Invoice Number
        if (userSettings != null) {
            assignInvoiceNumber2(billResponse, userSettings);
        }

        if (billResponse.getId() == null) {
            return BillingResponse.builder().status("FAILURE").build();
        }

        // 3. Process Cart Items (Calculates taxes, profits, stock)
        persistCartItems(cartResult, billResponse, userSettings, username);
        billingProcess.saveGstListing(billResponse.getInvoiceNumber(), username);

        // 4. Update Bill with final profit
        billResponse.setInvoiceStatus("ACTIVE");
        billRepo.save(billResponse);

        // 5. Process Payment
        PaymentEntity payment = processPayment(request, billResponse, username);

        // 6. Post-Payment Actions
        savePaymentHistorySafe(billResponse, payment);
        updateCustomerMetricsSafe(request, billResponse, username);
        handleInvoiceEmail(request, billResponse, userSettings);
        clearSalesCachesSafe(username);

        return BillingResponse.builder()
                .paymentReferenceNumber(payment.getPaymentReferenceNumber())
                .invoiceNumber(billResponse.getInvoiceNumber())
                .status("SUCCESS")
                .build();
    }

    private Map<String, Object> validateBillingRequest(BillingRequest request) {
        Map<String, Object> res=new HashMap<>();
        if(request == null || request.getSelectedCustomer()==null) {
            var validateResponse=  BillingResponse.builder()
                    .errorCode("401")
                    .errorMessage("Please select valid customer")
                    .status("VALIDATED")
                    .build();
            res.put("validated", Boolean.FALSE);
            res.put("validateResponse", validateResponse);
            return res;
        }
        if(request.getCart() == null || request.getCart().isEmpty()) {
            var validateResponse=  BillingResponse.builder()
                    .errorCode("401")
                    .errorMessage("No item in cart")
                    .status("VALIDATED")
                    .build();
            res.put("validated", Boolean.FALSE);
            res.put("validateResponse", validateResponse);
            return res;
        }
        if (MoneyUtils.decimal(request.getPayingAmount()).signum() < 0) {
            var validateResponse = BillingResponse.builder()
                    .errorCode("401")
                    .errorMessage("Paying amount cannot be negative")
                    .status("VALIDATED")
                    .build();
            res.put("validated", Boolean.FALSE);
            res.put("validateResponse", validateResponse);
            return res;
        }
        BigDecimal invoiceDiscount = MoneyUtils.percentage(request.getDiscountPercentage());
        if (invoiceDiscount.signum() < 0 || invoiceDiscount.compareTo(BigDecimal.valueOf(100)) > 0) {
            var validateResponse = BillingResponse.builder()
                    .errorCode("401")
                    .errorMessage("Discount percentage must be between 0 and 100")
                    .status("VALIDATED")
                    .build();
            res.put("validated", Boolean.FALSE);
            res.put("validateResponse", validateResponse);
            return res;
        }

        res.put("validated", Boolean.TRUE);
        return res;
    }

    private BillingResponse validateCalculatedPayment(BillingRequest request, BigDecimal calculatedTotal) {
        BigDecimal payingAmount = MoneyUtils.amount(request.getPayingAmount());
        if (payingAmount.compareTo(calculatedTotal) <= 0) {
            return null;
        }

        return BillingResponse.builder()
                .errorCode("401")
                .errorMessage("Paying amount cannot be more than total amount")
                .status("VALIDATED")
                .build();
    }

    private int calculateTotalUnits(List<ProductBillDTO> cart) { // Note: Replace CartItemDto with your actual class name
        return cart.stream().mapToInt(obj -> obj.getQuantity()).sum();
    }

    private BillingEntity createInitialBill(BillingRequest request, int unitsSold, String username,
                                            CartProcessingResult cartResult) {
        BigDecimal payingAmount = MoneyUtils.amount(request.getPayingAmount());
        BigDecimal remainingAmount = MoneyUtils.amount(cartResult.totalAmount().subtract(payingAmount));

        Double discountPercentage=calculateDiscountPercentage(cartResult.totalAmount(), cartResult.discountAmount());

        BillingEntity billingEntity = BillingEntity.builder()
                .customerId(request.getSelectedCustomer().getId())
                .unitsSold(unitsSold)
                .taxAmount(MoneyUtils.asAmountDouble(cartResult.taxAmount()))
                .userId(username)
                .totalAmount(MoneyUtils.asAmountDouble(cartResult.totalAmount()))
                .payingAmount(MoneyUtils.asAmountDouble(payingAmount))
                .gstin(request.getGstin())
                .dueReminderCount(0)
                .remainingAmount(MoneyUtils.asAmountDouble(remainingAmount))
                .discountPercent(MoneyUtils.asPercentageDouble(discountPercentage))
                .discountAmount(MoneyUtils.asAmountDouble(cartResult.discountAmount()))
                .cgstAmount(MoneyUtils.asAmountDouble(cartResult.cgstAmount()))
                .sgstAmount(MoneyUtils.asAmountDouble(cartResult.sgstAmount()))
                .igstAmount(MoneyUtils.asAmountDouble(cartResult.igstAmount()))
                .totalProfitOnCP(MoneyUtils.asAmountDouble(cartResult.totalProfit()))
                .remarks(request.getRemarks())
                .subTotalAmount(MoneyUtils.asAmountDouble(cartResult.baseAmount()))
                .createdDate(LocalDateTime.now())
                .invoiceStatus("PROCESSING")
                .build();

        return billRepo.save(billingEntity);
    }

    private Double calculateDiscountPercentage(BigDecimal payingAmount, BigDecimal discountAmount) {
        if (payingAmount == null || discountAmount == null) {
            return 0.0;
        }

        BigDecimal total = payingAmount.add(discountAmount);

        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

         return discountAmount.multiply(new BigDecimal("100"))
                .divide(total, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private void assignInvoiceNumber(BillingEntity billResponse, UserSettingsEntity userSettings) {
        String orderPrefix = userSettings.getSerialNumberPattern();
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String sequentialPart = String.format("%04d", billResponse.getId());



        String invoiceNumber = (orderPrefix != null ? orderPrefix : "CB") + "-" + datePart + "-" + sequentialPart;

        billResponse.setInvoiceNumber(invoiceNumber);
        billRepo.save(billResponse);
    }

    private void assignInvoiceNumber2(BillingEntity billResponse, UserSettingsEntity userSettings) {
        String shopId = extractUsername();

         LocalDate now = LocalDate.now();
        int currentYear = now.getYear() % 100;
        String financialYear;

         if (now.getMonthValue() >= 4) {
            financialYear = currentYear + "-" + (currentYear + 1);
        } else {
            financialYear = (currentYear - 1) + "-" + currentYear;
        }

        // 2. Fetch and lock the sequence row for THIS specific shop and FY
        InvoiceSequence seq = invoiceSeqRepo.findAndLockByShopIdAndFinancialYear(shopId, financialYear)
                .orElseGet(() -> {
                    // If no sequence exists for this FY, initialize it
                    InvoiceSequence newSeq = new InvoiceSequence();
                    newSeq.setShopId(shopId);
                    newSeq.setFinancialYear(financialYear);

                    String prefix = userSettings.getSerialNumberPattern();
                    newSeq.setPrefix((prefix != null && !prefix.trim().isEmpty()) ? prefix : "CB");
                    newSeq.setCurrentValue(0);

                    return newSeq;
                });

         int nextNumber = seq.getCurrentValue() + 1;
        seq.setCurrentValue(nextNumber);
        String prefix = userSettings.getSerialNumberPattern();
         invoiceSeqRepo.save(seq);

         String sequentialPart = String.format("%05d", nextNumber);

         String invoiceNumber = (prefix != null && !prefix.trim().isEmpty()) ? prefix+ "-" + seq.getFinancialYear() + "-" + sequentialPart : "CB"+ "-" + seq.getFinancialYear() + "-" + sequentialPart;

         billResponse.setInvoiceNumber(invoiceNumber);
        billRepo.save(billResponse);
    }

    private record ProductSaleDraft(ProductSalesEntity sale, ProductEntity product) {
    }

    private record CartProcessingResult(
            List<ProductSaleDraft> lines,
            BigDecimal baseAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            BigDecimal cgstAmount,
            BigDecimal sgstAmount,
            BigDecimal igstAmount,
            BigDecimal discountAmount,
            BigDecimal totalProfit) {
    }

    private CartProcessingResult calculateCartItems(BillingRequest request, String username) {
        List<ProductSaleDraft> lines = new ArrayList<>();
        BigDecimal baseAmount = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal cgstAmount = BigDecimal.ZERO;
        BigDecimal sgstAmount = BigDecimal.ZERO;
        BigDecimal igstAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;

        ShopBasicEntity shop = shopBasicRepo.findByUserId(username);
        String shopState = shop != null && shop.getShopState() != null ? shop.getShopState() : "West Bengal";
        String customerState = request.getSelectedCustomer().getState();
        boolean intraStateSale = customerState != null && customerState.equalsIgnoreCase(shopState);

        for (ProductBillDTO item : request.getCart()) {
            ProductEntity product = prodRepo.findByIdAndUserId(item.getId(), username);
            if (product == null) {
                throw new IllegalArgumentException("Product not found: " + item.getId());
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Product quantity must be greater than zero: " + item.getId());
            }

            BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
            BigDecimal originalUnitPrice = MoneyUtils.amount(item.getPrice());
            if (originalUnitPrice.signum() < 0) {
                throw new IllegalArgumentException("Product price cannot be negative: " + item.getId());
            }
            BigDecimal discountPercentage = MoneyUtils.percentage(item.getDiscountPercentage());
            if (discountPercentage.signum() < 0
                    || discountPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Discount percentage must be between 0 and 100: " + item.getId());
            }
            BigDecimal discountPerUnit = originalUnitPrice
                    .multiply(discountPercentage)
                    .divide(BigDecimal.valueOf(100), 8, MoneyUtils.ROUNDING_MODE);
            BigDecimal discountedUnitPrice = originalUnitPrice.subtract(discountPerUnit);

            BigDecimal originalLineTotal = MoneyUtils.amount(originalUnitPrice.multiply(quantity));
            BigDecimal lineTotal = MoneyUtils.amount(discountedUnitPrice.multiply(quantity));
            BigDecimal lineDiscount = MoneyUtils.amount(originalLineTotal.subtract(lineTotal));

            BigDecimal taxPercentage = MoneyUtils.percentage(product.getTaxPercent());
            if (taxPercentage.signum() < 0) {
                throw new IllegalArgumentException("GST percentage cannot be negative: " + item.getId());
            }
            BigDecimal taxDivisor = BigDecimal.ONE.add(
                    taxPercentage.divide(BigDecimal.valueOf(100), 8, MoneyUtils.ROUNDING_MODE));
            BigDecimal lineBaseAmount = MoneyUtils.amount(
                    lineTotal.divide(taxDivisor, 8, MoneyUtils.ROUNDING_MODE));
            BigDecimal lineTaxAmount = MoneyUtils.amount(lineTotal.subtract(lineBaseAmount));

            BigDecimal cgst = BigDecimal.ZERO.setScale(MoneyUtils.AMOUNT_SCALE);
            BigDecimal sgst = BigDecimal.ZERO.setScale(MoneyUtils.AMOUNT_SCALE);
            BigDecimal igst = BigDecimal.ZERO.setScale(MoneyUtils.AMOUNT_SCALE);
            BigDecimal cgstPercentage = BigDecimal.ZERO.setScale(MoneyUtils.PERCENTAGE_SCALE);
            BigDecimal sgstPercentage = BigDecimal.ZERO.setScale(MoneyUtils.PERCENTAGE_SCALE);
            BigDecimal igstPercentage = BigDecimal.ZERO.setScale(MoneyUtils.PERCENTAGE_SCALE);

            if (intraStateSale) {
                cgst = MoneyUtils.amount(lineTaxAmount.divide(BigDecimal.valueOf(2), 8, MoneyUtils.ROUNDING_MODE));
                sgst = MoneyUtils.amount(lineTaxAmount.subtract(cgst));
                cgstPercentage = MoneyUtils.percentage(
                        taxPercentage.divide(BigDecimal.valueOf(2), 8, MoneyUtils.ROUNDING_MODE));
                sgstPercentage = cgstPercentage;
            } else {
                igst = lineTaxAmount;
                igstPercentage = taxPercentage;
            }

            BigDecimal lineProfit = MoneyUtils.amount(
                    discountedUnitPrice
                            .subtract(MoneyUtils.decimal(product.getCostPrice()))
                            .multiply(quantity));

            ProductSalesEntity sale = ProductSalesEntity.builder()
                    .profitOnCP(MoneyUtils.asAmountDouble(lineProfit))
                    .sgstPercentage(MoneyUtils.asPercentageDouble(sgstPercentage))
                    .sgst(MoneyUtils.asAmountDouble(sgst))
                    .cgstPercentage(MoneyUtils.asPercentageDouble(cgstPercentage))
                    .cgst(MoneyUtils.asAmountDouble(cgst))
                    .igstPercentage(MoneyUtils.asPercentageDouble(igstPercentage))
                    .igst(MoneyUtils.asAmountDouble(igst))
                    .productId(item.getId())
                    .productDetails(item.getDetails())
                    .userId(username)
                    .discountPercentage(MoneyUtils.asPercentageDouble(discountPercentage))
                    .discountAmount(MoneyUtils.asAmountDouble(lineDiscount))
                    .quantity(item.getQuantity())
                    .tax(MoneyUtils.asAmountDouble(lineTaxAmount))
                    .subTotal(MoneyUtils.asAmountDouble(lineBaseAmount))
                    .total(MoneyUtils.asAmountDouble(lineTotal))
                    .updatedAt(LocalDateTime.now())
                    .build();

            lines.add(new ProductSaleDraft(sale, product));
            baseAmount = baseAmount.add(lineBaseAmount);
            taxAmount = taxAmount.add(lineTaxAmount);
            totalAmount = totalAmount.add(lineTotal);
            cgstAmount = cgstAmount.add(cgst);
            sgstAmount = sgstAmount.add(sgst);
            igstAmount = igstAmount.add(igst);
            discountAmount = discountAmount.add(lineDiscount);
            totalProfit = totalProfit.add(lineProfit);
        }

        return new CartProcessingResult(
                List.copyOf(lines),
                MoneyUtils.amount(baseAmount),
                MoneyUtils.amount(taxAmount),
                MoneyUtils.amount(totalAmount),
                MoneyUtils.amount(cgstAmount),
                MoneyUtils.amount(sgstAmount),
                MoneyUtils.amount(igstAmount),
                MoneyUtils.amount(discountAmount),
                MoneyUtils.amount(totalProfit));
    }

    private void persistCartItems(CartProcessingResult cartResult, BillingEntity billResponse,
                                  UserSettingsEntity userSettings, String username) {
        boolean allowNoStockBilling = userSettings != null
                && Boolean.TRUE.equals(userSettings.getAllowNoStockBilling());

        for (ProductSaleDraft draft : cartResult.lines()) {
            ProductSalesEntity sale = draft.sale();
            sale.setBillingId(billResponse.getId());
            ProductSalesEntity savedSale = prodSalesRepo.save(sale);

            if (!allowNoStockBilling && savedSale.getId() != null) {
                prodRepo.updateProductStock(
                        draft.product().getId(), sale.getQuantity(), username, LocalDateTime.now());
            }
        }
    }

    private PaymentEntity processPayment(BillingRequest request, BillingEntity billResponse, String username) {
        String paymentMethod = request.getPaymentMethod() != null ? request.getPaymentMethod() : "CASH";
        String payingStatus = "Paid";
        BigDecimal paidAmount = MoneyUtils.amount(billResponse.getPayingAmount());
        BigDecimal totalAmount = MoneyUtils.amount(billResponse.getTotalAmount());

        if (paidAmount.signum() == 0) {
            payingStatus = "UnPaid";
        } else if (totalAmount.compareTo(paidAmount) > 0) {
            payingStatus = "SemiPaid";
        }

        PaymentEntity paymentEntity = PaymentEntity.builder()
                .billingId(billResponse.getId())
                .createdDate(LocalDateTime.now())
                .paymentMethod(paymentMethod)
                .status(payingStatus)
                .tax(MoneyUtils.asAmountDouble(billResponse.getTaxAmount()))
                .userId(username)
                .orderNumber(billResponse.getInvoiceNumber())
                .paid(MoneyUtils.asAmountDouble(paidAmount))
                .toBePaid(MoneyUtils.asAmountDouble(billResponse.getRemainingAmount()))
                .reminderCount(0)
                .updatedBy(username)
                .updatedDate(LocalDateTime.now())
                .subtotal(MoneyUtils.asAmountDouble(billResponse.getSubTotalAmount()))
                .total(MoneyUtils.asAmountDouble(totalAmount))
                .build();

        return salesPaymentRepo.save(paymentEntity);
    }

    private void savePaymentHistorySafe(BillingEntity bill, PaymentEntity payment) {
        try {
            utils.asyncSavePaymentHistory(
                    bill.getId(), payment.getId(), MoneyUtils.asAmountDouble(payment.getPaid()), bill.getInvoiceNumber());
        } catch (Exception e) {
            throw new RuntimeException("Failed to save payment history", e);
        }
    }

    private void updateCustomerMetricsSafe(BillingRequest request, BillingEntity bill, String username) {
        try {
            shopRepo.updateCustomerSpentAmountAndOrdersCount(
                    request.getSelectedCustomer().getId(),
                    MoneyUtils.asAmountDouble(bill.getTotalAmount()
                    ), 1,
                    username);
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

        if ("Anonymous".equals(request.getSelectedCustomer().getName())) {
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

    @Cacheable(value = "sales", keyGenerator = "userScopedKeyGenerator")
    public Page<SalesResponseDTO> getAllSales(int page, int size, String sort, String dir, String searchTerm) {
        String username = extractUsername();
        boolean isGstPrioritySort = "gstBilling".equalsIgnoreCase(sort);
        boolean isDuePrioritySort = "dueAmount".equalsIgnoreCase(sort);
        Pageable pageable;
        Page<BillingEntity> billingPage;

        if (isGstPrioritySort || isDuePrioritySort) {
            pageable = PageRequest.of(Math.max(0, page - 1), size);
            billingPage = billRepo.findSalesWithPriority(
                    username,
                    searchTerm == null ? "" : searchTerm.trim(),
                    isGstPrioritySort ? "gst" : "due",
                    pageable
            );
        } else {
            String sortField = sort;

            // Map API field name to DB field
            if ("date".equalsIgnoreCase(sortField)) sortField = "created_date";
            if ("id".equalsIgnoreCase(sortField)) sortField = "invoice_number";
            if ("total".equalsIgnoreCase(sortField)) sortField = "total_amount";
            if ("customer".equalsIgnoreCase(sortField)) sortField = "customer_id";
            if ("paid".equalsIgnoreCase(sortField)) sortField = "paying_amount";

            Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            Sort sortOrder = Sort.by(direction, sortField);

            // Follow same paging convention as getAllProducts (1-based page param)
            pageable = PageRequest.of(Math.max(0, page - 1), size, sortOrder);

            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                billingPage = billRepo.findByUserIdAndSearchNative(username, searchTerm.trim(), pageable);
            } else {
                billingPage = billRepo.findAllByUserId(username, pageable);
            }
        }

        List<SalesResponseDTO> dtoList = billingPage.getContent().stream()
                .map(obj -> {
                    String customerName = null;
                    String customerEmail=null;
                    String paymentStatus = null;
                    try {
                   CustomerEntity custEntity=     shopRepo.findByIdAndUserId(obj.getCustomerId(), username);
                        customerName = custEntity.getName();
                        try {
                            customerEmail=custEntity.getEmail();
                        } catch (Exception e) {
                            customerEmail="na";
                        }

                        if(obj.getInvoiceStatus().equals("CANCELLED"))
                             paymentStatus ="CANCELLED";
                         else
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
                            .customerEmail(customerEmail)
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
        log.info("selected dashboard range {}", range);
        DashboardDateRange.Range dateRange = DashboardDateRange.resolve(range);
        String username = extractUsername();
        List<BillingEntity> billList;
        List<String> availableFinancialYears = new ArrayList<>();
        if (dateRange.financialYear()) {
            LocalDate today = LocalDate.now();
            int currentFinancialYearStart = today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1;
            LocalDate availabilityStart = LocalDate.of(currentFinancialYearStart - 2, 4, 1);
            LocalDate availabilityEnd = LocalDate.of(currentFinancialYearStart + 1, 4, 1);
            List<BillingEntity> availableBills = billRepo.findSalesNDays(
                    username, availabilityStart.atStartOfDay(), availabilityEnd.atStartOfDay());
            availableFinancialYears = availableBills.stream()
                    .filter(bill -> bill.getCreatedDate() != null)
                    .map(bill -> {
                        LocalDate date = bill.getCreatedDate().toLocalDate();
                        int startYear = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
                        return "fy-" + startYear + "-" + String.format("%02d", (startYear + 1) % 100);
                    })
                    .distinct()
                    .toList();
            billList = availableBills.stream()
                    .filter(bill -> bill.getCreatedDate() != null
                            && !bill.getCreatedDate().isBefore(dateRange.startInclusive())
                            && bill.getCreatedDate().isBefore(dateRange.endExclusive()))
                    .toList();
        } else {
            billList = billRepo.findSalesNDays(
                    username, dateRange.startInclusive(), dateRange.endExclusive());
        }
        List<ProductEntity> prodList;

        List<String> roles = extractRoles();
        log.info("The user roles" + roles);
        prodList = prodRepo.findAllByStatus(Boolean.TRUE, username);
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
                .taxCollected(taxCollected).totalUnitsSold(totalUnitsSold).countOfSales(countOfOrders)
                .heatmapGranularity(dateRange.hourly() ? "hour" : "day")
                .salesHeatmap(buildSalesHeatmap(billList, dateRange))
                .availableFinancialYears(availableFinancialYears)
                .build();
    }

    private List<SalesHeatmapPointDTO> buildSalesHeatmap(
            List<BillingEntity> bills, DashboardDateRange.Range dateRange) {
        Map<String, Double> amountByBucket = new HashMap<>();
        Map<String, Integer> salesByBucket = new HashMap<>();

        for (BillingEntity bill : bills) {
            if (bill.getCreatedDate() == null) continue;

            String key = dateRange.hourly()
                    ? String.format("%02d", bill.getCreatedDate().getHour())
                    : bill.getCreatedDate().toLocalDate().toString();

            amountByBucket.merge(key, Optional.ofNullable(bill.getTotalAmount()).orElse(0d), Double::sum);
            salesByBucket.merge(key, 1, Integer::sum);
        }

        List<SalesHeatmapPointDTO> response = new ArrayList<>();
        if (dateRange.hourly()) {
            DateTimeFormatter hourLabel = DateTimeFormatter.ofPattern("h a");
            for (int hour = 0; hour < 24; hour++) {
                String key = String.format("%02d", hour);
                response.add(SalesHeatmapPointDTO.builder()
                        .key(key)
                        .label(LocalTime.of(hour, 0).format(hourLabel))
                        .amount(amountByBucket.getOrDefault(key, 0d))
                        .salesCount(salesByBucket.getOrDefault(key, 0))
                        .build());
            }
            return response;
        }

        LocalDate firstDay = dateRange.startInclusive().toLocalDate();
        long numberOfDays = java.time.temporal.ChronoUnit.DAYS.between(
                firstDay, dateRange.endExclusive().toLocalDate());
        DateTimeFormatter dayLabel = DateTimeFormatter.ofPattern("dd MMM");

        for (long offset = 0; offset < numberOfDays; offset++) {
            LocalDate bucketDate = firstDay.plusDays(offset);
            String key = bucketDate.toString();
            response.add(SalesHeatmapPointDTO.builder()
                    .key(key)
                    .label(bucketDate.format(dayLabel))
                    .amount(amountByBucket.getOrDefault(key, 0d))
                    .salesCount(salesByBucket.getOrDefault(key, 0))
                    .build());
        }
        return response;
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

    public List<PaymentDetails> getPaymentListNew(String fromDate, String toDate) {

        LocalDateTime startDate = LocalDate.parse(fromDate).atStartOfDay();
        LocalDateTime endDate = LocalDate.parse(toDate).atTime(LocalTime.MAX);

        return salesPaymentRepo.getPaymentListNew(startDate, endDate, extractUsername()).stream()
                .map(payment -> PaymentDetails.builder()
                        .id(payment.getId())
                        .amount(payment.getAmount())
                        .date(String.valueOf(payment.getPaymentDate()))
                        .saleId(payment.getSaleId())
                        .reminderCount(payment.getReminderCount())
                        .method(payment.getMethod())
                        .paid(payment.getPaid() != null ? payment.getPaid() : 0d)
                        .due(payment.getDue() != null ? payment.getDue() : 0d)
                        .status(payment.getStatus())
                        .customerName(payment.getCustomerName())
                        .build())
                .toList();
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

    @Transactional
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
            BillingEntity billDetails = null;
            try {
                billDetails = billRepo.findOrderByJustReference(orderReferenceNumber);
            } catch (Exception e) {
                billDetails=   billRepo.findOrderByReference(orderReferenceNumber, extractUsername());
            }
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
                BillingEntity billDetails2 = null;
                try {
                    billDetails2 = billRepo.findOrderByJustReference(orderReferenceNumber);
                } catch (Exception e) {
                    billDetails2=   billRepo.findOrderByReference(orderReferenceNumber, extractUsername());
                }
                username2 = billDetails2.getUserId();
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
                .status(billDetails.getInvoiceStatus())
                .paid(paid).build();
        return response;
    }

    public String cancelOrder(String orderNumber, String reason){

        String username=extractUsername();
        BillingEntity billDetails = billRepo.findOrderByReference(orderNumber, username);
        InvoiceDetails invoiceDetails=    getOrderDetails( orderNumber);

        List<ProductSalesEntity> prodSales = prodSalesRepo.findByOrderId(billDetails.getId(), username);

        prodSales.stream().forEach(j->{
            prodRepo.restoreProductStocks(j.getProductId(), j.getQuantity(), username, LocalDateTime.now());
        });

        try {
            shopRepo.updateCustomerSpentAmountAndOrdersCountForCancelled(billDetails.getCustomerId(), billDetails.getTotalAmount(), username);
        } catch (Exception e) {
            e.printStackTrace();
        }
        billRepo.updateOrderStatus(orderNumber, "CANCELLED", LocalDateTime.now(), username);


        clearSalesCachesSafe(username);


        return "Update Successful";
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
            BillingEntity billDetails = null;
            try {
                billDetails = billRepo.findOrderByJustReference(orderId);
            } catch (Exception e) {
                billDetails=   billRepo.findOrderByReference(orderId, extractUsername());
            }

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
        productImageRepo.deleteByProductIdAndUserId(id, extractUsername());

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

        UserProfileDto response = UserProfileDto.builder().username(username).phoneNumber(res.getPhoneNumber()).name(res.getName()).roles(extractRoles()).source(res.getSource()).build();

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
        List<Object[]> resultsSales = new ArrayList<>();
        LocalDateTime endDate = LocalDateTime.now();

        DashboardDateRange.Range dateRange = DashboardDateRange.resolve(range);
        if (dateRange.financialYear()) {
            List<BillingEntity> bills = billRepo.findSalesNDays(
                    userId, dateRange.startInclusive(), dateRange.endExclusive());
            Map<YearMonth, Double> revenueByMonth = new HashMap<>();
            Map<YearMonth, Integer> unitsByMonth = new HashMap<>();

            bills.forEach(bill -> {
                if (bill.getCreatedDate() == null) return;
                YearMonth month = YearMonth.from(bill.getCreatedDate());
                revenueByMonth.merge(month, Optional.ofNullable(bill.getTotalAmount()).orElse(0d), Double::sum);
                unitsByMonth.merge(month, Optional.ofNullable(bill.getUnitsSold()).orElse(0), Integer::sum);
            });

            YearMonth firstMonth = YearMonth.from(dateRange.startInclusive());
            DateTimeFormatter monthLabel = DateTimeFormatter.ofPattern("MMM");
            for (int offset = 0; offset < 12; offset++) {
                YearMonth month = firstMonth.plusMonths(offset);
                response.add(WeeklySales.builder()
                        .day(month.format(monthLabel))
                        .totalSales(revenueByMonth.getOrDefault(month, 0d))
                        .unitsSold(unitsByMonth.getOrDefault(month, 0))
                        .build());
            }
            return response;
        }

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
                resultsSales = billRepo.getSalesAndStocksYearly(endDate, userId);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        for (Object[] row : resultsSales) {
            WeeklySales weeklysales = new WeeklySales();
            String day = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            Integer stocksCount = ((Number) row[3]).intValue();
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
            DashboardDateRange.Range dateRange = DashboardDateRange.resolve(range);
            if (dateRange.financialYear()) {
                billingDetails = billRepo.findTopNSalesForGivenRange(
                        username, dateRange.startInclusive(), dateRange.endInclusive(), count);
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
        DashboardDateRange.Range dateRange = DashboardDateRange.resolve(range);
        List<BillingEntity> billingDetails = billRepo.findSalesNDays(
                username, dateRange.startInclusive(), dateRange.endExclusive());
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
        DashboardDateRange.Range dateRange = DashboardDateRange.resolve(timeRange);
        LocalDateTime startDate = dateRange.startInclusive();
        LocalDateTime endDate = dateRange.endInclusive();

        List<ProductPerformanceProjection> topProducts = new ArrayList<>();
        List<TopProductDto> response = new ArrayList<>();

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
        DashboardDateRange.Range dateRange = DashboardDateRange.resolve(timeRange);
        LocalDateTime startDate = dateRange.startInclusive();
        LocalDateTime endDate = dateRange.endInclusive();

        List<ProductPerformanceProjection> topProducts = new ArrayList<>();
        List<TopOrdersDto> response = new ArrayList<>();

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
        DashboardDateRange.Range dateRange = DashboardDateRange.resolve(timeRange);
        LocalDateTime startDate = dateRange.startInclusive();
        LocalDateTime endDate = dateRange.endInclusive();
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
        DashboardDateRange.Range dateRange = DashboardDateRange.resolve(timeRange);
        LocalDateTime startDate = dateRange.startInclusive();
        LocalDateTime endDate = dateRange.endInclusive();
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

    public String updateShopSignature(MultipartFile shopSign) throws IOException {

        String username = extractUsername();
        log.info("entered updateShopLogo with username " + username);

        // 1. Create a unique filename (e.g., "junaid_logo_16789..._logo.png")
        String originalFilename = shopSign.getOriginalFilename();
        String safeFilename = username + "_logo_" + System.currentTimeMillis() + "_" + originalFilename;

        // 2. Ensure the directory exists on the server
        Path uploadPath = Paths.get(SIGN_UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 3. Save the file to the Ubuntu hard drive
        Path filePath = uploadPath.resolve(safeFilename);
        Files.copy(shopSign.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 4. Update the Database
        UserProfilePicEntity picRes = userProfilePicRepo.findByUsername(username);
        if (picRes != null) {
            picRes.setSignature(safeFilename);
            picRes.setUpdated_date(LocalDateTime.now());
            userProfilePicRepo.save(picRes);
        } else {
            UserProfilePicEntity picResNew = new UserProfilePicEntity();
            picResNew.setUsername(username);

            // BUG FIX: Your original code accidentally saved this to setProfilePic!
            // Changed it to setShopLogo to ensure it goes into the correct column.
            picResNew.setSignature(safeFilename);

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

    public byte[] getShopSign(String username) throws IOException {

         log.info("Entered getShopSign with request username " + username);

         UserInfo res = userinfoRepo.findByUsername(username).orElseThrow(() ->
                new RuntimeException("User not found: " + username));

        byte[] content = null;

        try {
            UserProfilePicEntity picRes = userProfilePicRepo.findByUsername(username);

             if (picRes != null && picRes.getSignature() != null) {

                 Path logoPath = Paths.get(SIGN_UPLOAD_DIR, picRes.getSignature());

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
    System.out.println("The content of the signature is " + content);
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
        try {

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

        } catch (Exception e) {
            e.printStackTrace();
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
        BigDecimal amount;
        try {
            amount = MoneyUtils.amount(new BigDecimal(String.valueOf(request.get("amount"))));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Payment amount must be a valid number", e);
        }

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }


        try {
            billRepo.updateDuePayment(orderNo, extractUsername(), LocalDateTime.now(), amount);
            salesPaymentRepo.updateDueAmount(orderNo, extractUsername(), LocalDateTime.now(), amount);
            salesCacheService.evictUserSales(extractUsername());
            salesCacheService.evictUserPayments(extractUsername());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        PaymentEntity paymentDetails = salesPaymentRepo.findByOrderNumber(orderNo, extractUsername());

        try {
            String status = "SemiPaid";
            if (paymentDetails.getToBePaid() <= 0) {
                status = "Paid";
            }
            salesPaymentRepo.updatePaymentStatus(orderNo, extractUsername(), status);

            utils.asyncSavePaymentHistory(
                    paymentDetails.getBillingId(), paymentDetails.getId(), amount.doubleValue(), orderNo);

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

            sqsUtil.ifPresent(util ->
                    util.sendOrderDetailsJustAfterOrderCompletion("send-invoice-email-queue", "SQS", body));

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
                BillingEntity billDetails = null;
                try {
                    billDetails = billRepo.findOrderByJustReference(invoiceNumber);
                } catch (Exception e) {
                    billDetails=   billRepo.findOrderByReference(invoiceNumber, extractUsername());
                }
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
                            fileBytes, template, "Instabill");
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
        log.info("Entered extractTextFromImage with fileName={}, contentType={}, size={}",
                file.getOriginalFilename(), file.getContentType(), file.getSize());

        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        List<GeminiTextExtract> apiLogs = apiSaveRepo.findCreatedWithinLast24Hours(last24Hours, extractUsername(), "Gemini Text Extraction API");
        Integer count = apiLogs.size();
        log.info("Found {} Gemini text extraction API log entries in the last 24 hours for user={}", count, extractUsername());

        if ("USER".equals(extractRole())) {
            if (count > 2) {
                log.warn("Free usage limit exceeded for text extraction, user={}", extractUsername());
                return "Sorry, you have exceeded the free usage limit for text extraction. Please upgrade to Premium for unlimited access.";
            }
        }
        if (count > 10) {
            log.warn("Daily usage limit exceeded for text extraction, user={}", extractUsername());
            return "Sorry, you have exceeded the usage limit for text extraction for the day. Please retry tomorrow.";
        }

        // Pre-scale and compress image to a max dimension of 1024px
        log.info("Optimizing uploaded image before Gemini request");
        byte[] optimizedImageBytes = resizeAndCompressImage(file.getBytes(), 1024);
        log.info("Image optimization completed, optimizedSizeBytes={}", optimizedImageBytes.length);
        String base64Image = Base64.getEncoder().encodeToString(optimizedImageBytes);
        String mimeType = "image/jpeg"; // Resized image is always output as JPEG

        log.info("Calling Gemini API for text extraction");
        String response = geminiCalls.geminiApiCall(base64Image, mimeType);

        log.info("Received Gemini text extraction response, responseLength={}",
                response != null ? response.length() : 0);

        return response;
    }
    private byte[] resizeAndCompressImage(byte[] originalImageBytes, int maxDimension) {
        try {
            log.info("Starting image resize/compress operation, originalSizeBytes={}, maxDimension={}",
                    originalImageBytes.length, maxDimension);
            ByteArrayInputStream bais = new ByteArrayInputStream(originalImageBytes);
            BufferedImage originalImage = ImageIO.read(bais);

            if (originalImage == null) {
                // If ImageIO cannot parse the format, return original bytes as fallback
                log.warn("ImageIO could not parse the uploaded image, returning original bytes");
                return originalImageBytes;
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            log.info("Original image dimensions width={} height={}", originalWidth, originalHeight);

            // If the image is already smaller than the max dimension, keep original size but convert to JPEG
            if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
                maxDimension = Math.max(originalWidth, originalHeight);
            }

            // Calculate scaled dimensions while preserving aspect ratio
            int targetWidth = originalWidth;
            int targetHeight = originalHeight;

            if (originalWidth > originalHeight) {
                if (originalWidth > maxDimension) {
                    targetWidth = maxDimension;
                    targetHeight = (int) ((double) originalHeight / originalWidth * maxDimension);
                }
            } else {
                if (originalHeight > maxDimension) {
                    targetHeight = maxDimension;
                    targetWidth = (int) ((double) originalWidth / originalHeight * maxDimension);
                }
            }

            // Render scaled image using high-quality rendering hints
            BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resizedImage.createGraphics();

            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose();
            log.info("Image resize completed, targetWidth={} targetHeight={}", targetWidth, targetHeight);

            // Write out compressed JPEG bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpg", baos);
            log.info("Image compression completed, outputSizeBytes={}", baos.size());
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Failed to resize image, falling back to original image", e);
            return originalImageBytes;
        }
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

    public byte[] getPaymentReminderImage(String orderId) {
        log.info("Generating payment reminder image for orderNumber-->" + orderId);

        InvoiceData invoiceData = utils.getFullInvoiceDetails(extractUsername(), orderId);

        byte[] response = pdfgstutil.getReminderImage(invoiceData);
        return response;
    }

    public byte[] getPaymentQrCode(String orderReference) {
        String username = extractUsername();
        PaymentEntity payment = salesPaymentRepo.findByOrderNumber(orderReference, username);
        if (payment == null) {
            throw new IllegalArgumentException("Payment not found for order " + orderReference);
        }

        ShopUPIEntity upiDetails = salesUPIRepo.findByShopFinanceId(username);
        if (upiDetails == null || upiDetails.getUpiId() == null || upiDetails.getUpiId().isBlank()) {
            throw new IllegalArgumentException("UPI details are not configured");
        }

        ShopBasicEntity shopDetails = shopBasicRepo.findByUserId(username);
        InvoiceData qrData = InvoiceData.builder()
                .invoiceId(orderReference)
                .paidAmount(payment.getPaid() != null ? payment.getPaid() : 0d)
                .upiId(upiDetails.getUpiId())
                .shopName(shopDetails != null ? shopDetails.getShopName() : username)
                .build();

        return pdfgstutil.getPaymentQrCodeImage(qrData);
    }


}
