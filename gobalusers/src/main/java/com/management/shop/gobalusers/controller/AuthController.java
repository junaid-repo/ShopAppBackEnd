package com.management.shop.gobalusers.controller;

import com.management.shop.gobalusers.dto.*;
import com.management.shop.gobalusers.entity.RefreshToken;
import com.management.shop.gobalusers.entity.UserInfo;
import com.management.shop.gobalusers.service.AuthPhoneService;
import com.management.shop.gobalusers.service.AuthService;
import com.management.shop.gobalusers.service.JwtService;
import com.management.shop.gobalusers.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
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

import java.util.*;


@RestController
@Slf4j
public class AuthController {

    @Autowired
    private Environment environment;
    @Autowired
    private RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final AuthService serv;
    private final JwtService jwtService;
    private final AuthPhoneService authPhoneService;

    // ✅ Constructor Injection
    public AuthController(AuthenticationManager authenticationManager,
                          AuthService serv,
                          JwtService jwtService,
                          AuthPhoneService authPhoneService,
                          RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.serv = serv;
        this.jwtService = jwtService;
        this.authPhoneService = authPhoneService;
        this.refreshTokenService = refreshTokenService;
    }


 /*   @PostMapping("auth/new/user")
    public String addNewUser(@RequestBody UserInfo userInfo) {
        return serv.addUser(userInfo);
    }*/

    @GetMapping("auth/new/welcome")
    public ResponseEntity<String> welcome() {
        return ResponseEntity.status(HttpStatus.OK).body("welcome to the app");

    }

