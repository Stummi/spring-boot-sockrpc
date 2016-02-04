package org.stummi.test.ws;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RPCWebsocketHandlerDemo extends RPCWebsocketHandler<RPCWebsocketHandlerDemo.Client> {
	protected static interface Client {
		void alert(String name);
	}

	@Override
	protected void afterConnectionEstablished(Client client) {
		System.out.println("after connection established");
		client.alert("Welcome RPC Websocket DEMO");
	}

	@WSExport
	public void greet(Client client, String name) {
		log.info("greet: " + name);
		client.alert("Hello " + name);
	}

}
