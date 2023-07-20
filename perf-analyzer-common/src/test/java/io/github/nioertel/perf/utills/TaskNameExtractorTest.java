package io.github.nioertel.perf.utills;

import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.sleuth.SpanNamer;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.async.TraceRunnable;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;

import io.github.nioertel.perf.utils.TaskNameExtractor;

class TaskNameExtractorTest {

	private static final class TestRunnable implements Runnable {

		@Override
		public void run() {
		}

	}

	private static final class TestPrivilegedAction implements PrivilegedAction<Long> {

		@Override
		public Long run() {
			return 1L;
		}

	}

	private static final class TestPrivilegedExceptionAction implements PrivilegedExceptionAction<Long> {

		@Override
		public Long run() throws Exception {
			return 1L;
		}

	}

	@Test
	void testExtractFromCustomRunnable() {
		Runnable r = () -> {
		};
		BDDAssertions.assertThat(TaskNameExtractor.extractTaskName(r)).startsWith(TaskNameExtractorTest.class.getName() + "$$Lambda$");
	}

	@Test
	void textExtractFromRunnableWithNamedClass() {
		Runnable r = new TestRunnable();
		BDDAssertions.assertThat(TaskNameExtractor.extractTaskName(r)).isEqualTo(TestRunnable.class.getName());
	}

	@Test
	void testExtractFromFutureTask() {
		Runnable r = new FutureTask<>(new TestRunnable(), 1);
		BDDAssertions.assertThat(TaskNameExtractor.extractTaskName(r)).isEqualTo(TestRunnable.class.getName());
	}

	@Test
	void testExtractFromStackedFutureTask() {
		Runnable r = new FutureTask<>(new FutureTask<>(new TestRunnable(), 1), 2);
		BDDAssertions.assertThat(TaskNameExtractor.extractTaskName(r)).isEqualTo(TestRunnable.class.getName());
	}

	@Test
	void testExtractFromTraceRunnableInFutureTask() {
		Runnable r = new FutureTask<>(new TraceRunnable(Mockito.mock(Tracer.class), Mockito.mock(SpanNamer.class), new TestRunnable()), 1);
		BDDAssertions.assertThat(TaskNameExtractor.extractTaskName(r)).isEqualTo(TestRunnable.class.getName());
	}

	@Test
	void testExtractFromDelegatingSecurityContextRunnable() {
		Runnable r = new DelegatingSecurityContextRunnable(new TestRunnable());
		BDDAssertions.assertThat(TaskNameExtractor.extractTaskName(r)).isEqualTo(TestRunnable.class.getName());
	}

	@Test
	void testExtractFromRunnableAdapter() {
		Callable<Integer> c = Executors.callable(new TestRunnable(), 1);
		BDDAssertions.assertThat(TaskNameExtractor.extractTaskName(c)).isEqualTo(TestRunnable.class.getName());
	}

	@Test
	void testExtractFromPrivilegedActionCallable() throws Exception {
		Callable<?> c = Executors.callable(new TestPrivilegedAction());
		BDDAssertions.assertThat(TaskNameExtractor.extractTaskName(c)).isEqualTo(TestPrivilegedAction.class.getName());
	}

	@Test
	void testExtractFromPrivilegedExceptionActionCallable() throws Exception {
		Callable<?> c = Executors.callable(new TestPrivilegedExceptionAction());
		BDDAssertions.assertThat(TaskNameExtractor.extractTaskName(c)).isEqualTo(TestPrivilegedExceptionAction.class.getName());
	}
}
