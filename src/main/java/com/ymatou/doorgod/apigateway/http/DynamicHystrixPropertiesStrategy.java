package com.ymatou.doorgod.apigateway.http;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;
import com.ymatou.doorgod.apigateway.SpringContextHolder;
import com.ymatou.doorgod.apigateway.cache.Cache;
import com.ymatou.doorgod.apigateway.cache.HystrixConfigCache;
import com.ymatou.doorgod.apigateway.model.BlacklistRule;
import com.ymatou.doorgod.apigateway.model.HystrixConfig;
import com.ymatou.doorgod.apigateway.model.LimitTimesRule;
import com.ymatou.doorgod.apigateway.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Hystrix动态配置实现
 * Created by tuwenjie on 2016/9/23.
 */
@Component
public class DynamicHystrixPropertiesStrategy extends HystrixPropertiesStrategy implements Cache {

    @Autowired
    private HystrixConfigCache hystrixConfigCache;


    private LoadingCache<String, HystrixCommandProperties> commandKeyToProperties;



    @Override
    public HystrixCommandProperties getCommandProperties(HystrixCommandKey commandKey, HystrixCommandProperties.Setter builder) {
        if (commandKey.name().equalsIgnoreCase(Constants.HYSTRIX_COMMAND_KEY_FILTERS_EXECUTOR)) {
            return super.getCommandProperties(commandKey, builder);
        }
        return null;
    }

    @Override
    public String getCommandPropertiesCacheKey(HystrixCommandKey commandKey, HystrixCommandProperties.Setter builder) {
        if (commandKey.name().equalsIgnoreCase(Constants.HYSTRIX_COMMAND_KEY_FILTERS_EXECUTOR)) {
            return super.getCommandPropertiesCacheKey(commandKey, builder);
        } else {
            //用自己的缓存方案
            return null;
        }
    }

    public HystrixCommandProperties.Setter build(HystrixConfig config ) {
        HystrixCommandProperties.Setter setter = HystrixCommandProperties.Setter();

        setter.withExecutionTimeoutEnabled(false);

        setter.withRequestLogEnabled(false);

        setter.withExecutionIsolationSemaphoreMaxConcurrentRequests(Integer.MAX_VALUE);

        if ( config != null ) {
            if ( config.getMaxConcurrentReqs() != null && config.getMaxConcurrentReqs() > 0 ) {
                setter.withExecutionIsolationSemaphoreMaxConcurrentRequests(config.getMaxConcurrentReqs());
            }
            if (config.getForceCircuitBreakerClose() != null && config.getForceCircuitBreakerClose()) {
                setter.withCircuitBreakerForceClosed(true);
            }
            if (config.getForceCircuitBreakerOpen() != null && config.getForceCircuitBreakerOpen()) {
                setter.withCircuitBreakerForceOpen(true);
            }
            if (config.getErrorThresholdPercentageOfCircuitBreaker() != null && config.getErrorThresholdPercentageOfCircuitBreaker() > 0 ) {
                setter.withCircuitBreakerErrorThresholdPercentage(config.getErrorThresholdPercentageOfCircuitBreaker());
            }
            if (config.getTimeout() != null && config.getTimeout() > 0 ) {
                setter.withExecutionTimeoutEnabled(true);
                setter.withExecutionTimeoutInMilliseconds(config.getTimeout( ));
            }
        }

        return setter;
    }

    @Override
    public void reload() throws Exception {
        if (commandKeyToProperties == null) {
            commandKeyToProperties = CacheBuilder.newBuilder()
                    .maximumSize(Constants.MAX_CACHED_URIS)
                    .build(
                            new CacheLoader<String, HystrixCommandProperties>() {
                                public HystrixCommandProperties load(String uri) {
                                    return null;
                                }
                            });
        }
        commandKeyToProperties.invalidateAll();
    }
}