package com.management.shop.util;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.mailjet.client.resource.Emailv31;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class EmailSender {
	@Async("mailAsync")
	public  CompletableFuture<String> sendEmail2(String emailId, String message, String name, ByteArrayOutputStream byteArrayOutputStream) throws MailjetException, MailjetSocketTimeoutException {
		MailjetClient client;
		MailjetRequest request;
		MailjetResponse response;
		//System.out.println("From emailId is-->"+fromEmail);
		System.out.println("To emailId is-->"+emailId);

		client = new MailjetClient("3e292e1e3e850abe850793dbb22554b9",
				"2fa15000afb8c7ad2cd676c9828bcd5e", new ClientOptions("v3.1"));
		request = new MailjetRequest(Emailv31.resource).property(Emailv31.MESSAGES,
				new JSONArray().put(new JSONObject()
						.put(Emailv31.Message.FROM, new JSONObject().put("Email", "tahanasim3001@gmail.com")
								.put("Name", "Instabill"))
						.put(Emailv31.Message.TO,
								new JSONArray().put(
										new JSONObject().put("Email", emailId).put("JPC Waqf Board", "Hello")))
						.put(Emailv31.Message.SUBJECT, "Order has been confirmed")
						.put(Emailv31.Message.TEXTPART, "Dear Mr. "+name+"  Your order has been confirmed")
						.put(Emailv31.Message.HTMLPART,
								"<h3>"+"Dear Mr. "+name+"  Your order has been confirmed")
						.put(Emailv31.Message.CUSTOMID, "AppGettingStartedTest")));
		response = client.post(request);
		System.out.println(response.getStatus());
		System.out.println(response.getData());
		return CompletableFuture.completedFuture(response.getData().toString());
	}
	 public CompletableFuture<String> sendEmailForTicketIntimation(String emailId, String ticketNumber, String name, String htmlContent, String shopName) throws MailjetException, MailjetSocketTimeoutException {




	        String base64Content = "";


	        MailjetClient client;
	        MailjetRequest request;
	        MailjetResponse response;
	        client = new MailjetClient("3e292e1e3e850abe850793dbb22554b9",
					"2fa15000afb8c7ad2cd676c9828bcd5e", new ClientOptions("v3.1"));
	        request = new MailjetRequest(Emailv31.resource)
	                .property(Emailv31.MESSAGES, new JSONArray()
	                        .put(new JSONObject()
	            					.put(Emailv31.Message.FROM, new JSONObject().put("Email", "email@clearbill.store")
	        								.put("Name", shopName))
	        						.put(Emailv31.Message.TO,
	        								new JSONArray().put(
	        										new JSONObject().put("Email", emailId).put(shopName, "Hello")))
	        						.put(Emailv31.Message.SUBJECT, "A support ticket created "+ticketNumber)
	                                .put(Emailv31.Message.TEXTPART, "Dear Mr."+name+" Please address this")
	                                .put(Emailv31.Message.HTMLPART, htmlContent
	                                		+ "\n"
	                                		+ "\n"
	                                		+ "")));

	        response = client.post(request);
	        System.out.println(response.getStatus());
	        System.out.println(response.getData());
			return CompletableFuture.completedFuture(response.getData().toString());

	    }

    public CompletableFuture<String> sendEmailForPaymentReminder(String emailId, String orderNo, String name, String htmlContent, String shopName) throws MailjetException, MailjetSocketTimeoutException {




        String base64Content = "";


        MailjetClient client;
        MailjetRequest request;
        MailjetResponse response;
        client = new MailjetClient("3e292e1e3e850abe850793dbb22554b9",
                "2fa15000afb8c7ad2cd676c9828bcd5e", new ClientOptions("v3.1"));
        request = new MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, new JSONArray()
                        .put(new JSONObject()
                                .put(Emailv31.Message.FROM, new JSONObject().put("Email", "email@clearbill.store")
                                        .put("Name", shopName))
                                .put(Emailv31.Message.TO,
                                        new JSONArray().put(
                                                new JSONObject().put("Email", emailId).put(shopName, "Hello")))
                                .put(Emailv31.Message.SUBJECT, "Payment Reminder for Order# "+orderNo)
                                .put(Emailv31.Message.TEXTPART, "Dear Mr."+name+" Please address this")
                                .put(Emailv31.Message.HTMLPART, htmlContent
                                        + "\n"
                                        + "\n"
                                        + "")));

        response = client.post(request);
        System.out.println(response.getStatus());
        System.out.println(response.getData());
        return CompletableFuture.completedFuture(response.getData().toString());

    }

    public CompletableFuture<String> sendEmailReportWithAttachment(String emailId, String subject, String fileName, byte[] pdfStream, String htmlContent, String shopName) throws MailjetException, MailjetSocketTimeoutException {



        String base64Content = "";
        try {

            base64Content = Base64.getEncoder().encodeToString(pdfStream);
        } catch (Exception e) {
            e.printStackTrace();

        }

        MailjetClient client;
        MailjetRequest request;
        MailjetResponse response;
        client = new MailjetClient("3e292e1e3e850abe850793dbb22554b9",
                "2fa15000afb8c7ad2cd676c9828bcd5e", new ClientOptions("v3.1"));
        request = new MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, new JSONArray()
                        .put(new JSONObject()
                                .put(Emailv31.Message.FROM, new JSONObject().put("Email", "email@instabill.in")
                                        .put("Name", shopName))
                                .put(Emailv31.Message.TO,
                                        new JSONArray().put(
                                                new JSONObject().put("Email", emailId).put(shopName, "Hello")))
                                .put(Emailv31.Message.SUBJECT, subject)
                                .put(Emailv31.Message.TEXTPART, "")
                                .put(Emailv31.Message.HTMLPART, htmlContent
                                        + "\n"
                                        + "\n"
                                        + "")
                                .put(Emailv31.Message.ATTACHMENTS, new JSONArray()
                                        .put(new JSONObject()
                                                .put("ContentType", "application/pdf")
                                                .put("Filename", fileName)
                                                .put("Base64Content", base64Content)))));

        response = client.post(request);
        System.out.println(response.getStatus());
        System.out.println(response.getData());
        return CompletableFuture.completedFuture(response.getData().toString());

    }

    public String sendEmailReportWithAttachmentForReports(String emailId, String subject, String fileName, byte[] pdfStream, String htmlContent, String shopName) throws MailjetException, MailjetSocketTimeoutException {



        String base64Content = "";
        try {

            base64Content = Base64.getEncoder().encodeToString(pdfStream);
        } catch (Exception e) {
            e.printStackTrace();

        }

        MailjetClient client;
        MailjetRequest request;
        MailjetResponse response;
        client = new MailjetClient("3e292e1e3e850abe850793dbb22554b9",
                "2fa15000afb8c7ad2cd676c9828bcd5e", new ClientOptions("v3.1"));
        request = new MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, new JSONArray()
                        .put(new JSONObject()
                                .put(Emailv31.Message.FROM, new JSONObject().put("Email", "email@clearbill.store")
                                        .put("Name", shopName))
                                .put(Emailv31.Message.TO,
                                        new JSONArray().put(
                                                new JSONObject().put("Email", emailId).put(shopName, "Hello")))
                                .put(Emailv31.Message.SUBJECT, subject)
                                .put(Emailv31.Message.TEXTPART, "")
                                .put(Emailv31.Message.HTMLPART, htmlContent
                                        + "\n"
                                        + "\n"
                                        + "")
                                .put(Emailv31.Message.ATTACHMENTS, new JSONArray()
                                        .put(new JSONObject()
                                                .put("ContentType", "application/pdf")
                                                .put("Filename", fileName)
                                                .put("Base64Content", base64Content)))));

        response = client.post(request);
        System.out.println(response.getStatus());
        System.out.println(response.getData());
        return  response.getData().toString();

    }


    public String sendEmailWithMultipleAttachments(String emailId, String subject, List<String> fileNames, List<byte[]> fileStreams, String htmlContent, String shopName) throws MailjetException, MailjetSocketTimeoutException {

        // 1. Build the dynamic attachments JSON Array
        JSONArray attachmentsArray = new JSONArray();

        if (fileStreams != null && fileNames != null && fileStreams.size() == fileNames.size()) {
            for (int i = 0; i < fileStreams.size(); i++) {
                byte[] stream = fileStreams.get(i);
                String fileName = fileNames.get(i);

                String base64Content = "";
                try {
                    base64Content = Base64.getEncoder().encodeToString(stream);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue; // Skip this file if encoding fails, but continue with the rest
                }

                // Dynamically assign Content-Type based on extension
                String contentType = "application/pdf"; // Default
                if (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".xls")) {
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                } else if (fileName.toLowerCase().endsWith(".csv")) {
                    contentType = "text/csv";
                }

                // Add the attachment object to our array
                attachmentsArray.put(new JSONObject()
                        .put("ContentType", contentType)
                        .put("Filename", fileName)
                        .put("Base64Content", base64Content));
            }
        }

        // 2. Initialize Client
        MailjetClient client = new MailjetClient("3e292e1e3e850abe850793dbb22554b9",
                "2fa15000afb8c7ad2cd676c9828bcd5e", new ClientOptions("v3.1"));

        // 3. Build and send the request
        MailjetRequest request = new MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, new JSONArray()
                        .put(new JSONObject()
                                .put(Emailv31.Message.FROM, new JSONObject()
                                        .put("Email", "email@clearbill.store")
                                        .put("Name", shopName))
                                .put(Emailv31.Message.TO, new JSONArray()
                                        .put(new JSONObject()
                                                .put("Email", emailId)
                                                .put("Name", "Customer"))) // Fixed to standard Mailjet property
                                .put(Emailv31.Message.SUBJECT, subject)
                                .put(Emailv31.Message.TEXTPART, "")
                                .put(Emailv31.Message.HTMLPART, htmlContent)
                                .put(Emailv31.Message.ATTACHMENTS, attachmentsArray))); // Pass our dynamic array here

        MailjetResponse response = client.post(request);

        System.out.println(response.getStatus());
        System.out.println(response.getData());

        return response.getData().toString();
    }

    @Async("mailAsync")
    public CompletableFuture<String> sendEmail(String emailId, String orderId, String name, byte[] pdfStream, String htmlContent, String shopName)   {
        // Assume you have a ByteArrayOutputStream named 'pdfStream'
        // This stream would contain the PDF data, for example, from a PDF generator library.
        // ByteArrayOutputStream pdfStream = new ByteArrayOutputStream();

        // **NOTE:** In a real-world scenario, you would write the PDF data to 'pdfStream' here.
        // For this example, we'll simulate a small PDF's content.
        /*
         * try { pdfStream.write("This is a simulated PDF file content.".getBytes()); }
         * catch (IOException e) { e.printStackTrace(); }
         */



        String base64Content = "";
        try {
            // 1. Get the byte array from the ByteArrayOutputStream
            // byte[] fileContent = pdfStream.toByteArray();

            // 2. Encode the byte array to a Base64 string
            base64Content = Base64.getEncoder().encodeToString(pdfStream);
        } catch (Exception e) {
            e.printStackTrace();

        }

        MailjetClient client;
        MailjetRequest request;
        MailjetResponse response;
        client = new MailjetClient("3e292e1e3e850abe850793dbb22554b9",
                "2fa15000afb8c7ad2cd676c9828bcd5e", new ClientOptions("v3.1"));
        request = new MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, new JSONArray()
                        .put(new JSONObject()
                                .put(Emailv31.Message.FROM, new JSONObject().put("Email", "email@clearbill.store")
                                        .put("Name", shopName))
                                .put(Emailv31.Message.TO,
                                        new JSONArray().put(
                                                new JSONObject().put("Email", emailId).put(shopName, "Hello")))
                                .put(Emailv31.Message.SUBJECT, "Order has been confirmed with Order Number "+orderId)
                                .put(Emailv31.Message.TEXTPART, "Dear Mr."+name+" Welcome to Instabill")
                                .put(Emailv31.Message.HTMLPART, htmlContent
                                        + "\n"
                                        + "\n"
                                        + "")
                                .put(Emailv31.Message.ATTACHMENTS, new JSONArray()
                                        .put(new JSONObject()
                                                .put("ContentType", "application/pdf")
                                                .put("Filename", orderId+".pdf")
                                                .put("Base64Content", base64Content)))));

        try {
            response = client.post(request);
        } catch (MailjetException e) {
            throw new RuntimeException(e);
        } catch (MailjetSocketTimeoutException e) {
            throw new RuntimeException(e);
        }
        System.out.println(response.getStatus());
        System.out.println(response.getData());
        System.out.println("Email sent successfully, the response is--> "+response.getData().toString());
        return CompletableFuture.completedFuture(response.getData().toString());

    }
    public CompletableFuture<String> sendSupportEmail(String emailId, String subject, String name, byte[] pdfStream, String htmlContent, String shopName) throws MailjetException, MailjetSocketTimeoutException {
        // Assume you have a ByteArrayOutputStream named 'pdfStream'
        // This stream would contain the PDF data, for example, from a PDF generator library.
        // ByteArrayOutputStream pdfStream = new ByteArrayOutputStream();

        // **NOTE:** In a real-world scenario, you would write the PDF data to 'pdfStream' here.
        // For this example, we'll simulate a small PDF's content.
        /*
         * try { pdfStream.write("This is a simulated PDF file content.".getBytes()); }
         * catch (IOException e) { e.printStackTrace(); }
         */



        String base64Content = "";
        try {
            // 1. Get the byte array from the ByteArrayOutputStream
            // byte[] fileContent = pdfStream.toByteArray();

            // 2. Encode the byte array to a Base64 string
            if(pdfStream!=null) {
                base64Content = Base64.getEncoder().encodeToString(pdfStream);
            }
        } catch (Exception e) {
            e.printStackTrace();

        }

        MailjetClient client;
        MailjetRequest request=null;
        MailjetResponse response;
        client = new MailjetClient("3e292e1e3e850abe850793dbb22554b9",
                "2fa15000afb8c7ad2cd676c9828bcd5e", new ClientOptions("v3.1"));
        if(pdfStream!=null) {
            request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, new JSONArray()
                            .put(new JSONObject()
                                    .put(Emailv31.Message.FROM, new JSONObject().put("Email", "email@clearbill.store")
                                            .put("Name", shopName))
                                    .put(Emailv31.Message.TO,
                                            new JSONArray().put(
                                                    new JSONObject().put("Email", emailId).put(shopName, "Hello")))
                                    .put(Emailv31.Message.SUBJECT, subject)

                                    .put(Emailv31.Message.HTMLPART, htmlContent
                                            + "\n"
                                            + "\n"
                                            + "")
                                    .put(Emailv31.Message.ATTACHMENTS, new JSONArray()
                                            .put(new JSONObject()
                                                    .put("ContentType", "application/pdf")
                                                    .put("Filename", name + ".jpg")
                                                    .put("Base64Content", base64Content)))));
        }
        else{

                request = new MailjetRequest(Emailv31.resource)
                        .property(Emailv31.MESSAGES, new JSONArray()
                                .put(new JSONObject()
                                        .put(Emailv31.Message.FROM, new JSONObject().put("Email", "email@clearbill.store")
                                                .put("Name", shopName))
                                        .put(Emailv31.Message.TO,
                                                new JSONArray().put(
                                                        new JSONObject().put("Email", emailId).put(shopName, "Hello")))
                                        .put(Emailv31.Message.SUBJECT, subject)

                                        .put(Emailv31.Message.HTMLPART, htmlContent
                                                + "\n"
                                                + "\n"
                                                + "")));

        }

        response = client.post(request);
        System.out.println(response.getStatus());
        System.out.println(response.getData());
        return CompletableFuture.completedFuture(response.getData().toString());

    }
}
