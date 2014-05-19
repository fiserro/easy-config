package com.socialbakers.config;

public interface IParamDefinition {
	String getDescription();

	String getEnv();

	String getName();

	String getOption();

	Integer getOrder();

	boolean isRequired();
}