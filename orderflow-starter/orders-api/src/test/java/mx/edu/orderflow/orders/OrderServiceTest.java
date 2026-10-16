package mx.edu.orderflow.orders;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

    private OrderService service;

    @BeforeEach
    void setUp() {
        // Arrange: cada prueba comienza con un servicio y almacenamiento limpios.
        service = new OrderService();
    }

    @Test
    void createsOrderWithGeneratedIdAndCreatedStatus() {
        // Arrange
        String customerId = "student-1";
        BigDecimal total = new BigDecimal("150.00");

        // Act
        Order order = service.create(customerId, total);

        // Assert
        assertAll(
                () -> assertEquals(1001L, order.id()),
                () -> assertEquals(customerId, order.customerId()),
                () -> assertEquals(total, order.total()),
                () -> assertEquals(OrderStatus.CREATED, order.status()));
    }

    @Test
    void rejectsInvalidCustomerIds() {
        // Arrange
        String[] invalidCustomerIds = { null, "", "  ", "ab" };

        // Act / Assert
        for (String customerId : invalidCustomerIds) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.create(customerId, new BigDecimal("10.00")),
                    "Debe rechazar customerId: " + customerId);
        }
    }

    @Test
    void rejectsNullZeroAndNegativeTotals() {
        // Arrange
        BigDecimal[] invalidTotals = { null, BigDecimal.ZERO, new BigDecimal("-1.00") };

        // Act / Assert
        for (BigDecimal total : invalidTotals) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.create("student-1", total));
        }
    }

    @Test
    void rejectsTotalAboveMaximum() {
        // Arrange
        BigDecimal totalAboveMaximum = new BigDecimal("50000.01");

        // Act / Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.create("student-1", totalAboveMaximum));
    }

    @Test
    void calculatesDiscountForEachTier() {
        // Arrange
        BigDecimal tierOneAmount = new BigDecimal("1000.00");
        BigDecimal tierTwoAmount = new BigDecimal("500.00");
        BigDecimal noDiscountAmount = new BigDecimal("499.99");

        // Act
        BigDecimal tierOneDiscount = service.calculateDiscount(tierOneAmount);
        BigDecimal tierTwoDiscount = service.calculateDiscount(tierTwoAmount);
        BigDecimal noDiscount = service.calculateDiscount(noDiscountAmount);

        // Assert
        assertAll(
                () -> assertEquals(new BigDecimal("100.00"), tierOneDiscount),
                () -> assertEquals(new BigDecimal("25.00"), tierTwoDiscount),
                () -> assertEquals(new BigDecimal("0.00"), noDiscount));
    }

    @Test
    void rejectsInvalidDiscountAmounts() {
        // Arrange
        BigDecimal[] invalidAmounts = { null, BigDecimal.ZERO, new BigDecimal("-1.00") };

        // Act / Assert
        for (BigDecimal amount : invalidAmounts) {
            assertThrows(IllegalArgumentException.class, () -> service.calculateDiscount(amount));
        }
    }

    @Test
    void calculatesShippingBasedOnAmountAfterDiscount() {
        // Arrange
        BigDecimal eligibleForFreeShipping = new BigDecimal("300.00");
        BigDecimal chargedShipping = new BigDecimal("299.99");

        // Act
        BigDecimal freeShipping = service.calculateShippingCost(eligibleForFreeShipping);
        BigDecimal standardShipping = service.calculateShippingCost(chargedShipping);

        // Assert
        assertAll(
                () -> assertEquals(new BigDecimal("0.00"), freeShipping),
                () -> assertEquals(new BigDecimal("50.00"), standardShipping));
    }

    @Test
    void rejectsNullOrNegativeShippingAmounts() {
        // Arrange
        BigDecimal[] invalidAmounts = { null, new BigDecimal("-0.01") };

        // Act / Assert
        for (BigDecimal amount : invalidAmounts) {
            assertThrows(IllegalArgumentException.class, () -> service.calculateShippingCost(amount));
        }
    }

    @Test
    void calculatesFinalTotalWithDiscountAndShipping() {
        // Arrange
        BigDecimal subtotal = new BigDecimal("100.00");

        // Act
        BigDecimal finalTotal = service.calculateFinalTotal(subtotal);

        // Assert: sin descuento, se agregan 50.00 de envío.
        assertEquals(new BigDecimal("150.00"), finalTotal);
    }

    @Test
    void rejectsInvalidFinalTotalSubtotal() {
        // Arrange
        BigDecimal[] invalidSubtotals = { null, BigDecimal.ZERO, new BigDecimal("-1.00") };

        // Act / Assert
        for (BigDecimal subtotal : invalidSubtotals) {
            assertThrows(IllegalArgumentException.class, () -> service.calculateFinalTotal(subtotal));
        }
    }

    @Test
    void confirmsCreatedOrderAndMakesItFindable() {
        // Arrange
        Order created = service.create("student-1", new BigDecimal("150.00"));

        // Act
        Order confirmed = service.confirm(created.id());

        // Assert
        assertEquals(OrderStatus.CONFIRMED, confirmed.status());
        assertEquals(confirmed, service.find(created.id()).orElseThrow());
    }

    @Test
    void cancelsCreatedOrder() {
        // Arrange
        Order created = service.create("student-1", new BigDecimal("150.00"));

        // Act
        Order cancelled = service.cancel(created.id());

        // Assert
        assertEquals(OrderStatus.CANCELLED, cancelled.status());
    }

    @Test
    void rejectsConfirmingOrCancellingAnOrderThatIsNotCreated() {
        // Arrange
        Order created = service.create("student-1", new BigDecimal("150.00"));
        service.confirm(created.id());

        // Act / Assert
        assertThrows(IllegalStateException.class, () -> service.confirm(created.id()));
        assertThrows(IllegalStateException.class, () -> service.cancel(created.id()));
    }

    @Test
    void rejectsOperationsForUnknownOrder() {
        // Arrange
        long unknownOrderId = 9999L;

        // Act / Assert
        assertAll(
                () -> assertThrows(NoSuchElementException.class, () -> service.confirm(unknownOrderId)),
                () -> assertThrows(NoSuchElementException.class, () -> service.cancel(unknownOrderId)));
    }

    @Test
    void listsOrdersAndReturnsEmptyOptionalForUnknownId() {
        // Arrange
        Order firstOrder = service.create("student-1", new BigDecimal("150.00"));
        Order secondOrder = service.create("student-2", new BigDecimal("250.00"));

        // Act
        var orders = service.list();
        var unknownOrder = service.find(9999L);

        // Assert
        assertEquals(java.util.List.of(firstOrder, secondOrder), orders);
        assertTrue(unknownOrder.isEmpty());
    }
}
