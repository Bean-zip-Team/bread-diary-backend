package com.bean.breaddiary.global.image;

import org.imgscalr.Scalr;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Service
public class ImageResizeService {

    private static final int ORIGINAL_MAX_SIZE = 1200;
    private static final int THUMBNAIL_MAX_SIZE = 400;
    private static final String WEBP_FORMAT = "webp";
    private static final String WEBP_CONTENT_TYPE = "image/webp";
    private static final String WEBP_EXTENSION = ".webp";
    private static final float WEBP_QUALITY = 0.8F;

    public ProcessedBreadPhoto processBreadPhoto(MultipartFile photo) {
        BufferedImage sourceImage = readImage(photo);
        BufferedImage originalImage = resizeToMaxLongSide(sourceImage, ORIGINAL_MAX_SIZE);
        BufferedImage thumbnailImage = resizeToMaxLongSide(sourceImage, THUMBNAIL_MAX_SIZE);

        return new ProcessedBreadPhoto(
                toResizedImage(originalImage),
                toResizedImage(thumbnailImage)
        );
    }

    private BufferedImage readImage(MultipartFile photo) {
        try {
            BufferedImage image = ImageIO.read(photo.getInputStream());
            if (image == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바른 이미지 파일을 업로드해주세요.");
            }

            return image;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 파일을 읽을 수 없습니다.", exception);
        }
    }

    private BufferedImage resizeToMaxLongSide(BufferedImage image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= maxSize && height <= maxSize) {
            return image;
        }

        return Scalr.resize(
                image,
                Scalr.Method.QUALITY,
                Scalr.Mode.AUTOMATIC,
                maxSize
        );
    }

    private ResizedImage toResizedImage(BufferedImage image) {
        BufferedImage webpCompatibleImage = toWebpCompatibleImage(image);

        ImageWriter writer = getWebpWriter();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            if (imageOutputStream == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 변환에 실패했습니다.");
            }

            writer.setOutput(imageOutputStream);
            writer.write(null, new IIOImage(webpCompatibleImage, null, null), createWebpWriteParam(writer));
            imageOutputStream.flush();

            return new ResizedImage(
                    outputStream.toByteArray(),
                    WEBP_CONTENT_TYPE,
                    WEBP_EXTENSION
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 변환에 실패했습니다.", exception);
        } finally {
            writer.dispose();
        }
    }

    private ImageWriter getWebpWriter() {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(WEBP_FORMAT);
        if (!writers.hasNext()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "WebP 이미지 변환 설정이 필요합니다."
            );
        }

        return writers.next();
    }

    private ImageWriteParam createWebpWriteParam(ImageWriter writer) {
        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        if (writeParam.canWriteCompressed()) {
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            String[] compressionTypes = writeParam.getCompressionTypes();
            if (compressionTypes != null && compressionTypes.length > 0) {
                writeParam.setCompressionType(compressionTypes[0]);
            }
            writeParam.setCompressionQuality(WEBP_QUALITY);
        }

        return writeParam;
    }

    private BufferedImage toWebpCompatibleImage(BufferedImage image) {
        BufferedImage convertedImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = convertedImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        return convertedImage;
    }
}
