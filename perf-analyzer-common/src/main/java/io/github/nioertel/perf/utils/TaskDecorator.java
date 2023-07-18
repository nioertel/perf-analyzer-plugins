package io.github.nioertel.perf.utils;

@FunctionalInterface
public interface TaskDecorator {

	Runnable decorate(Runnable runnable);

}