    @PostMapping("auth/new/google/user")
    public ResponseEntity<GoogleAuthResponse> addNewGoogleUser(
            @RequestBody GoogleLoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse httpResponse) throws Exception {


        GoogleAuthResponse response = serv.googleLogin(loginRequest, request, httpResponse);
        log.info("The final response from googleLogin in controller is --> " + response);

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @PostMapping("/auth/validate-contact")
    public ValidateContactResponse validateContact(@RequestBody ValidateContactRequest userInfo) {
        log.info("Entered validateContact with payload  " + userInfo);
        return serv.validateContact(userInfo);
    }

    @PostMapping("/auth/forgot-password")
    public ValidateContactResponse forgotPassword(@RequestBody ForgotPassRequest forgotPassRequest) {
        log.info("Entered forgotPassword with payload  " + forgotPassRequest);
        return serv.forgotPaswrod(forgotPassRequest);
    }

    @PostMapping("/auth/update-password")
    public ValidateContactResponse confirmOtpAndUpdatePassword(@RequestBody UpdatePasswordRequest updatePassRequest) {
        log.info("Entered confirmOtpAndUpdatePassword with payload  " + updatePassRequest);
        return serv.confirmOtpAndUpdatePassword(updatePassRequest);
    }

    @PostMapping("/auth/register/newuser")
    public RegisterResponse addNewThirdPartyUser(@RequestBody RegisterRequest userInfo) {
        log.info("Entered addNewThirdPartyUser with payload  " + userInfo);
        return serv.registerNewUser(userInfo);
    }



    @PostMapping("/auth/resend-otp")
    public OtpVerifyResponse reEnterOtp(@RequestBody OtpVerifyRequest userInfo) {
        log.info("Entered reEnterOtp with payload  for user" + userInfo);
        return serv.reEnterOtp(userInfo);
    }

    @GetMapping("auth/otp-retry-count")
    public Map<String, String> fetchRetries(@RequestParam String username) {
        log.info("Entered fetchRetries with payload  from frontend" + username);
        return serv.fetchRetries(username);
    }



    @PostMapping("/auth/verify-otp")
    public OtpVerifyResponse verifyOTP(@RequestBody OtpVerifyRequest userInfo) {
        log.info("Entered verifyOTP with payload  " + userInfo);
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
        log.info("Entered fetchRetries for today with payload  " + phone);
        return authPhoneService.fetchRetriesForToday(phone);
    }

    @PostMapping("/auth/register/phone/newuser")
    public RegisterResponse addNewThirdPartyUserWithPhone(@RequestBody RegisterRequest userInfo) {
        log.info("Entered addNewThirdPartyUser with payload  " + userInfo);
        return authPhoneService.registerNewUserWithPhone(userInfo);
    }

    @PostMapping("/auth/phone/resend-otp")
    public OtpVerifyResponse reSendOtpPhone(@RequestBody OtpVerifyRequest userInfo) {
        log.info("Entered reSendOtpPhone with payload  " + userInfo);
        return authPhoneService.reSendOtpPhone(userInfo);
    }
    @PostMapping("/auth/phone/verify-otp")
    public OtpVerifyResponse verifyOTPPhone(@RequestBody OtpVerifyRequest userInfo) {
        log.info("Entered verifyOTP with payload  " + userInfo);
        return authPhoneService.verifyOTP(userInfo);
    }



    @PostMapping("/auth/phone/forgot-password")
    public ValidateContactResponse forgotPasswordPhone(@RequestBody ForgotPassRequest forgotPassRequest) {
        log.info("Entered forgotPassword with payload  " + forgotPassRequest);
        return authPhoneService.forgotPasswordPhone(forgotPassRequest);
    }

    @PostMapping("/auth/phone/update-password")
    public ValidateContactResponse confirmOtpAndUpdatePasswordPhone(@RequestBody UpdatePasswordRequest updatePassRequest) {
        log.info("Entered confirmOtpAndUpdatePassword with payload  " + updatePassRequest);
        return authPhoneService.confirmOtpAndUpdatePasswordPhone(updatePassRequest);
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshAccessToken(
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            HttpServletRequest request,
            HttpServletResponse response) {

        // 1. Gather all candidate refresh tokens from cookies (in case of duplicate/stale path cookies)
        List<String> candidates = new ArrayList<>();
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (serv.getRefreshCookieName().equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    candidates.add(cookie.getValue().trim());
                }
            }
        }

        // Fallback: check request body if cookie was not provided
        if (requestBody != null && requestBody.getRefreshToken() != null && !requestBody.getRefreshToken().isBlank()) {
            candidates.add(requestBody.getRefreshToken().trim());
        }

        if (candidates.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(RefreshTokenResponse.builder()
                            .success(false)
                            .message("Refresh token is missing")
                            .build());
        }

        try {
            // 2. Validate token: search candidates for an active, non-revoked token
            RefreshToken validToken = null;
            for (String candidate : candidates) {
                Optional<RefreshToken> tokenOpt = refreshTokenService.findByToken(candidate);
                if (tokenOpt.isPresent()) {
                    RefreshToken t = tokenOpt.get();
                    if (!t.isRevoked() && !t.getExpiryDate().isBefore(java.time.Instant.now())) {
                        validToken = t;
                        break;
                    }
                }
            }

            if (validToken == null) {
                throw new RuntimeException("Invalid or revoked refresh token");
            }

            String username = validToken.getUsername();

            // 3. Token Rotation: Revoke old refresh token, generate a new one
            RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(validToken);
            serv.setRefreshTokenCookie(newRefreshToken.getToken(), request, response);

            // 4. Generate fresh short-lived JWT Access Token
            String newAccessToken = jwtService.generateToken(username);

            // 5. Update the Access Token Cookie using environment-specific domain & name
            serv.setAccessTokenCookie(newAccessToken, request, response);

            return ResponseEntity.ok(RefreshTokenResponse.builder()
                    .success(true)
                    .message("Token refreshed successfully")
                    .accessToken(newAccessToken)
                    .build());

        } catch (Exception e) {
            log.error("Failed to refresh token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(RefreshTokenResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<String> logout(
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshTokenStr = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (serv.getRefreshCookieName().equals(cookie.getName())) {
                    refreshTokenStr = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshTokenStr == null && requestBody != null) {
            refreshTokenStr = requestBody.getRefreshToken();
        }

        if (refreshTokenStr != null) {
            refreshTokenService.revokeToken(refreshTokenStr);
        }

        serv.clearAuthCookies(request, response);
        return ResponseEntity.ok("Logged out successfully");
    }
}
