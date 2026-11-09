package com.example.catalog.controller;

import com.example.catalog.model.ProductCount;
import com.example.catalog.model.ProductRequest;
import com.example.catalog.model.ProductResponse;
import com.example.catalog.model.ProductWithPrice;
import com.example.catalog.service.ProductImageService;
import com.example.catalog.service.ProductPricingService;
import com.example.catalog.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Same paths, parameters and status codes as REACTIVE's ProductRouter / ProductHandler. */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductPricingService pricingService;
    private final ProductImageService imageService;

    @GetMapping
    public List<ProductResponse> findAll() {
        return productService.findAll();
    }

    @GetMapping("/category/{category}")
    public List<ProductResponse> findByCategory(@PathVariable String category) {
        return productService.findByCategory(category);
    }

    @GetMapping("/search")
    public List<ProductResponse> searchByName(@RequestParam(defaultValue = "") String name) {
        return productService.findByName(name);
    }

    /**
     * MVC-only addition (not in REACTIVE). The literal "/count" pattern is more
     * specific than "/{id}", so Spring MVC picks this mapping; ProductControllerTest
     * checks that it never reaches findById.
     */
    @GetMapping("/count")
    public ProductCount count() {
        return new ProductCount(productService.countProducts());
    }

    @GetMapping("/{id}/price")
    public ProductWithPrice getProductWithCurrency(@PathVariable Long id,
                                                   @RequestParam(defaultValue = "USD") String currency) {
        return pricingService.getProductWithPrice(id, currency);
    }

    /** multipart/form-data upload with the image in a part named "file". */
    @PostMapping(value = "/{id}/image",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public String uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return imageService.uploadImage(id, file);
    }

    /** REACTIVE's contract: the raw image bytes as the body; Content-Type defaults to image/jpeg. */
    @PostMapping(value = "/{id}/image", produces = MediaType.APPLICATION_JSON_VALUE)
    public String uploadImageBytes(@PathVariable Long id,
                                   @RequestBody byte[] imageBytes,
                                   @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType) {
        return imageService.uploadImage(id, imageBytes, contentType);
    }

    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }
}
