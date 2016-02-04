package org.stummi.test.ws;

import lombok.Data;

@Data
public class MethodInvocationDTO {
	private String methodName;
	private Object args;
}
