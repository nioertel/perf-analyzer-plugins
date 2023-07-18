package io.github.nioertel.perf.utils;

@FunctionalInterface
public interface ThrowingSupplier<T, E extends Throwable> {

	public T get() throws E;
}
