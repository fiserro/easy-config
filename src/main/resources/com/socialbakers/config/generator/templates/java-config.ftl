package ${package};

import com.socialbakers.config.IParamDefinition;
import com.socialbakers.config.AbstractConfiguration;
import com.socialbakers.config.generator.ParamValueSeparator;

/**
 * !!! Generated file - Do not modify it !!!
 */
public <#if abstract>abstract </#if>class ${className} extends AbstractConfiguration {

	static {
		if (CONF_DIR_ENV == null) {
			CONF_DIR_ENV = "${confDirEnv}";
		}
		if (PARAM_VALUE_SEPARATOR == null) {
			PARAM_VALUE_SEPARATOR = ParamValueSeparator.${paramValueSeparator.name()};
		}
		ALWAYS_RELOAD = ${alwaysReload};
	}

<#list params as param>
	<#assign x0 = "">
	<#assign x1 = "">
	<#if param.javaType=="String">
		<#assign x0 = '"'>
		<#assign x1 = '"'>
	</#if>
	private ${param.javaType} ${param.name}<#if param.defaultValue??> = ${x0}${param.defaultValue}${x1}</#if>;
</#list>

	public ${className}(String... args) {
		super(Def.values(), "${helpName}", "${helpDescription}");
		addResource("${configFileDefault}");
		addResource("${configFileSite}");
		setArgs(args);
		reload();
	}
	
<#list params as param>
	public ${param.javaType} get${param.name?cap_first}() {
		if (ALWAYS_RELOAD) {
			reload();
		}		
		return ${param.name};
	}
	
	public void set${param.name?cap_first}(${param.javaType} ${param.name}) {
		this.${param.name} = ${param.name};
		if (!suspendValidation) {
			validate();
		}
	}
	
</#list>

	private enum Def implements IParamDefinition {
	
<#list params as param>
	<#assign name = param.name>
	<#if param.description??><#assign desc = '"' + param.description + '"'><#else><#assign desc = "null"></#if>
	<#if param.env??><#assign env = '"' + param.env + '"'><#else><#assign env = "null"></#if>
	<#if param.option??><#assign option = '"' + param.option + '"'><#else><#assign option = "null"></#if>
	<#if param.order??><#assign order = param.order><#else><#assign order = "null"></#if>
	<#if param.required><#assign required = "true"><#else><#assign required = "false"></#if>
		${name}(${desc}, ${env}, ${option}, ${order}, ${required}),
</#list>
		;
		
		private final String desc;
		private final String env;
		private final String option;
		private final Integer order;
		private final boolean required;
		
		private Def(String desc, String env, String option, Integer order, boolean required) {
			this.desc = desc;
			this.env = env;
			this.option = option;
			this.order = order;
			this.required = required;
		}
		
		@Override
		public String getDescription() {
			return desc;
		}
		
		@Override
		public String getEnv() {
			return env;
		}

		@Override
		public String getName() {
			return name();
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

}