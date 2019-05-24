package works.weave.socks.orders.resources;

import org.hibernate.validator.constraints.URL;

import java.net.URI;
import java.util.UUID;

public class NewOrderResource {
    @URL
    public URI customer;
    @URL
    public URI address;
    @URL
    public URI card;
    @URL
    public URI items;
    public UUID id = UUID.randomUUID();
}
