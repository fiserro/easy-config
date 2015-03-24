package com.socialbakers.config.test;

import org.junit.Assert;
import org.junit.Test;

import com.socialbakers.config.Envio;
import com.socialbakers.config.exception.ConfigurationException;
import com.socialbakers.config.exception.DumpException;
import com.socialbakers.config.exception.HelpException;

public class ConfigurationExceptionTest {

	@Test(expected = ConfigurationException.class)
	public void shouldThrowConfigurationException() {
		try {
			Envio.setEnv("TEST_CONF_DIR_ENV", "not/existing/dir");
			new Configuration(new String[0]);
		} catch (ConfigurationException e) {
			Assert.assertTrue(e.getMessage().startsWith("You must pass/set at least 2 parameters: <port> <zookeeper>"));
			throw e;
		}
	}

	@Test(expected = DumpException.class)
	public void shouldThrowDumpException() {
		new Configuration(new String[] { "-dump" });
	}

	@Test(expected = HelpException.class)
	public void shouldThrowHelpException() {
		new Configuration(new String[] { "-help" });
	}

}
