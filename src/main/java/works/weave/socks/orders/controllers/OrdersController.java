package works.weave.socks.orders.controllers;

import com.feedzai.commons.tracing.api.TraceContext;
import com.feedzai.commons.tracing.engine.TraceUtil;
import com.spikhalskiy.futurity.Futurity;
import org.aspectj.weaver.tools.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.mvc.TypeReferences;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import works.weave.socks.orders.config.OrdersConfigurationProperties;
import works.weave.socks.orders.entities.*;
import works.weave.socks.orders.repositories.CustomerOrderRepository;
import works.weave.socks.orders.resources.NewOrderResource;
import works.weave.socks.orders.services.AsyncGetService;
import works.weave.socks.orders.values.PaymentRequest;
import works.weave.socks.orders.values.PaymentResponse;

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@RepositoryRestController
public class OrdersController {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private OrdersConfigurationProperties config;

    @Autowired
    private AsyncGetService asyncGetService;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Value(value = "${http.timeout:5}")
    private long timeout;


    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(path = "/orders", consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    public
    @ResponseBody
    CustomerOrder newOrder(@RequestBody NewOrderResource item, @RequestHeader HttpHeaders headers) {
        return TraceUtil.instance().newProcess(() -> {
            try {
                if (item.address == null || item.customer == null || item.card == null || item.items == null) {
                    throw new InvalidOrderException("Invalid order request. Order requires customer, address, card and items.");
                }
                TraceUtil.instance().mapEventId(item.id.toString(), TraceUtil.instance().currentSpan());
                LOG.debug("Starting calls");
                CompletableFuture<Resource<Address>> addressFuture = TraceUtil.instance().addToTraceAsync(() -> asyncGetService.getResource(item.address, new TypeReferences.ResourceType<Address>() {
                }), "Get Address", item.id.toString());
                CompletableFuture<Resource<Customer>> customerFuture = TraceUtil.instance().addToTraceAsync(() -> asyncGetService.getResource(item.customer, new TypeReferences.ResourceType<Customer>() {
                }), "Get Customer", item.id.toString());
                CompletableFuture<Resource<Card>> cardFuture = TraceUtil.instance().addToTraceAsync(() -> asyncGetService.getResource(item.card, new TypeReferences.ResourceType<Card>() {
                }), "Get Card", item.id.toString());
                CompletableFuture<List<Item>> itemsFuture = TraceUtil.instance().addToTraceAsync(() -> asyncGetService.getDataList(item.items, new ParameterizedTypeReference<List<Item>>() {
                }), "Get Items", item.id.toString());
                LOG.debug("End of calls.");


                float amount = calculateTotal(itemsFuture.get(timeout, TimeUnit.SECONDS));

                // Call payment service to make sure they've paid
                PaymentRequest paymentRequest = new PaymentRequest(
                        addressFuture.get(timeout, TimeUnit.SECONDS).getContent(),
                        cardFuture.get(timeout, TimeUnit.SECONDS).getContent(),
                        customerFuture.get(timeout, TimeUnit.SECONDS).getContent(),
                        amount);
                LOG.info("Sending payment request: " + paymentRequest);
                CompletableFuture<PaymentResponse> paymentFuture = TraceUtil.instance().addToTraceAsync(() -> asyncGetService.postResource(
                        config.getPaymentUri(),
                        paymentRequest,
                        new ParameterizedTypeReference<PaymentResponse>() {
                        }), "Payment", item.id.toString());
                PaymentResponse paymentResponse = paymentFuture.get(timeout, TimeUnit.SECONDS);
                LOG.info("Received payment response: " + paymentResponse);
                if (paymentResponse == null) {
                    throw new PaymentDeclinedException("Unable to parse authorisation packet");
                }
                if (!paymentResponse.isAuthorised()) {
                    throw new PaymentDeclinedException(paymentResponse.getMessage());
                }

                // Ship
                String customerId = parseId(customerFuture.get(timeout, TimeUnit.SECONDS).getId().getHref());
                CompletableFuture<Shipment> shipmentFuture = TraceUtil.instance().addToTraceAsync(() -> asyncGetService.postResource(config.getShippingUri(), new Shipment
                        (customerId), new ParameterizedTypeReference<Shipment>() {
                }), "Shipping", item.id.toString());

                CustomerOrder order = new CustomerOrder(
                        null,
                        customerId,
                        customerFuture.get(timeout, TimeUnit.SECONDS).getContent(),
                        addressFuture.get(timeout, TimeUnit.SECONDS).getContent(),
                        cardFuture.get(timeout, TimeUnit.SECONDS).getContent(),
                        itemsFuture.get(timeout, TimeUnit.SECONDS),
                        shipmentFuture.get(timeout, TimeUnit.SECONDS),
                        Calendar.getInstance().getTime(),
                        amount);
                LOG.debug("Received data: " + order.toString());

                CustomerOrder savedOrder = TraceUtil.instance().addToTrace(() -> customerOrderRepository.save(order), "Save order", item.id.toString());
                LOG.debug("Saved order: " + savedOrder);

                return savedOrder;
            } catch (TimeoutException e) {
                throw new IllegalStateException("Unable to create order due to timeout from one of the services.", e);
            } catch (InterruptedException | ExecutionException e) {
                throw new IllegalStateException("Unable to create order due to unspecified IO error.", e);
            }
        }, "New Order", TraceUtil.instance().deserializeContext((Serializable) headers.toSingleValueMap()));
    }

    private String parseId(String href) {
        Pattern idPattern = Pattern.compile("[\\w-]+$");
        Matcher matcher = idPattern.matcher(href);
        if (!matcher.find()) {
            throw new IllegalStateException("Could not parse user ID from: " + href);
        }
        return matcher.group(0);
    }

//    TODO: Add link to shipping
//    @RequestMapping(method = RequestMethod.GET, value = "/orders")
//    public @ResponseBody
//    ResponseEntity<?> getOrders() {
//        List<CustomerOrder> customerOrders = customerOrderRepository.findAll();
//
//        Resources<CustomerOrder> resources = new Resources<>(customerOrders);
//
//        resources.forEach(r -> r);
//
//        resources.add(linkTo(methodOn(ShippingController.class, CustomerOrder.getShipment::ge)).withSelfRel());
//
//        // add other links as needed
//
//        return ResponseEntity.ok(resources);
//    }

    private float calculateTotal(List<Item> items) {
        float amount = 0F;
        float shipping = 4.99F;
        amount += items.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum();
        amount += shipping;
        return amount;
    }

    @ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE)
    public class PaymentDeclinedException extends IllegalStateException {
        public PaymentDeclinedException(String s) {
            super(s);
        }
    }

    @ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE)
    public class InvalidOrderException extends IllegalStateException {
        public InvalidOrderException(String s) {
            super(s);
        }
    }
}
