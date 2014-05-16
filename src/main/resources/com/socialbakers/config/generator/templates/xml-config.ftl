<?xml version="1.0"?>
<configuration>
<#if params??>
<#list params as param>
	<#if !param.defaultValue??><!--</#if><property>
		<name>${param.name}</name>
		<value><#if param.defaultValue??>${param.defaultValue}</#if></value>
		<description><#if param.description??>${param.description}</#if></description>
	</property><#if !param.defaultValue??>--></#if>
	
</#list>
</#if>
</configuration>