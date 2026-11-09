package com.example.catalog.storage;

import com.example.catalog.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * S3 adapter for product images: same bucket, key format and returned URL as
 * REACTIVE's ProductImageService. REACTIVE's deleteImage was never called, so
 * it is not ported.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class S3ImageStorage {

    private final S3Client s3Client;
    private final AwsProperties awsProperties;

    /** Uploads the image and returns its public S3 URL. Throws SdkException on failure. */
    public String upload(Long productId, byte[] imageBytes, String contentType) {
        String key = "products/" + productId + "/image.jpg";

        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(awsProperties.s3().bucket())
            .key(key)
            .contentType(contentType)
            .build();

        try {
            s3Client.putObject(request, RequestBody.fromBytes(imageBytes));
        } catch (SdkException e) {
            log.error("Failed to upload image for product {}: {}", productId, e.getMessage());
            throw e;
        }

        String url = buildImageUrl(key);
        log.info("Uploaded image for product {}: {}", productId, url);
        return url;
    }

    private String buildImageUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
            awsProperties.s3().bucket(), awsProperties.region(), key);
    }
}
