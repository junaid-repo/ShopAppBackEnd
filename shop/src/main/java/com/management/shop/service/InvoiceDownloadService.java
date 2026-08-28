package com.management.shop.service;

import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import com.management.shop.entity.BillingEntity;
import com.management.shop.entity.InvoiceDownloadTokenEntity;
import com.management.shop.repository.BillingRepository;
import com.management.shop.repository.InvoiceDownloadTokenRepository;
import com.management.shop.util.QRCodeGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class InvoiceDownloadService {

    private static final int TOKEN_BYTES = 32;
    private static final float POINTS_PER_MILLIMETRE = 72f / 25.4f;

    private final InvoiceDownloadTokenRepository tokenRepository;
    private final BillingRepository billingRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${invoice.download.base-url:}")
    private String configuredBaseUrl;

    @Value("${invoice.download.expiry-hours:168}")
    private long expiryHours;

    public InvoiceDownloadService(
            InvoiceDownloadTokenRepository tokenRepository,
            BillingRepository billingRepository
    ) {
        this.tokenRepository = tokenRepository;
        this.billingRepository = billingRepository;
    }

    @Transactional
    public byte[] createInvoiceDownloadQr(String invoiceNumber, String userId, String requestBaseUrl) {
        BillingEntity invoice = billingRepository.findOrderByReference(invoiceNumber, userId);
        if (invoice == null) {
            throw new IllegalArgumentException("Invoice not found");
        }

        String rawToken = generateToken();
        LocalDateTime now = LocalDateTime.now();
        long validForHours = Math.max(1, expiryHours);

        tokenRepository.save(InvoiceDownloadTokenEntity.builder()
                .tokenHash(hashToken(rawToken))
                .invoiceNumber(invoice.getInvoiceNumber())
                .userId(invoice.getUserId())
                .createdAt(now)
                .expiresAt(now.plusHours(validForHours))
                .revoked(false)
                .build());

        String baseUrl = configuredBaseUrl == null || configuredBaseUrl.isBlank()
                ? requestBaseUrl
                : configuredBaseUrl.trim();

        String downloadUrl = UriComponentsBuilder
                .fromUriString(removeTrailingSlash(baseUrl))
                .path("/api/public/invoices/{token}/download")
                .buildAndExpand(rawToken)
                .toUriString();

        String qrCode = QRCodeGenerator.generateQRCodeBase64(downloadUrl, 500, 500);
        if (qrCode == null) {
            throw new IllegalStateException("Unable to generate invoice download QR code");
        }

        return Base64.getDecoder().decode(qrCode);
    }

    @Transactional(readOnly = true)
    public InvoiceDownloadAccess validateToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Invalid invoice download token");
        }

        InvoiceDownloadTokenEntity token = tokenRepository
                .findByTokenHashAndRevokedFalse(hashToken(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("Invalid invoice download token"));

        if (!token.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invoice download token has expired");
        }

        BillingEntity invoice = billingRepository.findOrderByReference(
                token.getInvoiceNumber(),
                token.getUserId()
        );
        if (invoice == null) {
            throw new IllegalArgumentException("Invoice is unavailable");
        }

        return new InvoiceDownloadAccess(invoice.getInvoiceNumber(), invoice.getUserId());
    }

    public byte[] wrapInvoiceImageAsPdf(byte[] invoiceImage) throws Exception {
        if (invoiceImage == null || invoiceImage.length == 0) {
            throw new IllegalArgumentException("Invoice image is empty");
        }

        Image image = Image.getInstance(invoiceImage);
        float aspectRatio = image.getHeight() / image.getWidth();
        float pageWidthMm = aspectRatio > 2f ? 80f : 210f;
        float pageHeightMm = pageWidthMm * aspectRatio;

        if (pageHeightMm > 1000f) {
            float scale = 1000f / pageHeightMm;
            pageWidthMm *= scale;
            pageHeightMm = 1000f;
        }

        float pageWidth = pageWidthMm * POINTS_PER_MILLIMETRE;
        float pageHeight = pageHeightMm * POINTS_PER_MILLIMETRE;
        Rectangle pageSize = new Rectangle(pageWidth, pageHeight);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(pageSize, 0, 0, 0, 0);
            PdfWriter.getInstance(document, output);
            document.open();
            image.scaleAbsolute(pageWidth, pageHeight);
            image.setAbsolutePosition(0, 0);
            document.add(image);
            document.close();
            return output.toByteArray();
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String removeTrailingSlash(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public record InvoiceDownloadAccess(String invoiceNumber, String userId) {
    }
}
