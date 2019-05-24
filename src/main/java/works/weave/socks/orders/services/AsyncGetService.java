package works.weave.socks.orders.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feedzai.commons.tracing.engine.TraceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.hateoas.mvc.TypeReferences;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import works.weave.socks.orders.config.RestProxyTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.springframework.hateoas.MediaTypes.HAL_JSON;

@Service
public class AsyncGetService {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private final RestProxyTemplate restProxyTemplate;

    private final RestTemplate halTemplate;

    @Autowired
    public AsyncGetService(RestProxyTemplate restProxyTemplate) {
        this.restProxyTemplate = restProxyTemplate;
        this.halTemplate = new RestTemplate(restProxyTemplate.getRestTemplate().getRequestFactory());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new Jackson2HalModule());
        MappingJackson2HttpMessageConverter halConverter = new MappingJackson2HttpMessageConverter();
        halConverter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON));
        halConverter.setObjectMapper(objectMapper);
        halTemplate.setMessageConverters(Collections.singletonList(halConverter));
    }


    public <T> CompletableFuture<Resource<T>> getResource(URI url, TypeReferences.ResourceType<T> type) {
        RequestEntity request = getRequestEntity(url, HAL_JSON);
        LOG.debug("Requesting: " + request.toString());
        CompletableFuture body = CompletableFuture.supplyAsync(() -> restProxyTemplate.getRestTemplate().exchange(request, type).getBody());
        LOG.debug("Received: " + body.toString());
        return body;
    }

    private RequestEntity getRequestEntity(URI url, MediaType type) {
        RequestEntity.HeadersBuilder<?> builder = RequestEntity.get(url).accept(type);
        ((Map<String, String>) TraceUtil.instance().serializeContext()).forEach((k, v) -> builder.header(k, v));
        return builder.build();
    }

    public <T> CompletableFuture<Resources<T>> getDataList(URI url, TypeReferences.ResourcesType<T> type) {
        RequestEntity<Void> request = getRequestEntity(url, HAL_JSON);
        LOG.debug("Requesting: " + request.toString());
        CompletableFuture<Resources<T>> body = CompletableFuture.supplyAsync(() -> restProxyTemplate.getRestTemplate().exchange(request, type).getBody());
        LOG.debug("Received: " + body.toString());
        return body;
    }

    public <T> CompletableFuture<List<T>> getDataList(URI url, ParameterizedTypeReference<List<T>> type) {
        RequestEntity<Void> request = getRequestEntity(url, MediaType.APPLICATION_JSON);
        LOG.debug("Requesting: " + request.toString());
        CompletableFuture<List<T>> body = CompletableFuture.supplyAsync(() -> restProxyTemplate.getRestTemplate().exchange(request, type).getBody());
        LOG.debug("Received: " + body.toString());
        return body;
    }


    public <T, B> CompletableFuture<T> postResource(URI uri, B body, ParameterizedTypeReference<T> returnType) {
        RequestEntity.BodyBuilder builder = RequestEntity.post(uri).contentType(MediaType.APPLICATION_JSON).accept(MediaType
                .APPLICATION_JSON);
        ((Map<String, String>) TraceUtil.instance().serializeContext()).forEach((k, v) -> builder.header(k, v));
        RequestEntity<B> request = builder.body(body);
        LOG.debug("Requesting: " + request.toString());
        CompletableFuture<T> responseBody = CompletableFuture.supplyAsync(() -> restProxyTemplate.getRestTemplate().exchange(request, returnType).getBody());
        LOG.debug("Received: " + responseBody);
        return responseBody;
    }


}
