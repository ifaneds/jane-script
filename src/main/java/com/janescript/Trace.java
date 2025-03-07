package com.janescript;

import jakarta.xml.bind.annotation.XmlValue;

public class Trace {

	private String value; // The coordinate data

	@XmlValue
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}