package com.management.shop.gobalusers.util;


import org.springframework.stereotype.Component;

import java.time.Year;

@Component
public class AccountEmailTemplate {

      String year = String.valueOf(java.time.Year.now());

    public String generateForgetPasswordHtml(String username, String name, String otp, String validDuration) {
        // Build table rows for items



        // HTML template with {{placeholders}}
        String htmlTemplate = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\" />\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
                "  <title>Password Reset</title>\n" +
                "  <style>\n" +
                "    body {\n" +
                "      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
                "      background-color: #f4f6f8;\n" +
                "      margin: 0;\n" +
                "      padding: 0;\n" +
                "    }\n" +
                "    .email-container {\n" +
                "      max-width: 600px;\n" +
                "      margin: 2rem auto;\n" +
                "      background: #ffffff;\n" +
                "      border-radius: 12px;\n" +
                "      overflow: hidden;\n" +
                "      box-shadow: 0 4px 20px rgba(0,0,0,0.08);\n" +
                "    }\n" +
                "    .email-header {\n" +
                "      background: linear-gradient(135deg, #667eea, #667eea);\n" +
                "      color: #ffffff;\n" +
                "      padding: 1.5rem;\n" +
                "      text-align: center;\n" +
                "    }\n" +
                "    .email-header h1 {\n" +
                "      margin: 0;\n" +
                "      font-size: 1.5rem;\n" +
                "    }\n" +
                "    .email-body {\n" +
                "      padding: 2rem;\n" +
                "      color: #333333;\n" +
                "    }\n" +
                "    .email-body h2 {\n" +
                "      margin-top: 0;\n" +
                "      font-size: 1.2rem;\n" +
                "      color: #444444;\n" +
                "    }\n" +
                "    .otp-box {\n" +
                "      background: #f9fafb;\n" +
                "      border: 2px dashed #667eea;\n" +
                "      border-radius: 8px;\n" +
                "      padding: 1rem;\n" +
                "      text-align: center;\n" +
                "      font-size: 1.5rem;\n" +
                "      font-weight: bold;\n" +
                "      color: #333;\n" +
                "      letter-spacing: 2px;\n" +
                "      margin: 1.5rem 0;\n" +
                "    }\n" +
                "    .email-footer {\n" +
                "      text-align: center;\n" +
                "      font-size: 0.8rem;\n" +
                "      color: #777;\n" +
                "      padding: 1rem;\n" +
                "      background: #f9f9f9;\n" +
                "    }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"email-container\">\n" +
                "    <!-- Header -->\n" +
                "    <div class=\"email-header\">\n" +
                "      <h1>Password Reset Request</h1>\n" +
                "    </div>\n" +
                "\n" +
                "    <!-- Body -->\n" +
                "    <div class=\"email-body\">\n" +
                "      <p>Hi <strong>{{customerName}}</strong>,</p>\n" +
                "      <p>We received a request to reset the password for your account <strong>{{username}}</strong>.</p>\n" +
                "      \n" +
                "      <h2>Your One-Time Password (OTP)</h2>\n" +
                "      <div class=\"otp-box\">{{otp}}</div>\n" +
                "\n" +
                "      <p>This OTP is valid for <strong>{{validMinutes}} minutes</strong>.  \n" +
                "      Please use it promptly to reset your password.</p>\n" +
                "\n" +
                "      <p>If you didn’t request this change, please ignore this email or contact support.</p>\n" +
                "\n" +
                "      <p>Thanks,<br/>The Support Team</p>\n" +
                "    </div>\n" +
                "\n" +
                "    <!-- Footer -->\n" +
                "    <div class=\"email-footer\">\n" +
                "      © {{year}} Clear Bill. All rights reserved.\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>\n";

        // Replace placeholders
        return htmlTemplate
                .replace("{{customerName}}", name)
                .replace("{{username}}", username)
                .replace("{{otp}}", otp)
                .replace("{{validMinutes}}", validDuration);
    }


