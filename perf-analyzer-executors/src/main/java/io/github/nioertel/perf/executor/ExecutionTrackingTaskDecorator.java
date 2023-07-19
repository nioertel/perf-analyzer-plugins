package io.github.nioertel.perf.executor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.nioertel.perf.utils.TaskDecorator;

public class ExecutionTrackingTaskDecorator implements TaskDecorator, ExecutionMetrics {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionTrackingTaskDecorator.class);

	private final AtomicLong taskIdGenerator = new AtomicLong();

	private final String name;

	private final Map<Long, ActiveTaskStats> activeTasks = new ConcurrentHashMap<>();

	private class TrackingExecutionWrapper implements Runnable {

		private final long taskId;

		private final Runnable delegate;

		private final ActiveTaskStats taskState;

		public TrackingExecutionWrapper(long taskId, Runnable delegate) {
			this.taskId = taskId;
			this.delegate = delegate;
			this.taskState = new ActiveTaskStats(taskId, delegate);
			activeTasks.put(taskId, taskState);
		}

		@Override
		public void run() {
			boolean success = true;
			try {
				taskState.taskStarted();
				delegate.run();
			} catch (Throwable t) {
				success = false;
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Task {} ({}) finished with error.", taskId, taskState.getTaskName(), t);
				}
				throw t;
			} finally {
				if (success && LOGGER.isDebugEnabled()) {
					LOGGER.debug("Task {} ({}) finished successfully.", taskId, taskState.getTaskName());
				}
				taskState.taskFinished();
				activeTasks.remove(taskId);
			}
		}

	}

	public ExecutionTrackingTaskDecorator(String name) {
		this.name = name;
	}

	@Override
	public Runnable decorate(Runnable runnable) {
		return new TrackingExecutionWrapper(taskIdGenerator.incrementAndGet(), runnable);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<ActiveTaskSnapshot> getActiveTasks() {
		return activeTasks.values().stream().map(ActiveTaskSnapshot::fromActiveTaskStats).collect(Collectors.toUnmodifiableList());
	}

}
