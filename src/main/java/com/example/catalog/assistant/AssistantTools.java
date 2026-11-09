package com.example.catalog.assistant;

import com.example.catalog.client.InventoryClient;
import com.example.catalog.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Same tool names, parameters and descriptions as REACTIVE. The return type is
 * Object so that, instead of throwing, a tool can hand the model a plain
 * message it can relay (an unknown product, or inventory data that is
 * temporarily unavailable).
 */
@Component
@RequiredArgsConstructor
public class AssistantTools {

    public static final String AUTHORIZATION_HEADER_KEY = "authorizationHeader";

    private final ProductService productService;
    private final InventoryClient inventoryClient;

    @Tool(description = "Get current stock status for a product by its numeric product ID. Returns the product's status (e.g. IN_STOCK, LOW_STOCK, OUT_OF_STOCK), its reorder threshold and quantity, and when it was last updated. Does not return an exact current quantity on hand.")
    public Object getStock(Long productId, ToolContext toolContext) {
        if (!inCatalog(productId)) {
            return notInCatalog(productId);
        }
        return inventoryClient.getStock(productId, authHeader(toolContext))
            .<Object>map(stock -> stock)
            .orElseGet(() -> inventoryUnavailable(productId));
    }

    @Tool(description = "Get a reorder cost estimate for a product by its numeric product ID, including reorder quantity, unit price, and total cost. This is a cost estimate, not a reorder history or a restock date.")
    public Object getReorderEstimate(Long productId, ToolContext toolContext) {
        if (!inCatalog(productId)) {
            return notInCatalog(productId);
        }
        return inventoryClient.getReorderEstimate(productId, authHeader(toolContext))
            .<Object>map(estimate -> estimate)
            .orElseGet(() -> inventoryUnavailable(productId));
    }

    private boolean inCatalog(Long productId) {
        return productId != null && productService.exists(productId);
    }

    private static String notInCatalog(Long productId) {
        return "No product with ID " + productId + " exists in the catalog.";
    }

    private static String inventoryUnavailable(Long productId) {
        return "Inventory data unavailable for product " + productId
            + " right now. Tell the user it could not be retrieved; do not estimate it.";
    }

    private static String authHeader(ToolContext toolContext) {
        return (String) toolContext.getContext().get(AUTHORIZATION_HEADER_KEY);
    }
}
