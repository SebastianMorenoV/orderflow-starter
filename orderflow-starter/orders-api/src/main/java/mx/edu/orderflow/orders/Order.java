package mx.edu.orderflow.orders; import java.math.BigDecimal; public record Order(long id,String customerId,BigDecimal total,OrderStatus status) {}
