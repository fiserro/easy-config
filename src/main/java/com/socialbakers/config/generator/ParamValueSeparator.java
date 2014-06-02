package com.socialbakers.config.generator;

public enum ParamValueSeparator {

	NO_SEPARATOR(""), SPACE_SEPARATOR(" "), EQUAL_SEPARATOR("="), ;

	private String separator;

	private ParamValueSeparator(String separator) {
		this.separator = separator;
	}

	public String getSeparator() {
		return separator;
	}

}
