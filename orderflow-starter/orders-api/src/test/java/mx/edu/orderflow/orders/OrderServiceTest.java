package mx.edu.orderflow.orders;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  ", "ab" })
    void rejectsInvalidCustomerIds(String customerId) {
        // Arrange
        BigDecimal total = new BigDecimal("10.00");

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> service.create(customerId, total));
    }

    @ParameterizedTest
    @MethodSource("invalidTotals")
    void rejectsNullZeroAndNegativeTotals(BigDecimal total) {
        // Arrange
        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> service.create("student-1", total));
    }

    static Stream<Arguments> invalidTotals() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of(BigDecimal.ZERO),
                Arguments.of(new BigDecimal("-1.00")));
    }

    @ParameterizedTest
    @MethodSource("orderTotalsAtMaximumBoundary")
    void validatesOrderTotalAtMaximumBoundary(BigDecimal total, boolean isValid) {
        // Arrange
        if (isValid) {
            // Act
            Order order = service.create("student-1", total);

            // Assert
            assertEquals(total, order.total());
        } else {
            // Act / Assert
            assertThrows(IllegalArgumentException.class, () -> service.create("student-1", total));
        }
    }

    static Stream<Arguments> orderTotalsAtMaximumBoundary() {
        return Stream.of(
                Arguments.of(new BigDecimal("49999.99"), true),
                Arguments.of(new BigDecimal("50000.00"), true),
                Arguments.of(new BigDecimal("50000.01"), false));
    }

    @ParameterizedTest
    @MethodSource("discountCases")
    void calculatesDiscountAtTierBoundaries(BigDecimal amount, BigDecimal expectedDiscount) {
        // Arrange
        // Act
        BigDecimal discount = service.calculateDiscount(amount);

        // Assert
        assertEquals(expectedDiscount, discount);
    }

    static Stream<Arguments> discountCases() {
        return Stream.of(
                Arguments.of(new BigDecimal("499.99"), new BigDecimal("0.00")),
                Arguments.of(new BigDecimal("500.00"), new BigDecimal("25.00")),
                Arguments.of(new BigDecimal("999.99"), new BigDecimal("50.00")),
                Arguments.of(new BigDecimal("1000.00"), new BigDecimal("100.00")));
    }

    @ParameterizedTest
    @MethodSource("invalidTotals")
    void rejectsInvalidDiscountAmounts(BigDecimal amount) {
        // Arrange
        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> service.calculateDiscount(amount));
    }

    @ParameterizedTest
    @CsvSource({ "299.99, 50.00", "300.00, 0.00", "300.01, 0.00" })
    void calculatesShippingAtFreeShippingBoundary(String amountText, String expectedCostText) {
        // Arrange
        BigDecimal amount = new BigDecimal(amountText);
        BigDecimal expectedCost = new BigDecimal(expectedCostText);

        // Act
        BigDecimal shippingCost = service.calculateShippingCost(amount);

        // Assert
        assertEquals(expectedCost, shippingCost);
    }

    @ParameterizedTest
    @MethodSource("invalidShippingAmounts")
    void rejectsNullOrNegativeShippingAmounts(BigDecimal amount) {
        // Arrange
        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> service.calculateShippingCost(amount));
    }

    static Stream<Arguments> invalidShippingAmounts() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of(new BigDecimal("-0.01")));
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

    @ParameterizedTest
    @MethodSource("invalidTotals")
    void rejectsInvalidFinalTotalSubtotal(BigDecimal subtotal) {
        // Arrange
        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> service.calculateFinalTotal(subtotal));
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
