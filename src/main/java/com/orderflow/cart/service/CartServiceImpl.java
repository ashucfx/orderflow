package com.orderflow.cart.service;

import com.orderflow.cart.domain.Cart;
import com.orderflow.cart.domain.CartItem;
import com.orderflow.cart.dto.AddToCartRequest;
import com.orderflow.cart.dto.CartResponse;
import com.orderflow.cart.dto.UpdateCartItemRequest;
import com.orderflow.cart.repository.CartRepository;
import com.orderflow.common.exception.InsufficientInventoryException;
import com.orderflow.common.exception.ResourceNotFoundException;
import com.orderflow.inventory.domain.Inventory;
import com.orderflow.inventory.repository.InventoryRepository;
import com.orderflow.product.domain.Product;
import com.orderflow.product.repository.ProductRepository;
import com.orderflow.user.domain.User;
import com.orderflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public CartResponse getCart(String userEmail) {
        Cart cart = getOrCreateCart(userEmail);
        return CartResponse.fromEntity(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(String userEmail, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userEmail);

        Product product = productRepository.findByIdAndActiveTrue(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));

        int existingQty = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .mapToInt(CartItem::getQuantity)
                .findFirst()
                .orElse(0);

        int totalRequested = existingQty + request.getQuantity();

        Inventory inventory = inventoryRepository.findByProductId(product.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory for product", product.getId()));

        if (inventory.getAvailableQuantity() < totalRequested) {
            throw new InsufficientInventoryException(
                    "Cannot add " + request.getQuantity() + " item(s). Available inventory: "
                            + inventory.getAvailableQuantity() + " (already in cart: " + existingQty + ")"
            );
        }

        cart.addItem(product, request.getQuantity());
        Cart saved = cartRepository.save(cart);
        return CartResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(String userEmail, UUID itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userEmail);
        CartItem item = cart.findItemById(itemId);

        Inventory inventory = inventoryRepository.findByProductId(item.getProduct().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory for product", item.getProduct().getId()));

        if (inventory.getAvailableQuantity() < request.getQuantity()) {
            throw new InsufficientInventoryException(
                    "Requested quantity " + request.getQuantity() + " exceeds available stock of "
                            + inventory.getAvailableQuantity()
            );
        }

        cart.updateItemQuantity(itemId, request.getQuantity());
        Cart saved = cartRepository.save(cart);
        return CartResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public CartResponse removeItem(String userEmail, UUID itemId) {
        Cart cart = getOrCreateCart(userEmail);
        cart.removeItem(itemId);
        Cart saved = cartRepository.save(cart);
        return CartResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public CartResponse clearCart(String userEmail) {
        Cart cart = getOrCreateCart(userEmail);
        cart.clear();
        Cart saved = cartRepository.save(cart);
        return CartResponse.fromEntity(saved);
    }

    private Cart getOrCreateCart(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }
}
