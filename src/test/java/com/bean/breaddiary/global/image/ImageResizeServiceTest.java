package com.bean.breaddiary.global.image;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageResizeServiceTest {

    private final ImageResizeService imageResizeService = new ImageResizeService();

    @Test
    void processBreadPhotoCreatesWebpOriginalAndThumbnail() throws IOException {
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "bread.jpg",
                "image/jpeg",
                imageBytes("jpg", 1600, 900)
        );

        ProcessedBreadPhoto processedBreadPhoto = imageResizeService.processBreadPhoto(photo);

        assertWebp(processedBreadPhoto.getOriginal());
        assertWebp(processedBreadPhoto.getThumbnail());
        assertMaxLongSide(processedBreadPhoto.getOriginal(), 1200);
        assertMaxLongSide(processedBreadPhoto.getThumbnail(), 400);
    }

    @Test
    void processBreadPhotoAcceptsPngImage() throws IOException {
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "bread.png",
                "image/png",
                imageBytes("png", 900, 1600)
        );

        ProcessedBreadPhoto processedBreadPhoto = imageResizeService.processBreadPhoto(photo);

        assertWebp(processedBreadPhoto.getOriginal());
        assertWebp(processedBreadPhoto.getThumbnail());
        assertMaxLongSide(processedBreadPhoto.getOriginal(), 1200);
        assertMaxLongSide(processedBreadPhoto.getThumbnail(), 400);
    }

    @Test
    void processBreadPhotoAcceptsWebpImage() throws IOException {
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "bread.webp",
                "image/webp",
                imageBytes("webp", 800, 600)
        );

        ProcessedBreadPhoto processedBreadPhoto = imageResizeService.processBreadPhoto(photo);

        assertWebp(processedBreadPhoto.getOriginal());
        assertWebp(processedBreadPhoto.getThumbnail());
        assertMaxLongSide(processedBreadPhoto.getOriginal(), 1200);
        assertMaxLongSide(processedBreadPhoto.getThumbnail(), 400);
    }

    @Test
    void processBreadPhotoRejectsInvalidImageBytes() {
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "invalid.jpg",
                "image/jpeg",
                "not-image".getBytes()
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> imageResizeService.processBreadPhoto(photo)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private void assertWebp(ResizedImage image) {
        assertEquals("image/webp", image.getContentType());
        assertEquals(".webp", image.getExtension());
        assertTrue(image.getBytes().length > 0);
    }

    private void assertMaxLongSide(ResizedImage image, int maxSize) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(image.getBytes()));

        assertNotNull(bufferedImage);
        assertTrue(bufferedImage.getWidth() <= maxSize);
        assertTrue(bufferedImage.getHeight() <= maxSize);
    }

    private byte[] imageBytes(String format, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.ORANGE);
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(Color.DARK_GRAY);
            graphics.fillOval(width / 4, height / 4, width / 2, height / 2);
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            boolean written = ImageIO.write(image, format, outputStream);
            assertTrue(written);
            return outputStream.toByteArray();
        }
    }
}
