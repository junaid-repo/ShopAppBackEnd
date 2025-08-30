package com.management.shop.gobalusers.controller;

import com.management.shop.gobalusers.dto.*;
import com.management.shop.gobalusers.entity.UserInfo;
import com.management.shop.gobalusers.service.AuthService;
import com.management.shop.gobalusers.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AuthService serv;
    private final JwtService jwtService;

    // ✅ Constructor Injection
    public AuthController(AuthenticationManager authenticationManager,
                          AuthService serv,
                          JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.serv = serv;
        this.jwtService = jwtService;
    }


    @PostMapping("auth/new/user")
    public String addNewUser(@RequestBody UserInfo userInfo) {
        return serv.addUser(userInfo);
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
        System.out.println("Entered reEnterOtp with payload  " + userInfo);
        return serv.reEnterOtp(userInfo);
    }


    @PostMapping("/auth/verify-otp")
    public OtpVerifyResponse verifyOTP(@RequestBody OtpVerifyRequest userInfo) {
        System.out.println("Entered verifyOTP with payload  " + userInfo);
        return serv.verifyOTP(userInfo);
    }

    @PostMapping("/auth/authenticate")
    public String authenticateAndGetToken(@RequestBody AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));

        //boolean isUserActive =serv.checkUserStatus(authRequest.getUsername());

        if (authentication.isAuthenticated()) {
            String token = jwtService.generateToken(authRequest.getUsername());
             System.out.println("The generated token --> "+token);
            return token;
        } else {
            throw new UsernameNotFoundException("invalid user request !");
        }

    }
}
