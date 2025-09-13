package com.management.shop.gobalusers.service;


import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.management.shop.gobalusers.dto.*;
import com.management.shop.gobalusers.entity.RegisterUserOTPEntity;
import com.management.shop.gobalusers.entity.UserInfo;
import com.management.shop.gobalusers.repository.RegisterUserRepo;
import com.management.shop.gobalusers.repository.UserInfoRepository;
import com.management.shop.gobalusers.repository.UserProfilePicRepo;
import com.management.shop.gobalusers.util.AccountEmailTemplate;
import com.management.shop.gobalusers.util.OTPSender;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@Slf4j
public class AuthService {

    @Autowired
    private UserInfoRepository userinfoRepo;



    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserProfilePicRepo userProfilePicRepo;

    @Autowired
    private RegisterUserRepo newUserRepo;

    @Autowired
    private AccountEmailTemplate emailTemplateUtil;


    @Autowired
    private OTPSender otpSender;

    private final Random random = new Random();

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

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

      /*  if(validateContactResponse!=null){
            if(!validateContactResponse.isStatus()){
                return RegisterResponse.builder().message("Email/Phone already registered").success(false).build();
            }
        }*/


        var userInfo = UserInfo.builder().email(regRequest.getEmail()).isActive(false).name(regRequest.getFullName())
                .password(regRequest.getPassword()).phoneNumber(regRequest.getPhone())
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

                RegisterUserOTPEntity res2 = newUserRepo.getByUsername(userInfo.getUsername());
                if (res2 != null) {
                    newUserRepo.updateOldOTP(res.getId(), "stale");
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
                    newUserRepo.save(regsiterUserTemp);
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


            RegisterUserOTPEntity res2 = newUserRepo.getByUsername(res.get(0).getUsername());
            if (res2 != null) {
                newUserRepo.removeOldOTP(res.get(0).getUsername());
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
                otpSender.sendEmail(userInfo.getEmail(), "help@clearbill.store", userInfo.getName(), "Clear Bill",
                        "OPT Verification For Account", "Please enter OTP " + number + " to verify you account ");
            } catch (MailjetException | MailjetSocketTimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


            var regsiterUserTemp = RegisterUserOTPEntity.builder().username(res.getUsername())
                    .createdDate(LocalDateTime.now()).otp(String.valueOf(number)).status("fresh").retries(res.getRetries() + 1).build();
            newUserRepo.save(regsiterUserTemp);
            newUserRepo.removeOldOTPById(res.getId());

        }

        return OtpVerifyResponse.builder().success(true).username(otpVerifyReq.getUsername()).message("A new OTP has been resent to your registered email. Please check your inbox.").build();
    }


    @Transactional
    public OtpVerifyResponse verifyOTP(OtpVerifyRequest otpInfo) {
        RegisterUserOTPEntity res = newUserRepo.getByUsername(otpInfo.getUsername());

        if (res.getOtp().equals(otpInfo.getOtp())) {

            userinfoRepo.updateUserStatus(res.getUsername());
            UserInfo userInfo = userinfoRepo.findByUsername(res.getUsername()).get();
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
        RegisterUserOTPEntity res = newUserRepo.getByUsername(username);

        if (res != null) {
            return Map.of("retryLeft", String.valueOf(5-res.getRetries()));
        }
        return Map.of("retryLeft", "0");
    }
}
