package demo.io;

import demo.Order;

import java.util.List;

public record GetMerchantOrdersResponse(List<Order> orders) {
}
