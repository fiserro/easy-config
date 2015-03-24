package com.socialbakers.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ParamValueSeparator {

	SPACE_SEPARATOR(null, ValuePlace.NEXT_ARG),
	EQUAL_SEPARATOR("=", ValuePlace.SAME_ARG);

	private final String separator;
	private final ParamValueSeparator.ValuePlace valuePlace;
	private String namePattern = AbstractConfiguration.NAME_PREFIX + "(?<name>" + AbstractConfiguration.NAME_PATTERN
			+ ")";
	private String optionPattern = AbstractConfiguration.OPTION_PREFIX + "(?<option>"
			+ AbstractConfiguration.OPTION_PATTERN + ")";
	private String replacePattern = "(" + namePattern + "|" + optionPattern + ")";

	private ParamValueSeparator(String separator, ParamValueSeparator.ValuePlace valuePlace) {
		this.separator = separator;
		this.valuePlace = valuePlace;
		if (separator != null) {
			namePattern += "\\Q" + separator + "\\E";
			optionPattern += "\\Q" + separator + "\\E";
			replacePattern += "\\Q" + separator + "\\E";
		}
	}

	public String getSeparator() {
		return separator;
	}

	public String getStringValue(String arg) {
		if (valuePlace == ValuePlace.SAME_ARG) {
			return arg.replaceFirst(replacePattern, "");
		}
		return arg;
	}

	public ParamValueSeparator.ValuePlace getValuePlace() {
		return valuePlace;
	}

	public boolean matchName(String arg) {
		return getNameMatcher(arg).matches();
	}

	public boolean matchOption(String arg) {
		return getOptionMatcher(arg).matches();
	}

	public String name(String arg) {
		Matcher matcher = getNameMatcher(arg);
		if (matcher.matches()) {
			return matcher.group("name");
		}
		return null;
	}

	public String option(String arg) {
		Matcher matcher = getOptionMatcher(arg);
		if (matcher.matches()) {
			return matcher.group("option");
		}
		return null;
	}

	private Matcher getNameMatcher(String arg) {
		return Pattern.compile(namePattern + valuePlace.patternSuffix).matcher(arg);
	}

	private Matcher getOptionMatcher(String arg) {
		return Pattern.compile(optionPattern + valuePlace.patternSuffix).matcher(arg);
	}

	public enum ValuePlace {
		SAME_ARG(".*"), NEXT_ARG("");

		private final String patternSuffix;

		private ValuePlace(String patternSuffix) {
			this.patternSuffix = patternSuffix;
		}
	}
}