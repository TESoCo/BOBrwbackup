package com.example.servicioWeb;

import com.example.servicioWeb.BOBWS;
import jakarta.xml.ws.Endpoint;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.transport.http.MessageDispatcherServlet;


@Configuration
@EnableWs
@EnableWebMvc
public class WebServiceConfig {

    private int serverPort;

    @EventListener
    public void onApplicationEvent(WebServerInitializedEvent event) {
        this.serverPort = event.getWebServer().getPort();
    }

    @Bean
    public Endpoint endpoint() {
        String url = "http://localhost:" + serverPort + "/BOBWS";
        return Endpoint.publish(url, new BOBWS());
    }


}

