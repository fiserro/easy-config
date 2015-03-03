package com.socialbakers.config.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jdt.internal.core.Assert;

import com.socialbakers.config.AbstractConfiguration;
import com.socialbakers.config.AbstractConfiguration.ParamValueSeparator;
import com.socialbakers.config.IParamDefinition;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

/**
 * @author <a href="mailto:robert.fiser@socialbakers.com">Robert Fišer</a>
 */
@Mojo(name = "generate-config")
public class GenerateConfig extends AbstractMojo {

	private static final String PACKAGE_CLASS_REGEX = "(([a-z_]+\\.)*)([A-Z_][A-Za-z0-9_]*)";

	public static final String HELP = "help";
	public static final String DUMP = "dump";

	@Parameter
	private List<ParamDefinition> params;

	private Map<String, IParamDefinition> iParams = new LinkedHashMap<String, IParamDefinition>();

	@Parameter(alias = "outputDir")
	private String outputDirName;

	@Parameter
	private String configClass;

	@Parameter
	private String envFile = ".env";

	@Parameter
	private String configFileDefault = "configuration-default.xml";

	@Parameter
	private String configFileSite = "configuration-site.xml";

	@Parameter
	private String helpName = "";

	@Parameter
	private String helpDescription = "";

	@Parameter(alias = "abstract")
	private boolean abstr;

	@Parameter
	private ParamValueSeparator paramValueSeparator = ParamValueSeparator.SPACE_SEPARATOR;

	@Parameter
	private boolean alwaysReload;

	@Parameter
	private String confDirEnv = "conf/";

	@Parameter
	private String superClass = "com.socialbakers.config.AbstractConfiguration";

	@Parameter
	private boolean genEnv = false;

	@Parameter
	private boolean genXml = true;
	private String className;
	private String packagePath;
	private File javaConfig;
	private File envConfig;
	private File xmlDefaultConfig;
	private File xmlSiteConfig;

	private File outputDir;

	private static final String START_WITH_NUMBER = "[0-9].*";

