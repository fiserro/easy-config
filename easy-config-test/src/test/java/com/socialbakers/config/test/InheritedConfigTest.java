package com.socialbakers.config.test;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class InheritedConfigTest {

	@Test
	public void testLoadArgs() {
		InheritedConf config = InheritedConf.create("-i", "input1", "input2", "input3", "-n", "100");

		assertThat(config.getInput_files(), hasItems("input1", "input2", "input3"));
		assertThat(config.getNumber(), is(100));
	}

	@Test
	public void testMergedParams() {
		assertThat(InheritedConf.Def.input_files.getEnv(), is("INPUT_FILES_X"));
		assertThat(InheritedConf.Def.input_files.getOption(), is("i"));
		assertThat(InheritedConf.Def.input_files.getDescription(), is("Overriden description"));
	}

	@Test
	public void testStaticShortcut() {
		assertThat(InheritedConf.Def.input_files, is(InheritedConf._INPUT_FILES));
		assertThat(InheritedConf.Def.number, is(InheritedConf._NUMBER));
	}
}
