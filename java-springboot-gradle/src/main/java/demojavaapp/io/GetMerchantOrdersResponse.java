package demojavaapp.io;

import demojavaapp.Order;

import java.util.List;

public record GetMerchantOrdersResponse(List<Order> orders) {
}
