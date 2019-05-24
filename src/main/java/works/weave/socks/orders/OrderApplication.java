package works.weave.socks.orders;

import com.feedzai.commons.tracing.engine.JaegerTracingEngine;
import com.feedzai.commons.tracing.engine.TraceUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Duration;

@SpringBootApplication
@EnableAsync
public class OrderApplication {

    public static void main(String[] args) {
        TraceUtil.init(new JaegerTracingEngine.Builder().withSampleRate(1).withCacheMaxSize(10000).withCacheDuration(Duration.ofDays(2)).withProcessName("Order Service").withIp("172.31.0.10").build());
        SpringApplication.run(OrderApplication.class, args);
    }
}
