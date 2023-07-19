package io.github.nioertel.perf.executor.spring;

import java.lang.ref.WeakReference;
import java.util.Map;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.github.nioertel.perf.executor.ExecutorStateInfoExtractor;
import io.github.nioertel.perf.utils.CollectionUtils;

public class ThreadPoolTaskExecutorStateInfoExtractor implements ExecutorStateInfoExtractor {

	private final String name;

	private final WeakReference<ThreadPoolTaskExecutor> threadPoolTaskExecutor;

	public ThreadPoolTaskExecutorStateInfoExtractor(String name, ThreadPoolTaskExecutor threadPoolTaskExecutor) {
		this.name = name;
		this.threadPoolTaskExecutor = new WeakReference<>(threadPoolTaskExecutor);
	}

	@Override
	public Map<String, Object> getExecutorStateInfo() {
		ThreadPoolTaskExecutor theRealOne = threadPoolTaskExecutor.get();
		if (null == theRealOne) {
			return null;
		} else {
			return CollectionUtils.orderedMap(//
					"threadNamePrefix", theRealOne.getThreadNamePrefix(), //
					"corePoolSize", theRealOne.getCorePoolSize(), //
					"poolSize", theRealOne.getPoolSize(), //
					"queueCapacity", theRealOne.getQueueCapacity(), //
					"queueSize", theRealOne.getQueueSize(), //
					"keepAliveSeconds", theRealOne.getKeepAliveSeconds(), //
					"activeCount", theRealOne.getActiveCount() //
			);
		}
	}

	@Override
	public String getName() {
		return name;
	}

}
