package com.socialbakers.config;

import static org.apache.commons.beanutils.PropertyUtils.getPropertyDescriptors;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.socialbakers.config.exception.ConfigurationException;
import com.socialbakers.config.exception.DumpException;
import com.socialbakers.config.exception.HelpException;

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

	static final String NAME_PREFIX = "--";
	static final String OPTION_PREFIX = "-";
	public static final String OPTION_PATTERN = "[a-zA-Z]";
	public static final String NAME_PATTERN = "[a-zA-Z0-9]+([_\\.][a-zA-Z0-9]+)*";
	private static final String HELP_NAME = NAME_PREFIX + IParamDefinition.HELP;
	private static final String HELP_OPTION = OPTION_PREFIX + IParamDefinition.HELP;
	private static final String DUMP_NAME = NAME_PREFIX + IParamDefinition.DUMP;
	private static final String DUMP_OPTION = OPTION_PREFIX + IParamDefinition.DUMP;

	private static final Set<String> SKIP_ARGS = new HashSet<String>(Arrays.asList(HELP_NAME, HELP_OPTION, DUMP_NAME, DUMP_OPTION));

	public static String replaceDots(String name) {
		return name.replaceAll("\\.", "_");
	}

	public static void setArgFormatIfItsEmpty(ParamValueSeparator argFormat) {
		if (PARAM_VALUE_SEPARATOR == null) {
			PARAM_VALUE_SEPARATOR = argFormat;
		}
	}

	public static void setConfDirEnvNameIfItsEmpty(String confDirEnvName) {
		if (CONF_DIR_ENV == null) {
			CONF_DIR_ENV = confDirEnvName;
		}
	}

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private List<Object> resources = new ArrayList<Object>();

	private String[] args = new String[0];
	private String helpName = "app-name";
	private String helpDescription = "";

	private String envFile;
	private List<IParamDefinition> confDefs;
	private Map<IParamDefinition, PropertyDescriptor> properties;
	private Map<String, IParamDefinition> byName = new HashMap<String, IParamDefinition>();
	private Map<String, IParamDefinition> byEnv = new HashMap<String, IParamDefinition>();
	private Map<String, IParamDefinition> byOption = new HashMap<String, IParamDefinition>();
	private Map<Integer, IParamDefinition> byOrder = new HashMap<Integer, IParamDefinition>();

	private boolean initLoad;
	protected boolean suspendValidation;

	/*
	 * TODO configurable
	 */
	private String multivalueSeparator = " ";

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

	public AbstractConfiguration(String[] args) {
		this.args = args;
		this.confDefs = new ArrayList<IParamDefinition>();
		confDefs.addAll(knownParams());
		Collections.sort(this.confDefs, paramOrderComparator);

		for (IParamDefinition confDef : this.confDefs) {
			if (StringUtils.isNotBlank(confDef.getName())) {
				byName.put(confDef.getName(), confDef);
			}
			if (StringUtils.isNotBlank(confDef.getName())) {
				byName.put(replaceDots(confDef.getName()), confDef);
			}
			for (String env : confDef.getEnvs()) {
				byEnv.put(env, confDef);
			}
			if (StringUtils.isNotBlank(confDef.getOption())) {
				byOption.put(confDef.getOption(), confDef);
			}
			if (confDef.getOrder() != null) {
				byOrder.put(confDef.getOrder(), confDef);
			}
		}
	}

	public void addResource(File file) {
		resources.add(file);
	}

	public void addResource(String filename) {
		resources.add(filename);
	}

	public String getHelpDescription() {
		return helpDescription;
	}

	public String getHelpName() {
		return helpName;
	}

	protected PropertyDescriptor getDescriptor(IParamDefinition confDef) {
		PropertyDescriptor descriptor = getProperties().get(confDef);
		Preconditions.checkNotNull(descriptor, "Property '" + confDef + "' not found");
		return descriptor;
	}

	protected String getEnvFile() {
		return envFile;
	}

	protected Object getValue(IParamDefinition confDef) {
		try {
			return getDescriptor(confDef).getReadMethod().invoke(this);
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
	}

	protected Collection<IParamDefinition> knownParams() {
		return Collections.emptyList();
	}

	protected void reload() {
		if (!initLoad) {
			Envio.loadConfiguration(envFile);
		}
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
			initLoad = true;
		}
	}

	protected void reloadIfNecessary() {
		if (ALWAYS_RELOAD || !initLoad) {
			reload();
		}
	}

	protected void setEnvFile(String envFile) {
		this.envFile = envFile;
	}

	protected void setHelpDescription(String helpDescription) {
		this.helpDescription = helpDescription;
	}

	protected void setHelpName(String helpName) {
		this.helpName = helpName;
	}

	protected void setValue(IParamDefinition confDef, String stringValue, ConfigSource sourceType, String source) {

		try {
			Object value = instantiateValue(stringValue, confDef);
			PropertyDescriptor descriptor = getDescriptor(confDef);
			Object previous = descriptor.getReadMethod().invoke(this);
			if (previous == null || !previous.equals(value)) {
				logger.info("Setting value '{}' of property '{}' from source {} '{}'", value, confDef.getName(), sourceType, source);
			}

			boolean hold = suspendValidation;
			suspendValidation = true;
			descriptor.getWriteMethod().invoke(this, value);
			suspendValidation = hold;
			for (String env : confDef.getEnvs()) {
				Envio.setEnv(env, stringValue);
			}
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
	}

	protected void validate() {

		if (suspendValidation) {
			return;
		}

		StringBuilder msg = new StringBuilder();

		boolean valid;

		try {
			Set<IParamDefinition> notSetRequiredParams = new LinkedHashSet<IParamDefinition>();
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
			input.put("name", getHelpName());
			input.put("description", getHelpDescription());
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

	private Object instantiateValue(String stringValue, Class<?> type) {
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
		return value;
	}

	private Object instantiateValue(String stringValue, IParamDefinition confDef) throws ClassNotFoundException {
		PropertyDescriptor descriptor = getProperties().get(confDef);
		Class<?> type = descriptor.getPropertyType();
		if (type.isAssignableFrom(List.class)) {
			if (StringUtils.isBlank(stringValue)) {
				return Collections.emptyList();
			}
			String innerTypeName = confDef.getJavaType().replaceFirst("List<", "").replaceFirst(">", "");
			if (!innerTypeName.startsWith("java.lang.")) {
				innerTypeName = "java.lang." + innerTypeName;
			}
			type = Class.forName(innerTypeName);
			List<Object> value = new ArrayList<Object>();
			for (String sv : stringValue.split(multivalueSeparator)) {
				value.add(instantiateValue(sv, type));
			}
			return value;
		}
		return instantiateValue(stringValue, type);
	}

	private boolean isDumpArg(String arg) {
		return arg.startsWith(DUMP_NAME) || arg.startsWith(DUMP_OPTION);
	}

	private boolean isHelpArg(String arg) {
		return arg.startsWith(HELP_NAME) || arg.startsWith(HELP_OPTION);
	}

	private void reloadFromArgs() {

		int iPos = 0;

		for (int i = 0; i < args.length; i++) {

			if (SKIP_ARGS.contains(args[i])) {
				continue;
			}

			String arg = args[i];
			IParamDefinition confDef;
			String source = arg;

			if (PARAM_VALUE_SEPARATOR.matchName(arg)) {
				confDef = byName.get(PARAM_VALUE_SEPARATOR.name(arg));
			} else if (PARAM_VALUE_SEPARATOR.matchOption(arg)) {
				confDef = byOption.get(PARAM_VALUE_SEPARATOR.option(arg));
			} else {
				confDef = byOrder.get(iPos++);
				if (confDef == null) {
					continue;
				}
				setValue(confDef, args[i], ConfigSource.ARG, source);
				continue;
			}

			if (confDef == null) {
				throw new IllegalArgumentException("Invalid argument: " + arg);
			}

			if (PARAM_VALUE_SEPARATOR.getValuePlace() == ParamValueSeparator.ValuePlace.NEXT_ARG && (i + 1) >= args.length) {
				throw new IllegalArgumentException("Missing value for argument: " + arg);
			}
			PropertyDescriptor descriptor = getProperties().get(confDef);
			if (PARAM_VALUE_SEPARATOR.getValuePlace() == ParamValueSeparator.ValuePlace.NEXT_ARG
					&& descriptor.getPropertyType().isAssignableFrom(List.class)) {
				String value = "";
				while ((i + 1) < args.length && !PARAM_VALUE_SEPARATOR.matchName(args[i + 1])
						&& !PARAM_VALUE_SEPARATOR.matchOption(args[i + 1])) {
					if (StringUtils.isNotBlank(value)) {
						value += " ";
					}
					value += args[i + 1];
					i++;
				}
				setValue(confDef, value, ConfigSource.ARG, source);
			} else {
				if (PARAM_VALUE_SEPARATOR.getValuePlace() == ParamValueSeparator.ValuePlace.NEXT_ARG) {
					i++;
				}
				setValue(confDef, PARAM_VALUE_SEPARATOR.getStringValue(args[i]), ConfigSource.ARG, source);
			}
		}

		for (String arg : args) {
			Preconditions.checkNotNull(arg);
			if (isHelpArg(arg)) {
				String msg;
				msg = helpMsg();
				throw new HelpException(msg);
			} else if (isDumpArg(arg)) {
				String msg;
				suspendValidation = true;
				try {
					msg = dump();
				} catch (Exception e) {
					msg = e.getMessage();
				}
				throw new DumpException(msg);
			}
		}
	}

	private void reloadFromEnvVars() {
		for (IParamDefinition confDef : byEnv.values()) {
			for (String env : confDef.getEnvs()) {
				String value = System.getenv(env);
				if (value != null) {
					setValue(confDef, value, ConfigSource.ENV, env);
					break;
				}
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
