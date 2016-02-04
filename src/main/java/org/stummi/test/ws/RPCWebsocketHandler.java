package org.stummi.test.ws;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.core.GenericTypeResolver;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class RPCWebsocketHandler<T> extends TextWebSocketHandler {
	private final ObjectMapper mapper = new ObjectMapper();
	private final Class<T> clientIface;
	private final Map<String, MethodInvocer> invocers;

	private final Map<WebSocketSession, T> clients = new HashMap<>();

	public RPCWebsocketHandler() {
		this.clientIface = getClientInterface();
		this.invocers = new HashMap<>();
		collectExportMethods();
	}

	private void collectExportMethods() {
		ReflectionUtils.doWithMethods(getClass(), this::collectExportMethod, RPCWebsocketHandler::matchExportMethod);
	}

	private void collectExportMethod(Method m) {
		invocers.put(m.getName(), new MethodInvocer(m));
	}

	@Override
	public final void afterConnectionEstablished(WebSocketSession session) throws Exception {
		try {
			T client = WebsocketClientMethodProxy.wrap(clientIface, session);
			clients.put(session, client);
			exportMethods(session);
			afterConnectionEstablished(client);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private void exportMethods(WebSocketSession session) throws IOException {
		String list = StringUtils.collectionToCommaDelimitedString(invocers.keySet());
		String msg = "export:" + list;
		session.sendMessage(new TextMessage(msg));
	}

	private static boolean matchExportMethod(Method m) {
		return m.getAnnotation(WSExport.class) != null;
	}

	@Override
	public final void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		T client = clients.remove(session);
		afterConnectionClosed(client, status);
	}

	protected void afterConnectionEstablished(T client) throws Exception {}

	protected void afterConnectionClosed(T client, CloseStatus status) {}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		T client = clients.get(session);
		if (client == null) {
			return;
		}

		String[] spl = message.getPayload().split(":", 2);
		String command = spl[0];
		if (!command.equals("invoke") || spl.length != 2) {
			return;
		}

		MethodInvocationDTO dto = mapper.readValue(spl[1], MethodInvocationDTO.class);

		String name = dto.getMethodName();
		Object args = dto.getArgs();
		MethodInvocer invocer = invocers.get(name);

		if (name == null) {
			return;
		}

		invocer.invoke(client, args);
	}

	@SuppressWarnings("unchecked")
	protected Class<T> getClientInterface() {
		return (Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(), RPCWebsocketHandler.class);
	}

	@RequiredArgsConstructor
	private class MethodInvocer {
		private final Method method;

		public void invoke(T session, Object args) throws Exception {
			Parameter[] params = method.getParameters();
			Object[] methodArgs = new Object[params.length];

			int paramCount = (int) Stream.of(params).filter(p -> p.getType() != clientIface).count();

			if (!(args instanceof Map) && paramCount > 1) {
				log.error("cannot dispatch arguments " + args + " to method " + method);
			}

			for (int idx = 0; idx < params.length; ++idx) {
				Parameter param = params[idx];
				Object value = resolveArgument(param, session, args);
				methodArgs[idx] = value;
			}

			method.invoke(RPCWebsocketHandler.this, methodArgs);
		}

		private Object resolveArgument(Parameter param, T session, Object args) {
			if (param.getType() == clientIface) {
				return session;
			}

			Object arg;
			if (args instanceof Map) {
				arg = ((Map<?, ?>) args).get(param.getName());
			} else {
				arg = args;
			}

			if (arg == null) {
				return null;
			}

			return mapper.convertValue(arg, param.getType());
		}

	}
}
