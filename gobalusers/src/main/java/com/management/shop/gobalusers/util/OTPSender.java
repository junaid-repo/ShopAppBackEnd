package com.management.shop.gobalusers.util;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.mailjet.client.resource.Emailv31;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class OTPSender {

    @Value("${smsalert.apiKey}")
    private String smsAlertApiKey;

    @Value("${smsalert.baseUrl}")
    private String smsAlertApiUrl;

    @Value("${smsalert.senderId}")
    private String smsAlertSenderId;

	public MailjetResponse sendEmailForOrderConfirmations(String toEmailId, String fromEmailId, String receiptName,
			String senderName, String subject, String content) {
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			return sendEmail(toEmailId, fromEmailId, receiptName, senderName, subject, content);
		} catch (MailjetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MailjetSocketTimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public MailjetResponse sendEmail(String toEmailId, String fromEmailId, String receiptName, String senderName,
			String subject, String content) throws MailjetException, MailjetSocketTimeoutException {
		MailjetClient client;
		MailjetRequest request;
		MailjetResponse response;
		System.out.println("From emailId  is-->" + fromEmailId);
		System.out.println("To emailId is-->" + toEmailId);

		client = new MailjetClient("3e292e1e3e850abe850793dbb22554b9", "2fa15000afb8c7ad2cd676c9828bcd5e",
				new ClientOptions("v3.1"));
		request = new MailjetRequest(Emailv31.resource).property(Emailv31.MESSAGES, new JSONArray().put(new JSONObject()
				.put(Emailv31.Message.FROM, new JSONObject().put("Email", "support@clearbill.store")
                        .put("Name", "Clear Bill"))
				.put(Emailv31.Message.TO,
						new JSONArray().put(new JSONObject().put("Email", toEmailId).put(receiptName, "Hello")))
				.put(Emailv31.Message.SUBJECT, subject).put(Emailv31.Message.TEXTPART, subject)
				.put(Emailv31.Message.HTMLPART, content)
				.put(Emailv31.Message.CUSTOMID, "AppGettingStartedTest")));
		response = client.post(request);
		System.out.println(response.getStatus());
		System.out.println(response.getData());
		return response;
	}

    public String sendOtpWithPhone(String phoneNumber, String otp, String timing) throws IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();
        String msgBody = "Dear User,\n\n" +
                "Your OTP is " + otp +
                ". Valid for " + timing +
                " minutes. Please do not share this OTP.\n\n" +
                "Regards,\n" +
                "Lumenapps";

// ✅ Encode message
        String encodedMessage =
                URLEncoder.encode(msgBody, StandardCharsets.UTF_8);

        String url = smsAlertApiUrl
                + "?apikey=" + smsAlertApiKey
                + "&sender=" + smsAlertSenderId
                + "&mobileno=" + phoneNumber
                + "&text=" + encodedMessage;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response=client.send(
                request, HttpResponse.BodyHandlers.ofString()
        );

        System.out.println("SMS Alert API response status code: " + response.statusCode());
        System.out.println("SMS Alert API response body: " + response.body());

        return response.body();

    }
}
