package com.socialbakers.config.test;

import org.junit.Assert;
import org.junit.Test;

import com.socialbakers.config.ParamValueSeparator;

public class ConfigurationLoadFromArgsTest {

	@Test
	public void testLoadFromArgsEqualsSeparator() {

		// equal separator
		Configuration.PARAM_VALUE_SEPARATOR = ParamValueSeparator.EQUAL_SEPARATOR;

		Configuration config = new Configuration(new String[] { "80", "zookeeper", "-m=300", "--hera.zk=hera",
				"-x=prefix" });
		Assert.assertEquals(300, config.getMaxPoolSize());
		Assert.assertEquals("hera", config.getHera_zk());
		Assert.assertEquals("prefix", config.getHeraVersionPrefix());

		config = new Configuration(new String[] { "80", "zookeeper", "--maxPoolSize=300" });
		Assert.assertEquals(300, config.getMaxPoolSize());

		// by order
		config = new Configuration(new String[] { "-m=300", "80", "zookeeper" });
		Assert.assertEquals("zookeeper", config.getZookeeper());
		Assert.assertEquals(new Integer(80), config.getPort());

	}

	@Test
	public void testLoadFromArgsSpaceSeparator() {
		// space separator
		Configuration.PARAM_VALUE_SEPARATOR = ParamValueSeparator.SPACE_SEPARATOR;

		Configuration config = new Configuration(new String[] { "80", "zookeeper", "-m", "300" });
		Assert.assertEquals(300, config.getMaxPoolSize());
		Assert.assertEquals(80, config.getPort().intValue());
		Assert.assertEquals("zookeeper", config.getZookeeper());

		config = new Configuration(new String[] { "-m", "300", "80", "zookeeper" });
		Assert.assertEquals(300, config.getMaxPoolSize());
		Assert.assertEquals(80, config.getPort().intValue());
		Assert.assertEquals("zookeeper", config.getZookeeper());

		config = new Configuration(new String[] { "80", "--maxPoolSize", "300", "zookeeper" });
		Assert.assertEquals(300, config.getMaxPoolSize());
		Assert.assertEquals(80, config.getPort().intValue());
		Assert.assertEquals("zookeeper", config.getZookeeper());
	}

}
