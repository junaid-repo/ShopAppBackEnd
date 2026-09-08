package com.management.shop.gobalusers.service;

import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
 import com.management.shop.gobalusers.constants.EventConstants;
import com.management.shop.gobalusers.dto.*;
import com.management.shop.gobalusers.entity.RegisterUserOTPEntity;
import com.management.shop.gobalusers.entity.UserInfo;
import com.management.shop.gobalusers.entity.UserInfoStatus;
import com.management.shop.gobalusers.entity.UserPaymentModes;
import com.management.shop.gobalusers.event.UserRegistrationCompletedEvent;
import com.management.shop.gobalusers.repository.UserInfoRepository;
import com.management.shop.gobalusers.repository.UserInfoStatusRepository;
import com.management.shop.gobalusers.repository.UserOtpRepo;
import com.management.shop.gobalusers.repository.UserPaymentModesRepo;
import com.management.shop.gobalusers.util.AccountEmailTemplate;
import com.management.shop.gobalusers.util.OTPSender;
 import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@Slf4j
public class AuthPhoneService {

    @Autowired
    private UserOtpRepo otpRepo;

    @Autowired
    private OTPSender otpSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountEmailTemplate emailTemplateUtil;

    @Autowired
    private UserInfoRepository userinfoRepo;

    @Autowired
    private UserPaymentModesRepo paymentModesRepo;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserInfoStatusRepository userStatusRepo;

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationEventPublisher eventPublisher;



