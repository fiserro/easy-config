package com.socialbakers.config;

public interface IParamDefinition {

	String getDefaultValue();

	String getDescription();

	String getEnv();

	String getJavaName();

	String getJavaType();

	String getName();

	String getOption();

	Integer getOrder();

	boolean isRequired();
}