package com.management.shop.gobalusers.service;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.management.shop.gobalusers.dto.*;
import com.management.shop.gobalusers.entity.RegisterUserOTPEntity;
import com.management.shop.gobalusers.entity.UserInfo;
import com.management.shop.gobalusers.entity.UserPaymentModes;
import com.management.shop.gobalusers.entity.UserSettingsEntity;
import com.management.shop.gobalusers.repository.*;
import com.management.shop.gobalusers.util.AccountEmailTemplate;
import com.management.shop.gobalusers.util.OTPSender;
import jakarta.servlet.http.Cookie;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

    public AuthService(AuthenticationManager authenticationManager,

                          JwtService jwtService) {
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

        ValidateContactResponse validateContactResponse=    validateContact(ValidateContactRequest.builder().phone(regRequest.getPhone()).email(regRequest.getEmail()).build());

        if(validateContactResponse!=null){
            if(!validateContactResponse.isStatus()){
                return RegisterResponse.builder().message("Email/Phone already registered").success(false).build();
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

                String htmlContent=emailTemplateUtil.registerUserOTP( regRequest.getFullName(), String.valueOf(number), String.valueOf(20));

                try {
                    mailResponse =  otpSender.sendEmail(regRequest.getEmail(), "support@clearbill.store",regRequest.getFullName(), "Clear Bill",
                            "OTP for Register of new account", htmlContent);
                } catch (MailjetException | MailjetSocketTimeoutException e) {
                    // TODO Auto-generated catch block
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

          //  return RegisterResponse.builder().username(res.getUsername()).build();

        }

        return RegisterResponse.builder().message("Something went wrong while creating the user, try after sometime.").success(false).build();
    }

    public ValidateContactResponse validateContact(ValidateContactRequest userInfo) {

        List<UserInfo> res = userinfoRepo.validateContact(userInfo.getEmail(), userInfo.getPhone(), true);


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


            RegisterUserOTPEntity res2 = otpRepo.getByUsername(res.get(0).getUsername());
            if (res2 != null) {
                otpRepo.removeOldOTP(res.get(0).getUsername());
            }

            String htmlContent=emailTemplateUtil.generateForgetPasswordHtml(res.get(0).getUsername(), res.get(0).getName(), String.valueOf(otp), String.valueOf(20));

            try {
                otpSender.sendEmail(res.get(0).getEmail(), "support@clearbill.store", res.get(0).getName(), "Clear Bill",
                        "OTP for resetting you password", htmlContent);
            } catch (MailjetException | MailjetSocketTimeoutException e) {
                // TODO Auto-generated catch block
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
                // TODO Auto-generated catch block
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




            String htmlContent=emailTemplateUtil.registerUserSucess(userInfo.getName(), userInfo.getUsername());

            try {
                otpSender.sendEmail(userInfo.getEmail(), "support@clearbill.store", userInfo.getName(), "Clear Bill",
                        "Account Creation Success", htmlContent);
            } catch (MailjetException | MailjetSocketTimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }




            var response = OtpVerifyResponse.builder().success(true)
                    .username(otpInfo.getUsername())
                    .message("Registration complete! Your username is "+otpInfo.getUsername()+" Please login with this username and password to use the system.")
                    .build();
            return response;
        }
        var response = OtpVerifyResponse.builder().success(false)
                .username(otpInfo.getOtp())
                .message("Your entered OTP " + otpInfo.getOtp()+" is incorrect, please re-enter")
                .build();


        return response;
    }

    public ValidateContactResponse confirmOtpAndUpdatePassword(UpdatePasswordRequest updatePassRequest) {
        List<UserInfo> userInfo = userinfoRepo.validateUser(updatePassRequest.getEmailId(), updatePassRequest.getUserId(), true);

        if (userInfo.size() > 0) {
            RegisterUserOTPEntity otpedUser = otpRepo.getLatestOtp(userInfo.get(0).getUsername());

            if (otpedUser != null) {
                if (otpedUser.getOtp().equals(updatePassRequest.getOtp())) {

                    LocalDateTime updatedAt=LocalDateTime.now();

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
        // TODO Auto-generated method stub
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
            return Map.of("retryLeft", String.valueOf(5-res.getRetries()));
        }
        return Map.of("retryLeft", "0");
    }

    public GoogleAuthResponse googleLogin(GoogleLoginRequest request, HttpServletResponse httpServletResponse) throws Exception {
        GoogleAuthResponse response = new GoogleAuthResponse();
       try {
           GoogleIdToken.Payload payload = googleVerifier.verify(request.getIdToken());
           String email = payload.getEmail();
            String profilePicLink=(String) payload.get("picture");
           String sub = payload.getSubject(); // Google's user ID
           String name = (String) payload.get("name");
           System.out.println("google email ->" + email);
           System.out.println("google name ->" + name);
           System.out.println("google profilePicLink ->" + profilePicLink);
           String jwtToken=null;
           List<UserInfo> res = userinfoRepo.validateUser(email, "na", true);
            try{
           if (res.size() > 0) {
               var authRequest = AuthRequest.builder().username(res.get(0).getUsername()).build();

               jwtToken=    authAndsetCookiesGoogle(authRequest, httpServletResponse);
           } else {
               var userInfo = UserInfo.builder().email(email).isActive(true).name(name)
                       .phoneNumber("0000000000")
                       .source("google")
                       .profilePiclink(profilePicLink)
                       .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

               userInfo.setRoles("USER");
               //userInfo.setPassword(passwordEncoder.encode(userInfo.getPassword()));
               UserInfo userRes = userinfoRepo.save(userInfo);
               if (userRes.getId() > 0) {
                   String username = userRes.getName().replace(" ", "").toLowerCase() + String.valueOf(userRes.getId());
                   userInfo.setUsername(username);
                   userinfoRepo.save(userInfo);
                   paymentModesRepo.save(UserPaymentModes.builder().userId(userInfo.getUsername()).cash(true).card(false).upi(true).createdBy("junaid1").updatedBy("junaid1").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

                   var authRequest = AuthRequest.builder().username(username).build();
                   jwtToken=    authAndsetCookiesGoogle(authRequest, httpServletResponse);
               }


           }
           if(jwtToken!=null){
               response.setMessage("Login successful");
               response.setSuccess(Boolean.TRUE);
               response.setToken(jwtToken);
           }
           else {
               response.setMessage("Login unsuccessful");
               response.setSuccess(Boolean.FALSE);
               response.setToken(null);
           }
           return response;

       }
           catch (Exception e){
               response.setMessage(e.getMessage());
               response.setSuccess(Boolean.FALSE);
               response.setToken(null);
           }
       }
       catch (Exception e){
           response.setMessage(e.getMessage());
           response.setSuccess(Boolean.FALSE);
           response.setToken(null);
        }

        return response;
    }

    public String authAndsetCookiesGoogle(AuthRequest authRequest, HttpServletResponse response){

        String userSource=  userinfoRepo.findByUsername(authRequest.getUsername()).get().getSource();

        boolean isUserActive = checkUserStatus(authRequest.getUsername());

         if ( isUserActive && userSource.equals("google")) {
            String token = jwtService.generateToken(authRequest.getUsername());

            Cookie cookie = new Cookie("jwt", token);
            if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
                cookie.setHttpOnly(true);       // ✅ Prevent JS access
                cookie.setSecure(true);         // ✅ Required for HTTPS
                cookie.setPath("/");            // ✅ Makes cookie accessible for all paths
                cookie.setMaxAge(3600);         // ✅ 1 hour
                cookie.setDomain(".friendsmobile.info"); // ✅ Share across subdomains
// Note: cookie.setSameSite("None"); is not available directly in Servlet Cookie API

                response.addHeader("Set-Cookie",
                        "jwt=" + token + "; Path=/; HttpOnly; Secure; SameSite=None; Domain=.friendsmobile.info; Max-Age=36000");
            } else {
                cookie.setHttpOnly(true);      // Prevent JS access
                cookie.setSecure(false);       // ✅ In dev, must be false (unless using HTTPS with localhost)
                cookie.setPath("/");           // Available on all paths
                cookie.setMaxAge(3600);
                cookie.setDomain("localhost");// 1 hour
// Do NOT set cookie.setDomain(...)

                response.addCookie(cookie);
            }
            // System.out.println("The generated token --> "+token);
            return token;
        }else {
            throw new UsernameNotFoundException("invalid user request !");
        }

    }

    public String authAndsetCookies(AuthRequest authRequest, HttpServletResponse response){

      String userSource=  userinfoRepo.findByUsername(authRequest.getUsername()).get().getSource();

        boolean isUserActive = checkUserStatus(authRequest.getUsername());
      if(userSource.equals("email")||authRequest.getUsername().equals("junaid1")) {
          Authentication authentication = authenticationManager.authenticate(
                  new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));
          System.out.println("The authentication object is --> " + authentication);
          if (authentication.isAuthenticated() && isUserActive) {
              String token = jwtService.generateToken(authRequest.getUsername());

              Cookie cookie = new Cookie("jwt", token);
              if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
                  cookie.setHttpOnly(true);       // ✅ Prevent JS access
                  cookie.setSecure(true);         // ✅ Required for HTTPS
                  cookie.setPath("/");            // ✅ Makes cookie accessible for all paths
                  cookie.setMaxAge(3600);         // ✅ 1 hour
                  cookie.setDomain(".friendsmobile.info"); // ✅ Share across subdomains
// Note: cookie.setSameSite("None"); is not available directly in Servlet Cookie API

                  response.addHeader("Set-Cookie",
                          "jwt=" + token + "; Path=/; HttpOnly; Secure; SameSite=None; Domain=.friendsmobile.info; Max-Age=36000");
              } else {
                  cookie.setHttpOnly(true);      // Prevent JS access
                  cookie.setSecure(true);       // Don't require HTTPS in dev
                  cookie.setPath("/");           // Available on all paths
                  cookie.setMaxAge(3600);        // 1 hour
                  cookie.setDomain("localhost"); // Or remove for simpler case

                  response.addCookie(cookie);
              }
               System.out.println("The generated token --> "+token);
              return token;
          }
      }
      else if(userSource.equals("google")){
          return "Please login using google login";
      }
   else {
            throw new UsernameNotFoundException("invalid user request !");
        }
       return null;
    }
}
