package com.management.shop.gobalusers.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.management.shop.gobalusers.constants.EventConstants;
import com.management.shop.gobalusers.dto.*;
import com.management.shop.gobalusers.entity.RegisterUserOTPEntity;
import com.management.shop.gobalusers.entity.UserInfo;
import com.management.shop.gobalusers.entity.UserPaymentModes;
import com.management.shop.gobalusers.repository.*;
import com.management.shop.gobalusers.util.AccountEmailTemplate;
import com.management.shop.gobalusers.util.OTPSender;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class AuthService {

    @Autowired
    private UserInfoRepository userinfoRepo;

    @Autowired
    private GoogleTokenVerifierService googleVerifier;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserProfilePicRepo userProfilePicRepo;

    @Autowired
    private UserOtpRepo otpRepo;

    @Autowired
    private AccountEmailTemplate emailTemplateUtil;

    @Autowired
    private final AuthenticationManager authenticationManager;

    @Autowired
    private Environment environment;

    private final JwtService jwtService;

    @Autowired
    private OTPSender otpSender;

    @Autowired
    private UserPaymentModesRepo paymentModesRepo;

    @Autowired
    private UserSettingsRepository userSetRepo;

    private final Random random = new Random();

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public String addUser(UserInfo userInfo) {
        userInfo.setRoles("USER");
        userInfo.setIsActive(true);
        userInfo.setPassword(passwordEncoder.encode(userInfo.getPassword()));
        userInfo.setCreatedAt(LocalDateTime.now());
        userInfo.setUpdatedAt(LocalDateTime.now());
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

        ValidateContactResponse validateContactResponse = validateContact(ValidateContactRequest.builder().phone(regRequest.getPhone()).email(regRequest.getEmail()).build());
        log.info("The validate contact response is --> " + validateContactResponse);
        if (validateContactResponse != null) {
            if (!validateContactResponse.isStatus()) {
                return RegisterResponse.builder().message("Email/Phone already registered").success(false).build();
            } else {

                Random random = new Random();
                int number = 100000 + random.nextInt(900000);

                RegisterUserOTPEntity res2 = otpRepo.getByUsername(validateContactResponse.getUsername());
                if (res2 != null) {
                    otpRepo.updateOldOTP(validateContactResponse.getUserId(), "stale");
                }
                MailjetResponse mailResponse = null;

                String htmlContent = emailTemplateUtil.registerUserOTP(regRequest.getFullName(), String.valueOf(number), String.valueOf(20));

                try {
                    mailResponse = otpSender.sendEmail(regRequest.getEmail(), "support@clearbill.store", regRequest.getFullName(), "Clear Bill",
                            "OTP for Register of new account", htmlContent);
                } catch (MailjetException | MailjetSocketTimeoutException e) {
                    e.printStackTrace();
                }
                if (mailResponse.getStatus() == 200) {
                    var regsiterUserTemp = RegisterUserOTPEntity.builder().username(validateContactResponse.getUsername())
                            .createdDate(LocalDateTime.now()).otp(String.valueOf(number)).status("fresh").retries(0).build();
                    otpRepo.save(regsiterUserTemp);
                    return RegisterResponse.builder().message("User created successfully. Please verify the OTP sent to your email to activate your account.").success(true).username(validateContactResponse.getUsername()).build();
                } else {
                    log.error("Failed to send OTP email to " + regRequest.getEmail() + ". Mailjet response status: " + mailResponse.getStatus());
                    return RegisterResponse.builder().message("Failed to send OTP email. Please try again later.").success(false).build();
                }
            }
        }

        var userInfo = UserInfo.builder().email(regRequest.getEmail()).isActive(false).name(regRequest.getFullName())
                .password(regRequest.getPassword()).phoneNumber(regRequest.getPhone())
                .source("email")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

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

                RegisterUserOTPEntity res2 = otpRepo.getByUsername(userInfo.getUsername());
                if (res2 != null) {
                    otpRepo.updateOldOTP(res.getId(), "stale");
                }
                MailjetResponse mailResponse = null;

                String htmlContent = emailTemplateUtil.registerUserOTP(regRequest.getFullName(), String.valueOf(number), String.valueOf(20));

                try {
                    mailResponse = otpSender.sendEmail(regRequest.getEmail(), "support@clearbill.store", regRequest.getFullName(), "Clear Bill",
                            "OTP for Register of new account", htmlContent);
                } catch (MailjetException | MailjetSocketTimeoutException e) {
                    e.printStackTrace();
                }
                if (mailResponse.getStatus() == 200) {
                    var regsiterUserTemp = RegisterUserOTPEntity.builder().username(res.getUsername())
                            .createdDate(LocalDateTime.now()).otp(String.valueOf(number)).status("fresh").retries(0).build();
                    otpRepo.save(regsiterUserTemp);
                    return RegisterResponse.builder().message("User created successfully. Please verify the OTP sent to your email to activate your account.").success(true).username(res.getUsername()).build();
                } else {
                    log.error("Failed to send OTP email to " + regRequest.getEmail() + ". Mailjet response status: " + mailResponse.getStatus());
                    return RegisterResponse.builder().message("Failed to send OTP email. Please try again later.").success(false).build();
                }
            }
        }

        return RegisterResponse.builder().message("Something went wrong while creating the user, try after sometime.").success(false).build();
    }

    public ValidateContactResponse validateContact(ValidateContactRequest userInfo) {
        List<UserInfo> res = userinfoRepo.validateContact(userInfo.getEmail(), userInfo.getPhone(), true);

        if (res.size() > 0) {
            return ValidateContactResponse.builder().userId(res.get(0).getId()).username(res.get(0).getUsername()).status(false).message("Email/Phone already registered").build();
        }
        return ValidateContactResponse.builder().status(true).message("Email/Phone already registered").build();
    }

    public ValidateContactResponse forgotPaswrod(ForgotPassRequest forgotPassRequest) {
        List<UserInfo> res = userinfoRepo.validateUser(forgotPassRequest.getEmailId(), forgotPassRequest.getUserId(), true);

        if (res.size() > 0) {
            log.info(String.valueOf(res.get(0)));
            Random random = new Random();
            int otp = 100000 + random.nextInt(900000);
            var otpVerifyReq = OtpVerifyRequest.builder().otp(String.valueOf(otp)).username(res.get(0).getUsername()).build();

            RegisterUserOTPEntity res2 = otpRepo.getByUsername(res.get(0).getUsername());
            if (res2 != null) {
                otpRepo.removeOldOTP(res.get(0).getUsername());
            }

            String htmlContent = emailTemplateUtil.generateForgetPasswordHtml(res.get(0).getUsername(), res.get(0).getName(), String.valueOf(otp), String.valueOf(20));

            try {
                otpSender.sendEmail(res.get(0).getEmail(), "support@clearbill.store", res.get(0).getName(), "Clear Bill",
                        "OTP for resetting you password", htmlContent);
            } catch (MailjetException | MailjetSocketTimeoutException e) {
                e.printStackTrace();
            }

            var regsiterUserTemp = RegisterUserOTPEntity.builder().username(res.get(0).getUsername())
                    .createdDate(LocalDateTime.now()).otp(String.valueOf(otp)).status("fresh").retries(0).build();
            otpRepo.save(regsiterUserTemp);

            return ValidateContactResponse.builder().status(true).message("OTP sent to your email Id").build();
        }
        return ValidateContactResponse.builder().status(false).message("No user found with provided details").build();
    }

    @Transactional
    public OtpVerifyResponse reEnterOtp(OtpVerifyRequest otpVerifyReq) {

        RegisterUserOTPEntity res = otpRepo.getByUsername(otpVerifyReq.getUsername());
        UserInfo userInfo = userinfoRepo.findByUsername(res.getUsername()).get();

        if (res != null) {
            otpRepo.updateOldOTP(res.getId(), "stale");
            Random random = new Random();
            int number = 100000 + random.nextInt(900000);

            try {
                otpSender.sendEmail(userInfo.getEmail(), "help@clearbill.store", userInfo.getName(), "Clear Bill",
                        "OPT Verification For Account", "Please enter OTP " + number + " to verify you account ");
            } catch (MailjetException | MailjetSocketTimeoutException e) {
                e.printStackTrace();
            }

            var regsiterUserTemp = RegisterUserOTPEntity.builder().username(res.getUsername())
                    .createdDate(LocalDateTime.now()).otp(String.valueOf(number)).status("fresh").retries(res.getRetries() + 1).build();
            otpRepo.save(regsiterUserTemp);
            otpRepo.removeOldOTPById(res.getId());
        }

        return OtpVerifyResponse.builder().success(true).username(otpVerifyReq.getUsername()).message("A new OTP has been resent to your registered email. Please check your inbox.").build();
    }


    @Transactional
    public OtpVerifyResponse verifyOTP(OtpVerifyRequest otpInfo) {
        RegisterUserOTPEntity res = otpRepo.getByUsername(otpInfo.getUsername());

        if (res.getOtp().equals(otpInfo.getOtp())) {

            userinfoRepo.updateUserStatus(res.getUsername());
            UserInfo userInfo = userinfoRepo.findByUsername(res.getUsername()).get();

            paymentModesRepo.save(UserPaymentModes.builder().userId(userInfo.getUsername()).cash(true).card(false).upi(true).createdBy("junaid1").updatedBy("junaid1").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

            String htmlContent = emailTemplateUtil.registerUserSucess(userInfo.getName(), userInfo.getUsername());

            try {
                otpSender.sendEmail(userInfo.getEmail(), "support@clearbill.store", userInfo.getName(), "Clear Bill",
                        "Account Creation Success", htmlContent);
            } catch (MailjetException | MailjetSocketTimeoutException e) {
                e.printStackTrace();
            }

            var response = OtpVerifyResponse.builder().success(true)
                    .username(otpInfo.getUsername())
                    .message("Registration complete! Your username is " + otpInfo.getUsername() + " Please login with this username and password to use the system.")
                    .build();
            return response;
        }
        var response = OtpVerifyResponse.builder().success(false)
                .username(otpInfo.getOtp())
                .message("Your entered OTP " + otpInfo.getOtp() + " is incorrect, please re-enter")
                .build();

        return response;
    }

    public ValidateContactResponse confirmOtpAndUpdatePassword(UpdatePasswordRequest updatePassRequest) {
        List<UserInfo> userInfo = userinfoRepo.validateUser(updatePassRequest.getEmailId(), updatePassRequest.getUserId(), true);

        if (userInfo.size() > 0) {
            RegisterUserOTPEntity otpedUser = otpRepo.getLatestOtp(userInfo.get(0).getUsername(), "fresh", EventConstants.PASSWORD_RESET_REQUESTED.getEventName(), userInfo.get(0).getUsername());

            if (otpedUser != null) {
                if (otpedUser.getOtp().equals(updatePassRequest.getOtp())) {
                    LocalDateTime updatedAt = LocalDateTime.now();
                    updatePassword(UserInfo.builder().username(userInfo.get(0).getUsername()).password(updatePassRequest.getNewPassword()).updatedAt(updatedAt).build());
                    return ValidateContactResponse.builder().status(true).message("Your password has been updated successfully").build();
                } else {
                    return ValidateContactResponse.builder().status(false).message("Your otp doesn't matched please re-enter").build();
                }
            }
        }
        return null;
    }

    public boolean checkUserStatus(String username) {
        return userinfoRepo.findByUsername(username).get().getIsActive();
    }

    public String updatePassword(UserInfo userInfo) {
        UserInfo userRes = userinfoRepo.findByUsername(userInfo.getUsername()).get();
        userRes.setPassword(passwordEncoder.encode(userInfo.getPassword()));
        userRes.setUpdatedAt(LocalDateTime.now());
        userinfoRepo.save(userRes);
        return "success";
    }

    public Map<String, String> fetchRetries(String username) {
        RegisterUserOTPEntity res = otpRepo.getByUsername(username);
        if (res != null) {
            return Map.of("retryLeft", String.valueOf(5 - res.getRetries()));
        }
        return Map.of("retryLeft", "0");
    }

    public GoogleAuthResponse googleLogin(GoogleLoginRequest loginRequest, HttpServletRequest request, HttpServletResponse httpServletResponse) throws Exception {
        GoogleAuthResponse response = new GoogleAuthResponse();
        try {
            GoogleIdToken.Payload payload = googleVerifier.verify(loginRequest.getIdToken());
            String email = payload.getEmail();
            String profilePicLink = (String) payload.get("picture");
            String sub = payload.getSubject(); // Google's user ID
            String name = (String) payload.get("name");

            log.info("google email ->" + email);
            log.info("google name ->" + name);
            log.info("google profilePicLink ->" + profilePicLink);

            String jwtToken = null;
            List<UserInfo> res = userinfoRepo.validateUser(email, "na", true);
            String secureToken= UUID.randomUUID().toString();
            log.info("The secure token generated for google login is --> " + secureToken);
            try {
                if (res.size() > 0) {
                    var authRequest = AuthRequest.builder().username(res.get(0).getUsername()).build();
                    jwtToken = authAndsetCookiesGoogle(authRequest, request, httpServletResponse);
                } else {
                    var userInfo = UserInfo.builder().email(email).isActive(true).name(name)
                            .phoneNumber("0000000000")
                            .password(passwordEncoder.encode(secureToken))
                            .source("google")
                            .profilePiclink(profilePicLink)
                            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

                    userInfo.setRoles("USER");
                    UserInfo userRes = userinfoRepo.save(userInfo);

                    if (userRes.getId() > 0) {
                        String username = userRes.getName().replace(" ", "").substring(0, 7).toLowerCase() + String.valueOf(userRes.getId());
                        userInfo.setUsername(username);
                        userinfoRepo.save(userInfo);
                        paymentModesRepo.save(UserPaymentModes.builder().userId(userInfo.getUsername()).cash(true).card(false).upi(true).createdBy("junaid1").updatedBy("junaid1").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

                        var authRequest = AuthRequest.builder().username(username).build();
                        jwtToken = authAndsetCookiesGoogle(authRequest, request, httpServletResponse);
                    }
                }

                if (jwtToken != null) {
                    response.setMessage("Login successful");
                    response.setSuccess(Boolean.TRUE);
                    response.setUsername(email);
                    response.setSecureToken(secureToken);
                    response.setToken(jwtToken);
                } else {
                    response.setMessage("Login unsuccessful");
                    response.setSuccess(Boolean.FALSE);
                    response.setToken(null);
                }
                System.out.println("The final response from googleLogin is --> " + response);
                log.info("The final response from googleLogin is --> " + response);
                return response;

            } catch (Exception e) {
                response.setMessage(e.getMessage());
                response.setSuccess(Boolean.FALSE);
                response.setToken(null);
            }
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            response.setSuccess(Boolean.FALSE);
            response.setToken(null);
        }

        return response;
    }

    public String authAndsetCookiesGoogle(AuthRequest authRequest, HttpServletRequest request, HttpServletResponse response) {

        log.info("Inside authAndsetCookiesGoogle with username --> " + authRequest.getUsername());
        String userSource = userinfoRepo.findByUsername(authRequest.getUsername()).get().getSource();
        boolean isUserActive = checkUserStatus(authRequest.getUsername());

        if (isUserActive && userSource.equals("google")) {
            log.info("Inside authAndsetCookiesGoogle with userSource --> " + userSource);

            String token = jwtService.generateToken(authRequest.getUsername());
            log.info("Inside authAndsetCookiesGoogle with token --> " + token);
            if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
                // Determine target domain dynamically
                String origin = request.getHeader("Origin");
                String host = request.getHeader("Host");
                String targetDomain = ".clearbills.info"; // Fallback default

                if ((origin != null && origin.contains("clearbill.store")) ||
                        (host != null && host.contains("clearbill.store"))) {
                    targetDomain = ".clearbill.store";
                }
                log.info("Inside authAndsetCookiesGoogle with targetDomain --> " + targetDomain);
                response.addHeader("Set-Cookie",
                        "jwt=" + token + "; Path=/; HttpOnly; Secure; SameSite=None; Domain=" + targetDomain + "; Max-Age=36000");
            } else {
                Cookie cookie = new Cookie("jwt", token);
                cookie.setHttpOnly(true);
                cookie.setSecure(false);
                cookie.setPath("/");
                cookie.setMaxAge(3600);
                cookie.setDomain("localhost");
                response.addCookie(cookie);
            }
            return token;
        } else {
            throw new UsernameNotFoundException("invalid user request !");
        }
    }

    public String authAndsetCookies(AuthRequest authRequest, HttpServletRequest request, HttpServletResponse response) {
        System.out.println("Inside authAndsetCookies with username --> " + authRequest.getUsername());
        System.out.println("Inside authAndsetCookies with pass --> " + authRequest.getPassword());
        UserInfo userInfo = userinfoRepo.findByPhoneNumber(authRequest.getUsername(), true);

        if (userInfo != null) {
            authRequest.setUsername(userInfo.getUsername());
        } else {
            throw new UsernameNotFoundException("invalid user request !");
        }

        String userSource = userinfoRepo.findByUsername(authRequest.getUsername()).get().getSource();
        boolean isUserActive = checkUserStatus(authRequest.getUsername());

        if (userSource.equals("phone") || authRequest.getUsername().equals("junaid1") ||userSource.equals("google") ) {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));
            log.info("The authentication object is --> " + authentication);

            if (authentication.isAuthenticated() && isUserActive) {
                String token = jwtService.generateToken(authRequest.getUsername());

                if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
                    // Determine target domain dynamically
                    String origin = request.getHeader("Origin");
                    String host = request.getHeader("Host");
                    String targetDomain = ".clearbills.info"; // Fallback default

                    if ((origin != null && origin.contains("clearbill.store")) ||
                            (host != null && host.contains("clearbill.store"))) {
                        targetDomain = ".clearbill.store";
                    }

                    // 🟢 FIXED: Domain parameter properly added here as well, and SameSite set to None
                    response.addHeader("Set-Cookie",
                            "jwt=" + token + "; Path=/; HttpOnly; Secure; SameSite=None; Domain=" + targetDomain + "; Max-Age=36000");
                } else {
                    String cookieHeader = String.format(
                            "jwt=%s; Path=/; HttpOnly; Max-Age=3600; SameSite=Lax",
                            token
                    );
                    response.addHeader("Set-Cookie", cookieHeader);
                }
                log.info("The generated token --> " + token);
                return token;
            }
        }  else {
            throw new UsernameNotFoundException("invalid user request !");
        }
        return null;
    }
}