	@Override
	public void execute() throws MojoExecutionException {

		getLog().info(this.toString());

		try {
			for (IParamDefinition param : params) {
				getLog().info("Adding param: " + param.getName());
				iParams.put(param.getName(), param);
			}
			validate();

			Matcher matcher = Pattern.compile(PACKAGE_CLASS_REGEX).matcher(configClass);
			boolean find = matcher.find();
			Assert.isTrue(find);

			packagePath = matcher.group(1);
			className = matcher.group(3);

			if (outputDirName == null) {
				outputDir = new File("target/generated-sources/");
			} else {
				outputDir = new File(outputDirName);
			}

			if (!packagePath.isEmpty()) {
				packagePath = packagePath.substring(0, packagePath.length() - 1);
			}

			Configuration cfg = new Configuration();
			cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
			cfg.setClassForTemplateLoading(getClass(), "templates");

			loadSuperParams();

			validate();

			Map<String, Object> input = new HashMap<String, Object>();
			input.put("params", iParams.values());
			input.put("package", packagePath);
			input.put("className", className);
			input.put("abstract", abstr);
			input.put("configFileDefault", configFileDefault);
			input.put("configFileSite", configFileSite);
			input.put("helpName", helpName);
			input.put("helpDescription", helpDescription);
			input.put("paramValueSeparator", paramValueSeparator);
			input.put("alwaysReload", Boolean.valueOf(alwaysReload).toString());
			input.put("confDirEnv", confDirEnv);
			input.put("superClass", superClass);
			input.put("genXml", genXml);
			input.put("genEnv", genEnv);

			javaConfig = new File(outputDir, packagePath.replaceAll("\\.", "/"));
			javaConfig.mkdirs();
			javaConfig = new File(javaConfig, className + ".java");
			getLog().info("Generating files:");
			getLog().info(javaConfig.getAbsolutePath());
			Template javaConfigTemplate = cfg.getTemplate("java-config.ftl");
			writeFile(javaConfigTemplate, javaConfig, input);

			if (genXml) {
				xmlDefaultConfig = new File(outputDir, configFileDefault);
				xmlSiteConfig = new File(outputDir, configFileSite);
				getLog().info(xmlDefaultConfig.getAbsolutePath());
				getLog().info(xmlSiteConfig.getAbsolutePath());
				Template xmlConfigTemplate = cfg.getTemplate("xml-config.ftl");
				writeFile(xmlConfigTemplate, xmlDefaultConfig, input);
				writeFile(xmlConfigTemplate, xmlSiteConfig, new HashMap<String, Object>());
			}

			if (genEnv) {
				envConfig = new File(outputDir, envFile);
				getLog().info(envConfig.getAbsolutePath());
				Template envConfigTemplate = cfg.getTemplate("env-config.ftl");
				writeFile(envConfigTemplate, envConfig, input);
			}

		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private void checkConflict(String s1, String s2, String what) {
		if (StringUtils.isBlank(s1) || StringUtils.isBlank(s2)) {
			return;
		}
		if (AbstractConfiguration.replaceDots(s1).equals(AbstractConfiguration.replaceDots(s2))) {
			throw new IllegalStateException(what + " '" + s1 + "' is in conflict with " + what + " '" + s2 + "'.");
		}
	}

	private void checkConflicts() {
		for (IParamDefinition p1 : iParams.values()) {
			for (IParamDefinition p2 : iParams.values()) {
				if (p1 == p2) {
					continue;
				}
				checkConflict(p1.getName(), p2.getName(), "name");
				checkConflict(p1.getOption(), p2.getOption(), "option");
				checkConflict(p1.getEnv(), p2.getEnv(), "env var");
			}
		}
	}

	private void checkNamesAreSet() {
		for (IParamDefinition p : iParams.values()) {
			if (StringUtils.isBlank(p.getName())) {
				throw new IllegalStateException("Some parameter hasn't set name.");
			} else if (!p.getName().matches(AbstractConfiguration.NAME_PATTERN)) {
				throw new IllegalStateException("Illegal parameter name: " + p.getName());
			} else if (p.getName().matches(START_WITH_NUMBER)) {
				throw new IllegalStateException("Illegal parameter name: " + p.getName());
			}
			if (StringUtils.isNotBlank(p.getOption()) && !p.getOption().matches(AbstractConfiguration.OPTION_PATTERN)) {
				throw new IllegalStateException("Illegal parameter(" + p.getName() + ") option: " + p.getOption());
			}
		}
	}

	private <T> void checkUnique(T value, String what, Set<T> set) {
		if (value == null) {
			return;
		}
		if (set.contains(value)) {
			throw new IllegalStateException("Multiple usage of " + what + ": " + value);
		}
		if (HELP.equals(value)) {
			throw new IllegalStateException(HELP + " is reserved word and cannot be used for " + what);
		}
		if (DUMP.equals(value)) {
			throw new IllegalStateException(DUMP + " is reserved word and cannot be used for " + what);
		}
		set.add(value);
	}

	private void checkUniqueIdentifiers() {
		Set<String> names = new HashSet<String>();
		Set<String> envVars = new HashSet<String>();
		Set<String> options = new HashSet<String>();
		Set<Integer> orders = new HashSet<Integer>();

		for (ParamDefinition p : params) {
			checkUnique(p.getName(), "name", names);
			checkUnique(p.getEnv(), "env. var.", envVars);
			checkUnique(p.getOption(), "option", options);
			checkUnique(p.getOrder(), "order", orders);
		}
	}

	private void loadSuperParams() {
		try {
			recLoadSuperParams(Class.forName(superClass));
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	private void recLoadSuperParams(Class<?> configSuperClass) {
		if (com.socialbakers.config.AbstractConfiguration.class == configSuperClass || Object.class == configSuperClass) {
			return;
		}
		for (Class<?> class1 : configSuperClass.getDeclaredClasses()) {
			if (!class1.isEnum()) {
				continue;
			}
			if (!Arrays.asList(class1.getInterfaces()).contains(IParamDefinition.class)) {
				continue;
			}
			for (Object object : class1.getEnumConstants()) {
				IParamDefinition param = (IParamDefinition) object;
				if (!iParams.containsKey(param.getName())) {
					getLog().info("Addind param from superClass:" + class1 + "." + param.getName());
					iParams.put(param.getName(), param);
				}
			}
		}
		recLoadSuperParams(configSuperClass.getSuperclass());
	}

	private void validate() {
		checkNamesAreSet();
		checkUniqueIdentifiers();
		checkConflicts();
	}

	private void writeFile(Template template, File targetFile, Map<String, Object> input) throws Exception {
		Writer fileWriter = new FileWriter(targetFile);
		try {
			template.process(input, fileWriter);
		} finally {
			fileWriter.close();
		}
	}

}