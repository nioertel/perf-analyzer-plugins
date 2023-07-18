package io.github.nioertel.perf.executor.spring;

import java.util.List;

import org.springframework.core.task.TaskDecorator;

import io.github.nioertel.perf.executor.ActiveTaskSnapshot;
import io.github.nioertel.perf.executor.ExecutionMetrics;
import io.github.nioertel.perf.executor.ExecutionTrackingTaskDecorator;

public class ExecutionTrackingTaskDecoratorSpring implements TaskDecorator, ExecutionMetrics {

	private final ExecutionTrackingTaskDecorator delegate;

	public ExecutionTrackingTaskDecoratorSpring(String name) {
		this.delegate = new ExecutionTrackingTaskDecorator(name);
	}

	@Override
	public Runnable decorate(Runnable runnable) {
		return delegate.decorate(runnable);
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public List<ActiveTaskSnapshot> getActiveTasks() {
		return delegate.getActiveTasks();
	}

}
