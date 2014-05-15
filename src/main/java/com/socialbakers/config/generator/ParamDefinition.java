package com.socialbakers.config.generator;

import com.socialbakers.config.IParamDefinition;

public class ParamDefinition implements IParamDefinition {

	private String name;
	private String option;
	private String env;
	private Integer order;
	private String description;
	private String javaType;
	private String defaultValue;
	private boolean required;

	public String getDefaultValue() {
		return defaultValue;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String getEnv() {
		return env;
	}

	public String getJavaType() {
		return javaType;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getOption() {
		return option;
	}

	@Override
	public Integer getOrder() {
		return order;
	}

	@Override
	public boolean isRequired() {
		return required;
	}

}