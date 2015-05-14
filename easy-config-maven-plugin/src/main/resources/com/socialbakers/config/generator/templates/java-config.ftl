package ${package};

<#list imports as i>
import ${i};
</#list>

/**
 * !!! Generated file - Do not modify it !!!
 */
public <#if abstract>abstract </#if>class ${className} extends ${superClass} {

	static {
		setArgFormatIfItsEmpty(ParamValueSeparator.${paramValueSeparator.name()});
		setConfDirEnvNameIfItsEmpty("${confDirEnv}");
	}

<#list params as param>
	public static final IParamDefinition _${param.getJavaName()?upper_case} = Def.${param.getJavaName()};
</#list>

<#list params as param>
	<#assign x0 = "">
	<#assign x1 = "">
	<#if param.javaType=="String" && param.defaultValue?? && param.defaultValue!="null">
		<#assign x0 = '"'>
		<#assign x1 = '"'>
	</#if>
	private ${param.javaType} ${param.getJavaName()}<#if param.defaultValue??> = ${x0}${param.defaultValue}${x1}</#if>;
</#list>
	
<#if abstract == false>
	public static ${className} create(String... args) {
		${className} conf = new ${className}(args);
		conf.reload();
		return conf;
	}
	
	/**
	 * This constructor is for internal and inherit use only.
	 * @deprecated Do not use this constructor. Use {@link ${className}#create(String...)} instead.
	 */
	@Deprecated
<#else>
  /**
   * You should call a {@link ${className}#reload()} method after.
   */
</#if>
	protected ${className}(String[] args) {
		super(args);
		setHelpName("${helpName}");
		setHelpDescription("${helpDescription}");
		setEnvFile("${envFile}");
		<#if genXml>addResource("${configFileDefault}");
		addResource("${configFileSite}");</#if>
	}
	
	@Override
	protected Collection<IParamDefinition> knownParams() {
		return ParamDefinition.merge(Arrays.<IParamDefinition> asList(Def.values()), super.knownParams());
	}
	
<#list params as param>
	public ${param.javaType} get${param.getJavaName()?cap_first}() {
		if (ALWAYS_RELOAD) {
			reload();
		}
		return ${param.getJavaName()};
	}
	
	public void set${param.getJavaName()?cap_first}(${param.javaType} ${param.getJavaName()}) {
		this.${param.getJavaName()} = ${param.getJavaName()};
		if (!suspendValidation) {
			validate();
		}
	}
	
</#list>

	public static enum Def implements IParamDefinition {
	
<#list params as param>
	<#assign name = param.getJavaName()>
	<#if param.description??><#assign desc = '"' + param.description + '"'><#else><#assign desc = "null"></#if>
	<#assign desc = desc?replace("\\s+", " ", 'r')>
	<#assign desc = desc?replace("\\n|\\r\\n", "", 'r')>
	<#if param.env??><#assign env = '"' + param.env + '"'><#else><#assign env = "null"></#if>
	<#if param.option??><#assign option = '"' + param.option + '"'><#else><#assign option = "null"></#if>
	<#if param.order??><#assign order = param.order><#else><#assign order = "null"></#if>
	<#if param.required><#assign required = "true"><#else><#assign required = "false"></#if>
	<#if param.defaultValue??><#assign defaultValue = '"'+param.defaultValue+'"'><#else><#assign defaultValue = "null"></#if>
		${name}(${desc}, ${env}, ${option}, ${order}, ${required}, ${defaultValue}, "${param.javaType}", "${param.getName()}"),
</#list>
		;
		
		private final String desc;
		private final String env;
		private final String option;
		private final Integer order;
		private final boolean required;
		private final String defaultValue;
		private final String javaType;
		private final String paramName;
		
		private Def(String desc, String env, String option, Integer order, boolean required, String defaultValue, 
				String javaType, String paramName) {
			this.desc = desc;
			this.env = env;
			this.option = option;
			this.order = order;
			this.required = required;
			this.defaultValue = defaultValue;
			this.javaType = javaType;
			this.paramName = paramName;
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
		public String[] getEnvs() {
			return com.socialbakers.config.Envio.splitEnvNames(env);
		}

		@Override
		public String getJavaName() {
			return name();
		}

		@Override
		public String getName() {
			return paramName;
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

    @Override
    public boolean hasMoreEnvs() {
      return getEnvs().length > 1;
    }
	}

}