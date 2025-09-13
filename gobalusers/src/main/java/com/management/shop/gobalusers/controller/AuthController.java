package com.management.shop.gobalusers.controller;

import com.management.shop.gobalusers.dto.*;
import com.management.shop.gobalusers.entity.UserInfo;
import com.management.shop.gobalusers.service.AuthService;
import com.management.shop.gobalusers.service.JwtService;
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

    @PostMapping("auth/new/welcome")
    public ResponseEntity<String> welcome(@RequestBody UserInfo userInfo) {
        return ResponseEntity.status(HttpStatus.OK).body("welcome to the app");

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

    @GetMapping("auth/otp-retry-count")
    public Map<String, String> fetchRetries(@RequestParam String username) {
        System.out.println("Entered fetchRetries with payload  " + username);
        return serv.fetchRetries(username);
    }


    @PostMapping("/auth/verify-otp")
    public OtpVerifyResponse verifyOTP(@RequestBody OtpVerifyRequest userInfo) {
        System.out.println("Entered verifyOTP with payload  " + userInfo);
        return serv.verifyOTP(userInfo);
    }

    @PostMapping("/auth/authenticate")
    public String authenticateAndGetToken(@RequestBody AuthRequest authRequest, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));

        boolean isUserActive = serv.checkUserStatus(authRequest.getUsername());
        System.out.println("The authentication object is --> " + authentication);
        if (authentication.isAuthenticated() && isUserActive) {
            String token = jwtService.generateToken(authRequest.getUsername());

            Cookie cookie = new Cookie("jwt", token);
            if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
                cookie.setHttpOnly(true);       // ✅ Prevent JS access
                cookie.setSecure(true);         // ✅ Required for HTTPS
                cookie.setPath("/");            // ✅ Makes cookie accessible for all paths
                cookie.setMaxAge(3600);         // ✅ 1 hour
                cookie.setDomain(".clearbill.store"); // ✅ Share across subdomains
// Note: cookie.setSameSite("None"); is not available directly in Servlet Cookie API

                response.addHeader("Set-Cookie",
                        "jwt=" + token + "; Path=/; HttpOnly; Secure; SameSite=None; Domain=.clearbill.store; Max-Age=3600");
            } else {
                cookie.setHttpOnly(true);      // Prevent JS access
                cookie.setSecure(true);       // Don't require HTTPS in dev
                cookie.setPath("/");           // Available on all paths
                cookie.setMaxAge(3600);        // 1 hour
                cookie.setDomain("localhost"); // Or remove for simpler case

                response.addCookie(cookie);
            }
            // System.out.println("The generated token --> "+token);
            return token;
        } else {
            throw new UsernameNotFoundException("invalid user request !");
        }

    }
}
