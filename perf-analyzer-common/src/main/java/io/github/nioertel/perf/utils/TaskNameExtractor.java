package io.github.nioertel.perf.utils;

import java.lang.reflect.Field;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.function.Function;

public final class TaskNameExtractor {

	private static final Field F_FUTURE_TASK_CALLABLE = ClassUtils.getAccessibleFieldIfPossible(//
			FutureTask.class, //
			"callable");

	private static final Field F_RUNNABLE_ADAPTER_TASK = ClassUtils.getAccessibleFieldIfPossible(//
			"java.util.concurrent.Executors$RunnableAdapter", //
			"task");

	private static final Field F_PRIV_ACTION_CALLABLE_ACTION = ClassUtils.getAccessibleFieldIfPossible(//
			"java.util.concurrent.Executors$1", //
			"val$action");

	private static final Field F_PRIV_EX_ACTION_CALLABLE_ACTION = ClassUtils.getAccessibleFieldIfPossible(//
			"java.util.concurrent.Executors$2", //
			"val$action");

	private static final Field F_TRACE_RUNNABLE_DELEGATE = ClassUtils.getAccessibleFieldIfPossible(//
			"org.springframework.cloud.sleuth.instrument.async.TraceRunnable", //
			"delegate");

	private static final Field F_DELEG_SEC_CONTEXT_RUNNABLE_DELEGATE = ClassUtils.getAccessibleFieldIfPossible(//
			"org.springframework.security.concurrent.DelegatingSecurityContextRunnable", //
			"delegate");

	// TODO: Support The Futures created by Spring's @Async
	// java.util.concurrent.CompletableFuture$AsyncSupply.run

	private TaskNameExtractor() {
	}

	@SuppressWarnings("unchecked")
	private static <T> String extractTaskName(Object taskWrapper, Field field, Function<T, String> downstreamExtractor) {
		if (null != field) {
			try {
				return downstreamExtractor.apply((T) field.get(taskWrapper));
			} catch (Exception e) {
				// silently fall back to class name
			}
		}
		// can't further extract -> end here
		return taskWrapper.getClass().getName();
	}

	public static String extractTaskName(Runnable task) {
		String taskClassName = task.getClass().getName();
		switch (taskClassName) {
		case "java.util.concurrent.FutureTask":
			return extractTaskName(task, F_FUTURE_TASK_CALLABLE, (Callable<?> delegate) -> extractTaskName(delegate));
		case "org.springframework.cloud.sleuth.instrument.async.TraceRunnable":
			return extractTaskName(task, F_TRACE_RUNNABLE_DELEGATE, (Runnable delegate) -> extractTaskName(delegate));
		case "org.springframework.security.concurrent.DelegatingSecurityContextRunnable":
			return extractTaskName(task, F_DELEG_SEC_CONTEXT_RUNNABLE_DELEGATE, (Runnable delegate) -> extractTaskName(delegate));
		default:
			return taskClassName;
		}
	}

	public static String extractTaskName(Callable<?> task) {
		String taskClassName = task.getClass().getName();
		switch (taskClassName) {
		case "java.util.concurrent.Executors$RunnableAdapter":
			return extractTaskName(task, F_RUNNABLE_ADAPTER_TASK, (Runnable delegate) -> extractTaskName(delegate));
		case "java.util.concurrent.Executors$1":
			return extractTaskName(task, F_PRIV_ACTION_CALLABLE_ACTION, (PrivilegedAction<?> delegate) -> delegate.getClass().getName());
		case "java.util.concurrent.Executors$2":
			return extractTaskName(task, F_PRIV_EX_ACTION_CALLABLE_ACTION, (PrivilegedExceptionAction<?> delegate) -> delegate.getClass().getName());
		default:
			return taskClassName;
		}
	}
}
