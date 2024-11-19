package demo;

import demo.io.PurchaseItem;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Database {

    public static final Database INSTANCE = new Database();

    private final AtomicInteger sequence = new AtomicInteger();
    private final List<Merchant> merchants = new ArrayList<>();
    private final List<Order> orders = new ArrayList<>();


    public Merchant createMerchant(String name) {
        Merchant merchant = new Merchant(sequence.incrementAndGet(), name, UUID.randomUUID().toString());
        this.merchants.add(merchant);
        return merchant;
    }

    public Optional<Merchant> findMerchantByID(int id) {
        return this.merchants.stream()
            .filter((merchant) -> merchant.id() == id)
            .findFirst();
    }

    public Optional<Merchant> findMerchantByAuthToken(String authToken) {
        return this.merchants.stream()
            .filter((merchant) -> merchant.authToken().equals(authToken))
            .findFirst();
    }

    public Order createOrder(int merchantId, List<PurchaseItem> lineItems) {
        Order order = new Order(sequence.incrementAndGet(), merchantId, lineItems);
        orders.add(order);
        return order;
    }

    public Optional<Order> findOrder(int id) {
        return this.orders.stream()
            .filter((order) -> order.id == id)
            .findFirst();
    }

    public Optional<Order> findOrder(int id, long merchantId) {
        return this.orders.stream()
            .filter((order) -> order.id == id && order.merchantId == merchantId)
            .findFirst();
    }

    public List<Order> findMerchantOrders(int merchantId) {
        return this.orders.stream()
            .filter((order) -> order.merchantId == merchantId)
            .toList();
    }
}
