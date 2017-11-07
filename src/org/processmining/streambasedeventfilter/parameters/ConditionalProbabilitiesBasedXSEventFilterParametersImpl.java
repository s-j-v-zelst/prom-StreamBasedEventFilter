package org.processmining.streambasedeventfilter.parameters;

public class ConditionalProbabilitiesBasedXSEventFilterParametersImpl extends XSEventFilterParametersImpl {

	private int maxPatternLength = 3;
	private FilterMethod filtermethod= FilterMethod.Forward;
	private AdjustmentDistribution adjustdistribute = AdjustmentDistribution.MaxNZAbg;
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

	public FilterMethod getFiltermethod() {
		return filtermethod;
	}

	public void setFiltermethod(FilterMethod filtermethod) {
		this.filtermethod = filtermethod;
	}

	public AdjustmentDistribution getAdjustdistribute() {
		return adjustdistribute;
	}

	public void setAdjustdistribute(AdjustmentDistribution adjustdistribute) {
		this.adjustdistribute = adjustdistribute;
	}

}
