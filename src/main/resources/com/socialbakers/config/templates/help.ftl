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

RESOURCES
<#list resources as resource>
	${resource}
</#list>
