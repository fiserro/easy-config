NAME
	${name}
	
USAGE
	${name}${usage}
	${name}${helpOption}
	${name}${dumpOption}
	
DESCRIPTION
	${description}
	
OPTIONS
	${helpName}, ${helpOption} 
		Print a usage and options description	

	${dumpName}, ${dumpOption} 
		Print an actual state of configuration	
<#list params as param>
	
	${namePrefix}${param.getName()}<#if param.getOption()??>, ${optionPrefix}${param.getOption()}</#if>
		<#if param.getDescription()??>${param.getDescription()}</#if>
</#list>

CONFIG FILES
<#list configFiles as file>
	'${file.getAbsolutePath()}'<#if !file.exists()> - file not found</#if>
</#list>
