package com.management.shop.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class BarcodeGenerator {
    public static String generateBarcodeBase64(String text) throws Exception {
        int width = 30;
        int height = 20; // horizontal style
        BitMatrix bitMatrix = new MultiFormatWriter()
                .encode(text, BarcodeFormat.CODE_128, width, height);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "png", baos);

        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
