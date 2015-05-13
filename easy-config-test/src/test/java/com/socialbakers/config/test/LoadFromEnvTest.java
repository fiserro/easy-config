package com.socialbakers.config.test;

import org.junit.Assert;
import org.junit.Test;

import com.socialbakers.config.Envio;

public class LoadFromEnvTest {

	static final String TEST_CONF_DIR_ENV = "TEST_CONF_DIR_ENV";

	@Test
	public void testLoadDevEnv() {

		Envio.setEnv(TEST_CONF_DIR_ENV, "src/test/resources/test/env/dev");
		BasicConf config = BasicConf.create(new String[0]);

		Assert.assertEquals(1, config.getKeepAliveTime());
		Assert.assertEquals("dev.connection.string", config.getZookeeper());
		Assert.assertEquals(new Integer(1111), config.getPort());
	}

	@Test
	public void testLoadProductionEnv() {

		Envio.setEnv(TEST_CONF_DIR_ENV, "src/test/resources/test/env/production");
		BasicConf config = BasicConf.create(new String[0]);

		Assert.assertEquals(60000, config.getKeepAliveTime());
		Assert.assertEquals("zk", config.getZookeeper());
		Assert.assertEquals(new Integer(2181), config.getPort());
	}

}
