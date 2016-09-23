package works.weave.socks.orders.controllers;

import io.prometheus.client.exporter.common.TextFormat;
import org.springframework.boot.actuate.endpoint.mvc.AbstractEndpointMvcAdapter;
import org.springframework.boot.actuate.endpoint.mvc.HypermediaDisabled;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;

public class PrometheusMvcEndpoint extends AbstractEndpointMvcAdapter<PrometheusEndpoint> {

    public PrometheusMvcEndpoint(PrometheusEndpoint delegate) {
        super(delegate);
    }

    @RequestMapping(method = RequestMethod.GET, produces = TextFormat.CONTENT_TYPE_004)
    @ResponseBody
    @HypermediaDisabled
    protected Object invoke() {
        if (!getDelegate().isEnabled()) {
            return new ResponseEntity<>(
                    Collections.singletonMap("message", "This endpoint is disabled"),
                    HttpStatus.NOT_FOUND);
        }
        return super.invoke();
    }
}
