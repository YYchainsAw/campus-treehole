package xyz.yychainsaw.campus_treehole.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration(proxyBeanMethods = false)
@Profile("local")
class LocalWebConfiguration {
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> localOnlyWebServer() throws UnknownHostException {
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        return factory -> factory.setAddress(loopback);
    }
}
