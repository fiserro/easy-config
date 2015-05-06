package com.socialbakers.config.test;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.socialbakers.config.Envio;

public class LoadMultivalTest {

	@Test
	public void testLoadArgs() {
		MultivalConf config = new MultivalConf(new String[] {
				"--input.files", "input1", "input2", "input3",
				"--operations", "10", "20", "30",
				"--output.file", "outputFile"
		});

		assertThat(config.getInput_files(), hasItems("input1", "input2", "input3"));
		assertThat(config.getOperations(), hasItems(10, 20, 30));
		assertThat(config.getOutput_file(), is("outputFile"));
	}

	@Test
	public void testLoadDevEnv() {

		Envio.setEnv(LoadFromEnvTest.TEST_CONF_DIR_ENV, "src/test/resources/test/env/dev");
		MultivalConf config = new MultivalConf(new String[0]);

		assertThat(config.getInput_files(), hasItems("file1", "file2", "file3"));
		assertThat(config.getOperations(), hasItems(1, 2, 3));
	}
}
