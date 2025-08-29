package com.management.shop.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.management.shop.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import com.management.shop.entity.BillingEntity;
import com.management.shop.entity.CustomerEntity;
import com.management.shop.entity.PaymentEntity;
import com.management.shop.entity.ProductEntity;
import com.management.shop.entity.ProductSalesEntity;
import com.management.shop.entity.RegisterUserOTPEntity;
import com.management.shop.entity.Report;
import com.management.shop.entity.ShopDetailsEntity;
import com.management.shop.entity.UserInfo;
import com.management.shop.entity.UserProfilePicEntity;
import com.management.shop.repository.BillingRepository;
import com.management.shop.repository.ProductRepository;
import com.management.shop.repository.ProductSalesRepository;
import com.management.shop.repository.RegisterUserRepo;
import com.management.shop.repository.ReportDetailsRepo;
import com.management.shop.repository.SalesPaymentRepository;
import com.management.shop.repository.ShopDetailsRepo;
import com.management.shop.repository.ShopRepository;
import com.management.shop.repository.UserInfoRepository;
import com.management.shop.repository.UserProfilePicRepo;
import com.management.shop.util.CSVUpload;
import com.management.shop.util.EmailSender;
import com.management.shop.util.OTPSender;
import com.management.shop.util.PDFInvoiceUtil;
import com.management.shop.util.ReportsGenerate;

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

    private final Random random = new Random();

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public String addUser(UserInfo userInfo) {
        userInfo.setRoles("USER");
        userInfo.setIsActive(true);
        userInfo.setPassword(passwordEncoder.encode(userInfo.getPassword()));
        UserInfo res = userinfoRepo.save(userInfo);
        if (res.getId() > 0) {
            String username = res.getName().replace(" ", "").toLowerCase() + String.valueOf(res.getId());
            userInfo.setUsername(username);

            res = userinfoRepo.save(userInfo);

            return "user with username " + res.getUsername() + " created";

        }
        return null;
    }

    public RegisterResponse registerNewUser(RegisterRequest regRequest) {

        var userInfo = UserInfo.builder().email(regRequest.getEmail()).isActive(false).name(regRequest.getFullName())
                .password(regRequest.getPassword()).phoneNumber(regRequest.getPhone()).build();

        userInfo.setRoles("USER");
        userInfo.setPassword(passwordEncoder.encode(userInfo.getPassword()));
        UserInfo res = userinfoRepo.save(userInfo);
        if (res.getId() > 0) {
            String username = res.getName().replace(" ", "").toLowerCase() + String.valueOf(res.getId());
            userInfo.setUsername(username);

            res = userinfoRepo.save(userInfo);

            if (res != null) {

                Random random = new Random();
                int number = 100000 + random.nextInt(900000);

                RegisterUserOTPEntity res2 = newUserRepo.getByUsername(userInfo.getUsername());
                if (res2 != null) {
                    newUserRepo.updateOldOTP(res.getId(), "stale");
                }

                try {
                    otpSender.sendEmail(regRequest.getEmail(), "help@friendsmobile.store", regRequest.getFullName(), "Friends Mobile",
                            "OPT Verification For Account", "Please enter OTP " + number + " to verify you account ");
                } catch (MailjetException | MailjetSocketTimeoutException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                var regsiterUserTemp = RegisterUserOTPEntity.builder().username(res.getUsername())
                        .createdDate(LocalDateTime.now()).otp(String.valueOf(number)).status("fresh").retries(0).build();
                newUserRepo.save(regsiterUserTemp);
            }

            return RegisterResponse.builder().username(res.getUsername()).build();

        }

        return null;
    }

    public ValidateContactResponse validateContact(ValidateContactRequest userInfo) {

        List<UserInfo> res = userinfoRepo.validateContact(userInfo.getEmail(), userInfo.getPhone(), true);
        System.out.println(res.get(0));

        if (res.size() > 0) {

            return ValidateContactResponse.builder().status(false).message("Email/Phone already registered").build();
        }
        return ValidateContactResponse.builder().status(true).message("Email/Phone already registered").build();


    }

    public ValidateContactResponse forgotPaswrod(ForgotPassRequest forgotPassRequest) {
        List<UserInfo> res = userinfoRepo.validateUser(forgotPassRequest.getEmailId(), forgotPassRequest.getUserId(), true);


        if (res.size() > 0) {
            System.out.println(res.get(0));
            Random random = new Random();
            int otp = 100000 + random.nextInt(900000);
            var otpVerifyReq = OtpVerifyRequest.builder().otp(String.valueOf(otp)).username(res.get(0).getUsername()).build();


            RegisterUserOTPEntity res2 = newUserRepo.getByUsername(res.get(0).getUsername());
            if (res2 != null) {
                newUserRepo.removeOldOTP(res.get(0).getUsername());
            }

            try {
                otpSender.sendEmail(forgotPassRequest.getEmailId(), "help@friendsmobile.store", res.get(0).getName(), "Friends Mobile",
                        "OTP for resetting you password", "Please enter OTP " + otp + " to proceed forward ");
            } catch (MailjetException | MailjetSocketTimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            var regsiterUserTemp = RegisterUserOTPEntity.builder().username(res.get(0).getUsername())
                    .createdDate(LocalDateTime.now()).otp(String.valueOf(otp)).status("fresh").retries(0).build();
            newUserRepo.save(regsiterUserTemp);


            return ValidateContactResponse.builder().status(true).message("OTP sent to your email Id").build();
        }
        return ValidateContactResponse.builder().status(false).message("No user found with provided details").build();

    }

    @Transactional
    public OtpVerifyResponse reEnterOtp(OtpVerifyRequest otpVerifyReq) {

        RegisterUserOTPEntity res = newUserRepo.getByUsername(otpVerifyReq.getUsername());

        UserInfo userInfo = userinfoRepo.findByUsername(res.getUsername()).get();

        if (res != null) {

            newUserRepo.updateOldOTP(res.getId(), "stale");
            Random random = new Random();
            int number = 100000 + random.nextInt(900000);
            try {
                otpSender.sendEmail(userInfo.getEmail(), "help@friendsmobile.store", userInfo.getName(), "Friends Mobile",
                        "OPT Verification For Account", "Please enter OTP " + number + " to verify you account ");
            } catch (MailjetException | MailjetSocketTimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


            var regsiterUserTemp = RegisterUserOTPEntity.builder().username(res.getUsername())
                    .createdDate(LocalDateTime.now()).otp(String.valueOf(number)).status("fresh").retries(res.getRetries() + 1).build();
            newUserRepo.save(regsiterUserTemp);

        }

        return null;
    }


    @Transactional
    public OtpVerifyResponse verifyOTP(OtpVerifyRequest otpInfo) {
        RegisterUserOTPEntity res = newUserRepo.getByUsername(otpInfo.getUsername());

        if (res.getOtp().equals(otpInfo.getOtp())) {

            userinfoRepo.updateUserStatus(res.getUsername());
            UserInfo userInfo = userinfoRepo.findByUsername(res.getUsername()).get();

            try {
                otpSender.sendEmail(userInfo.getEmail(), "help@friendsmobile.store", userInfo.getName(), "Friends Mobile",
                        "User creation confirmation", "Your account has been created with " + userInfo.getUsername() + "");
            } catch (MailjetException | MailjetSocketTimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


            var response = OtpVerifyResponse.builder().success(true)
                    .username(otpInfo.getUsername())
                    .message("Your account is created")
                    .build();
            return response;
        }
        var response = OtpVerifyResponse.builder().success(false)
                .username(otpInfo.getOtp())
                .message("Your entered OTP " + otpInfo.getOtp())
                .build();


        return response;
    }

    public ValidateContactResponse confirmOtpAndUpdatePassword(UpdatePasswordRequest updatePassRequest) {
        List<UserInfo> userInfo = userinfoRepo.validateUser(updatePassRequest.getEmailId(), updatePassRequest.getUserId(), true);

        if (userInfo.size() > 0) {
            RegisterUserOTPEntity otpedUser = newUserRepo.getLatestOtp(userInfo.get(0).getUsername());

            if (otpedUser != null) {
                if (otpedUser.getOtp().equals(updatePassRequest.getOtp())) {

                    updatePassword(UserInfo.builder().username(userInfo.get(0).getUsername()).password(updatePassRequest.getNewPassword()).build());

                    return ValidateContactResponse.builder().status(true).message("Your password has been updated successfully").build();
                } else {
                    return ValidateContactResponse.builder().status(false).message("Your otp doesn't matched please re-enter").build();

                }
            }

        }
        return null;

    }

    public boolean checkUserStatus(String username) {
        // TODO Auto-generated method stub
        return userinfoRepo.findByUsername(username).get().getIsActive();
    }

    public CustomerSuccessDTO saveCustomer(CustomerRequest request) {
        System.out.println("entered into saveCustomer with" + request.toString());

        List<CustomerEntity> existingCustomer = shopRepo.findByPhone(request.getPhone(), "ACTIVE");

        CustomerEntity ent = null;

        if (existingCustomer.size() > 0) {

            var customerEntity = CustomerEntity.builder().id(existingCustomer.get(0).getId()).name(request.getName()).email(request.getEmail())
                    .createdDate(LocalDateTime.now()).phone(request.getPhone()).status("ACTIVE").totalSpent(existingCustomer.get(0).getTotalSpent()).build();

            ent = shopRepo.save(customerEntity);

        } else {

            var customerEntity = CustomerEntity.builder().name(request.getName()).email(request.getEmail())
                    .createdDate(LocalDateTime.now()).phone(request.getPhone()).status("ACTIVE").totalSpent(0).build();

            ent = shopRepo.save(customerEntity);
        }


        if (ent.getId() != null) {

            return CustomerSuccessDTO.builder().success(true).customer(request).build();
        }

        return CustomerSuccessDTO.builder().id(ent.getId()).success(false).customer(request).build();

    }

    public CustomerEntity saveCustomerForBilling(CustomerRequest request) {
        System.out.println("entered into saveCustomer with" + request.toString());
        List<CustomerEntity> existingCustomer = shopRepo.findByPhone(request.getPhone(), "ACTIVE");

        CustomerEntity ent = null;

        if (existingCustomer.size() > 0) {

            var customerEntity = CustomerEntity.builder().id(existingCustomer.get(0).getId()).name(request.getName()).email(request.getEmail())
                    .createdDate(LocalDateTime.now()).phone(request.getPhone()).status("ACTIVE").totalSpent(existingCustomer.get(0).getTotalSpent()).build();

            ent = shopRepo.save(customerEntity);

        } else {

            var customerEntity = CustomerEntity.builder().name(request.getName()).email(request.getEmail())
                    .createdDate(LocalDateTime.now()).phone(request.getPhone()).status("ACTIVE").totalSpent(0).build();

            ent = shopRepo.save(customerEntity);
        }

        if (ent.getId() != null) {

            return ent;
        }

        return ent;

    }

    public List<CustomerEntity> getAllCustomer() {

        return shopRepo.findAllActiveCustomer("ACTIVE");
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
                    .category(request.getCategory()).status(status).stock(request.getStock()).active(true)
                    .taxPercent(request.getTax()).price(request.getPrice()).costPrice(request.getCostPrice()).build();

        } else {

            productEntity = ProductEntity.builder().name(request.getName()).category(request.getCategory()).active(true)
                    .status(status).stock(request.getStock()).taxPercent(request.getTax()).costPrice(request.getCostPrice()).price(request.getPrice())
                    .build();

        }

        ProductEntity ent = prodRepo.save(productEntity);
        if (ent.getId() != null) {

            return ProductSuccessDTO.builder().success(true).product(request).build();
        }

        return ProductSuccessDTO.builder().success(false).product(request).build();

    }

    public ProductSuccessDTO updateProduct(ProductRequest request) {

        String status = "In Stock";
        if (request.getStock() < 1)
            status = "Out of Stock";
        System.out.println("The updated request" + request.getTax());
        var productEntity = ProductEntity.builder().id(request.getSelectedProductId()).name(request.getName())
                .active(true).category(request.getCategory()).status(status).stock(request.getStock())
                .taxPercent(request.getTax()).price(request.getPrice()).costPrice(request.getCostPrice()).build();

        ProductEntity ent = prodRepo.save(productEntity);

        if (ent.getId() != null) {

            return ProductSuccessDTO.builder().success(true).product(request).build();
        }

        return ProductSuccessDTO.builder().success(false).product(request).build();

    }

    public List<ProductEntity> getAllProducts() {

        return prodRepo.findAllActiveProducts(Boolean.TRUE);
    }

    @Transactional
    public BillingResponse doPayment(BillingRequest request) throws Exception {

        Integer unitsSold = 0;
        for (var obj : request.getCart()) {
            unitsSold += obj.getQuantity();
        }
        var billingEntity = BillingEntity.builder().customerId(request.getSelectedCustomer().getId())
                .unitsSold(unitsSold).taxAmount(request.getTax()).totalAmount(request.getTotal())
                .discountPercent(request.getDiscountPercentage()).remarks(request.getRemarks()).subTotalAmount(request.getTotal() - request.getTax()).createdDate(LocalDateTime.now()).build();

        BillingEntity billResponse = billRepo.save(billingEntity);

        if (billResponse.getId() != null) {
            request.getCart().stream().forEach(obj -> {

                ProductEntity prodRes = prodRepo.findById(obj.getId()).get();
                System.out.println("Product details " + prodRes);
                Integer tax = (prodRes.getTaxPercent() * obj.getQuantity() * obj.getPrice()) / 100;
                Integer subTotal = obj.getQuantity() * obj.getPrice();
                Integer total = tax + subTotal;

                var productSalesEntity = ProductSalesEntity.builder().billingId(billResponse.getId())
                        .productId(obj.getId()).discountPercentage(obj.getDiscountPercentage()).quantity(obj.getQuantity()).tax(tax).subTotal(subTotal).total(total)
                        .build();

                ProductSalesEntity prodSalesResponse = prodSalesRepo.save(productSalesEntity);

                if (prodSalesResponse.getId() != null) {
                    prodRepo.updateProductStock(obj.getId(), obj.getQuantity());

                }

            });
            String paymentMethod = "CASH";
            if (request.getPaymentMethod() != null) {
                paymentMethod = request.getPaymentMethod();
            }

            var paymentEntity = PaymentEntity.builder().billingId(billResponse.getId()).createdDate(LocalDateTime.now())
                    .paymentMethod(paymentMethod).status("Paid").tax(request.getTax())
                    .subtotal(request.getTotal() - request.getTax()).total(request.getTotal()).build();

            salesPaymentRepo.save(paymentEntity);

            try {
                shopRepo.updateCustomerSpentAmount(request.getSelectedCustomer().getId(), request.getTotal());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            InvoiceDetails order = getOrderDetails(billResponse.getInvoiceNumber());
            try {
                CompletableFuture<String> futureResult = email.sendEmail(order.getCustomerEmail(),
                        billResponse.getInvoiceNumber(), order.getCustomerName(),
                        generateInvoicePdf(billResponse.getInvoiceNumber()));
                System.out.println(futureResult);
            } catch (MailjetException | MailjetSocketTimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return BillingResponse.builder().paymentReferenceNumber(paymentEntity.getPaymentReferenceNumber())
                    .invoiceNumber(billResponse.getInvoiceNumber()).status("SUCCESS").build();
        }

        return BillingResponse.builder().status("FAILURE").build();
    }

    public List<SalesResponseDTO> getAllSales() {

        List<BillingEntity> listOfBills = billRepo.findAll();
        listOfBills.sort(Comparator.comparing(BillingEntity::getCreatedDate).reversed());
        List<SalesResponseDTO> response = new ArrayList();
        // String name =
        // shopRepo.findById(listOfBills.get(0).getCustomerId()).get().getName();
        // salesPaymentRepo.findPaymentDetails(listOfBills.get(0).getId()).getStatus();

        try {
            listOfBills.stream().forEach(obj -> { // SalesResponseDTO salesResponse
                var salesResponse = SalesResponseDTO.builder()
                        .customer(shopRepo.findById(obj.getCustomerId()).get().getName())
                        .remarks(obj.getRemarks())
                        .date(String.valueOf(obj.getCreatedDate())).id(obj.getInvoiceNumber())
                        .total(obj.getTotalAmount())
                        .status(salesPaymentRepo.findPaymentDetails(obj.getId()).getStatus()).build();

                response.add(salesResponse);

            });
        } catch (Exception e) { // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return response;
    }

    public Page<SalesResponseDTO> getAllSalesWithPagination(Integer page, Integer size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

        return billRepo.findAll(pageable)
                .map(sale -> new SalesResponseDTO(sale.getInvoiceNumber(),
                        shopRepo.findById(sale.getCustomerId()).get().getName(), String.valueOf(sale.getCreatedDate()),
                        sale.getTotalAmount(), sale.getRemarks(), salesPaymentRepo.findPaymentDetails(sale.getId()).getStatus()));
    }

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
            billList = billRepo.findAllByDayRange(LocalDateTime.now().minusDays(days));

        } else if (range.equals("today")) {
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay(); // today 00:00
            LocalDateTime endOfDay = startOfDay.plusDays(1); // tomorrow 00:00
            billList = billRepo.findAllCreatedToday(startOfDay, endOfDay);
            // prodList = prodRepo.findAllCreatedToday(startOfDay, endOfDay);

        }
        prodList = prodRepo.findAllByStatus(Boolean.TRUE);
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

    public List<PaymentDetails> getPaymentList() {
        List<BillingEntity> billList = billRepo.findAll();
        billList.sort(Comparator.comparing(BillingEntity::getCreatedDate).reversed());
        List<PaymentDetails> response = new ArrayList<>();
        billList.stream().forEach(obj -> {

            response.add(PaymentDetails.builder()
                    .id(salesPaymentRepo.findPaymentDetails(obj.getId()).getPaymentReferenceNumber())
                    .amount(obj.getTotalAmount()).date(String.valueOf(obj.getCreatedDate()))
                    .saleId(obj.getInvoiceNumber())
                    .method(salesPaymentRepo.findPaymentDetails(obj.getId()).getPaymentMethod()).build());
        });

        return response;
    }

    public ProductSuccessDTO uploadProduct(File request) {

        return null;
    }

    public List<ProductRequest> uploadBulkProduct(MultipartFile file) {

        try {
            List<ProductRequest> prodList = util.parseCsv(file);
            System.out.println(prodList);
            prodList.stream().forEach(obj -> {
                ProductSuccessDTO prodsaveResponse = saveProduct(obj);
                System.out.println(prodsaveResponse);
            });
            return prodList;

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    public ByteArrayOutputStream generateOrderInvoice(String orderId) {

        InvoiceDetails order = getOrderDetails(orderId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter.getInstance(document, baos);
        document.open();

        // Colors
        Color indigo = new Color(63, 81, 181);
        Color headerText = Color.WHITE;
        Color altRow = new Color(240, 240, 240);
        Color accentBlue = new Color(33, 150, 243);
        Color muted = Color.GRAY;

        // Fonts
        Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD);
        Font headerFont = new Font(Font.HELVETICA, 14, Font.BOLD, headerText);
        Font bodyFont = new Font(Font.HELVETICA, 12);
        Font totalFont = new Font(Font.HELVETICA, 14, Font.BOLD, accentBlue);
        Font footerFont = new Font(Font.HELVETICA, 10, Font.ITALIC, muted);
        Font badgePaid = new Font(Font.HELVETICA, 14, Font.BOLD, Color.GREEN);
        Font badgeUnpaid = new Font(Font.HELVETICA, 14, Font.BOLD, Color.RED);

        // Logo
        try {
            Image logo = Image.getInstance(this.getClass().getResource("/static/logo.png"));
            logo.scaleAbsolute(60, 60);
            logo.setAlignment(Image.ALIGN_LEFT);
            document.add(logo);
        } catch (Exception ignored) {
        }

        // Title + Payment Status Badge
        Paragraph title = new Paragraph(new Phrase("INVOICE", titleFont));
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        String statusText = order.isPaid() ? "PAID" : "UNPAID";
        Font statusFont = order.isPaid() ? badgePaid : badgeUnpaid;
        Paragraph status = new Paragraph(new Phrase(statusText, statusFont));
        status.setAlignment(Element.ALIGN_CENTER);
        document.add(status);

        document.add(new Paragraph(" ")); // spacer

        NumberFormat inr = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        document.add(new Paragraph(new Phrase("Order ID: " + order.getInvoiceId(), bodyFont)));
        document.add(new Paragraph(new Phrase("Date: " + LocalDate.now(), bodyFont)));
        document.add(new Paragraph(new Phrase("Customer: " + order.getCustomerName(), bodyFont)));
        /*
         * if (order.getCustomerAddress() != null) { document.add(new Paragraph(new
         * Phrase("Address: " + order.getCustomerAddress(), bodyFont))); }
         */
        document.add(new Paragraph(" "));

        // Items table
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setWidths(new float[]{5f, 1.5f, 2f, 2f});

        Stream.of("Item", "Qty", "Price", "Total").forEach(col -> {
            PdfPCell cell = new PdfPCell(new Phrase(col, headerFont));
            cell.setBackgroundColor(indigo);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            cell.setBorderColor(indigo.darker());
            table.addCell(cell);
        });

        int rowIndex = 0;
        for (OrderItem item : order.getItems()) {
            Color bg = (rowIndex % 2 == 0) ? Color.WHITE : altRow;
            PdfPCell c1 = new PdfPCell(new Phrase(item.getProductName(), bodyFont));
            PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), bodyFont));
            PdfPCell c3 = new PdfPCell(new Phrase(inr.format(item.getUnitPrice()), bodyFont));
            PdfPCell c4 = new PdfPCell(new Phrase(inr.format(item.getQuantity() * item.getUnitPrice()), bodyFont));

            for (PdfPCell c : Arrays.asList(c1, c2, c3, c4)) {
                c.setBackgroundColor(bg);
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                c.setPadding(6);
                c.setBorderColor(indigo.darker());
            }
            table.addCell(c1);
            table.addCell(c2);
            table.addCell(c3);
            table.addCell(c4);
            rowIndex++;
        }

        document.add(table);

        // GST & Discount rows
        double gstRate = 0.18; // 18%
        double discountRate = 0.10; // 10%
        double gstAmount = order.getTotalAmount() * gstRate;
        double discountAmount = order.getTotalAmount() * discountRate;
        double finalTotal = order.getTotalAmount() + gstAmount - discountAmount;

        document.add(new Paragraph(new Phrase("GST (18%): " + inr.format(gstAmount), bodyFont)));
        document.add(new Paragraph(new Phrase("Discount (10%): -" + inr.format(discountAmount), bodyFont)));

        // Final total highlighted
        Paragraph total = new Paragraph(new Phrase("Grand Total: " + inr.format(finalTotal), totalFont));
        total.setAlignment(Element.ALIGN_RIGHT);
        document.add(total);

        document.add(new Paragraph(" "));

        // Footer
        Paragraph footer = new Paragraph(new Phrase("Thank you for your business!", footerFont));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();

        return baos;
    }

    public InvoiceDetails getOrderDetails(String orderReferenceNumber) {

        BillingEntity billDetails = billRepo.findOrderByReference(orderReferenceNumber);

        PaymentEntity paymentEntity = salesPaymentRepo.findPaymentDetails(billDetails.getId());

        boolean paid = false;
        if (paymentEntity.getStatus().equalsIgnoreCase("Paid")) {
            paid = true;
        }

        CustomerEntity customerEntity = shopRepo.findById(billDetails.getCustomerId()).get();

        List<ProductSalesEntity> prodSales = prodSalesRepo.findByOrderId(billDetails.getId());
        Double gst = 0d;
        for (ProductSalesEntity orders : prodSales) {
            gst = gst + orders.getTax();
        }

        List<OrderItem> items = prodSales.stream().map(obj -> {

            ProductEntity prodRes = prodRepo.findById(obj.getProductId()).get();

            var orderItems = OrderItem.builder().productName(prodRes.getName()).unitPrice(prodRes.getPrice())
                    .quantity(obj.getQuantity()).build();
            return orderItems;
        }).collect(Collectors.toList());
        var response = InvoiceDetails.builder().discountRate(0).invoiceId(orderReferenceNumber)
                .paymentReferenceNumber(paymentEntity.getPaymentReferenceNumber()).items(items).gstRate(gst)
                .customerPhone(customerEntity.getPhone()).customerEmail(customerEntity.getEmail())
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

        byte[] fileBytes = repogen.downloadReport(request.getReportType(), fromDateTime, toDateTime);

        return fileBytes;
    }

    public String saveReportDetails(Report request) {
        request.setStatus("READY");
        reportDRepo.save(request);
        return "Success";
    }

    public List<ReportResponse> getReportsList(Integer limit) {
        List<Report> reportList = reportDRepo.findByLimit(limit);

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
            shopDetailsRepo.save(shopDetails);
        } else {
            ShopDetailsEntity shopDetailsNew = new ShopDetailsEntity();
            shopDetailsNew.setUsername(request.getUsername());
            shopDetailsNew.setAddresss(request.getAddress());
            shopDetailsNew.setOwnerName(request.getShopOwner());
            shopDetailsNew.setName(request.getName());
            shopDetailsNew.setGstNumber(request.getGstNumber());
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
        shopRepo.updateStatus(id, "IN-ACTIVE");

    }

    public byte[] generateInvoicePdf(String orderId) throws Exception {
        System.out.println(orderId);
        InvoiceDetails order = getOrderDetails(orderId);
        byte[] response = pdfutil.generateInvoice(order.getCustomerName(), order.getCustomerEmail(),
                order.getCustomerPhone(), order.getInvoiceId(), order.getItems());

        return response;
    }

    public UpdateUserDTO getUserProfile(String username) {

        System.out.println("entered getUserProfile with request  username " + username);

        UserInfo userinfo = userinfoRepo.findByUsername(username).get();

        ShopDetailsEntity shopDetails = shopDetailsRepo.findbyUsername(username);

        var response = UpdateUserDTO.builder().address(shopDetails.getAddresss()).email(userinfo.getEmail())
                .gstNumber(shopDetails.getGstNumber()).name(userinfo.getName()).phone(userinfo.getPhoneNumber())
                .shopLocation(shopDetails.getAddresss()).shopOwner(shopDetails.getOwnerName()).username(username)
                .build();

        return response;
    }

    public byte[] getProfilePic(String username) throws IOException {

        System.out.println("entered getProfilePic with request  username " + username);

        UserProfilePicEntity picRes = userProfilePicRepo.findByUsername(username);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(picRes.getProfilePic())
                .build();
        byte[] content = null;
        try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest)) {
            content = s3Object.readAllBytes();
        }

        return content;
    }

    @Transactional
    public void deleteProduct(Integer id) {
        System.out.println("endtered deleteProduct with productId " + id);

        prodRepo.deActivateProduct(id, Boolean.FALSE);

    }

    public AnalyticsResponse getAnalytics(AnalyticsRequest request) {

        AnalyticsResponse response = new AnalyticsResponse();

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

        List<Object[]> resultsSales = billRepo.getMonthlySalesSummary(startDate, endDate);

        for (Object[] row : resultsSales) {
            String month = (String) row[0];
            labels.add(month);
            Long count = ((Number) row[1]).longValue();
            sales.add(count);
        }

        List<Object[]> resultsStocks = billRepo.getMonthlyStocksSold(startDate, endDate);
        for (Object[] row : resultsStocks) {

            Long count = ((Number) row[1]).longValue();
            stocks.add(count);
        }
        List<Object[]> resultsTaxes = billRepo.getMonthlyTaxesSummary(startDate, endDate);
        for (Object[] row : resultsTaxes) {
            Integer count = ((Number) row[1]).intValue();
            taxes.add(count);
        }
        List<Object[]> resultsCustomers = shopRepo.getMonthlyCustomerCount(startDate, endDate);
        for (Object[] row : resultsCustomers) {
            Integer count = ((Number) row[1]).intValue();
            customers.add(count);
        }
        List<Object[]> resultsOnlinePaymentCount = salesPaymentRepo.getMonthlyPaymentCounts(startDate, endDate);

        for (Object[] row : resultsOnlinePaymentCount) {
            Integer count = ((Number) row[1]).intValue();
            onlinePaymentCounts.add(count);
        }


        for (Object[] row : resultsSales) {
            double percentage = 0.08 + (0.20 - 0.08) * random.nextDouble();

            Long count = ((Number) row[1]).longValue();
            Long estimatedProfit = (long) (count * percentage);
            profits.add(estimatedProfit);
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


}
