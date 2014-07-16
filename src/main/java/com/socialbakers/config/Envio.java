package com.socialbakers.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Envio {

	private static final Logger LOGGER = LoggerFactory.getLogger(Envio.class);

	public static void loadConfiguration() {

		File envFile = new File(System.getProperty("user.dir"), File.separator + ".env");

		if (!envFile.exists()) {
			return;
		}

		LOGGER.info("Loading envio configuration: {}", envFile);

		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(System.getProperty("user.dir") + File.separator + ".env"));

			Map<String, String> currentEnv = System.getenv();
			Map<String, String> newEnv = new HashMap<String, String>();

			for (Entry<String, String> entry : currentEnv.entrySet()) {
				newEnv.put(entry.getKey(), entry.getValue());
			}
			String line;

			while ((line = br.readLine()) != null) {
				String[] conf = line.split("=", 2);

				if (conf.length == 2) {
					newEnv.put(conf[0], conf[1]);
				}
			}
			setEnv(newEnv);
		} catch (Exception e1) {
			LOGGER.error(e1.getMessage(), e1);
		} finally {
			IOUtils.closeQuietly(br);
		}
	}

	/**
	 * code from http://stackoverflow.com/a/7201825
	 */
	protected static void setEnv(Map<String, String> newenv) {
		try {
			Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
			Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
			theEnvironmentField.setAccessible(true);
			Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
			env.putAll(newenv);
			Field theCaseInsensitiveEnvironmentField = processEnvironmentClass
					.getDeclaredField("theCaseInsensitiveEnvironment");
			theCaseInsensitiveEnvironmentField.setAccessible(true);
			Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
			cienv.putAll(newenv);
		} catch (NoSuchFieldException e) {
			try {
				Class[] classes = Collections.class.getDeclaredClasses();
				Map<String, String> env = System.getenv();
				for (Class cl : classes) {
					if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
						Field field = cl.getDeclaredField("m");
						field.setAccessible(true);
						Object obj = field.get(env);
						Map<String, String> map = (Map<String, String>) obj;
						map.clear();
						map.putAll(newenv);
					}
				}
			} catch (Exception e2) {
				LOGGER.error(e2.getMessage(), e2);
			}
		} catch (Exception e1) {
			LOGGER.error(e1.getMessage(), e1);
		}
	}
}
