package com.socialbakers.config;

import static org.apache.commons.beanutils.PropertyUtils.getPropertyDescriptors;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.socialbakers.config.exception.ConfigurationException;

public abstract class AbstractConfiguration {

	public static File CONF_DIR = new File("conf/");

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private List<File> configFiles = new ArrayList<File>();
	private String[] args = new String[0];

	private Map<IParamDefinition, PropertyDescriptor> properties;
	private Map<String, IParamDefinition> byName = new HashMap<String, IParamDefinition>();
	private Map<String, IParamDefinition> byEnv = new HashMap<String, IParamDefinition>();
	private Map<String, IParamDefinition> byOption = new HashMap<String, IParamDefinition>();
	private Map<Integer, IParamDefinition> byOrder = new HashMap<Integer, IParamDefinition>();

	private boolean suspendValidation;

	protected AbstractConfiguration(IParamDefinition[] confDefs) {
		for (IParamDefinition confDef : confDefs) {
			if (StringUtils.isNotBlank(confDef.getName())) {
				byName.put(confDef.getName(), confDef);
			}
			if (StringUtils.isNotBlank(confDef.getEnv())) {
				byEnv.put(confDef.getEnv(), confDef);
			}
			if (StringUtils.isNotBlank(confDef.getOption())) {
				byOption.put(confDef.getOption(), confDef);
			}
			if (confDef.getOrder() != null) {
				byOrder.put(confDef.getOrder(), confDef);
			}
		}
	}

	public void addConfigFile(File file) {
		if (!file.exists()) {
			logger.debug("Config file '{}' does not exists", file.getAbsolutePath());
			return;
		}
		logger.debug("Added file '{}'", file.getAbsolutePath());
		configFiles.add(file);
	}

