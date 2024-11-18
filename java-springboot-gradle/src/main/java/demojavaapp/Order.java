package demojavaapp;

import demojavaapp.io.PurchaseItem;
import demojavaapp.io.ShipmentMethod;

import java.util.ArrayList;
import java.util.List;

public class Order {
    public final int id;
    public final int merchantId;
    public List<PurchaseItem> items;
    public ShipmentMethod shipment;

    public Order(int id, int merchantId, List<PurchaseItem> lineItems) {
        this.id = id;
        this.merchantId = merchantId;
        this.items = new ArrayList<>(lineItems);
    }
}
