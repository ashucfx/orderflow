package com.orderflow.cart.controller;

import com.orderflow.auth.util.SecurityUtils;
import com.orderflow.cart.dto.AddToCartRequest;
import com.orderflow.cart.dto.CartResponse;
import com.orderflow.cart.dto.UpdateCartItemRequest;
import com.orderflow.cart.service.CartService;
import com.orderflow.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart operations for authenticated users")
@SecurityRequirement(name = "BearerAuth")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get the current user's shopping cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        String email = SecurityUtils.getCurrentUserEmail();
        CartResponse cart = cartService.getCart(email);
        return ResponseEntity.ok(ApiResponse.ok(cart));
    }

    @PostMapping("/items")
    @Operation(summary = "Add an item to the shopping cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(@Valid @RequestBody AddToCartRequest request) {
        String email = SecurityUtils.getCurrentUserEmail();
        CartResponse cart = cartService.addItem(email, request);
        return ResponseEntity.ok(ApiResponse.ok("Item added to cart", cart));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update quantity of a cart item")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        String email = SecurityUtils.getCurrentUserEmail();
        CartResponse cart = cartService.updateItemQuantity(email, itemId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cart item updated", cart));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove an item from the shopping cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(@PathVariable UUID itemId) {
        String email = SecurityUtils.getCurrentUserEmail();
        CartResponse cart = cartService.removeItem(email, itemId);
        return ResponseEntity.ok(ApiResponse.ok("Item removed from cart", cart));
    }

    @DeleteMapping
    @Operation(summary = "Clear all items from the shopping cart")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        String email = SecurityUtils.getCurrentUserEmail();
        cartService.clearCart(email);
        return ResponseEntity.ok(ApiResponse.ok("Cart cleared"));
    }
}
