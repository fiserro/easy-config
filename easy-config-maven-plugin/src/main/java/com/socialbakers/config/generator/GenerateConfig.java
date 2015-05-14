package com.socialbakers.config.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.ToolProvider;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.google.common.base.Preconditions;
import com.socialbakers.config.AbstractConfiguration;
import com.socialbakers.config.IParamDefinition;
import com.socialbakers.config.ParamDefinition;
import com.socialbakers.config.ParamValueSeparator;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

/**
 * @author <a href="mailto:robert.fiser@socialbakers.com">Robert Fišer</a>
 */
@Mojo(name = "generate-config")
public class GenerateConfig extends AbstractMojo {

	private static final String PACKAGE_CLASS_REGEX = "(([a-z_]+\\.)*)([A-Z_][A-Za-z0-9_]*)";

	@Parameter(required = true)
	private List<ParamDefinition> params;

	@Parameter(alias = "outputDir")
	private String outputDirName;

	@Parameter(required = true)
	private String configClass;

	@Parameter(defaultValue = ".env")
	private String envFile = ".env";

	@Parameter(defaultValue = "configuration-default.xml")
	private String configFileDefault = "configuration-default.xml";

	@Parameter(defaultValue = "configuration-site.xml")
	private String configFileSite = "configuration-site.xml";

	@Parameter(alias = "name", defaultValue = "<WARNING: Name is not set!>")
	private String helpName = "<WARNING: Name is not set!>";

	@Parameter(alias = "description", defaultValue = "<WARNING: Description is not set!>")
	private String helpDescription = "<WARNING: Description is not set!>";

	@Parameter(alias = "abstract", defaultValue = "false")
	private boolean abstr;

	@Parameter(defaultValue = "SPACE_SEPARATOR")
	private ParamValueSeparator paramValueSeparator = ParamValueSeparator.SPACE_SEPARATOR;

	@Parameter(defaultValue = "false")
	private boolean alwaysReload;

	@Parameter(defaultValue = "CONF_DIR")
	private String confDirEnv = "CONF_DIR";

	@Parameter(defaultValue = "com.socialbakers.config.AbstractConfiguration")
	private String superClass = "com.socialbakers.config.AbstractConfiguration";

	@Parameter(defaultValue = "false")
	private boolean genEnv = false;

	@Parameter(defaultValue = "false")
	private boolean genXml = false;

	private Map<String, IParamDefinition> iParams = new LinkedHashMap<String, IParamDefinition>();
	private String className;
	private String packagePath;
	private File javaConfig;
	private File envConfig;
	private File xmlDefaultConfig;
	private File xmlSiteConfig;
	private File outputDir;

	private static final String START_WITH_NUMBER = "[0-9].*";

	private static final Set<String> RESERVED_WORDS = new HashSet<String>(Arrays.asList(IParamDefinition.HELP,
			IParamDefinition.DUMP, "desc", "env", "envs",
			"option", "order", "required", "defaultValue", "javaType", "paramName", "helpName", "helpDescription"));

	private static String LIST_JAVA_TYPE_PATTERN = "List\\([A-Z][a-z]+\\)";

	@Override
	public void execute() throws MojoExecutionException {

		getLog().info(this.toString());

		try {
			Set<String> imports = new LinkedHashSet<String>();
			imports.add("java.util.Collection");
			imports.add("java.util.Arrays");
			imports.add("com.socialbakers.config.IParamDefinition");
			imports.add("com.socialbakers.config.ParamValueSeparator");
			imports.add("com.socialbakers.config.ParamDefinition");
			for (ParamDefinition param : params) {
				getLog().info("Adding param: " + param.getName());
				iParams.put(param.getName(), param);
				if (param.getJavaType().matches(LIST_JAVA_TYPE_PATTERN)) {
					imports.add("java.util.List");
					param.setJavaType(param.getJavaType().replaceFirst("\\(", "<").replaceFirst("\\)", ">"));
				}
			}
			validate();

			Matcher matcher = Pattern.compile(PACKAGE_CLASS_REGEX).matcher(configClass);
			boolean find = matcher.find();
			Preconditions.checkState(find);

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
			input.put("imports", imports);
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
			input.put("envFile", envFile);

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
				for (String env1 : p1.getEnvs()) {
					for (String env2 : p2.getEnvs()) {
						checkConflict(env1, env2, "env var");
					}
				}
			}
		}
	}

	private void checkMultivalPositional() {
		boolean hasMultival = false;
		boolean hasPositional = false;
		for (IParamDefinition p : iParams.values()) {
			hasMultival |= p.getJavaName().matches(LIST_JAVA_TYPE_PATTERN);
			hasPositional |= p.getOrder() != null;
		}
		if (hasMultival && hasPositional) {
			throw new IllegalStateException("Cannot use multival and positional parametter in the same config");
		}
		if (hasMultival && paramValueSeparator != ParamValueSeparator.SPACE_SEPARATOR) {
			throw new IllegalStateException("Cannot use multival without space-value-separator");
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
		if (RESERVED_WORDS.contains(value)) {
			throw new IllegalStateException(value + " is reserved word and cannot be used for " + what);
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
			for (String env : p.getEnvs()) {
				checkUnique(env, "env. var.", envVars);
			}
			checkUnique(p.getOption(), "option", options);
			checkUnique(p.getOrder(), "order", orders);
		}
	}

	private URLClassLoader createOutputDirClassLoader() throws MalformedURLException {
		return URLClassLoader.newInstance(new URL[] { outputDir.toURI().toURL() }, getCurrentClassLoader());
	}

	private ClassLoader getCurrentClassLoader() {
		return getClass().getClassLoader();
	}

	private String getCurrentClassPath() {
		StringBuilder classpath = new StringBuilder();
		for (URL url : ((URLClassLoader) getCurrentClassLoader()).getURLs()) {
			classpath.append(url.getFile());
			classpath.append(":");
		}
		classpath.append(".");
		return classpath.toString();
	}

	private File getSuperClassFile() {
		String superPackage = superClass.substring(0, superClass.lastIndexOf('.'));
		String superClassName = superClass.substring(superClass.lastIndexOf('.') + 1, superClass.length());
		File dir = new File(outputDir, superPackage.replaceAll("\\.", "/"));
		File file = new File(dir, superClassName + ".java");
		return file;
	}

	private String getSuperClassFilePath() {
		return getSuperClassFile().getPath();
	}

	private void loadSuperParams() {
		try {
			recLoadSuperParams(Class.forName(superClass));
		} catch (ClassNotFoundException e) {
			if (getSuperClassFile().exists()) {
				try {
					ToolProvider.getSystemJavaCompiler().run(null, null, null, "-cp", getCurrentClassPath(), getSuperClassFilePath());
				} catch (Exception e1) {
					throw new IllegalStateException("Can't compile '" + getSuperClassFile()
							+ "' make suer you're using JDK instead of JRE. Or some dependencies maybe missing. " + e1.getMessage(), e1);
				}
				try {
					recLoadSuperParams(Class.forName(superClass, true, createOutputDirClassLoader()));
				} catch (Exception e1) {
					throw new IllegalStateException(e1);
				}
			} else {
				throw new IllegalStateException("'" + getSuperClassFilePath() + "' does not exists.", e);
			}
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
				} else {
					getLog().info("Merging param from superClass:" + class1 + "." + param.getName());
					iParams.put(param.getName(), ParamDefinition.merge(iParams.get(param.getName()), param));
				}
			}
		}
		recLoadSuperParams(configSuperClass.getSuperclass());
	}

	private void validate() {
		checkMultivalPositional();
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