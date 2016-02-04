package org.stummi.test.ws;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WebsocketClientMethodProxy implements InvocationHandler {

	private final WebSocketSession session;
	private final ObjectMapper mapper = new ObjectMapper();

	@SuppressWarnings("unchecked")
	public static <T> T wrap(Class<T> t, WebSocketSession session) {
		ClassLoader cls = WebsocketClientMethodProxy.class.getClassLoader();
		if (cls == null) {
			cls = ClassLoader.getSystemClassLoader();
		}
		return (T) Proxy.newProxyInstance(cls, new Class<?>[] { t }, new WebsocketClientMethodProxy(session));
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getDeclaringClass() == Object.class) {
			return method.invoke(this, args);
		}

		MethodInvocationDTO dto = new MethodInvocationDTO();
		dto.setMethodName(method.getName());

		Parameter[] params = method.getParameters();
		if (params.length == 1) {
			dto.setArgs(args[0]);
		} else {
			Map<String, Object> argMap = new HashMap<>();
			for (int idx = 0; idx < params.length; ++idx) {
				argMap.put(params[idx].getName(), args[idx]);
			}
		}

		String str = mapper.writeValueAsString(dto);
		String message = "invoke:" + str;
		session.sendMessage(new TextMessage(message));
		return null;
	}
}
