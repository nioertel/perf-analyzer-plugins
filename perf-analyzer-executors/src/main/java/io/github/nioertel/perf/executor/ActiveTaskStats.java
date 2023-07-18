package io.github.nioertel.perf.executor;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.ref.WeakReference;

public class ActiveTaskStats {

	private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

	public enum State {
		WAITING, RUNNING, FINISHED
	}

	private final long taskId;

	private final long enqueueDateEpochMillis;

	private final WeakReference<Runnable> taskReference;

	private State state;

	private long executingThreadId;

	private long startDateEpochMillis;

	private long startThreadCpuTimeNanos;

	private long startThreadUserTimeNanos;

	public ActiveTaskStats(long taskId, Runnable task) {
		this.taskId = taskId;
		this.taskReference = new WeakReference<>(task);
		this.enqueueDateEpochMillis = System.currentTimeMillis();
		this.state = State.WAITING;
	}

	public long getTaskId() {
		return taskId;
	}

	public long getEnqueueDateEpochMillis() {
		return enqueueDateEpochMillis;
	}

	public Runnable getTask() {
		return taskReference.get();
	}

	public State getState() {
		return state;
	}

	public long getExecutingThreadId() {
		return executingThreadId;
	}

	public long getStartDateEpochMillis() {
		return startDateEpochMillis;
	}

	public long getStartThreadCpuTimeNanos() {
		return startThreadCpuTimeNanos;
	}

	public long getStartThreadUserTimeNanos() {
		return startThreadUserTimeNanos;
	}

	public void taskStarted() {
		this.startDateEpochMillis = System.currentTimeMillis();
		this.startThreadCpuTimeNanos = THREAD_MX_BEAN.getCurrentThreadCpuTime();
		this.startThreadUserTimeNanos = THREAD_MX_BEAN.getCurrentThreadUserTime();
		this.executingThreadId = Thread.currentThread().getId();
		this.state = State.RUNNING;
	}

	public void taskFinished() {
		this.state = State.FINISHED;
	}
}
