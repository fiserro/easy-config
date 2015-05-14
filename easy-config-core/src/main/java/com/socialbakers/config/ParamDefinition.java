package com.socialbakers.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class ParamDefinition implements IParamDefinition {

	public static List<IParamDefinition> merge(Collection<IParamDefinition> p1, Collection<IParamDefinition> p2) {
		Map<String, IParamDefinition> byNames = new HashMap<String, IParamDefinition>();
		for (IParamDefinition p : p2) {
			byNames.put(p.getName(), p);
		}
		List<IParamDefinition> result = new ArrayList<IParamDefinition>();
		for (IParamDefinition p : p1) {
			if (byNames.containsKey(p.getName())) {
				result.add(merge(p, byNames.get(p.getName())));
			} else {
				result.add(p);
			}
		}
		return result;
	}

	public static IParamDefinition merge(IParamDefinition p1, IParamDefinition p2) {
		ParamDefinition paramDefinition = new ParamDefinition();
		paramDefinition.setName(preferFirstValue(p1.getName(), p2.getName()));
		paramDefinition.setOption(preferFirstValue(p1.getOption(), p2.getOption()));
		paramDefinition.setEnv(preferFirstValue(p1.getEnv(), p2.getEnv()));
		paramDefinition.setOrder(preferFirstValue(p1.getOrder(), p2.getOrder()));
		paramDefinition.setDescription(preferFirstValue(p1.getDescription(), p2.getDescription()));
		paramDefinition.setJavaType(preferFirstValue(p1.getJavaType(), p2.getJavaType()));
		paramDefinition.setDefaultValue(preferFirstValue(p1.getDefaultValue(), p2.getDefaultValue()));
		paramDefinition.setRequired(preferFirstValue(p1.isRequired(), p2.isRequired()));
		return paramDefinition;
	}

	private static <T> T preferFirstValue(T v1, T v2) {
		if (v1 == null && v2 == null) {
			return null;
		} else if (v1 == null) {
			return v2;
		} else if (v2 == null) {
			return v1;
		} else if (v1 instanceof String) {
			if (StringUtils.isNotBlank((CharSequence) v1)) {
				return v1;
			}
			if (StringUtils.isNotBlank((CharSequence) v2)) {
				return v2;
			}
		}
		return v1;
	}

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
	public boolean hasMoreEnvs() {
		return getEnvs().length > 1;
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