	public void addConfigFile(String filename) {
		try {
			File configFile = new File(CONF_DIR, filename);
			addConfigFile(configFile);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	protected final void doValidate() {
		if (!suspendValidation) {
			validate();
		}
	}

	protected void reload() {
		try {
			reloadFromFiles();
			reloadFromEnvVars();
			reloadFromArgs();
			validate();
		} catch (ConfigurationException e) {
			throw e;
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
	}

	protected void setArgs(String... args) {
		this.args = args;
	}

	protected void validate() {

		StringBuilder msg = new StringBuilder();

		try {
			List<IParamDefinition> notSetRequiredParams = new ArrayList<IParamDefinition>();
			for (IParamDefinition confDef : byName.values()) {
				PropertyDescriptor descriptor = getProperties().get(confDef);
				Object value = descriptor.getReadMethod().invoke(this);
				if (value == null) {
					notSetRequiredParams.add(confDef);
				}
			}
			Comparator<IParamDefinition> c = new Comparator<IParamDefinition>() {
				@Override
				public int compare(IParamDefinition o1, IParamDefinition o2) {
					if (o1.getOrder() == null && o2.getOrder() == null) {
						return 0;
					} else if (o1.getOrder() == null) {
						return 1;
					} else if (o2.getOrder() == null) {
						return -1;
					}
					return o1.getOrder() - o2.getOrder();
				}
			};
			Collections.sort(notSetRequiredParams, c);
			if (!notSetRequiredParams.isEmpty()) {
				msg.append("You must pass/set at least ");
				msg.append(notSetRequiredParams.size());
				msg.append(" parameters:");
				for (IParamDefinition confDef : notSetRequiredParams) {
					msg.append(" " + formatOption(confDef, confDef.getName()));
				}
				msg.append("\n");

				List<IParamDefinition> options = new ArrayList<IParamDefinition>(byName.values());
				Collections.sort(options, c);
				msg.append("All options:");
				for (IParamDefinition confDef : options) {
					msg.append(" " + formatOption(confDef, confDef.getName()));
				}
				msg.append("\n");

				msg.append("Actual options:");
				for (IParamDefinition confDef : options) {
					PropertyDescriptor descriptor = getProperties().get(confDef);
					Object value = descriptor.getReadMethod().invoke(this);
					msg.append(" " + formatOption(confDef, value));
				}
			}
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}

		if (!msg.toString().isEmpty()) {
			throw new ConfigurationException(msg.toString());
		}
	}

	private String[] extractValue(String arg, String prefix1, Collection<String> prefixes2) {
		String[] result = new String[2];
		arg = arg.replaceFirst(prefix1, "");
		for (String prefix2 : prefixes2) {
			if (arg.startsWith(prefix2)) {
				result[0] = prefix2;
				result[1] = arg.replaceFirst(prefix2, "");
			}
		}
		if (StringUtils.isBlank(result[0]) || StringUtils.isBlank(result[1])) {
			throw new ConfigurationException("Invalid argument " + arg);
		}
		return result;
	}

	private String formatOption(IParamDefinition confDef, Object value) {
		if (confDef.getOrder() != null) {
			return "<" + value + ">";
		} else if (StringUtils.isNotBlank(confDef.getOption())) {
			return "-" + confDef.getOption() + "<" + value + ">";
		} else {
			return "--" + confDef.getName() + "<" + value + ">";
		}
	}

	private Map<IParamDefinition, PropertyDescriptor> getProperties() {
		if (properties != null) {
			return properties;
		}
		properties = new HashMap<IParamDefinition, PropertyDescriptor>();
		for (PropertyDescriptor descriptor : getPropertyDescriptors(this)) {
			String name = descriptor.getName();
			IParamDefinition confDef = byName.get(name);
			properties.put(confDef, descriptor);
		}
		return properties;
	}

	private void reloadFromArgs() {
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			String value;
			IParamDefinition confDef;
			if (arg.startsWith("--")) {
				String[] e = extractValue(arg, "--", byName.keySet());
				confDef = byName.get(e[0]);
				value = e[1];
			} else if (arg.startsWith("-")) {
				String[] e = extractValue(arg, "-", byOption.keySet());
				confDef = byOption.get(e[0]);
				value = e[1];
			} else {
				confDef = byOrder.get(i);
				value = arg;
			}

			setValue(confDef, value, ConfigSource.ARG);
		}
	}

	private void reloadFromEnvVars() {
		for (IParamDefinition confDef : byEnv.values()) {
			if (System.getenv(confDef.getEnv()) != null) {
				String value = System.getenv(confDef.getEnv());
				setValue(confDef, value, ConfigSource.ENV);
			}
		}
	}

	private void reloadFromFiles() {

		SAXBuilder builder = new SAXBuilder();

		for (File configFile : configFiles) {

			try {
				if (!configFile.exists()) {
					continue;
				}
				logger.debug("Reading configuration from file '{}':", configFile.getAbsolutePath());
				Document document = builder.build(configFile);
				Element rootNode = document.getRootElement();
				List<?> list = rootNode.getChildren("property");

				for (int i = 0; i < list.size(); i++) {

					Element node = (Element) list.get(i);

					String name = node.getChildText("name");
					String value = node.getChildText("value");
					IParamDefinition confDef = byName.get(name);

					setValue(confDef, value, ConfigSource.FILE);
				}

			} catch (Exception e) {
				throw new ConfigurationException(e);
			}
		}
	}

	private void setValue(IParamDefinition confDef, String stringValue, ConfigSource source) {

		try {

			PropertyDescriptor descriptor = getProperties().get(confDef);
			Class<?> type = descriptor.getPropertyType();

			Object value;
			if (type.isAssignableFrom(String.class)) {
				value = stringValue;
			} else if (Integer.class.isAssignableFrom(type) || int.class.isAssignableFrom(type)) {
				value = Integer.valueOf(stringValue);
			} else if (Long.class.isAssignableFrom(type) || long.class.isAssignableFrom(type)) {
				value = Long.valueOf(stringValue);
			} else {
				throw new ConfigurationException("Unsupported type " + type.getName());
			}

			Object previous = descriptor.getReadMethod().invoke(this);
			if (previous == null || !previous.equals(value)) {
				logger.debug("Setting value '{}' of property '{}' from source {}", value, confDef.getName(), source);
			}

			suspendValidation = true;
			descriptor.getWriteMethod().invoke(this, value);
			suspendValidation = false;

		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
	}

	private enum ConfigSource {
		ARG, ENV, FILE
	}
}
