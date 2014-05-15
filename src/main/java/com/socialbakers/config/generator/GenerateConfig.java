package com.socialbakers.config.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
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

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

@Mojo(name = "generate-config")
public class GenerateConfig extends AbstractMojo {

	private static final String PACKAGE_CLASS_REGEX = "(([a-z_]+\\.)*)([A-Z_][A-Za-z0-9_]*)";

	@Parameter
	private List<ParamDefinition> params;

	@Parameter(alias = "outputDir")
	private String outputDirName;

	@Parameter
	private String targetClass;

	@Parameter(alias = "abstract")
	private boolean abstr;

	private String className;
	private String packagePath;
	private File javaConfig;
	private File xmlDefaultConfig;
	private File xmlSiteConfig;

	@Override
	public void execute() throws MojoExecutionException {

		validate();

		Matcher matcher = Pattern.compile(PACKAGE_CLASS_REGEX).matcher(targetClass);
		boolean find = matcher.find();
		assert find;

		packagePath = matcher.group(1);
		className = matcher.group(3);

		File outputDir;
		if (outputDirName == null) {
			outputDir = new File("target/generated-sources/");
		} else {
			outputDir = new File(outputDirName);
		}

		javaConfig = new File(outputDir, packagePath.replaceAll("\\.", "/"));
		javaConfig.mkdirs();
		javaConfig = new File(javaConfig, className + ".java");

		outputDir = new File(outputDir, "conf");
		outputDir.mkdirs();
		xmlDefaultConfig = new File(outputDir, "configuration-default.xml");
		xmlSiteConfig = new File(outputDir, "configuration-site.xml");

		if (!packagePath.isEmpty()) {
			packagePath = packagePath.substring(0, packagePath.length() - 1);
		}

		getLog().info("Generating files:");
		getLog().info(javaConfig.getAbsolutePath());
		getLog().info(xmlDefaultConfig.getAbsolutePath());
		getLog().info(xmlSiteConfig.getAbsolutePath());

		try {
			writeFiles();
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private void checkNamesAreSet() {
		for (ParamDefinition p : params) {
			if (StringUtils.isBlank(p.getName())) {
				throw new IllegalStateException("Some parameter hasn't set name.");
			}
		}
	}

	private void checkSubsequence(String s1, String s2, String what) {
		if (StringUtils.isBlank(s1) || StringUtils.isBlank(s1)) {
			return;
		}
		if (s1.startsWith(s2)) {
			throw new IllegalStateException(what + " '" + s1 + "' starts with " + what + " '" + s2 + "'.");
		}
	}

	private void checkSubsequences() {
		for (ParamDefinition p1 : params) {
			for (ParamDefinition p2 : params) {

				if (p1 == p2) {
					continue;
				}

				checkSubsequence(p1.getName(), p2.getName(), "name");
				checkSubsequence(p1.getOption(), p2.getOption(), "option");
				checkSubsequence(p1.getEnv(), p2.getEnv(), "env var");
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

	private void validate() {
		checkNamesAreSet();
		checkUniqueIdentifiers();
		checkSubsequences();
	}

	private void writeFile(Template template, File targetFile, Map<String, Object> input) throws Exception {
		Writer fileWriter = new FileWriter(targetFile);
		try {
			template.process(input, fileWriter);
		} finally {
			fileWriter.close();
		}
	}

	private void writeFiles() throws Exception {

		Configuration cfg = new Configuration();
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		cfg.setClassForTemplateLoading(getClass(), "templates");

		Template javaConfigTemplate = cfg.getTemplate("java-config.ftl");
		Template xmlConfigTemplate = cfg.getTemplate("xml-config.ftl");

		Map<String, Object> input = new HashMap<String, Object>();
		input.put("params", params);
		input.put("package", packagePath);
		input.put("className", className);
		input.put("abstract", abstr);

		writeFile(javaConfigTemplate, javaConfig, input);
		writeFile(xmlConfigTemplate, xmlDefaultConfig, input);
		writeFile(xmlConfigTemplate, xmlSiteConfig, new HashMap<String, Object>());
	}
}