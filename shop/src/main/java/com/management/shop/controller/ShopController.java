package com.management.shop.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.management.shop.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ShopController {

	@Autowired
	ShopService serv;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthenticationManager authenticationManager;

	@PostMapping("auth/new/user")
	public String addNewUser(@RequestBody UserInfo userInfo) {
		return serv.addUser(userInfo);
	}

	@PostMapping("/auth/validate-contact")
	public ValidateContactResponse validateContact(@RequestBody ValidateContactRequest userInfo) {
		System.out.println("Entered validateContact with payload  "+userInfo);
		return serv.validateContact(userInfo);
	}
	
	@PostMapping("/auth/register/newuser")
	public RegisterResponse addNewThirdPartyUser(@RequestBody RegisterRequest userInfo) {
		System.out.println("Entered addNewThirdPartyUser with payload  "+userInfo);
		return serv.registerNewUser(userInfo);
	}
	@PostMapping("/auth/resend-otp")
	public OtpVerifyResponse reEnterOtp(@RequestBody OtpVerifyRequest userInfo) {
		System.out.println("Entered reEnterOtp with payload  "+userInfo);
		return serv.reEnterOtp(userInfo);
	}


	@PostMapping("/auth/verify-otp")
	public OtpVerifyResponse verifyOTP(@RequestBody OtpVerifyRequest userInfo) {
		System.out.println("Entered verifyOTP with payload  "+userInfo);
		return serv.verifyOTP(userInfo);
	}

	@PostMapping("auth/authenticate")
	public String authenticateAndGetToken(@RequestBody AuthRequest authRequest) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));
		
		//boolean isUserActive =serv.checkUserStatus(authRequest.getUsername());
		
		if (authentication.isAuthenticated()) {
			String token = jwtService.generateToken(authRequest.getUsername());
			// System.out.println("The generated token --> "+token);
			return token;
		} else {
			throw new UsernameNotFoundException("invalid user request !");
		}

	}
	
	@PostMapping("api/shop/user/updatepassword")
	public String addUpdatePassword(@RequestBody UserInfo userInfo) {
		System.out.println(userInfo.toString());
		return serv.updatePassword(userInfo);
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
	ResponseEntity<ProductSuccessDTO> updateCustomer(@RequestBody ProductRequest request) {

		ProductSuccessDTO response = serv.updateProduct(request);

		return ResponseEntity.status(HttpStatus.OK).body(response);

	}

	@GetMapping("api/shop/get/productsList")
	ResponseEntity<List<ProductEntity>> getProductsList() {

		List<ProductEntity> response = serv.getAllProducts();

		return ResponseEntity.status(HttpStatus.OK).body(response);

	}

	@PostMapping("api/shop/do/billing")
	ResponseEntity<BillingResponse> doBilling(@RequestBody BillingRequest request) throws Exception {

		System.out.println("The request payload for billing app is-->" + request);

		BillingResponse response = serv.doPayment(request);

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	@GetMapping("api/shop/get/sales")
	ResponseEntity<List<SalesResponseDTO>> getSalesList() {

		List<SalesResponseDTO> response = serv.getAllSales();

		return ResponseEntity.status(HttpStatus.OK).body(response);

	}

	@GetMapping("/api/shop/get/sales/withPages")
	ResponseEntity<Page<SalesResponseDTO>> getSalesListWithPagination(@RequestParam int page,
			@RequestParam int size) {

		Page<SalesResponseDTO> response = serv.getAllSalesWithPagination(page, size);

		return ResponseEntity.status(HttpStatus.OK).body(response);

	}

	@GetMapping("api/shop/get/dashboardDetails/{range}")
	ResponseEntity<DasbboardResponseDTO> getDashBoardDetails(@PathVariable String range) {

		DasbboardResponseDTO response = serv.getDashBoardDetails(range);

		return ResponseEntity.status(HttpStatus.OK).body(response);

	}

	@GetMapping("api/shop/get/paymentLists")
	ResponseEntity<List<PaymentDetails>> getPaymentList() {
		List<PaymentDetails> response = serv.getPaymentList();

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

	@GetMapping("api/shop/get/old/invoice/{orderReferenceNumber}")
	public ResponseEntity<byte[]> downloadStyledInvoice(@PathVariable String orderReferenceNumber) {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos = serv.generateOrderInvoice(orderReferenceNumber);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=invoice-" + orderReferenceNumber + ".pdf")
				.contentType(MediaType.APPLICATION_PDF).body(baos.toByteArray());
	}

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
	
	
	
	@PutMapping( "api/shop/user/edit/{userId}")
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
	public ResponseEntity<UpdateUserDTO> updateUserProfilePic(
	        @PathVariable String username) throws IOException {
	    
		UpdateUserDTO response = serv.getUserProfile( username);
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
	            byte[] pdfContents = serv.generateInvoicePdf(orderId);

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
	    	
	    	System.out.println("Entered analytic controller with payload-->"+ request);

	    	AnalyticsResponse response2=	serv.getAnalytics(request);


			return ResponseEntity.ok(response2);
		}

    @GetMapping("api/shop/get/order/{saleId}")
    public ResponseEntity<InvoiceDetails> getOrderDetails(
            @PathVariable String saleId) {

        System.out.println("Entered analytic getOrderDetails with payload-->" + saleId);

        InvoiceDetails response = serv.getOrderDetails(saleId);


        return ResponseEntity.ok(response);
    }

}
