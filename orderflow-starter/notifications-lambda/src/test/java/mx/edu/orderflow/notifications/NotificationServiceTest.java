package mx.edu.orderflow.notifications;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {

    private NotificationService service;

    @BeforeEach
    void setUp() {
        // Arrange
        service = new NotificationService();
    }

    @Test
    void createsConfirmationWithOrderAndCustomerDetails() {
        // Arrange
        String orderId = "1001";
        String customerId = "c-1";

        // Act
        NotificationMessage message = service.confirmation(orderId, customerId);

        // Assert
        assertAll(
                () -> assertEquals(orderId, message.orderId()),
                () -> assertEquals(customerId, message.customerId()),
                () -> assertEquals("Order 1001 confirmed", message.message()));
    }

    @Test
    void rejectsNullOrBlankOrderId() {
        // Arrange
        String[] invalidOrderIds = { null, "", "  " };

        // Act / Assert
        for (String orderId : invalidOrderIds) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.confirmation(orderId, "c-1"));
        }
    }

    @Test
    void allowsNullCustomerIdBecauseServiceDoesNotValidateIt() {
        // Arrange
        String orderId = "1001";

        // Act
        NotificationMessage message = service.confirmation(orderId, null);

        // Assert
        assertNull(message.customerId());
    }
}
