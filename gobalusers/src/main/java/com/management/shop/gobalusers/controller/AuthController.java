package com.management.shop.gobalusers.controller;

import com.management.shop.gobalusers.dto.*;
import com.management.shop.gobalusers.entity.UserInfo;
import com.management.shop.gobalusers.service.AuthPhoneService;
import com.management.shop.gobalusers.service.AuthService;
import com.management.shop.gobalusers.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;


@RestController
public class AuthController {

    @Autowired
    private Environment environment;

    private final AuthenticationManager authenticationManager;
    private final AuthService serv;
    private final JwtService jwtService;
    private final AuthPhoneService authPhoneService;

    // ✅ Constructor Injection
    public AuthController(AuthenticationManager authenticationManager,
                          AuthService serv,
                          JwtService jwtService,
                          AuthPhoneService authPhoneService) {
        this.authenticationManager = authenticationManager;
        this.serv = serv;
        this.jwtService = jwtService;
        this.authPhoneService=authPhoneService;
    }


    @PostMapping("auth/new/user")
    public String addNewUser(@RequestBody UserInfo userInfo) {
        return serv.addUser(userInfo);
    }

    @GetMapping("auth/new/welcome")
    public ResponseEntity<String> welcome() {
        return ResponseEntity.status(HttpStatus.OK).body("welcome to the app");

    }

    @PostMapping("auth/new/google/user")
    public ResponseEntity<GoogleAuthResponse> addNewGoogleUser(@RequestBody GoogleLoginRequest request, HttpServletResponse httpResponse) throws Exception {

        GoogleAuthResponse response=     serv.googleLogin(request, httpResponse);

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @PostMapping("/auth/validate-contact")
    public ValidateContactResponse validateContact(@RequestBody ValidateContactRequest userInfo) {
        System.out.println("Entered validateContact with payload  " + userInfo);
        return serv.validateContact(userInfo);
    }

    @PostMapping("/auth/forgot-password")
    public ValidateContactResponse forgotPassword(@RequestBody ForgotPassRequest forgotPassRequest) {
        System.out.println("Entered forgotPassword with payload  " + forgotPassRequest);
        return serv.forgotPaswrod(forgotPassRequest);
    }

    @PostMapping("/auth/update-password")
    public ValidateContactResponse confirmOtpAndUpdatePassword(@RequestBody UpdatePasswordRequest updatePassRequest) {
        System.out.println("Entered confirmOtpAndUpdatePassword with payload  " + updatePassRequest);
        return serv.confirmOtpAndUpdatePassword(updatePassRequest);
    }

    @PostMapping("/auth/register/newuser")
    public RegisterResponse addNewThirdPartyUser(@RequestBody RegisterRequest userInfo) {
        System.out.println("Entered addNewThirdPartyUser with payload  " + userInfo);
        return serv.registerNewUser(userInfo);
    }



    @PostMapping("/auth/resend-otp")
    public OtpVerifyResponse reEnterOtp(@RequestBody OtpVerifyRequest userInfo) {
        System.out.println("Entered reEnterOtp with payload  for user" + userInfo);
        return serv.reEnterOtp(userInfo);
    }

    @GetMapping("auth/otp-retry-count")
    public Map<String, String> fetchRetries(@RequestParam String username) {
        System.out.println("Entered fetchRetries with payload  from frontend" + username);
        return serv.fetchRetries(username);
    }



    @PostMapping("/auth/verify-otp")
    public OtpVerifyResponse verifyOTP(@RequestBody OtpVerifyRequest userInfo) {
        System.out.println("Entered verifyOTP with payload  " + userInfo);
        return serv.verifyOTP(userInfo);
    }

    @PostMapping("/auth/authenticate")
    public String authenticateAndGetToken(@RequestBody AuthRequest authRequest, HttpServletRequest request, HttpServletResponse response) {

        // 🟢 Passed 'request' so the service can determine the correct domain (.info or .store)
        String token = serv.authAndsetCookies(authRequest, request, response);
        return token;

    }


    @GetMapping("auth/phone/otp-retry-count")
    public Map<String, String> fetchRetiesForToday(@RequestParam String phone) {
        System.out.println("Entered fetchRetries for today with payload  " + phone);
        return authPhoneService.fetchRetriesForToday(phone);
    }

    @PostMapping("/auth/register/phone/newuser")
    public RegisterResponse addNewThirdPartyUserWithPhone(@RequestBody RegisterRequest userInfo) {
        System.out.println("Entered addNewThirdPartyUser with payload  " + userInfo);
        return authPhoneService.registerNewUserWithPhone(userInfo);
    }

    @PostMapping("/auth/phone/resend-otp")
    public OtpVerifyResponse reSendOtpPhone(@RequestBody OtpVerifyRequest userInfo) {
        System.out.println("Entered reSendOtpPhone with payload  " + userInfo);
        return authPhoneService.reSendOtpPhone(userInfo);
    }
    @PostMapping("/auth/phone/verify-otp")
    public OtpVerifyResponse verifyOTPPhone(@RequestBody OtpVerifyRequest userInfo) {
        System.out.println("Entered verifyOTP with payload  " + userInfo);
        return authPhoneService.verifyOTP(userInfo);
    }



    @PostMapping("/auth/phone/forgot-password")
    public ValidateContactResponse forgotPasswordPhone(@RequestBody ForgotPassRequest forgotPassRequest) {
        System.out.println("Entered forgotPassword with payload  " + forgotPassRequest);
        return authPhoneService.forgotPasswordPhone(forgotPassRequest);
    }

    @PostMapping("/auth/phone/update-password")
    public ValidateContactResponse confirmOtpAndUpdatePasswordPhone(@RequestBody UpdatePasswordRequest updatePassRequest) {
        System.out.println("Entered confirmOtpAndUpdatePassword with payload  " + updatePassRequest);
        return authPhoneService.confirmOtpAndUpdatePasswordPhone(updatePassRequest);
    }
}
