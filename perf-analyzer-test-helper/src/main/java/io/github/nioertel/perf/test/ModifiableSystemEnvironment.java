package io.github.nioertel.perf.test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ModifiableSystemEnvironment {

	private static final Map<String, String> ENV_MAP = prepare();

	/**
	 * Change the System environment (i.e. what is returned by {@link System#getenv()}).
	 * <p>
	 * Note: If this is called while another thread calls <code>System.getenv()</code> the JVM may crash.
	 * </p>
	 *
	 * @param key
	 *            The key.
	 * @param value
	 *            The value.
	 */
	public static void put(String key, String value) {
		ENV_MAP.put(key, value);
	}

	private static Map<String, String> prepare() {
		Map<String, String> envMap = new HashMap<>(System.getenv());
		try {
			Class<?> clazz = Class.forName("java.lang.ProcessEnvironment");
			Field theCaseInsensitiveEnvironmentField = clazz.getDeclaredField("theEnvironment");
			Field theUnmodifiableEnvironmentField = clazz.getDeclaredField("theUnmodifiableEnvironment");
			removeStaticFinalAndSetValue(theCaseInsensitiveEnvironmentField, envMap);
			removeStaticFinalAndSetValue(theUnmodifiableEnvironmentField, Collections.unmodifiableMap(envMap));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return envMap;
	}

	private static void removeStaticFinalAndSetValue(Field field, Object value) throws Exception {
		field.setAccessible(true);
		Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
		modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
		field.set(null, value);
	}

}
