package com.socialbakers.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Envio {

	public static final String ENV_FILE = ".env";
	private static final Logger LOGGER = LoggerFactory.getLogger(Envio.class);
	private static final String ENV_DELIMITER = "\\|";

	public static void loadConfiguration() {
		loadConfiguration(ENV_FILE);
	}

	public static void loadConfiguration(String envFile) {
		String confDirEnv = System.getenv(AbstractConfiguration.CONF_DIR_ENV);
		if (confDirEnv != null) {
			if (tryLoadConf(new File(confDirEnv, envFile))) {
				return;
			}
			if (tryLoadConf(new File(System.getProperty("user.dir") + File.separator + confDirEnv, envFile))) {
				return;
			}
		}
		if (tryLoadConf(new File(System.getProperty("user.dir"), File.separator + envFile))) {
			return;
		}
	}

	public static void setEnv(String name, String value) {
		HashMap<String, String> newEnv = new HashMap<String, String>();
		newEnv.put(name, value);
		setEnv(newEnv, true);
	}

	public static void setEnvIfMissing(String name, String value) {
		HashMap<String, String> newEnv = new HashMap<String, String>();
		newEnv.put(name, value);
		setEnv(newEnv, false);
	}

	public static String[] splitEnvNames(String env) {
		if (env == null || "".equals(env)) {
			return new String[0];
		}
		return env.trim().split(ENV_DELIMITER);
	}

	/**
	 * code from http://stackoverflow.com/a/7201825
	 */
	private static void setEnv(Map<String, String> newEnv, boolean overwrite) {

		for (Entry<String, String> entry : newEnv.entrySet()) {
			if (overwrite || System.getProperty(entry.getKey()) != null) {
				System.setProperty(entry.getKey(), entry.getValue());
			}
		}

		if (!overwrite) {
			Iterator<Entry<String, String>> iterator = newEnv.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, String> next = iterator.next();
				if (System.getenv(next.getKey()) != null) {
					iterator.remove();
				}
			}
		}

		try {
			Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
			Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
			theEnvironmentField.setAccessible(true);
			Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
			for (Entry<String, String> entry : newEnv.entrySet()) {
				if (entry.getValue() == null) {
					env.remove(entry.getKey());
				} else {
					env.put(entry.getKey(), entry.getValue());
				}
			}
			Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
			theCaseInsensitiveEnvironmentField.setAccessible(true);
			Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
			cienv.putAll(newEnv);
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
						for (Entry<String, String> entry : newEnv.entrySet()) {
							if (entry.getValue() == null) {
								map.remove(entry.getKey());
							} else {
								map.put(entry.getKey(), entry.getValue());
							}
						}
					}
				}
			} catch (Exception e2) {
				LOGGER.error(e2.getMessage(), e2);
			}
		} catch (Exception e1) {
			LOGGER.error(e1.getMessage(), e1);
		}
	}

	private static boolean tryLoadConf(File envFile) {
		if (!envFile.exists()) {
			return false;
		}

		LOGGER.info("Loading envio configuration: {}", envFile);

		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(envFile));

			Map<String, String> newEnv = new HashMap<String, String>();

			// Map<String, String> currentEnv = System.getenv();
			// for (Entry<String, String> entry : currentEnv.entrySet()) {
			// newEnv.put(entry.getKey(), entry.getValue());
			// }

			String line;
			while ((line = br.readLine()) != null) {
				String[] conf = line.split("=", 2);

				if (conf.length == 2) {
					newEnv.put(conf[0], conf[1]);
				}
			}
			setEnv(newEnv, false);
		} catch (Exception e1) {
			LOGGER.error(e1.getMessage(), e1);
		} finally {
			IOUtils.closeQuietly(br);
		}
		return true;
	}
}
