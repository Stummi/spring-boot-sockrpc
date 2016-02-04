package org.stummi.test;

import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.PerConnectionWebSocketHandler;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.stummi.test.ws.RPCWebsocketHandlerDemo;

@Configuration
public class LiveWebSocketConfigurer extends SpringBootServletInitializer implements WebSocketConfigurer {

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		PerConnectionWebSocketHandler handler = new PerConnectionWebSocketHandler(RPCWebsocketHandlerDemo.class);
		registry.addHandler(handler, "/ws/test").withSockJS().setClientLibraryUrl("/lib/sockjs-client/dist/sockjs.min.js");
	}

	@Bean
	public ServerEndpointExporter serverEndpointExporter() {
		return new ServerEndpointExporter();
	}

}
