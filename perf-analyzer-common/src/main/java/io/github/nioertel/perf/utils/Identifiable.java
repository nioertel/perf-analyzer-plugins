package io.github.nioertel.perf.utils;

public interface Identifiable {

	long getIdentifier();

	default String getName() {
		return Long.toString(getIdentifier());
	}

}
