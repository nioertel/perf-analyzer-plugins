package io.github.nioertel.perf.utills;

import java.util.concurrent.FutureTask;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

import io.github.nioertel.perf.utils.TaskNameExtractor;

class TaskNameExtractorTest {

	private static final class TestRunnable implements Runnable {

		@Override
		public void run() {
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
}
