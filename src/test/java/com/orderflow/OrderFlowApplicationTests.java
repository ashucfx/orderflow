package com.orderflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class OrderFlowApplicationTests {

    @Test
    void applicationClassInstantiates() {
        assertDoesNotThrow(OrderFlowApplication::new);
    }
}
