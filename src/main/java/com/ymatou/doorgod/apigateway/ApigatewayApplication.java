package com.ymatou.doorgod.apigateway;

import com.ymatou.doorgod.apigateway.reverseproxy.VertxVerticleDeployer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@ComponentScan("com.ymatou.doorgod.apigateway")
@ImportResource("classpath:spring/spring-extra-beans.xml")
public class ApigatewayApplication {

    private static Logger LOGGER = LoggerFactory.getLogger(ApigatewayApplication.class);

    public static void main(String[] args) {

        //指示vertx使用logback记日志
        System.setProperty("vertx.logger-delegate-factory-class-name",
                io.vertx.core.logging.SLF4JLogDelegateFactory.class.getName());
        ApplicationContext context = SpringApplication.run(ApigatewayApplication.class, args);

        try {
            context.getBean(VertxVerticleDeployer.class).deployVerticles();
        } catch ( Exception e ) {
            LOGGER.error("Failed to startup. {}", e.getMessage(), e);
            //Vertx verticles部署失败，拒绝应用启动
            System.exit(0);
        }

    }
}
