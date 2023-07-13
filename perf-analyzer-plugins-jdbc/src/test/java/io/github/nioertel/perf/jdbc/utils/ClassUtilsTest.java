package io.github.nioertel.perf.jdbc.utils;

import java.lang.reflect.Method;
import java.nio.charset.Charset;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

class ClassUtilsTest {

	@Test
	void testGetMethod() throws NoSuchMethodException, SecurityException {
		Method mGetBytesSimple = String.class.getMethod("getBytes");
		BDDAssertions.assertThat(ClassUtils.getShortMethodIdentifier(mGetBytesSimple))//
				.isEqualTo("java.lang.String.getBytes()");

		Method mGetBytesCharset = String.class.getMethod("getBytes", Charset.class);
		BDDAssertions.assertThat(ClassUtils.getShortMethodIdentifier(mGetBytesCharset))//
				.isEqualTo("java.lang.String.getBytes(java.nio.charset.Charset)");

		Method mGetChars = String.class.getMethod("getChars", int.class, int.class, char[].class, int.class);
		BDDAssertions.assertThat(ClassUtils.getShortMethodIdentifier(mGetChars))//
				.isEqualTo("java.lang.String.getChars(int, int, [C, int)");
	}
}
