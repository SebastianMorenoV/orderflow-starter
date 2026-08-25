package mx.edu.orderflow.orders;
import java.math.BigDecimal; import java.util.*; import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
@Service public class OrderService { private final Map<Long,Order> orders=new LinkedHashMap<>(); private final AtomicLong ids=new AtomicLong(1000);
 public synchronized Order create(String customerId, BigDecimal total){ if(customerId==null||customerId.isBlank()) throw new IllegalArgumentException("customerId required"); if(total==null||total.signum()<=0) throw new IllegalArgumentException("total must be positive"); long id=ids.incrementAndGet(); Order o=new Order(id,customerId,total,OrderStatus.CREATED); orders.put(id,o); return o;}
 public synchronized List<Order> list(){ return new ArrayList<>(orders.values()); }
 public synchronized Optional<Order> find(long id){ return Optional.ofNullable(orders.get(id)); } }
