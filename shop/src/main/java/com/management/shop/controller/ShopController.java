package com.management.shop.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.management.shop.dto.*;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.management.shop.entity.CustomerEntity;
import com.management.shop.entity.ProductEntity;
import com.management.shop.entity.Report;
import com.management.shop.entity.UserInfo;
import com.management.shop.service.JwtService;
import com.management.shop.service.ShopService;

@RestController
@Slf4j
public class ShopController {

    @Autowired
    ShopService serv;

    @Autowired
    private Environment environment;

    @Value("${razorpay.key.secret}")
    private String keySecret;
    @Value("${razorpay.key.id}")
    private String keyId;


    @PostMapping("api/shop/user/updatepassword")
    public String addUpdatePassword(@RequestBody UserInfo userInfo) {
        System.out.println("inside addUpdatePassword with details "+userInfo.toString());
        return serv.updatePassword(userInfo);
    }
    @PostMapping("auth/new/welcome")
    public ResponseEntity<String> addNewUser(@RequestBody UserInfo userInfo) {
          return ResponseEntity.status(HttpStatus.OK).body("welcome to the app");

    }
    @GetMapping("api/shop/user/profile")
    public ResponseEntity<AuthRequest> userProfile() {

        Map<String, String> servResponse = serv.getUserProfileDetails();

        AuthRequest response=new AuthRequest();
        response.setUsername(servResponse.get("username"));
        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @PostMapping("api/shop/create/customer")
    ResponseEntity<CustomerSuccessDTO> createCustomer(@RequestBody CustomerRequest request) {

        CustomerSuccessDTO response = serv.saveCustomer(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @PostMapping("api/shop/create/forBilling/customer")
    ResponseEntity<CustomerEntity> createCustomerForBilling(@RequestBody CustomerRequest request) {

        CustomerEntity response = serv.saveCustomerForBilling(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @GetMapping("api/shop/get/customersList")
    ResponseEntity<List<CustomerEntity>> getCustomersList() {

        List<CustomerEntity> response = serv.getAllCustomer();

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }
    @GetMapping("api/shopd/get/cacheable/customersList")
    ResponseEntity<List<CustomerEntity>> getCustomersListCacheable() {

        List<CustomerEntity> response = serv.getAllCustomer();

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @GetMapping("/api/shop/get/cacheable/customersList")
    public ResponseEntity<Map<String, Object>> getCustomersListCacheable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false, defaultValue = "") String search) {

        try {
            // Call the updated service method
            Page<CustomerEntity> customerPage =  serv.getCacheableCustomersList(search, page, limit);

            // Build the response map to match the frontend's expected structure
            Map<String, Object> response = new HashMap<>();
            response.put("data", customerPage.getContent());
            response.put("totalPages", customerPage.getTotalPages());
            response.put("totalCount", customerPage.getTotalElements());
            response.put("currentPage", customerPage.getNumber() + 1); // Send back the current page

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Basic error handling
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @DeleteMapping("api/shop/customer/delete/{id}")
    ResponseEntity<String> deleteCustomer(@PathVariable Integer id) {
        System.out.println("entered deleteCustomer");

        serv.deleteCustomer(id);

        return ResponseEntity.status(HttpStatus.OK).body("Success");

    }

    @DeleteMapping("api/shop/product/delete/{id}")
    ResponseEntity<String> deleteProduct(@PathVariable Integer id) {
        System.out.println("entered deleteProduct");

        serv.deleteProduct(id);

        return ResponseEntity.status(HttpStatus.OK).body("Success");

    }

    @PostMapping("api/shop/create/product")
    ResponseEntity<ProductSuccessDTO> createProduct(@RequestBody ProductRequest request) {

        ProductSuccessDTO response = serv.saveProduct(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @PostMapping("api/shop/upload/productList")
    ResponseEntity<ProductSuccessDTO> createCustomer(@RequestBody File request) {

        ProductSuccessDTO response = serv.uploadProduct(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @PutMapping("api/shop/update/product")
    ResponseEntity<ProductSuccessDTO> updateProduct(@RequestBody ProductRequest request) {

        ProductSuccessDTO response = serv.updateProduct(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @GetMapping("api/shop/get/productsList")
    ResponseEntity<List<ProductEntity>> getProductsList() {

        List<ProductEntity> response = serv.getAllProducts();

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }


    @GetMapping("api/shop/get/withCache/productsList")
    public ResponseEntity<Map<String, Object>> getProductsList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir
    ) {
        try {
            System.out.println("entered getProductsList");
            // Call the updated service method
            Page<ProductEntity> productPage = serv.getAllProducts(search, page, limit, sort, dir);

            log.info("Fetched {} products for page {} with limit {}", productPage.getNumberOfElements(), page, limit);

            // Build the response map to match the frontend's expected structure
            Map<String, Object> response = new HashMap<>();
            response.put("data", productPage.getContent());
            response.put("totalPages", productPage.getTotalPages());
            response.put("totalCount", productPage.getTotalElements());
            response.put("currentPage", productPage.getNumber() + 1); // Send back the current page

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Basic error handling
            return ResponseEntity.internalServerError().body(null);
        }
    }
    @GetMapping("api/shop/get/forBilling/withCache/productsList")
    public ResponseEntity<Map<String, Object>> getBillingProductsList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir
    ) {
        try {
            System.out.println("entered getProductsList");
            // Call the updated service method
            Page<ProductEntity> productPage = serv.getAllProductsForBilling(search, page, limit, sort, dir);

            log.info("Fetched {} products for page {} with limit {}", productPage.getNumberOfElements(), page, limit);

            // Build the response map to match the frontend's expected structure
            Map<String, Object> response = new HashMap<>();
            response.put("data", productPage.getContent());
            response.put("totalPages", productPage.getTotalPages());
            response.put("totalCount", productPage.getTotalElements());
            response.put("currentPage", productPage.getNumber() + 1); // Send back the current page

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Basic error handling
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping("/api/shop/get/sales/withPages")
    ResponseEntity<Page<SalesResponseDTO>> getSalesListWithPagination(@RequestParam int page,
                                                                      @RequestParam int size,
                                                                      @RequestParam(defaultValue = "createdAt") String sort,
                                                                      @RequestParam(defaultValue = "desc") String dir) {

        System.out.println("the page param is -->" + page);
        System.out.println("the size param is -->" + size);
        System.out.println("the sort param is -->" + sort);
        System.out.println("the dir param is -->" + dir);

        Page<SalesResponseDTO> response = serv.getAllSalesWithPagination(page, size, sort, dir);

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @PostMapping("api/shop/do/billing")
    ResponseEntity<BillingResponse> doBilling(@RequestBody BillingRequest request) throws Exception {

        System.out.println("The request payload for billing app is-->" + request);

        BillingResponse response = serv.doPayment(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("api/shop/get/sales")
    ResponseEntity<Page<SalesResponseDTO>> getSalesList(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(defaultValue = "createdAt") String sort,
                                                        @RequestParam(defaultValue = "desc") String dir,
                                                        @RequestParam String search) {

        System.out.println("the search param is -->" + search);
        Page<SalesResponseDTO> response = serv.getAllSales(page, size, sort, dir, search);

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }
    @GetMapping("api/shop/get/count/sales")
    ResponseEntity<List<SalesResponseDTO>> getLastNSales(@RequestParam(defaultValue = "3") int count) {


        List<SalesResponseDTO> response = serv.getLastNSales(count);


        return ResponseEntity.status(HttpStatus.OK).body(response);

    }
    @GetMapping("api/shop/get/top/sales/{range}")
    ResponseEntity<List<SalesResponseDTO>> getLastTopSales(@RequestParam(defaultValue = "3") int count, @PathVariable String range) {

      System.out.println("Entered getLastTopSales with the range param is -->" + range);
        List<SalesResponseDTO> response = serv.getTopNSales(count, range);


        return ResponseEntity.status(HttpStatus.OK).body(response);

    }



    @GetMapping("api/shop/get/dashboardDetails/{range}")
    ResponseEntity<DasbboardResponseDTO> getDashBoardDetails(@PathVariable String range) {

        DasbboardResponseDTO response = serv.getDashBoardDetails(range);

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @GetMapping("api/shop/get/paymentLists")
    ResponseEntity<List<PaymentDetails>> getPaymentList(
            @RequestParam String fromDate,
            @RequestParam String toDate) {
        List<PaymentDetails> response = serv.getPaymentList(fromDate, toDate);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping(path = "api/shop/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> bulkUpload(@RequestPart("file") MultipartFile file) {
        try {
            List<ProductRequest> products = serv.uploadBulkProduct(file);

            // TODO: persist products (e.g., productService.saveAll(products));

            Map<String, Object> body = new HashMap<>();
            body.put("count", products.size());
            body.put("items", products);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error("Bad CSV: " + ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(error("Upload failed: " + ex.getMessage()));
        }
    }

    private Map<String, String> error(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("message", message);
        return map;
    }

  /*  @GetMapping("api/shop/get/old/invoice/{orderReferenceNumber}")
    public ResponseEntity<byte[]> downloadStyledInvoice(@PathVariable String orderReferenceNumber) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos = serv.generateOrderInvoice(orderReferenceNumber);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=invoice-" + orderReferenceNumber + ".pdf")
                .contentType(MediaType.APPLICATION_PDF).body(baos.toByteArray());
    }*/

    @PostMapping("api/shop/report")
    ResponseEntity<byte[]> generateReport(@RequestBody ReportRequest request) {

        System.out.println("The request payload for billing app is-->" + request);

        byte[] response = serv.generateReport(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=REP-" + request.getReportType() + ".xlsx")
                .contentType(MediaType.APPLICATION_PDF).body(response);
    }

    @PostMapping("api/shop/report/saveDetails")
    ResponseEntity<String> saveReportDetails(@RequestBody Report request) {

        System.out.println("The request payload for saveReportDetails  is-->" + request);

        String response = serv.saveReportDetails(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("api/shop/report/recent")
    ResponseEntity<List<ReportResponse>> getReportDetails(@RequestParam Integer limit) {

        System.out.println("The request payload for getReportDetails  is-->" + limit);

        List<ReportResponse> response = serv.getReportsList(limit);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @PutMapping("api/shop/user/edit/{userId}")
    public ResponseEntity<UpdateUserDTO> updateUser(
            @PathVariable String userId,
            @RequestBody UpdateUserDTO userRequest) throws IOException {

        UpdateUserDTO response = serv.saveEditableUser(userRequest, userId);
        return ResponseEntity.ok(response);
    }


    @PutMapping(value = "api/shop/user/edit/profilePic/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateUserProfilePic(
            @PathVariable String userId,
            @RequestPart(value = "profilePic", required = false) MultipartFile profilePic) throws IOException {

        String response = serv.saveEditableUserProfilePic(profilePic, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("api/shop/user/get/userprofile/{username}")
    public ResponseEntity<UpdateUserDTO> getUserProfile(
            @PathVariable String username) throws IOException {

        UpdateUserDTO response = serv.getUserProfile(username);
        return ResponseEntity.ok(response);
    }

    @GetMapping("api/shop/user/{username}/profile-pic")
    public ResponseEntity<byte[]> getProfilePic(@PathVariable String username) throws IOException {

        byte[] imageBytes = serv.getProfilePic(username);

        if (imageBytes == null || imageBytes.length == 0) {
            return ResponseEntity.notFound().build();
        }

        // You can detect MIME type if you stored it in DB, or assume JPEG/PNG
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);

        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }


    @GetMapping("api/shop/get/invoice/{orderId}")
    public ResponseEntity<byte[]> generateInvoice(@PathVariable String orderId) {
        try {
            byte[] pdfContents = serv.generateGSTInvoicePdf(orderId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            // Instructs the browser to download the file with a specific name
            headers.setContentDispositionFormData("attachment", "invoice.pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfContents);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("api/shop/get/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @RequestBody AnalyticsRequest request) {

        System.out.println("Entered analytic controller with payload-->" + request);

        AnalyticsResponse response = serv.getAnalytics(request);

        AnalyticsResponse response2 =  AnalyticsResponse.builder()
                .labels(Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"))
                .sales(Arrays.asList(1200L, 1500L, 1800L, 2000L, 2200L, 2500L,
                        2700L, 2600L, 2800L, 3000L, 3200L, 3500L))
                .stocks(Arrays.asList(300L, 280L, 260L, 240L, 230L, 220L,
                        210L, 200L, 190L, 185L, 180L, 175L))
                .taxes(Arrays.asList(120, 150, 180, 200, 220, 250,
                        270, 260, 280, 300, 320, 350))
                .customers(Arrays.asList(50, 65, 70, 80, 90, 100,
                        110, 105, 120, 125, 130, 140))
                .profits(Arrays.asList(500L, 600L, 750L, 800L, 900L, 1000L,
                        1100L, 1050L, 1200L, 1250L, 1300L, 1400L))
                .onlinePayments(Arrays.asList(30, 40, 55, 60, 70, 85,
                        90, 88, 95, 100, 110, 120))
                .build();


        return ResponseEntity.ok(response);
    }

    @GetMapping("api/shop/get/order/{saleId}")
    public ResponseEntity<InvoiceDetails> getOrderDetails(
            @PathVariable String saleId) {

        System.out.println("Entered analytic getOrderDetails with payload-->" + saleId);

        InvoiceDetails response = serv.getOrderDetails(saleId);


        return ResponseEntity.ok(response);
    }
    @PostMapping("api/user/logout")
    public ResponseEntity<Map<String, Object>> logoutUser(
            HttpServletResponse httpResponse) {

        System.out.println("Inside the logout method");

        Map<String, Object> response = new HashMap<>();

        Cookie cookie = new Cookie("jwt", null);
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            cookie.setHttpOnly(true);       // ✅ Prevent JS access
            cookie.setSecure(true);         // ✅ Required for HTTPS
            cookie.setPath("/");            // ✅ Makes cookie accessible for all paths
            cookie.setMaxAge(0);         // ✅ 1 hour
            cookie.setDomain(".clearbill.store"); // ✅ Share across subdomains
// Note: cookie.setSameSite("None"); is not available directly in Servlet Cookie API

            httpResponse.addHeader("Set-Cookie",
                    "jwt=" + null + "; Path=/; HttpOnly; Secure; SameSite=None; Domain=.clearbill.store; Max-Age=3600");
        } else {
            cookie.setHttpOnly(true);      // Prevent JS access
            cookie.setSecure(false);       // ✅ In dev, must be false (unless using HTTPS with localhost)
            cookie.setPath("/");           // Available on all paths
            cookie.setMaxAge(0);
            cookie.setDomain("localhost");// 1 hour
// Do NOT set cookie.setDomain(...)

            httpResponse.addCookie(cookie);
        }
        response.put("status", Boolean.TRUE);



        return ResponseEntity.ok(response);
    }

    @GetMapping("api/shop/notifications/unseen")
    public ResponseEntity<Map<String, Object>> getUnseenNotifications() {

        //List<NotificationDTO> response = serv.getUnseenNotifications();


        NotificationDTO response = serv.getAllNotifications(1, 5, "desc", "all", "unseen", "desc");


        Map<String, Object> response2 = new HashMap<>();
        response2.put("notifications", response.getNotifications());
        response2.put("count", response.getNotifications().size());


        System.out.println(response);

        return ResponseEntity.status(HttpStatus.OK).body(response2);

    }

    @GetMapping("api/shop/notifications/all")
    public ResponseEntity<Map<String, Object>> getAllNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "all") String domain,
            @RequestParam(defaultValue = "all") String seen,
            @RequestParam(defaultValue = "desc") String sort) {

        NotificationDTO response = serv.getAllNotifications(page, limit, sort, domain, seen, sort);


        Map<String, Object> response2 = new HashMap<>();
        response2.put("notifications", response.getNotifications());
        response2.put("totalPages", response.getCount());


        System.out.println(response);

        return ResponseEntity.status(HttpStatus.OK).body(response2);

    }

    @PostMapping("/api/shop/notifications/update-status")
    public ResponseEntity<String> updateNotificationStatus(@RequestBody NotificationStatusUpdateRequest request) {
        serv.updateNotificationStatus(request);
        return ResponseEntity.ok("Notification status updated successfully");
    }
    @PostMapping("/api/shop/notifications/flag/{notificationId}")
    public ResponseEntity<Map<String, Object>> flagNotifications(
            @PathVariable Integer notificationId,
    @RequestBody Map<String, Boolean> requestBody)
    {

        Boolean flagged= requestBody.get("flagged");
       System.out.println(flagged);
        Map<String, Object> response= serv.flagNotifications(notificationId,flagged);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/api/shop/notifications/delete/{notificationId}")
    public ResponseEntity<Map<String, Object>> deleteNotifications(
            @PathVariable Integer notificationId)
    {
        Map<String, Object> response= serv.deleteNotifications(notificationId);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/api/shop/availablePaymentMethod")
    public ResponseEntity<Map<String, Boolean>> getPaymentMethods() {

        Map<String, Boolean> response=serv.getAvailablePaymentMethods();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/razorpay/create-order")
    public String createOrder(@RequestBody CreateOrderRequest request) throws RazorpayException {
        System.out.println("Inside the createOrder method for card payment");
        RazorpayClient razorpayClient = new RazorpayClient(keyId, keySecret);
        System.out.println("Razorpay client created with keyId: " + keyId);
        System.out.println("Razorpay client created with keySecret: " + keySecret);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", request.getAmount()); // amount in the smallest currency unit (e.g., paise)
        orderRequest.put("currency", request.getCurrency());
        orderRequest.put("receipt", "receipt_order_" + System.currentTimeMillis());

        Order order = razorpayClient.orders.create(orderRequest);

        return order.toString();
    }

    @PostMapping("/api/razorpay/verify-payment")
    // 2. Use the new combined request DTO
    public ResponseEntity<?> verifyPayment(@RequestBody VerifyAndBillRequest request) {
        System.out.println("Inside the verifyPayment method for card payment");

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.razorpay_order_id);
            options.put("razorpay_payment_id", request.razorpay_payment_id);
            options.put("razorpay_signature", request.razorpay_signature);

            boolean isValid = Utils.verifyPaymentSignature(options, this.keySecret);

            if (isValid) {
                // 3. If signature is valid, call your billing service!
                System.out.println("Payment verified. Proceeding to save the bill." + request.getRazorpay_payment_id());
                System.out.println("Payment verified. Proceeding to save the bill." + request.getRazorpay_order_id());
                System.out.println("Payment verified. Proceeding to save the bill." + request.getRazorpay_signature());

                // This replaces the direct call to /api/shop/do/billing
                BillingResponse billingResponse = serv.doPayment(request.billingDetails);

                if(billingResponse!=null && request.getRazorpay_payment_id()!=null){
                    serv.updatePaymentReferenceNumber(request.getRazorpay_payment_id(), billingResponse.getInvoiceNumber());
                }

                // Return the response from your billing service
                return ResponseEntity.ok(billingResponse);
            } else {
                return ResponseEntity.status(400).body("Invalid payment signature.");
            }
        } catch (Exception e) {
            // Your service's doPayment method might throw an exception
            return ResponseEntity.status(500).body("Error during billing: " + e.getMessage());
        }
    }

    @GetMapping("api/shop/get/analytics/weekly-sales/{range}")
    public ResponseEntity<List<WeeklySales>> getWeeklyAnalytics(@PathVariable String range) {

        System.out.println("Entered getWeeklyAnalytics controller with payload-->");

        List<WeeklySales> response = serv.getWeeklyAnalytics(range);




        return ResponseEntity.ok(response);
    }

    @PostMapping("api/shop/update/goals")
    public ResponseEntity<String> updateSalesEstimates(@RequestBody GoalRequest goalRequest) {

        System.out.println("Entered updateSalesEstimates controller with payload-->"+goalRequest);

        String response = serv.updateEstimatedGoals(goalRequest);
        return ResponseEntity.ok(response);
    }
    @GetMapping("api/shop/get/dashboard/goals/{timeRange}")
    public ResponseEntity<GoalData> getGoalData(@PathVariable String timeRange) {

        System.out.println("Entered getGoalData controller with payload-->"+timeRange);

        GoalData goalData = serv.getTimeRangeGoalData(timeRange);
        return ResponseEntity.ok(goalData);
    }
    @GetMapping("api/shop/get/top/products")
    public ResponseEntity<List<TopProductDto>> getTopProducts(
            @RequestParam(name = "count", defaultValue = "3") int count,
            @RequestParam("timeRange") String timeRange,
            @RequestParam("factor") String factor) {

        System.out.println("Entered getTopProducts controller with count: " + count +
                ", timeRange: " + timeRange + ", factor: " + factor);
        // Call the service to get the data
        List<TopProductDto> topProducts = serv.getTopProducts(count, timeRange, factor);

        // Return the data with a 200 OK status
        return ResponseEntity.ok(topProducts);
    }
    @GetMapping("api/shop/get/top/orders")
    public ResponseEntity<List<TopOrdersDto>> getTopOrders(
            @RequestParam(name = "count", defaultValue = "3") int count,
            @RequestParam("timeRange") String timeRange) {

        System.out.println("Entered getTopProducts controller with count: " + count +
                ", timeRange: " + timeRange);
        // Call the service to get the data
        List<TopOrdersDto> topOrders = serv.getTopOrders(count, timeRange);

        // Return the data with a 200 OK status
        return ResponseEntity.ok(topOrders);
    }

    @GetMapping("api/shop/get/payments/breakdown/{timeRange}")
    public ResponseEntity<Map<String, Double>> getPaymentBreakdown(@PathVariable String timeRange) {


        System.out.println("Entered getPaymentBreakdown controller "+
                ", timeRange: " + timeRange);
        // Call the service to get the data
       Map<String, Double> response = serv.getPaymentBreakdown(timeRange);

        // Return the data with a 200 OK status
        return ResponseEntity.ok(response);
    }



    // 1️⃣ Upload Shop Logo
    @PutMapping("api/shop/user/edit/details/shopLogo")
    public ResponseEntity<String> updateShopLogo(@RequestParam("shopLogo") MultipartFile shopLogo) throws IOException {
        String response=    serv.updateShopLogo(shopLogo);
        return ResponseEntity.ok("Shop logo updated successfully");
    }

    // 2️⃣ Update Basic Details
    @PutMapping("api/shop/user/edit/details/basic")
    public ResponseEntity<String> updateBasicDetails(@RequestBody ShopBasicDetailsRequest request) {
       String response= serv.updateBasicDetails(request);
        return ResponseEntity.ok("Basic details updated successfully");
    }

    // 3️⃣ Update Finance Details
    @PutMapping("api/shop/user/edit/details/finance")
    public ResponseEntity<String> updateFinanceDetails(@RequestBody ShopFinanceDetailsRequest request) {
        String response=  serv.updateFinanceDetails(request);
        return ResponseEntity.ok("Finance details updated successfully");
    }

    // 4️⃣ Update Other Details
    @PutMapping("api/shop/user/edit/details/others")
    public ResponseEntity<String> updateOtherDetails(@RequestBody ShopInvoiceTerms request) {
        System.out.println("Entered updateOtherDetails controller with payload-->"+request);
        String response=  serv.updateOtherDetails(request);
        return ResponseEntity.ok("Other details updated successfully");
    }



    @GetMapping("api/shop/user/{username}/shop-logo")
    public ResponseEntity<byte[]> getShopLogo(@PathVariable String username) throws IOException {

        byte[] imageBytes = serv.getShopLogo(username);

        if (imageBytes == null || imageBytes.length == 0) {
            return ResponseEntity.notFound().build();
        }

        // You can detect MIME type if you stored it in DB, or assume JPEG/PNG
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);

        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

    @GetMapping("api/shop/get/forGSTBilling/withCache/productsList")
    public ResponseEntity<Map<String, Object>> searchProducts(
            @RequestParam(value = "q", required = false, defaultValue = "") String query,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {

        // Call the service to perform the business logic
        List<ProductSearchDto> products = serv.findProductsByQuery(query, limit);

        // Create a response structure that matches what your frontend expects (data.data)
        Map<String, Object> response = new HashMap<>();
        response.put("data", products);

        return ResponseEntity.ok(response);
    }
}
