package io.github.nioertel.perf.jdbc.tracker;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.nioertel.perf.jdbc.utils.ClassUtils;
import io.github.nioertel.perf.jdbc.utils.Identifiable;

class JdbcConnectionInvocationHandler implements InvocationHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(JdbcConnectionInvocationHandler.class);

	private static final Map<Method, String> METHOD_IDENTIFIER_CACHE = new ConcurrentHashMap<>();

	private final JdbcConnectionMetricsTracker jdbcConnectionMetrics;

	private final Connection delegate;

	private final long connectionId;

	public JdbcConnectionInvocationHandler(JdbcConnectionMetricsTracker jdbcConnectionMetrics, Connection delegate, long connectionId) {
		this.jdbcConnectionMetrics = jdbcConnectionMetrics;
		this.delegate = delegate;
		this.connectionId = connectionId;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getDeclaringClass() == Identifiable.class) {
			if ("getIdentifier".equals(method.getName())) {
				return connectionId;
			}
		}

		updateMetricsBeforeInvocation(proxy, method, args);
		try {
			Object result = method.invoke(delegate, args);
			updateMetricsAfterInvocation(proxy, method, args, result);
			return result;
		} catch (Throwable e) {
			updateMetricsAfterFailedInvocation(proxy, method, args, e);
			throw e;
		}
	}

	// TODO: release on close + do something with final metrics
	public void updateMetricsBeforeInvocation(Object proxy, Method method, Object[] args) {
		String methodIdentifier = getMethodIdentifier(method);
		if (LOGGER.isDebugEnabled()) {
			if (null == args) {
				LOGGER.debug("Invoking [{}].", methodIdentifier);
			} else {
				LOGGER.debug("Invoking [{}] w/ args {}.", methodIdentifier, List.of(args));
			}
		}
		if ("close".equals(method.getName())) {
			jdbcConnectionMetrics.connectionReleaseStart((IdentifiableConnection) proxy);
		}
	}

	public void updateMetricsAfterInvocation(Object proxy, Method method, Object[] args, Object result) {
		String methodIdentifier = getMethodIdentifier(method);
		jdbcConnectionMetrics.updateInvocationCounter(methodIdentifier);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Invocation succeeded for [{}].", methodIdentifier);
		}
		if ("close".equals(method.getName())) {
			jdbcConnectionMetrics.connectionReleased((IdentifiableConnection) proxy);
		}
	}

	public void updateMetricsAfterFailedInvocation(Object proxy, Method method, Object[] args, Throwable error) {
		String methodIdentifier = getMethodIdentifier(method);
		jdbcConnectionMetrics.updateInvocationCounter(methodIdentifier);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Invocation failed for [{}].", methodIdentifier, error);
		}
		if ("close".equals(method.getName())) {
			jdbcConnectionMetrics.connectionReleaseFailed((IdentifiableConnection) proxy);
		}
	}

	private String getMethodIdentifier(Method method) {
		return METHOD_IDENTIFIER_CACHE.computeIfAbsent(method, ClassUtils::getShortMethodIdentifier);
	}

}