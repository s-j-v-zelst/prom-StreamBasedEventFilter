package org.processmining.streambasedeventfilter.parameters;

public class ConditionalProbabilitiesBasedXSEventFilterParametersImpl extends XSEventFilterParametersImpl {

	private int maxPatternLength = 3;

	private double cutoffThreshold = 0.01;

	public int getMaxPatternLength() {
		return maxPatternLength;
	}

	public void setMaxPatternLength(int maxPatternLength) {
		this.maxPatternLength = maxPatternLength;
	}

	public double getCutoffThreshold() {
		return cutoffThreshold;
	}

	public void setCutoffThreshold(double cutoffThreshold) {
		this.cutoffThreshold = cutoffThreshold;
	}

}
