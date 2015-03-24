package com.socialbakers.config.test;

import org.junit.Assert;
import org.junit.Test;

import com.socialbakers.config.Envio;

public class ConfigurationLoadFromEnvTest {

	private static final String TEST_CONF_DIR_ENV = "TEST_CONF_DIR_ENV";

	@Test
	public void testLoadDevEnv() {

		Envio.setEnv(TEST_CONF_DIR_ENV, "src/test/resources/test/env/dev");
		Configuration config = new Configuration(new String[0]);

		Assert.assertEquals(1, config.getMaxPoolSize());
		Assert.assertEquals("dev.hera", config.getHera_zk());
		Assert.assertEquals("dev", config.getHeraVersionPrefix());
		Assert.assertEquals("dev.connection.string", config.getZookeeper());
		Assert.assertEquals(new Integer(1111), config.getPort());
	}

	@Test
	public void testLoadProductionEnv() {

		Envio.setEnv(TEST_CONF_DIR_ENV, "src/test/resources/test/env/production");
		Configuration config = new Configuration(new String[0]);

		Assert.assertEquals(64, config.getMaxPoolSize());
		Assert.assertEquals("hera", config.getHera_zk());
		Assert.assertEquals(null, config.getHeraVersionPrefix());
		Assert.assertEquals("zk", config.getZookeeper());
		Assert.assertEquals(new Integer(2181), config.getPort());
	}

}
