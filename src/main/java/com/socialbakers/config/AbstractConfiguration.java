package com.socialbakers.config;

import static org.apache.commons.beanutils.PropertyUtils.getPropertyDescriptors;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.internal.core.Assert;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.socialbakers.config.exception.ConfigurationException;
import com.socialbakers.config.generator.GenerateConfig;
import com.socialbakers.config.generator.ParamValueSeparator;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

/**
 * @author <a href="mailto:robert.fiser@socialbakers.com">Robert Fišer</a>
 * 
 */
public abstract class AbstractConfiguration {

	public static ParamValueSeparator PARAM_VALUE_SEPARATOR;
	public static boolean ALWAYS_RELOAD;
	public static String CONF_DIR_ENV;
	public static String DEFAULT_CONF_DIR_ENV = "CONF_DIR";

	private static final String NAME_PREFIX = "--";
	private static final String OPTION_PREFIX = "-";
	private static final String HELP_NAME = NAME_PREFIX + GenerateConfig.HELP;
	private static final String HELP_OPTION = OPTION_PREFIX + GenerateConfig.HELP;
	private static final String DUMP_NAME = NAME_PREFIX + GenerateConfig.DUMP;
	private static final String DUMP_OPTION = OPTION_PREFIX + GenerateConfig.DUMP;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private List<Object> resources = new ArrayList<Object>();
	private String[] args = new String[0];
	private String helpName = "app-name";
	private String helpDescription = "";
	private List<IParamDefinition> confDefs;

	private Map<IParamDefinition, PropertyDescriptor> properties;
	private Map<String, IParamDefinition> byName = new HashMap<String, IParamDefinition>();
	private Map<String, IParamDefinition> byEnv = new HashMap<String, IParamDefinition>();
	private Map<String, IParamDefinition> byOption = new HashMap<String, IParamDefinition>();
	private Map<Integer, IParamDefinition> byOrder = new HashMap<Integer, IParamDefinition>();

	protected boolean suspendValidation;

