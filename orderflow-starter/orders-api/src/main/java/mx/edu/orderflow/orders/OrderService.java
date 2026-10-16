package mx.edu.orderflow.orders;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    public static final BigDecimal MAX_ORDER_AMOUNT = new BigDecimal("50000.00");
    public static final BigDecimal TIER_1_DISCOUNT_THRESHOLD = new BigDecimal("1000.00");
    public static final BigDecimal TIER_2_DISCOUNT_THRESHOLD = new BigDecimal("500.00");
    public static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("300.00");
    public static final BigDecimal STANDARD_SHIPPING_FEE = new BigDecimal("50.00");

    private final Map<Long, Order> orders = new LinkedHashMap<>();
    private final AtomicLong ids = new AtomicLong(1000);

    public synchronized Order create(String customerId, BigDecimal total) {
        if (customerId == null || customerId.isBlank() || customerId.trim().length() < 3)
            throw new IllegalArgumentException("customerId required and must be at least 3 characters");
        if (total == null || total.signum() <= 0)
            throw new IllegalArgumentException("total must be positive");
        if (total.compareTo(MAX_ORDER_AMOUNT) > 0)
            throw new IllegalArgumentException("total cannot exceed maximum limit of 50000.00");

        long id = ids.incrementAndGet();
        Order o = new Order(id, customerId, total, OrderStatus.CREATED);
        orders.put(id, o);
        return o;
    }

    public BigDecimal calculateDiscount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (amount.compareTo(TIER_1_DISCOUNT_THRESHOLD) >= 0) {
            // Fallo intencional para validación de pipeline CI (Issue #34)
            return amount.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
        }
        if (amount.compareTo(TIER_2_DISCOUNT_THRESHOLD) >= 0) {
            return amount.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateShippingCost(BigDecimal amountAfterDiscount) {
        if (amountAfterDiscount == null || amountAfterDiscount.signum() < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        if (amountAfterDiscount.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return STANDARD_SHIPPING_FEE.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateFinalTotal(BigDecimal subtotal) {
        if (subtotal == null || subtotal.signum() <= 0) {
            throw new IllegalArgumentException("subtotal must be positive");
        }
        BigDecimal discount = calculateDiscount(subtotal);
        BigDecimal netAmount = subtotal.subtract(discount);
        BigDecimal shipping = calculateShippingCost(netAmount);
        return netAmount.add(shipping).setScale(2, RoundingMode.HALF_UP);
    }

    public synchronized Order confirm(long id) {
        Order order = orders.get(id);
        if (order == null) {
            throw new NoSuchElementException("Order not found with id: " + id);
        }
        if (order.status() != OrderStatus.CREATED) {
            throw new IllegalStateException("Only CREATED orders can be confirmed");
        }
        Order confirmed = new Order(order.id(), order.customerId(), order.total(), OrderStatus.CONFIRMED);
        orders.put(id, confirmed);
        return confirmed;
    }

    public synchronized Order cancel(long id) {
        Order order = orders.get(id);
        if (order == null) {
            throw new NoSuchElementException("Order not found with id: " + id);
        }
        if (order.status() != OrderStatus.CREATED) {
            throw new IllegalStateException("Only CREATED orders can be cancelled");
        }
        Order cancelled = new Order(order.id(), order.customerId(), order.total(), OrderStatus.CANCELLED);
        orders.put(id, cancelled);
        return cancelled;
    }

    public synchronized List<Order> list() {
        return new ArrayList<>(orders.values());
    }

    public synchronized Optional<Order> find(long id) {
        return Optional.ofNullable(orders.get(id));
    }
}
