package com.socialbakers.config;

public interface IParamDefinition {
	String getEnv();

	String getName();

	String getOption();

	Integer getOrder();

	boolean isRequired();
}