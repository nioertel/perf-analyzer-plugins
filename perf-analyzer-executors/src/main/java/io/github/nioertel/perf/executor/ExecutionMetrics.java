package io.github.nioertel.perf.executor;

import java.util.List;

public interface ExecutionMetrics {

	String getName();

	List<ActiveTaskSnapshot> getActiveTasks();
}
