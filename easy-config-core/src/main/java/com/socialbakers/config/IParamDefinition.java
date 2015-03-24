package com.socialbakers.config;

public interface IParamDefinition {

	static final String HELP = "help";
	static final String DUMP = "dump";

	String getDefaultValue();

	String getDescription();

	String getEnv();

	String[] getEnvs();

	String getJavaName();

	String getJavaType();

	String getName();

	String getOption();

	Integer getOrder();

	boolean isRequired();
}