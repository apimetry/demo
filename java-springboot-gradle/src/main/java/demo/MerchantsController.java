package demo;

import demo.io.CreateMerchantRequest;
import demo.io.CreateMerchantResponse;
import demo.io.GetMerchantOrdersResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/merchants")
public class MerchantsController {

    private final Database database;

    public MerchantsController() {
        this.database = Database.INSTANCE;
    }

    @PostMapping
    @NoAuth
    public CreateMerchantResponse create(@RequestBody CreateMerchantRequest request) {
        Merchant merchant = this.database.createMerchant(request.name());
        return new CreateMerchantResponse(merchant.id(), merchant.authToken(), merchant.name());
    }

    @GetMapping(path = "/orders")
    public GetMerchantOrdersResponse getOrders(@RequestAttribute("Merchant-ID") int merchantId) {
        return new GetMerchantOrdersResponse(this.database.findMerchantOrders(merchantId));
    }
}
