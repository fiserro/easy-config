package com.socialbakers.config.generator;

import static com.socialbakers.config.ParamValueSeparator.EQUAL_SEPARATOR;
import static com.socialbakers.config.ParamValueSeparator.SPACE_SEPARATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ParamValueSeparatorTest {

	@Test
	public void testMatchNameEqualSep() {
		assertEquals("paramName", EQUAL_SEPARATOR.name("--paramName=paramValue"));
		assertEquals("param.name.2", EQUAL_SEPARATOR.name("--param.name.2=$D"));
		assertEquals("param_name_2", EQUAL_SEPARATOR.name("--param_name_2=$D"));
	}

	@Test
	public void testMatchNameSpaceSep() {
		assertEquals("paramName", SPACE_SEPARATOR.name("--paramName"));
		assertEquals("param.name.2", SPACE_SEPARATOR.name("--param.name.2"));
		assertEquals("param_name_2", SPACE_SEPARATOR.name("--param_name_2"));
	}

	@Test
	public void testMatchOptionEqualSep() {
		assertEquals("p", EQUAL_SEPARATOR.option("-p=paramValue"));
	}

	@Test
	public void testMatchOptionSpaceSep() {
		assertEquals("p", SPACE_SEPARATOR.option("-p"));
	}

	@Test
	public void testNotMatch() {
		assertNull(SPACE_SEPARATOR.option("-p.a"));
		assertNull(EQUAL_SEPARATOR.option("-p.a=abc"));
		assertNull(SPACE_SEPARATOR.option("--p.a$"));
		assertNull(EQUAL_SEPARATOR.option("--p.a$=abc"));
	}

}
