package com.socialbakers.config.generator;

import com.socialbakers.config.AbstractConfiguration;
import com.socialbakers.config.Envio;
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
	public String[] getEnvs() {
		return Envio.splitEnvNames(env);
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

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setEnv(String env) {
		this.env = env;
	}

	public void setJavaType(String javaType) {
		this.javaType = javaType;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setOption(String option) {
		this.option = option;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

}