	private Comparator<IParamDefinition> paramOrderComparator = new Comparator<IParamDefinition>() {
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

	protected AbstractConfiguration(String helpName, String helpDescription) {

		if (StringUtils.isNotBlank(helpName)) {
			this.helpName = helpName;
		}

		if (StringUtils.isNotBlank(helpDescription)) {
			this.helpDescription = helpDescription;
		}

		this.confDefs = new ArrayList<IParamDefinition>();

		Envio.loadConfiguration();
	}

	public void addResource(File file) {
		resources.add(file);
	}

	public void addResource(String filename) {
		resources.add(filename);
	}

	protected void addDef(IParamDefinition... def) {
		confDefs.addAll(Arrays.asList(def));
		Collections.sort(this.confDefs, paramOrderComparator);

		for (IParamDefinition confDef : this.confDefs) {
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

	protected void reload() {
		boolean alwaysReload = ALWAYS_RELOAD;
		ALWAYS_RELOAD = false;
		try {
			reloadFromFiles();
			reloadFromEnvVars();
			reloadFromArgs();
			validate();
		} catch (ConfigurationException e) {
			throw e;
		} catch (Exception e) {
			throw new ConfigurationException(e + "\n" + helpMsg());
		} finally {
			ALWAYS_RELOAD = alwaysReload;
		}
	}

	protected void setArgs(String... args) {

		this.args = args;

		for (String arg : args) {
			Assert.isNotNull(arg);
			if (arg.startsWith(HELP_NAME) || arg.startsWith(HELP_OPTION)) {
				String msg;
				msg = helpMsg();
				throw new ConfigurationException(msg);
			}
			if (arg.startsWith(DUMP_NAME) || arg.startsWith(DUMP_OPTION)) {
				String msg;
				suspendValidation = true;
				reload();
				try {
					msg = dump();
				} catch (Exception e) {
					msg = e.getMessage();
				}
				throw new ConfigurationException(msg);
			}
		}
	}

	protected void validate() {

		if (suspendValidation) {
			return;
		}

		StringBuilder msg = new StringBuilder();

		boolean valid;

		try {
			List<IParamDefinition> notSetRequiredParams = new ArrayList<IParamDefinition>();
			for (IParamDefinition confDef : byName.values()) {
				if (!confDef.isRequired()) {
					continue;
				}
				PropertyDescriptor descriptor = getProperties().get(confDef);
				Object value = descriptor.getReadMethod().invoke(this);
				if (value == null) {
					notSetRequiredParams.add(confDef);
				}
			}
			valid = notSetRequiredParams.isEmpty();

			msg.append("You must pass/set at least ");
			msg.append(notSetRequiredParams.size());
			msg.append(" parameters:");
			msg.append(usage(notSetRequiredParams));
			msg.append("\n");

			msg.append("All options:");
			msg.append(usage(confDefs));
			msg.append("\n");

			msg.append(dump());

		} catch (Exception e) {
			throw new ConfigurationException(e);
		}

		if (!valid) {
			throw new ConfigurationException(msg.toString());
		}
	}

	private String dump() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		StringBuilder sb = new StringBuilder();
		sb.append("ACTUAL STATE:");
		sb.append("\n");
		for (IParamDefinition confDef : confDefs) {
			PropertyDescriptor descriptor = getProperties().get(confDef);
			Object value = descriptor.getReadMethod().invoke(this);
			// sb.append(" " + formatOption(confDef, value, false));
			sb.append(confDef.getName());
			sb.append(":\t");
			sb.append(value);
			sb.append("\n");
		}
		return sb.toString();
	}

	private String[] extractValue(String arg, String prefix1, Collection<String> prefixes2) {
		String[] result = new String[2];
		arg = arg.replaceFirst(prefix1, "").replaceFirst(PARAM_VALUE_SEPARATOR.getSeparator(), "");
		for (String prefix2 : prefixes2) {
			if (arg.startsWith(prefix2)) {
				result[0] = prefix2;
				result[1] = arg.replaceFirst(prefix2, "");
			}
		}
		if (StringUtils.isBlank(result[0]) || StringUtils.isBlank(result[1])) {
			throw new IllegalArgumentException("Invalid argument " + arg);
		}
		return result;
	}

	private String formatOption(IParamDefinition confDef, Object value, boolean bracets) {
		if (bracets) {
			value = String.format("<%s>", value.toString());
		}
		if (confDef.getOrder() != null) {
			return value.toString();
		} else if (StringUtils.isNotBlank(confDef.getOption())) {
			return OPTION_PREFIX + confDef.getOption() + value;
		} else {
			return NAME_PREFIX + confDef.getName() + value;
		}
	}

	private File getConfDir() {
		String confDirName = System.getenv(CONF_DIR_ENV);
		if (confDirName == null) {
			confDirName = System.getenv(DEFAULT_CONF_DIR_ENV);
		}
		if (confDirName == null) {
			confDirName = "conf/";
		}
		return new File(confDirName);
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

	private List<String> getResourcesAsStrings() {
		List<String> result = new ArrayList<String>();
		for (Object resource : resources) {
			result.add(resource.toString());
		}
		return result;
	}

	private String helpMsg() {

		try {
			Configuration cfg = new Configuration();
			cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
			cfg.setClassForTemplateLoading(AbstractConfiguration.class, "templates");

			Template helpTemplate = cfg.getTemplate("help.ftl");

			Map<String, Object> input = new HashMap<String, Object>();
			input.put("name", helpName);
			input.put("description", helpDescription);
			input.put("usage", usage(confDefs));
			input.put("params", confDefs);
			input.put("helpName", HELP_NAME);
			input.put("helpOption", HELP_OPTION);
			input.put("dumpName", DUMP_NAME);
			input.put("dumpOption", DUMP_OPTION);
			input.put("namePrefix", NAME_PREFIX);
			input.put("optionPrefix", OPTION_PREFIX);
			input.put("resources", getResourcesAsStrings());

			StringWriter writer = new StringWriter();
			try {
				helpTemplate.process(input, writer);
				return writer.toString();
			} finally {
				writer.close();
			}
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	private void reloadFromArgs() {

		int iPos = 0;

		for (int i = 0; i < args.length; i++) {

			String arg = args[i];
			String value;
			IParamDefinition confDef;
			String source = arg;

			Set<String> skipArgs = new HashSet<String>(Arrays.asList(HELP_NAME, HELP_OPTION, DUMP_NAME, DUMP_OPTION));
			if (skipArgs.contains(arg)) {
				continue;
			}

			if (PARAM_VALUE_SEPARATOR == ParamValueSeparator.SPACE_SEPARATOR
					&& (arg.startsWith(NAME_PREFIX) || arg.startsWith(OPTION_PREFIX))) {
				if (arg.startsWith(NAME_PREFIX)) {
					confDef = byName.get(arg.replaceFirst(NAME_PREFIX, ""));
				} else {
					confDef = byOption.get(arg.replaceFirst(OPTION_PREFIX, ""));
				}
				if (confDef == null) {
					throw new IllegalArgumentException("Invalid argument: " + arg);
				}
				if (++i >= args.length) {
					throw new IllegalArgumentException("Missing value for argument: " + arg);
				}
				value = args[i];
			} else if (arg.startsWith(NAME_PREFIX)) {
				String[] e = extractValue(arg, NAME_PREFIX, byName.keySet());
				confDef = byName.get(e[0]);
				value = e[1];
			} else if (arg.startsWith(OPTION_PREFIX)) {
				String[] e = extractValue(arg, OPTION_PREFIX, byOption.keySet());
				confDef = byOption.get(e[0]);
				value = e[1];
			} else {
				confDef = byOrder.get(iPos++);
				value = arg;
				source = "arg[" + i + "]=" + arg;
			}

			setValue(confDef, value, ConfigSource.ARG, source);
		}
	}

	private void reloadFromEnvVars() {
		for (IParamDefinition confDef : byEnv.values()) {
			if (System.getenv(confDef.getEnv()) != null) {
				String value = System.getenv(confDef.getEnv());
				setValue(confDef, value, ConfigSource.ENV, confDef.getEnv());
			}
		}
	}

	private void reloadFromFiles() {

		SAXBuilder builder = new SAXBuilder();

		for (Object resource : resources) {

			try {
				Document document = null;

				File configFile = null;
				if (resource instanceof String) {
					configFile = new File(getConfDir(), (String) resource);
				} else if (resource instanceof File) {
					configFile = (File) resource;
				}
				if (configFile != null) {
					if (!configFile.exists()) {
						logger.info("Config file '{}' does not exists", configFile.getAbsolutePath());
						continue;
					}
					logger.info("Reading configuration from file '{}':", configFile.getAbsolutePath());
					document = builder.build(configFile);
				}

				// TODO inpustream, url, ...

				if (document != null) {
					Element rootNode = document.getRootElement();
					List<?> list = rootNode.getChildren("property");

					for (int i = 0; i < list.size(); i++) {

						Element node = (Element) list.get(i);

						String name = node.getChildText("name");
						String value = node.getChildText("value");
						IParamDefinition confDef = byName.get(name);

						setValue(confDef, value, ConfigSource.FILE, configFile.getName());
					}
				}

			} catch (Exception e) {
				throw new ConfigurationException(e);
			}
		}
	}

	private void setValue(IParamDefinition confDef, String stringValue, ConfigSource sourceType, String source) {

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
				logger.info("Setting value '{}' of property '{}' from source {} '{}'", value, confDef.getName(),
						sourceType, source);
			}

			boolean hold = suspendValidation;
			suspendValidation = true;
			descriptor.getWriteMethod().invoke(this, value);
			suspendValidation = hold;

		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
	}

	private String usage(Collection<IParamDefinition> params) {
		StringBuilder sb = new StringBuilder();
		for (IParamDefinition confDef : params) {
			sb.append(" ");
			if (!confDef.isRequired()) {
				sb.append("[");
			}
			sb.append(formatOption(confDef, confDef.getName(), true));
			if (!confDef.isRequired()) {
				sb.append("]");
			}
		}
		return sb.toString();
	}

	private enum ConfigSource {
		ARG, ENV, FILE
	}

}
