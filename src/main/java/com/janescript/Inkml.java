package com.janescript;

import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

//@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(name = "", propOrder = { "ink" })
@XmlRootElement(name = "ink", namespace = "http://www.w3.org/2003/InkML")
public class Inkml {

	private List<Trace> traces; // Assuming you have a Trace class

	@XmlElement(name = "trace", namespace = "http://www.w3.org/2003/InkML")
	public List<Trace> getTraces() {
		return traces;
	}

	public void setTraces(List<Trace> traces) {
		this.traces = traces;
	}
}