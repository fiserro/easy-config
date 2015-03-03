package com.socialbakers.config.generator;

import com.socialbakers.config.AbstractConfiguration;
import com.socialbakers.config.IParamDefinition;

public class ParamDefinition implements IParamDefinition {

	private String name;
	private String option;
	private String env;
	private Integer order;
	private String description;
	private String javaType = "String";
	private String defaultValue;
	private boolean required;

	@Override
	public String getDefaultValue() {
		return defaultValue;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getEnv() {
		return env;
	}

	@Override
	public String getJavaName() {
		return AbstractConfiguration.replaceDots(name);
	}

	@Override
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