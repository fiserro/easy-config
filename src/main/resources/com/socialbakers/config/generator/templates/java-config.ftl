package ${package};

import com.socialbakers.config.IParamDefinition;
import com.socialbakers.config.generator.ParamValueSeparator;

/**
 * !!! Generated file - Do not modify it !!!
 */
public <#if abstract>abstract </#if>class ${className} extends ${superClass} {

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
	<#if param.javaType=="String" && param.defaultValue?? && param.defaultValue!="null">
		<#assign x0 = '"'>
		<#assign x1 = '"'>
	</#if>
	private ${param.javaType} ${param.getName()}<#if param.defaultValue??> = ${x0}${param.defaultValue}${x1}</#if>;
</#list>
	
	protected ${className}(String helpName, String helpDescription) {
		super(helpName, helpDescription);
	}
	
	public ${className}(String[] args) {
		this("${helpName}", "${helpDescription}");
		addDef(Def.values());
		<#if genXml>addResource("${configFileDefault}");
		addResource("${configFileSite}");</#if>
		setArgs(args);
		reload();
	}
	
<#list params as param>
	public ${param.javaType} get${param.getName()?cap_first}() {
		if (ALWAYS_RELOAD) {
			reload();
		}		
		return ${param.getName()};
	}
	
	public void set${param.getName()?cap_first}(${param.javaType} ${param.getName()}) {
		this.${param.getName()} = ${param.getName()};
		if (!suspendValidation) {
			validate();
		}
	}
	
</#list>

	public enum Def implements IParamDefinition {
	
<#list params as param>
	<#assign name = param.getName()>
	<#if param.description??><#assign desc = '"' + param.description + '"'><#else><#assign desc = "null"></#if>
	<#if param.env??><#assign env = '"' + param.env + '"'><#else><#assign env = "null"></#if>
	<#if param.option??><#assign option = '"' + param.option + '"'><#else><#assign option = "null"></#if>
	<#if param.order??><#assign order = param.order><#else><#assign order = "null"></#if>
	<#if param.required><#assign required = "true"><#else><#assign required = "false"></#if>
	<#if param.defaultValue??><#assign defaultValue = '"'+param.defaultValue+'"'><#else><#assign defaultValue = "null"></#if>
		${name}(${desc}, ${env}, ${option}, ${order}, ${required}, ${defaultValue}, "${param.javaType}"),
</#list>
		;
		
		private final String desc;
		private final String env;
		private final String option;
		private final Integer order;
		private final boolean required;
		private final String defaultValue;
		private final String javaType;
		
		private Def(String desc, String env, String option, Integer order, boolean required, String defaultValue, 
				String javaType) {
			this.desc = desc;
			this.env = env;
			this.option = option;
			this.order = order;
			this.required = required;
			this.defaultValue = defaultValue;
			this.javaType = javaType;
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
		
		@Override
		public String getDefaultValue() {
			return defaultValue;
		}
	
		@Override
		public String getJavaType() {
			return javaType;
		}
	}	

}