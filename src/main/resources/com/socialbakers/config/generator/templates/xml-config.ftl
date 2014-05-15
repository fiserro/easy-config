<?xml version="1.0"?>
<configuration>
<#if params??>
<#list params as param><#if param.defaultValue??>
	<property>
		<name>${param.name}</name>
		<value>${param.defaultValue}</value>
		<#if param.description??><description>${param.description}</description></#if>
	</property>
	
</#if>
</#list>
</#if>
</configuration>