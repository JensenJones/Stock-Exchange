package org.jj;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class MatchingEngineImplTest {

    private MatchingEngineImpl subject;



    @BeforeEach
    void setup() {
        TimestampProvider timestampProvider = Mockito.mock(TimestampProvider.class);
        when(timestampProvider.getTimestamp()).thenReturn(10L);

        IdProvider idProvider = Mockito.mock(IdProvider.class);
        when(idProvider.getUUID()).thenReturn(new UUID(0x1234567890abcdefL, 0xfedcba0987654321L));
        subject = new MatchingEngineImpl(timestampProvider, idProvider);
    }

    @Test
    void shouldCreateOrder() {
        OrderState orderState = subject.createOrder(1, 10, BuySell.BUY);
        assertThat(orderState).isNotNull();
        assertThat(orderState.getOrderStatus()).isEqualTo(OrderStatus.NEW);
    }

    @Test
    void shouldCreateUniqueOrders() {
        OrderState order1 = subject.createOrder(1, 10, BuySell.BUY);
        OrderState order2 = subject.createOrder(2, 20, BuySell.BUY);

        assertThat(order1.getOrder().getPrice()).isEqualTo(1);
        assertThat(order2.getOrder().getPrice()).isEqualTo(2);
        assertThat(order1.getOrder().getQuantity()).isEqualTo(10);
        assertThat(order2.getOrder().getQuantity()).isEqualTo(20);
    }

    @Test
    void shouldNotFindNonExistentOrder() {
        OrderState lookedUpOrder = subject.getOrder(UUID.randomUUID());

        assertThat(lookedUpOrder).isNull();
    }

    @Test
    void shouldFindExistingOrder() {
        OrderState order = subject.createOrder(1, 10, BuySell.BUY);

        OrderState foundOrder = subject.getOrder(order.getOrder().getId());

        assert foundOrder != null;
        assertThat(foundOrder.getOrder().getId()).isEqualTo(order.getOrder().getId());
    }

    @Test
    void shouldCancelOrder() {
        OrderState order = subject.createOrder(1, 10, BuySell.BUY);

        order = subject.cancelOrder(order.getOrder().getId());

        assert order != null;
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void cancelNonExistentOrder() {
        OrderState order = subject.cancelOrder(UUID.randomUUID());
        assertThat(order).isNull();
    }
}