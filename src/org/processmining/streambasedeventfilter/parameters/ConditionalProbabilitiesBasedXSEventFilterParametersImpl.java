package org.processmining.streambasedeventfilter.parameters;

public class ConditionalProbabilitiesBasedXSEventFilterParametersImpl extends XSEventFilterParametersImpl {

	public enum FilteringMethod {
		BACKWARD, FORWARD, BOTH_DIRECTIONS, ANY;
	}

	public enum AdjustmentMethod {
		NONE, MAX, MAX_NZ_AVG;
	}

	public enum Abstraction {
		SEQUENCE, MULTISET, SET;
	}

	private int maxPatternLength = 3;
	private FilteringMethod filtermethod = FilteringMethod.BOTH_DIRECTIONS;
	private AdjustmentMethod adjustmentmethod = AdjustmentMethod.MAX_NZ_AVG;
	private Abstraction abstraction = Abstraction.MULTISET;

	public Abstraction getAbstraction() {
		return abstraction;
	}

	public void setAbstraction(Abstraction abstraction) {
		this.abstraction = abstraction;
	}

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

	public FilteringMethod getFiltermethod() {
		return filtermethod;
	}

	public void setFiltermethod(FilteringMethod filtermethod) {
		this.filtermethod = filtermethod;
	}

	public AdjustmentMethod getAdjustmentmethod() {
		return adjustmentmethod;
	}

	public void setAdjustmentmethod(AdjustmentMethod adjustmentmethod) {
		this.adjustmentmethod = adjustmentmethod;
	}

}
