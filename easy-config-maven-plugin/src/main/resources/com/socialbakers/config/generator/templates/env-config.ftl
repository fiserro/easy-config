<#if params??><#list params as param>
<#if param.env??>${param.env}=<#if param.defaultValue?? && param.defaultValue!="null">${param.defaultValue}</#if></#if>
</#list></#if>