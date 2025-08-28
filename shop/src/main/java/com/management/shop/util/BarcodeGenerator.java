/*
 * package com.management.shop.util;
 * 
 * import java.io.ByteArrayOutputStream; import java.io.IOException; import
 * java.util.Base64;
 * 
 * import com.google.zxing.BarcodeFormat; import
 * com.google.zxing.WriterException; import
 * com.google.zxing.client.j2se.MatrixToImageWriter; import
 * com.google.zxing.common.BitMatrix; import
 * com.google.zxing.oned.Code128Writer;
 * 
 * public class BarcodeGenerator { public static String
 * generateBarcodeBase64(String text) throws WriterException, IOException {
 * Code128Writer barcodeWriter = new Code128Writer(); BitMatrix bitMatrix =
 * barcodeWriter.encode(text, BarcodeFormat.CODE_128, 400, 120);
 * 
 * ByteArrayOutputStream baos = new ByteArrayOutputStream();
 * MatrixToImageWriter.writeToStream(bitMatrix, "png", baos);
 * 
 * return Base64.getEncoder().encodeToString(baos.toByteArray()); } }
 */