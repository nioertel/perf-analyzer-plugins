package io.github.nioertel.perf.jdbc.tracker;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Two things:
 * <ul>
 * <li>Remove connection stats that have no connection assigned (i.e. week reference is empty)</li>
 * <li>Remove connection stats for which there is another connection stat that references the same physical connection
 * and has a later acquired timestamp</li>
 * <li>Remove all connections if the tracker has a size > 10k (and log some relevant details to identify what
 * happened)</li>
 * </ul>
 */
public class JdbcConnectionMetricsHousekeeper implements AutoCloseable {

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setName("jdbc-cm-housekeeper");
			return t;
		}

	});

	public void start(JdbcConnectionMetricsTracker metricsTracker, long delay, TimeUnit unit) {
		executor.schedule(metricsTracker::cleanup, delay, unit);
	}

	@Override
	public void close() throws Exception {
		// just shut down and do not care whether house keeping is still doing something or not
		executor.shutdownNow();
	}

}
