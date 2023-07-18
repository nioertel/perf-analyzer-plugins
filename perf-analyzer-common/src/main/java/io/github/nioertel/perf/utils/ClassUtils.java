package io.github.nioertel.perf.utils;

import java.lang.reflect.Method;

public final class ClassUtils {

	private ClassUtils() {
	}

	// public abstract java.sql.Statement java.sql.Connection.createStatement() throws java.sql.SQLException[]
	public static String getShortMethodIdentifier(Method m) {
		StringBuilder id = new StringBuilder()//
				.append(m.getDeclaringClass().getName())//
				.append('.')//
				.append(m.getName())//
				.append('(');
		Class<?>[] paramTypes = m.getParameterTypes();
		for (int i = 0; i < paramTypes.length; ++i) {
			if (i > 0) {
				id.append(", ");
			}
			id.append(paramTypes[i].getName());
		}
		return id.append(')')//
				.toString();
	}

}