    public String registerUserOTP( String name, String otp, String validDuration) {
        // Build table rows for items



        // HTML template with {{placeholders}}
        String htmlTemplate = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\" />\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
                "  <title>Password Reset</title>\n" +
                "  <style>\n" +
                "    body {\n" +
                "      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
                "      background-color: #f4f6f8;\n" +
                "      margin: 0;\n" +
                "      padding: 0;\n" +
                "    }\n" +
                "    .email-container {\n" +
                "      max-width: 600px;\n" +
                "      margin: 2rem auto;\n" +
                "      background: #ffffff;\n" +
                "      border-radius: 12px;\n" +
                "      overflow: hidden;\n" +
                "      box-shadow: 0 4px 20px rgba(0,0,0,0.08);\n" +
                "    }\n" +
                "    .email-header {\n" +
                "      background: linear-gradient(135deg, #667eea, #667eea);\n" +
                "      color: #ffffff;\n" +
                "      padding: 1.5rem;\n" +
                "      text-align: center;\n" +
                "    }\n" +
                "    .email-header h1 {\n" +
                "      margin: 0;\n" +
                "      font-size: 1.5rem;\n" +
                "    }\n" +
                "    .email-body {\n" +
                "      padding: 2rem;\n" +
                "      color: #333333;\n" +
                "    }\n" +
                "    .email-body h2 {\n" +
                "      margin-top: 0;\n" +
                "      font-size: 1.2rem;\n" +
                "      color: #444444;\n" +
                "    }\n" +
                "    .otp-box {\n" +
                "      background: #f9fafb;\n" +
                "      border: 2px dashed #667eea;\n" +
                "      border-radius: 8px;\n" +
                "      padding: 1rem;\n" +
                "      text-align: center;\n" +
                "      font-size: 1.5rem;\n" +
                "      font-weight: bold;\n" +
                "      color: #333;\n" +
                "      letter-spacing: 2px;\n" +
                "      margin: 1.5rem 0;\n" +
                "    }\n" +
                "    .email-footer {\n" +
                "      text-align: center;\n" +
                "      font-size: 0.8rem;\n" +
                "      color: #777;\n" +
                "      padding: 1rem;\n" +
                "      background: #f9f9f9;\n" +
                "    }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"email-container\">\n" +
                "    <!-- Header -->\n" +
                "    <div class=\"email-header\">\n" +
                "      <h1>New User Registration Request</h1>\n" +
                "    </div>\n" +
                "\n" +
                "    <!-- Body -->\n" +
                "    <div class=\"email-body\">\n" +
                "      <p>Hi <strong>{{customerName}}</strong>,</p>\n" +
                "      <p>We received a request to create an account.</p>\n" +
                "      \n" +
                "      <h2>Your One-Time Password (OTP)</h2>\n" +
                "      <div class=\"otp-box\">{{otp}}</div>\n" +
                "\n" +
                "      <p>This OTP is valid for <strong>{{validMinutes}} minutes</strong>.  \n" +
                "      Please use it promptly to reset your password.</p>\n" +
                "\n" +
                "      <p>If you didn’t request this change, please ignore this email or contact support.</p>\n" +
                "\n" +
                "      <p>Thanks,<br/>The Support Team</p>\n" +
                "    </div>\n" +
                "\n" +
                "    <!-- Footer -->\n" +
                "    <div class=\"email-footer\">\n" +
                "      © {{year}} Clear Bill. All rights reserved.\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>\n";

        // Replace placeholders
        return htmlTemplate
                .replace("{{customerName}}", name)
                .replace("{{otp}}", otp)
                .replace("{{validMinutes}}", validDuration);
    }


    public String registerUserSucess( String name, String username) {String htmlTemplate = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "  <meta charset=\"UTF-8\" />\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
            "  <title>Registration Successful</title>\n" +
            "  <style>\n" +
            "    body {\n" +
            "      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
            "      background-color: #f4f6f8;\n" +
            "      margin: 0;\n" +
            "      padding: 0;\n" +
            "    }\n" +
            "    .email-container {\n" +
            "      max-width: 600px;\n" +
            "      margin: 2rem auto;\n" +
            "      background: #ffffff;\n" +
            "      border-radius: 12px;\n" +
            "      overflow: hidden;\n" +
            "      box-shadow: 0 4px 20px rgba(0,0,0,0.08);\n" +
            "    }\n" +
            "    .email-header {\n" +
            "      background: linear-gradient(135deg, #28a745, #218838);\n" +
            "      color: #ffffff;\n" +
            "      padding: 1.5rem;\n" +
            "      text-align: center;\n" +
            "    }\n" +
            "    .email-header h1 {\n" +
            "      margin: 0;\n" +
            "      font-size: 1.5rem;\n" +
            "    }\n" +
            "    .email-body {\n" +
            "      padding: 2rem;\n" +
            "      color: #333333;\n" +
            "    }\n" +
            "    .email-body h2 {\n" +
            "      margin-top: 0;\n" +
            "      font-size: 1.2rem;\n" +
            "      color: #444444;\n" +
            "    }\n" +
            "    .otp-box {\n" +
            "      background: #f9fafb;\n" +
            "      border: 2px dashed #28a745;\n" +
            "      border-radius: 8px;\n" +
            "      padding: 1rem;\n" +
            "      text-align: center;\n" +
            "      font-size: 1.5rem;\n" +
            "      font-weight: bold;\n" +
            "      color: #333;\n" +
            "      letter-spacing: 2px;\n" +
            "      margin: 1.5rem 0;\n" +
            "    }\n" +
            "    .email-footer {\n" +
            "      text-align: center;\n" +
            "      font-size: 0.8rem;\n" +
            "      color: #777;\n" +
            "      padding: 1rem;\n" +
            "      background: #f9f9f9;\n" +
            "    }\n" +
            "  </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "  <div class=\"email-container\">\n" +
            "    <!-- Header -->\n" +
            "    <div class=\"email-header\">\n" +
            "      <h1>🎉 Registration Successful</h1>\n" +
            "    </div>\n" +
            "\n" +
            "    <!-- Body -->\n" +
            "    <div class=\"email-body\">\n" +
            "      <p>Hi <strong>{{customerName}}</strong>,</p>\n" +
            "      <p>Welcome aboard! Your account has been created successfully. You can now log in using the username below.</p>\n" +
            "\n" +
            "      <h2>Your username</h2>\n" +
            "      <div class=\"otp-box\">{{username}}</div>\n" +
            "\n" +
            "      <p>We’re excited to have you with us. Start exploring your account and enjoy our services.</p>\n" +
            "      <p>Thanks,<br/>The Support Team</p>\n" +
            "    </div>\n" +
            "\n" +
            "    <!-- Footer -->\n" +
            "    <div class=\"email-footer\">\n" +
            "      © {{year}} Clear Bill. All rights reserved.\n" +
            "    </div>\n" +
            "  </div>\n" +
            "</body>\n" +
            "</html>\n";

        return htmlTemplate
                .replace("{{customerName}}", name)
                .replace("{{username}}", username)
                .replace("{{year}}", String.valueOf(Year.now().getValue()));
    }

}
