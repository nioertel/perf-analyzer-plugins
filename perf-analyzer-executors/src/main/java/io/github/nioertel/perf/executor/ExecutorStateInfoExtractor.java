package io.github.nioertel.perf.executor;

import java.util.Map;

public interface ExecutorStateInfoExtractor {

	String getName();

	Map<String, Object> getExecutorStateInfo();

}
