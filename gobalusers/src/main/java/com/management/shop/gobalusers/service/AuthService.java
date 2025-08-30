package com.management.shop.gobalusers.service;


import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.management.shop.gobalusers.dto.*;
import com.management.shop.gobalusers.entity.RegisterUserOTPEntity;
import com.management.shop.gobalusers.entity.UserInfo;
import com.management.shop.gobalusers.repository.*;
import com.management.shop.gobalusers.util.OTPSender;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private OTPSender otpSender;

    private final Random random = new Random();

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public String addUser(UserInfo userInfo) {
        userInfo.setRoles("USER");
        userInfo.setIsActive(true);
        userInfo.setPassword(passwordEncoder.encode(userInfo.getPassword()));
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

        var userInfo = UserInfo.builder().email(regRequest.getEmail()).isActive(false).name(regRequest.getFullName())
                .password(regRequest.getPassword()).phoneNumber(regRequest.getPhone()).build();

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

                try {
                    otpSender.sendEmail(regRequest.getEmail(), "help@friendsmobile.store", regRequest.getFullName(), "Friends Mobile",
                            "OPT Verification For Account", "Please enter OTP " + number + " to verify you account ");
                } catch (MailjetException | MailjetSocketTimeoutException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                var regsiterUserTemp = RegisterUserOTPEntity.builder().username(res.getUsername())
                        .createdDate(LocalDateTime.now()).otp(String.valueOf(number)).status("fresh").retries(0).build();
                newUserRepo.save(regsiterUserTemp);
            }

            return RegisterResponse.builder().username(res.getUsername()).build();

        }

        return null;
    }

    public ValidateContactResponse validateContact(ValidateContactRequest userInfo) {

        List<UserInfo> res = userinfoRepo.validateContact(userInfo.getEmail(), userInfo.getPhone(), true);
        System.out.println(res.get(0));

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

            try {
                otpSender.sendEmail(forgotPassRequest.getEmailId(), "help@friendsmobile.store", res.get(0).getName(), "Friends Mobile",
                        "OTP for resetting you password", "Please enter OTP " + otp + " to proceed forward ");
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
                otpSender.sendEmail(userInfo.getEmail(), "help@friendsmobile.store", userInfo.getName(), "Friends Mobile",
                        "OPT Verification For Account", "Please enter OTP " + number + " to verify you account ");
            } catch (MailjetException | MailjetSocketTimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


            var regsiterUserTemp = RegisterUserOTPEntity.builder().username(res.getUsername())
                    .createdDate(LocalDateTime.now()).otp(String.valueOf(number)).status("fresh").retries(res.getRetries() + 1).build();
            newUserRepo.save(regsiterUserTemp);

        }

        return null;
    }


    @Transactional
    public OtpVerifyResponse verifyOTP(OtpVerifyRequest otpInfo) {
        RegisterUserOTPEntity res = newUserRepo.getByUsername(otpInfo.getUsername());

        if (res.getOtp().equals(otpInfo.getOtp())) {

            userinfoRepo.updateUserStatus(res.getUsername());
            UserInfo userInfo = userinfoRepo.findByUsername(res.getUsername()).get();

            try {
                otpSender.sendEmail(userInfo.getEmail(), "help@friendsmobile.store", userInfo.getName(), "Friends Mobile",
                        "User creation confirmation", "Your account has been created with " + userInfo.getUsername() + "");
            } catch (MailjetException | MailjetSocketTimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


            var response = OtpVerifyResponse.builder().success(true)
                    .username(otpInfo.getUsername())
                    .message("Your account is created")
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

                    updatePassword(UserInfo.builder().username(userInfo.get(0).getUsername()).password(updatePassRequest.getNewPassword()).build());

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
        userinfoRepo.save(userRes);

        return "success";
    }


}
