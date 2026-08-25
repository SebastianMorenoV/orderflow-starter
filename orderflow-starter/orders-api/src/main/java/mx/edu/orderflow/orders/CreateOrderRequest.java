package mx.edu.orderflow.orders; import java.math.BigDecimal; public record CreateOrderRequest(String customerId, BigDecimal total) {}
