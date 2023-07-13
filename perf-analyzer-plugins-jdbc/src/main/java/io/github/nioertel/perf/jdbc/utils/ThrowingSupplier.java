package io.github.nioertel.perf.jdbc.utils;

@FunctionalInterface
public interface ThrowingSupplier<T, E extends Throwable> {

	public T get() throws E;
}
