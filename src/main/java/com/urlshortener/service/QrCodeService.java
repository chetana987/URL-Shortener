package com.urlshortener.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeService {

    private static final String CACHE_PREFIX = "qr:";
    private static final int DEFAULT_SIZE = 300;
    private static final int LOGO_SIZE = 50;

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.qr.cache-ttl:86400}")
    private long cacheTtl;

    @Value("${app.qr.size:300}")
    private int qrSize;

    @Value("${app.url.base-url:http://localhost:8080}")
    private String baseUrl;

    public byte[] generateQrCode(String shortCode) {
        String cacheKey = CACHE_PREFIX + shortCode;
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("QR cache hit for: {}", shortCode);
                return hexToBytes(cached);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for QR cache, generating fresh: {}", shortCode);
        }

        try {
            String url = baseUrl + "/" + shortCode;
            byte[] qrBytes = generateQrImage(url, qrSize);

            try {
                stringRedisTemplate.opsForValue().set(cacheKey, bytesToHex(qrBytes), cacheTtl, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Failed to cache QR code: {}", shortCode);
            }

            return qrBytes;
        } catch (Exception e) {
            log.error("Failed to generate QR code for: {}", shortCode, e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    private byte[] generateQrImage(String text, int size) throws WriterException, IOException {
        Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints);

        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        qrImage = addPadding(qrImage, 10);
        qrImage = addLogo(qrImage);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "PNG", baos);
        return baos.toByteArray();
    }

    private BufferedImage addPadding(BufferedImage image, int padding) {
        int newSize = image.getWidth() + padding * 2;
        BufferedImage padded = new BufferedImage(newSize, newSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = padded.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, newSize, newSize);
        g.drawImage(image, padding, padding, null);
        g.dispose();
        return padded;
    }

    private BufferedImage addLogo(BufferedImage qrImage) {
        try {
            int qrWidth = qrImage.getWidth();
            int qrHeight = qrImage.getHeight();

            BufferedImage combined = new BufferedImage(qrWidth, qrHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = combined.createGraphics();
            g.drawImage(qrImage, 0, 0, null);

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            int centerX = (qrWidth - LOGO_SIZE) / 2;
            int centerY = (qrHeight - LOGO_SIZE) / 2;
            g.fillRoundRect(centerX - 4, centerY - 4, LOGO_SIZE + 8, LOGO_SIZE + 8, 8, 8);

            g.setColor(new Color(108, 92, 231));
            g.fillRoundRect(centerX, centerY, LOGO_SIZE, LOGO_SIZE, 6, 6);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Segoe UI", Font.BOLD, 20));
            FontMetrics fm = g.getFontMetrics();
            String logoText = "URL";
            int textX = centerX + (LOGO_SIZE - fm.stringWidth(logoText)) / 2;
            int textY = centerY + ((LOGO_SIZE - fm.getHeight()) / 2) + fm.getAscent();
            g.drawString(logoText, textX, textY);

            g.dispose();
            return combined;
        } catch (Exception e) {
            log.warn("Failed to add logo to QR code, returning plain QR");
            return qrImage;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
