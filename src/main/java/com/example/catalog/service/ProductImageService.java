package com.example.catalog.service;

import com.example.catalog.storage.S3ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Like REACTIVE, this does not check that the product exists before uploading.
 * An S3 failure is surfaced as a 503; REACTIVE let it fall through to a
 * default 500.
 */
@Service
@RequiredArgsConstructor
public class ProductImageService {

    private static final String DEFAULT_CONTENT_TYPE = "image/jpeg";

    private final S3ImageStorage imageStorage;

    /** Raw request body, as REACTIVE accepts it. */
    public String uploadImage(Long productId, byte[] imageBytes, String contentType) {
        String type = (contentType == null || contentType.isBlank()) ? DEFAULT_CONTENT_TYPE : contentType;
        try {
            return imageStorage.upload(productId, imageBytes, type);
        } catch (SdkException e) {
            throw new DownstreamServiceException("Image storage unavailable", e);
        }
    }

    /** multipart/form-data upload. */
    public String uploadImage(Long productId, MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read uploaded image", e);
        }
        return uploadImage(productId, bytes, file.getContentType());
    }
}
