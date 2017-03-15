package works.weave.socks.orders.middleware;

import io.prometheus.client.Histogram;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class HTTPMonitoringInterceptor implements HandlerInterceptor {
    static final Histogram requestLatency = Histogram.build()
            .name("request_duration_seconds")
            .help("Request duration in seconds.")
            .labelNames("service", "method", "route", "status_code")
            .register();

    private static final String startTimeKey = "startTime";

    @Value("${spring.application.name:orders}")
    private String serviceName;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse
            httpServletResponse, Object o) throws Exception {
        httpServletRequest.setAttribute(startTimeKey, System.nanoTime());
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse
            httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {
        long start = (long) httpServletRequest.getAttribute(startTimeKey);
        long elapsed = System.nanoTime() - start;
        double seconds = (double) elapsed / 1000000000.0;
        requestLatency.labels(
                serviceName,
                httpServletRequest.getMethod(),
                getMatchingURLPattern(httpServletRequest),
                Integer.toString(httpServletResponse.getStatus())
        ).observe(seconds);
    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse
            httpServletResponse, Object o, Exception e) throws Exception {
    }

    private String getMatchingURLPattern(HttpServletRequest httpServletRequest) {
        String res = httpServletRequest.getServletPath();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> item : requestMappingHandlerMapping
                .getHandlerMethods().entrySet()) {
            RequestMappingInfo mapping = item.getKey();
            if (mapping.getPatternsCondition().getMatchingCondition(httpServletRequest) != null &&
                    mapping.getMethodsCondition().getMatchingCondition(httpServletRequest) !=
                            null) {
                res = mapping.getPatternsCondition().getMatchingCondition(httpServletRequest)
                        .getPatterns().iterator().next();
                break;
            }
        }
        return res;
    }
}
