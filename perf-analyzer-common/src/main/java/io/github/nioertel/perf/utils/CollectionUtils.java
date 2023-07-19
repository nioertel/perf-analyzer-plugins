package io.github.nioertel.perf.utils;

import java.util.LinkedHashMap;

public final class CollectionUtils {

	private CollectionUtils() {
	}

	public static <K, V> LinkedHashMap<K, V> orderedMap(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
		LinkedHashMap<K, V> result = new LinkedHashMap<>();
		result.put(k1, v1);
		result.put(k2, v2);
		result.put(k3, v3);
		result.put(k4, v4);
		result.put(k5, v5);
		result.put(k6, v6);
		result.put(k7, v7);
		return result;
	}

}
