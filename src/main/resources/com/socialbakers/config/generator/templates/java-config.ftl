package ${package};

import com.socialbakers.config.AbstractConfiguration;

/**
 * !!! Generated file - Do not modify it !!!
 */
public <#if abstract>abstract </#if>class ${className} extends AbstractConfiguration {

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
		super(Def.values());
		setArgs(args);
		addConfigFile("/conf/configuration-default.xml");
		addConfigFile("/conf/configuration-site.xml");
		reload();
	}
	
<#list params as param>
	public ${param.javaType} get${param.name?cap_first}() {
		return ${param.name};
	}
	
	public void set${param.name?cap_first}(${param.javaType} ${param.name}) {
		this.${param.name} = ${param.name};
		doValidate();
	}
	
</#list>

	private enum Def implements ConfDef {
	
<#list params as param>
	<#assign name = param.name>
	<#if param.env??><#assign env = '"' + param.env + '"'><#else><#assign env = "null"></#if>
	<#if param.option??><#assign option = '"' + param.option + '"'><#else><#assign option = "null"></#if>
	<#if param.order??><#assign order = param.order><#else><#assign order = "null"></#if>
	<#if param.required><#assign required = "true"><#else><#assign required = "false"></#if>
		${name}(${env}, ${option}, ${order}, ${required}),
</#list>
		;
		
		private final String env;
		private final String option;
		private final Integer order;
		private final boolean required;
		
		private Def(String env, String option, Integer order, boolean required) {
			this.env = env;
			this.option = option;
			this.order = order;
			this.required = required;
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