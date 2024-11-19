package demo;

import demo.io.CreateOrderRequest;
import demo.io.CreateOrderResponse;
import demo.io.GetOrderResponse;
import demo.io.ShipOrderRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(path = "/orders")
public class OrdersController {

    private final Database database;

    public OrdersController() {
        this.database = Database.INSTANCE;
    }

    @PostMapping
    public CreateOrderResponse create(
        @RequestAttribute("Merchant-ID") int merchantId,
        @RequestBody CreateOrderRequest request
    ) {
        Order order = this.database.createOrder(merchantId, request.items());
        return new CreateOrderResponse(order.id, merchantId);
    }

    @GetMapping(path = "/{orderId}")
    public GetOrderResponse getOrder(
        @RequestAttribute("Merchant-ID") int merchantId,
        @PathVariable("orderId") int orderId
    ) {
        Order order = this.database.findOrder(orderId, merchantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new GetOrderResponse(order.id, order.merchantId, order.items, order.shipment);
    }

    @PostMapping(path = "/{orderId}/ship")
    public void ship(
        @RequestAttribute("Merchant-ID") int merchantId,
        @PathVariable("orderId") int orderId,
        @RequestBody ShipOrderRequest request
    ) {
        Order order = this.database.findOrder(orderId, merchantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (order.shipment != null)  {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        order.shipment = request.method();
    }
}
