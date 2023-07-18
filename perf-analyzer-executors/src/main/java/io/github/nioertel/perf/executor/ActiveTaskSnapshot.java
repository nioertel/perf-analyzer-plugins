package io.github.nioertel.perf.executor;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;

import io.github.nioertel.perf.executor.ActiveTaskStats.State;
import io.github.nioertel.perf.utils.DateHelper;

public class ActiveTaskSnapshot {

	private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

	private final long taskId;

	private final long executingThreadId;

	private final State state;

	private final LocalDateTime enqueueDate;

	private final LocalDateTime startDate;

	private final long waitTimeUntilStartMillis;

	private final long activeSinceStartMillis;

	private final long consumedCpuTimeMillis;

	private final long consumedUserTimeMillis;

	public ActiveTaskSnapshot(long taskId, long executingThreadId, State state, LocalDateTime enqueueDate, LocalDateTime startDate,
			long waitTimeUntilStartMillis, long activeSinceStartMillis, long consumedCpuTimeMillis, long consumedUserTimeMillis) {
		this.taskId = taskId;
		this.executingThreadId = executingThreadId;
		this.state = state;
		this.enqueueDate = enqueueDate;
		this.startDate = startDate;
		this.waitTimeUntilStartMillis = waitTimeUntilStartMillis;
		this.activeSinceStartMillis = activeSinceStartMillis;
		this.consumedCpuTimeMillis = consumedCpuTimeMillis;
		this.consumedUserTimeMillis = consumedUserTimeMillis;
	}

	public static ActiveTaskSnapshot fromActiveTaskStats(ActiveTaskStats stats) {
		long executingThreadId = stats.getExecutingThreadId();
		long currentUserTimeNanos = THREAD_MX_BEAN.getThreadUserTime(executingThreadId);
		long currentCpuTimeNanos = THREAD_MX_BEAN.getThreadCpuTime(executingThreadId);
		boolean isRunning = State.RUNNING == stats.getState();

		return new ActiveTaskSnapshot(//
				stats.getTaskId(), // taskId
				stats.getExecutingThreadId(), // executingThreadId
				stats.getState(), // state
				DateHelper.toLocalDateTime(stats.getEnqueueDateEpochMillis()), // enqueueDate
				!isRunning ? null : DateHelper.toLocalDateTime(stats.getStartDateEpochMillis()), // startDate
				!isRunning ? -1 : stats.getStartDateEpochMillis() - stats.getEnqueueDateEpochMillis(), // waitTimeUntilStartMillis
				!isRunning ? -1 : System.currentTimeMillis() - stats.getStartDateEpochMillis(), // activeSinceStartMillis
				!isRunning ? 0 : (currentCpuTimeNanos - stats.getStartThreadCpuTimeNanos()) / 1_000_000L, // consumedCpuTimeNanos
				!isRunning ? 0 : (currentUserTimeNanos - stats.getStartThreadUserTimeNanos()) / 1_000_000L // consumedUserTimeNanos
		);
	}

	public long getTaskId() {
		return taskId;
	}

	public long getExecutingThreadId() {
		return executingThreadId;
	}

	public State getState() {
		return state;
	}

	public LocalDateTime getEnqueueDate() {
		return enqueueDate;
	}

	public LocalDateTime getStartDate() {
		return startDate;
	}

	public long getWaitTimeUntilStartMillis() {
		return waitTimeUntilStartMillis;
	}

	public long getActiveSinceStartMillis() {
		return activeSinceStartMillis;
	}

	public long getConsumedCpuTimeMillis() {
		return consumedCpuTimeMillis;
	}

	public long getConsumedUserTimeMillis() {
		return consumedUserTimeMillis;
	}

}