    @Transactional
    public RegisterResponse registerNewUserWithPhone(RegisterRequest regRequest) {

        ValidateContactResponse validateContactResponse=    validatePhone(ValidateContactRequest.builder().phone(regRequest.getPhone()).email(regRequest.getEmail()).build());
        System.out.println("The validate phone response is --> "+validateContactResponse);
        if(validateContactResponse!=null){
            if(!validateContactResponse.isStatus()){
                return RegisterResponse.builder().message("Phone already registered").success(false).build();
            }
            else {

                final SecureRandom random = new SecureRandom();
                int number = 100000 + random.nextInt(900000);

                List<RegisterUserOTPEntity> res2 = otpRepo.getByPhoneNumber(regRequest.getPhone());
                if (res2 != null) {

                    res2.stream().forEach(i->{otpRepo.updateOldOTPWithPhone(regRequest.getPhone(), "stale", EventConstants.USER_REG.getEventName(), "sms");});

                }
                String smsResponse="";
                if (isHostedEnvironment()) {
                    try {
                        smsResponse = otpSender.sendOtpWithPhoneForReg(regRequest.getPhone(), String.valueOf(number), "30");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(Arrays.asList(environment.getActiveProfiles()).contains("dev")){
                    smsResponse="success";
                }
                if (smsResponse.contains("success")) {
                    var regsiterUserTemp = RegisterUserOTPEntity.builder().username(validateContactResponse.getUsername()).phoneNumber(regRequest.getPhone())
                            .createdDate(LocalDateTime.now()).otp(String.valueOf(number)).status("fresh").retries(0).event(EventConstants.USER_REG.getEventName()).source("SMS").build();
                    otpRepo.save(regsiterUserTemp);
                    return RegisterResponse.builder().message("User created successfully. Please verify the OTP sent to your phone to activate your account.").success(true).username(validateContactResponse.getUsername()).build();
                } else {

                    return RegisterResponse.builder().message("Failed to send OTP sms. Please try again later.").success(false).build();
                }


            }
        }


        var userInfo = UserInfo.builder().email(regRequest.getEmail()).isActive(false).name(regRequest.getFullName())
                .password(regRequest.getPassword()).phoneNumber(regRequest.getPhone())
                .source("phone")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        userInfo.setRoles("USER");
        userInfo.setPassword(passwordEncoder.encode(userInfo.getPassword()));
        UserInfo res = userinfoRepo.save(userInfo);
        if (res.getId() > 0) {
            String username = res.getName().replace(" ", "").toLowerCase() + String.valueOf(res.getId());
            userInfo.setUsername(username);

            res = userinfoRepo.save(userInfo);

            if (res != null) {

                final SecureRandom random = new SecureRandom();
                int number = 100000 + random.nextInt(900000);

                RegisterUserOTPEntity res2 = otpRepo.getByUsername(userInfo.getUsername());
                if (res2 != null) {
                    otpRepo.updateOldOTP(res.getId(), "stale");
                }
                String smsResponse="";
                if (isHostedEnvironment()) {
                    try {
                        smsResponse = otpSender.sendOtpWithPhoneForReg(regRequest.getPhone(), String.valueOf(number), "30");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(Arrays.asList(environment.getActiveProfiles()).contains("dev")){
                    smsResponse="success";
                    System.out.println("The otp is "+number+" for user "+userInfo.getUsername());
                }
                if (smsResponse.contains("success")) {
                    var regsiterUserTemp = RegisterUserOTPEntity.builder().username(userInfo.getUsername()).phoneNumber(regRequest.getPhone())
                            .createdDate(LocalDateTime.now()).otp(String.valueOf(number)).status("fresh").retries(0).event(EventConstants.USER_REG.getEventName()).source("SMS").build();
                    otpRepo.save(regsiterUserTemp);
                    return RegisterResponse.builder().message("User created successfully. Please verify the OTP sent to your phone to activate your account.").success(true).username(userInfo.getUsername()).build();
                } else {

                    return RegisterResponse.builder().message("Failed to send OTP sms. Please try again later.").success(false).build();
                }
            }

            //  return RegisterResponse.builder().username(res.getUsername()).build();

        }

        return RegisterResponse.builder().message("Something went wrong while creating the user, try after sometime.").success(false).build();
    }

    public Map<String, String> fetchRetriesForToday(String phoneNumber) {
        List<RegisterUserOTPEntity>  res = otpRepo.getByPhoneOtpForToday(phoneNumber);
        Integer count=res.size();
        if (res != null) {
            return Map.of("retryLeft", String.valueOf(5-count));
        }
        return Map.of("retryLeft", "0");
    }

    public ValidateContactResponse validatePhone(ValidateContactRequest userInfo) {

        List<UserInfo> res = userinfoRepo.validatePhone( userInfo.getPhone());
       // List<UserInfo> res = userinfoRepo.validatePhoneAndStatus( userInfo.getPhone(), "active");



        if (res.size() > 0) {

            for(UserInfo user:res){
                if(user.getIsActive()){

                    return ValidateContactResponse.builder().username(user.getUsername()).status(false).message("Phone number already registered with an active account").build();
                }
            }

            for(UserInfo user:res){
                if(!user.getIsActive()){
                    UserInfoStatus userInfoStatus=userStatusRepo.validateUserStatus(user.getUsername());

                    if(userInfoStatus.getStatus().equals("DELETEDBYUSER")){
                        return null;
                    }
                    return ValidateContactResponse.builder().username(user.getUsername()).status(true).message("Phone number already registered with an inactive account").build();
                }
            }


        }

        return null;


    }

    @Transactional
    public OtpVerifyResponse reSendOtpPhone(OtpVerifyRequest otpVerifyReq) {

        final SecureRandom random = new SecureRandom();
        int number = 100000 + random.nextInt(900000);

        List<RegisterUserOTPEntity> res2 = otpRepo.getByPhoneNumber(otpVerifyReq.getPhone());
        if (res2 != null) {
            res2.stream().forEach(i->{otpRepo.updateOldOTPWithPhone(otpVerifyReq.getPhone(), "stale", EventConstants.USER_REG.getEventName(), "sms");});
        }
        String smsResponse="";

        try {
            smsResponse  =  otpSender.sendOtpWithPhoneForReg(otpVerifyReq.getPhone(), String.valueOf(number), "30");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (smsResponse.contains("success")) {
            var regsiterUserTemp = RegisterUserOTPEntity.builder().username(otpVerifyReq.getUsername()).phoneNumber(otpVerifyReq.getPhone())
                    .createdDate(LocalDateTime.now()).otp(String.valueOf(number)).status("fresh").event(EventConstants.USER_REG.getEventName()).source("SMS").retries(0).build();
            otpRepo.save(regsiterUserTemp);
            return OtpVerifyResponse.builder().message("User created successfully. Please verify the OTP sent to your phone to activate your account.").success(true).username(otpVerifyReq.getUsername()).build();
        } else {

            return OtpVerifyResponse.builder().message("Failed to send OTP sms. Please try again later.").success(false).build();
        }
    }

    @Transactional
    public OtpVerifyResponse verifyOTP(OtpVerifyRequest otpInfo) {
        RegisterUserOTPEntity res = otpRepo.getLatestByPhone(otpInfo.getPhone(), "fresh");

        if (res.getOtp().equals(otpInfo.getOtp())) {

            userinfoRepo.updateUserStatus(res.getUsername());
            UserInfo userInfo = userinfoRepo.findByUsername(res.getUsername()).orElseThrow(() -> new UsernameNotFoundException("User not found"));

            //paymentModesRepo.save(UserPaymentModes.builder().userId(userInfo.getUsername()).cash(true).card(false).upi(true).createdBy("junaid1").updatedBy("junaid1").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());



     if(userInfo.getEmail()!=null) {
         String htmlContent = emailTemplateUtil.registerUserSucess(userInfo.getName(), userInfo.getUsername());

         try {
             otpSender.sendEmail(userInfo.getEmail(), "support@instabill.in", userInfo.getName(), "Instabill",
                     "Account Creation Success", htmlContent);
         } catch (MailjetException | MailjetSocketTimeoutException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }

     }

            eventPublisher.publishEvent(new UserRegistrationCompletedEvent(userInfo.getUsername()));

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


    public ValidateContactResponse forgotPasswordPhone(ForgotPassRequest forgotPassRequest) {
        List<UserInfo> res = userinfoRepo.validateUserPhone(forgotPassRequest.getPhone(), forgotPassRequest.getUserId(), true);


        if (res.size() > 0) {
            System.out.println(res.get(0));
            final SecureRandom random = new SecureRandom();
            int otp = 100000 + random.nextInt(900000);
            var otpVerifyReq = OtpVerifyRequest.builder().otp(String.valueOf(otp)).username(res.get(0).getUsername()).build();


            List<RegisterUserOTPEntity> res2 = otpRepo.getByPhoneNumber(forgotPassRequest.getPhone());
            if (res2 != null) {

                res2.stream().forEach(i->{otpRepo.updateOldOTPWithPhone(forgotPassRequest.getPhone(), "stale", EventConstants.PASSWORD_RESET_REQUESTED.getEventName(), "sms");});

            }
            if (isHostedEnvironment()) {
                String smsResponse = "";

                try {
                    smsResponse = otpSender.sendOtpWithPhoneForPasswordReset(forgotPassRequest.getPhone(), String.valueOf(otp), "30");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            var regsiterUserTemp = RegisterUserOTPEntity.builder().username(res.get(0).getUsername())
                    .createdDate(LocalDateTime.now()).otp(String.valueOf(otp)).status("fresh").event(EventConstants.PASSWORD_RESET_REQUESTED.getEventName()).phoneNumber(forgotPassRequest.getPhone()).source("SMS").retries(0).build();
            otpRepo.save(regsiterUserTemp);


            return ValidateContactResponse.builder().status(true).message("OTP sent to your Phone Number").build();
        }
        return ValidateContactResponse.builder().status(false).message("No user found with provided details").build();

    }

    private boolean isHostedEnvironment() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        return activeProfiles.contains("prod") || activeProfiles.contains("preprod");
    }

    public ValidateContactResponse confirmOtpAndUpdatePasswordPhone(UpdatePasswordRequest updatePassRequest) {
        List<UserInfo> userInfo = userinfoRepo.validateUserPhone(updatePassRequest.getPhone(), updatePassRequest.getUserId(), true);

        if (userInfo.size() > 0) {
            RegisterUserOTPEntity otpedUser = otpRepo.getLatestOtp(userInfo.get(0).getUsername(), "fresh", EventConstants.PASSWORD_RESET_REQUESTED.getEventName(), "sms");

            if (otpedUser != null) {
                if (otpedUser.getOtp().equals(updatePassRequest.getOtp())) {

                    LocalDateTime updatedAt=LocalDateTime.now();

                    authService.updatePassword(UserInfo.builder().username(userInfo.get(0).getUsername()).password(updatePassRequest.getNewPassword()).updatedAt(updatedAt).build());

                    return ValidateContactResponse.builder().status(true).message("Your password has been updated successfully").build();
                } else {

                    return ValidateContactResponse.builder().status(false).message("Your otp doesn't matched please re-enter").build();

                }
            }

        }
        return null;

    }
}
