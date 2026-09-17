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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    private CartServiceImpl cartService;

    private User sampleUser;
    private Product sampleProduct;
    private Inventory sampleInventory;
    private Cart sampleCart;

    @BeforeEach
    void setUp() {
        cartService = new CartServiceImpl(cartRepository, userRepository, productRepository, inventoryRepository);

        sampleUser = new User();
        sampleUser.setId(UUID.randomUUID());
        sampleUser.setEmail("buyer@example.com");

        sampleProduct = new Product();
        sampleProduct.setId(UUID.randomUUID());
        sampleProduct.setName("Headphones");
        sampleProduct.setSku("HP-01");
        sampleProduct.setUnitPrice(new BigDecimal("99.99"));
        sampleProduct.setActive(true);

        sampleInventory = new Inventory();
        sampleInventory.setId(UUID.randomUUID());
        sampleInventory.setProduct(sampleProduct);
        sampleInventory.setAvailableQuantity(15);

        sampleCart = new Cart();
        sampleCart.setId(UUID.randomUUID());
        sampleCart.setUser(sampleUser);
    }

    @Test
    void getCart_whenCartExists_returnsCartResponse() {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(sampleUser));
        when(cartRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(sampleCart));

        CartResponse response = cartService.getCart("buyer@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(sampleCart.getId());
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void getCart_whenCartDoesNotExist_createsAndReturnsNewCart() {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(sampleUser));
        when(cartRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CartResponse response = cartService.getCart("buyer@example.com");

        assertThat(response).isNotNull();
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addItem_success() {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(sampleUser));
        when(cartRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(sampleCart));
        when(productRepository.findByIdAndActiveTrue(sampleProduct.getId())).thenReturn(Optional.of(sampleProduct));
        when(inventoryRepository.findByProductId(sampleProduct.getId())).thenReturn(Optional.of(sampleInventory));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddToCartRequest request = new AddToCartRequest(sampleProduct.getId(), 2);
        CartResponse response = cartService.addItem("buyer@example.com", request);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getTotalItems()).isEqualTo(2);
        assertThat(response.getSubtotal()).isEqualByComparingTo(new BigDecimal("199.98"));
    }

    @Test
    void addItem_insufficientInventory_throwsException() {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(sampleUser));
        when(cartRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(sampleCart));
        when(productRepository.findByIdAndActiveTrue(sampleProduct.getId())).thenReturn(Optional.of(sampleProduct));
        when(inventoryRepository.findByProductId(sampleProduct.getId())).thenReturn(Optional.of(sampleInventory));

        AddToCartRequest request = new AddToCartRequest(sampleProduct.getId(), 20); // only 15 available

        assertThatThrownBy(() -> cartService.addItem("buyer@example.com", request))
                .isInstanceOf(InsufficientInventoryException.class);
    }

    @Test
    void addItem_productNotFound_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(sampleUser));
        when(cartRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(sampleCart));
        when(productRepository.findByIdAndActiveTrue(nonExistentId)).thenReturn(Optional.empty());

        AddToCartRequest request = new AddToCartRequest(nonExistentId, 1);

        assertThatThrownBy(() -> cartService.addItem("buyer@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateItemQuantity_success() {
        sampleCart.addItem(sampleProduct, 2);
        UUID itemId = UUID.randomUUID();
        sampleCart.getItems().get(0).setId(itemId);

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(sampleUser));
        when(cartRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(sampleCart));
        when(inventoryRepository.findByProductId(sampleProduct.getId())).thenReturn(Optional.of(sampleInventory));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateCartItemRequest request = new UpdateCartItemRequest(5);
        CartResponse response = cartService.updateItemQuantity("buyer@example.com", itemId, request);

        assertThat(response.getTotalItems()).isEqualTo(5);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void removeItem_success() {
        sampleCart.addItem(sampleProduct, 2);
        UUID itemId = UUID.randomUUID();
        sampleCart.getItems().get(0).setId(itemId);

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(sampleUser));
        when(cartRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(sampleCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.removeItem("buyer@example.com", itemId);

        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void clearCart_success() {
        sampleCart.addItem(sampleProduct, 2);

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(sampleUser));
        when(cartRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(sampleCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.clearCart("buyer@example.com");

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalItems()).isEqualTo(0);
    }
}
