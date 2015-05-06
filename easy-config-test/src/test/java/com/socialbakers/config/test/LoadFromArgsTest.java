package com.socialbakers.config.test;

import org.junit.Assert;
import org.junit.Test;

import com.socialbakers.config.ParamValueSeparator;

public class LoadFromArgsTest {

	@Test
	public void testLoadFromArgsEqualsSeparator() {

		BasicConf.PARAM_VALUE_SEPARATOR = ParamValueSeparator.EQUAL_SEPARATOR;

		BasicConf config = new BasicConf(new String[] {
				"80",
				"zookeeper",
				"-k=300" });
		Assert.assertEquals(80, config.getPort().intValue());
		Assert.assertEquals("zookeeper", config.getZookeeper());
		Assert.assertEquals(300, config.getKeepAliveTime());

		config = new BasicConf(new String[] { "-k=300", "80", "zookeeper" });
		Assert.assertEquals(80, config.getPort().intValue());
		Assert.assertEquals("zookeeper", config.getZookeeper());
		Assert.assertEquals(300, config.getKeepAliveTime());
	}

	@Test
	public void testLoadFromArgsSpaceSeparator() {
		// space separator
		BasicConf.PARAM_VALUE_SEPARATOR = ParamValueSeparator.SPACE_SEPARATOR;

		BasicConf config = new BasicConf(new String[] { "80", "zookeeper", "-k", "300" });
		Assert.assertEquals(80, config.getPort().intValue());
		Assert.assertEquals("zookeeper", config.getZookeeper());
		Assert.assertEquals(300, config.getKeepAliveTime());

		config = new BasicConf(new String[] { "-k", "300", "80", "zookeeper" });
		Assert.assertEquals(80, config.getPort().intValue());
		Assert.assertEquals("zookeeper", config.getZookeeper());
		Assert.assertEquals(300, config.getKeepAliveTime());

		config = new BasicConf(new String[] { "80", "--keepAliveTime", "300", "zookeeper" });
		Assert.assertEquals(80, config.getPort().intValue());
		Assert.assertEquals("zookeeper", config.getZookeeper());
		Assert.assertEquals(300, config.getKeepAliveTime());
	}

}
