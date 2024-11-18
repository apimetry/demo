package demo.io;

import java.util.List;

public record CreateOrderRequest(List<PurchaseItem> items) {
}
