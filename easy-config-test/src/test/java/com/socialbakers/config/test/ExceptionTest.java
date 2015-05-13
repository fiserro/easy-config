package com.socialbakers.config.test;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.socialbakers.config.Envio;
import com.socialbakers.config.exception.ConfigurationException;
import com.socialbakers.config.exception.DumpException;
import com.socialbakers.config.exception.HelpException;

public class ExceptionTest {

	@Test(expected = ConfigurationException.class)
	public void shouldThrowConfigurationException() {
		try {
			// be sure that environment variables are not set from previous test
			Envio.setEnv(BasicConf.CONF_DIR_ENV, "not/existing/dir");
			for (String zkEnv : BasicConf.Def.zookeeper.getEnvs()) {
				Envio.setEnv(zkEnv, null);
			}
			Envio.setEnv(BasicConf.Def.port.getEnv(), null);
			BasicConf.create(new String[0]);
		} catch (ConfigurationException e) {
			assertThat(e.getMessage(), startsWith("You must pass/set at least 2 parameters: <port> <zookeeper>"));
			throw e;
		}
	}

	@Test(expected = DumpException.class)
	public void shouldThrowDumpException() {
		BasicConf.create(new String[] { "-dump" });
	}

	@Test(expected = HelpException.class)
	public void shouldThrowHelpException() {
		BasicConf.create(new String[] { "-help" });
	}

}
