package com.socialbakers.config.test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class LoadFromXmlTest {

	@Ignore
	@Test
	public void testConfigFilesExists() throws URISyntaxException {
		URL resource = getClass().getResource("/conf/configuration-default.xml");
		File file = new File(resource.toURI());
		Assert.assertTrue("File " + file.getAbsolutePath() + " does not exists", file.exists());
	}
}
