package demojavaapp.io;

import java.util.List;

public record CreateOrderRequest(List<PurchaseItem> items) {
}
