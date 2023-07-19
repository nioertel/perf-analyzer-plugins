package io.github.nioertel.perf.utils;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public final class TaskNameExtractor {

	private static final Field F_FUTURE_TASK_CALLABLE = ClassUtils.getAccessibleField(FutureTask.class, "callable");

	private static final Field F_RUNNABLE_ADAPTER_TASK = ClassUtils.getAccessibleField("java.util.concurrent.Executors$RunnableAdapter", "task");

	private TaskNameExtractor() {
	}

	public static String extractTaskName(Runnable task) {
		String taskClassName = task.getClass().getName();
		if ("java.util.concurrent.FutureTask".equals(taskClassName)) {
			try {
				return extractTaskName((Callable<?>) F_FUTURE_TASK_CALLABLE.get(task));
			} catch (Exception e) {
				// silently fall back to class name
			}
		}
		return taskClassName;
	}

	public static String extractTaskName(Callable<?> task) {
		String taskClassName = task.getClass().getName();
		if ("java.util.concurrent.Executors$RunnableAdapter".equals(taskClassName)) {
			try {
				return extractTaskName((Runnable) F_RUNNABLE_ADAPTER_TASK.get(task));
			} catch (Exception e) {
				// silently fall back to class name
			}
		}
		return taskClassName;
	}
}
