package com.ymatou.doorgod.apigateway.reverseproxy.hystrix;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import com.ymatou.doorgod.apigateway.SpringContextHolder;
import com.ymatou.doorgod.apigateway.integration.KafkaClient;
import com.ymatou.doorgod.apigateway.model.RejectReqEvent;
import com.ymatou.doorgod.apigateway.model.Sample;
import com.ymatou.doorgod.apigateway.reverseproxy.HttpServerRequestHandler;
import com.ymatou.doorgod.apigateway.reverseproxy.HttpServerVerticle;
import com.ymatou.doorgod.apigateway.utils.Utils;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;

/**
 * Created by tuwenjie on 2016/9/6.
 */
public class HystrixForwardReqCommand extends HystrixObservableCommand<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

    private HttpServerRequest httpServerReq;

    private HttpClient httpClient;

    private Vertx vertx;

    public HystrixForwardReqCommand(HttpClient httpClient, HttpServerRequest httpServerReq,
                                    Vertx vertx) {
        /**
         * command Hystrix属性通过{@link DynamicHystrixPropertiesStrategy}加载
         */
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("api.ymatou"))
            .andCommandKey(MyHystrixCommandKeyFactory.asKey(httpServerReq.path())));
        this.httpClient = httpClient;
        this.httpServerReq = httpServerReq;
        this.vertx = vertx;
    }

    @Override
    protected Observable<Void> construct() {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                try {
                    if (!subscriber.isUnsubscribed()) {
                        HttpServerRequestHandler handler = new HttpServerRequestHandler(subscriber, httpClient, vertx);
                        handler.handle(httpServerReq);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to transfer reverseproxy req {}:{}", httpServerReq.method(), httpServerReq.uri(), e);
                    subscriber.onError(e);
                }
            }
        } );
    }

    @Override
    protected Observable<Void> resumeWithFallback() {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                try {
                    if (Boolean.TRUE.equals(HystrixForwardReqCommand.this.getProperties().circuitBreakerForceOpen())
                        && HystrixForwardReqCommand.this.isCircuitBreakerOpen()) {
                        //对于断路器被配置为强制打开的uri,无需logger.error
                        LOGGER.warn("Circuit Breaker open for uri", httpServerReq.path());
                    } else {
                        LOGGER.error("Request is rejected by Hystrix:{}. circuitBreaker rejected:{}. maxConcurrent rejected:{}",
                                HystrixForwardReqCommand.this.httpServerReq.path(),
                                HystrixForwardReqCommand.this.isResponseShortCircuited(),
                                HystrixForwardReqCommand.this.isResponseSemaphoreRejected());

                        RejectReqEvent event = new RejectReqEvent();
                        event.setUri(HystrixForwardReqCommand.this.httpServerReq.path());
                        event.setTime(Utils.getCurrentTime());
                        event.setSample(new Sample());
                        event.setRuleName(HystrixForwardReqCommand.this.isResponseSemaphoreRejected() ? "MaxConcurrent" : "CircuitBreaker");
                        event.setFilterName("Hystrix");

                        //被拦截请求发到决策引擎进行落地
                        SpringContextHolder.getBean(KafkaClient.class).sendRejectReqEvent(event);
                    }
                    if (!subscriber.isUnsubscribed()) {
                        HttpServerRequestHandler.fallback(httpServerReq,
                                //对外统一为被断路器拦截
                                "Refused by CircuitBreaker");
                        subscriber.onCompleted();
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to do fallback process for req {}:{}", httpServerReq.method(), httpServerReq.path(), e);
                    subscriber.onError(e);
                }
            }
        } );
    }

}