package demo.io;

import java.util.List;

public record GetOrderResponse(
    int id,
    int merchantId,
    List<PurchaseItem> lineItems,
    ShipmentMethod shipment
) {
}
