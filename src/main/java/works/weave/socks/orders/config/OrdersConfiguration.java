package works.weave.socks.orders.config;

import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrdersConfiguration {
    @Bean
    @ConditionalOnMissingBean(OrdersConfigurationProperties.class)
    public OrdersConfigurationProperties frameworkMesosConfigProperties() {
        return new OrdersConfigurationProperties();
    }

    @Bean
    public ServletRegistrationBean servletRegistrationBean(){
        return new ServletRegistrationBean(new HystrixMetricsStreamServlet(),"/hystrix.stream");
    }
}
