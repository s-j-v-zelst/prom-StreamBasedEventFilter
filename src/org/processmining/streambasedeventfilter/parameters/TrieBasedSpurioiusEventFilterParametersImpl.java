package org.processmining.streambasedeventfilter.parameters;

public class TrieBasedSpurioiusEventFilterParametersImpl extends XSEventFilterParametersImpl {

	public enum InclusionDecisionStrategy {
		DEVIATE_MAX;
	}

	public enum LookAheadInclusionDecisionStrategy {
		DEVIATE_MAX;
	}

	private int emissionDelay = 0;

	private boolean ignoreTrainingCases = true;

	private InclusionDecisionStrategy inclDecStrat;

	private double inclusionDecisionThreshold = 0.05;

	private LookAheadInclusionDecisionStrategy lookAheadInclDec;

	private double lookaheadInclusionDecisionThreshold = 0.25;

	private byte maxLookAhead = 1;

	public int getEmissionDelay() {
		return emissionDelay;
	}

	public InclusionDecisionStrategy getInclusionDecisionStrategy() {
		return inclDecStrat;
	}

	public double getInclusionDecisionThreshold() {
		return inclusionDecisionThreshold;
	}

	public LookAheadInclusionDecisionStrategy getLookAheadInclusionDeccisionStrategy() {
		return lookAheadInclDec;
	}

	public double getLookaheadInclusionDecisionThreshold() {
		return lookaheadInclusionDecisionThreshold;
	}

	public byte getMaxLookAhead() {
		return maxLookAhead;
	}

	public boolean isIgnoreTrainingCases() {
		return ignoreTrainingCases;
	}

	public void setEmissionDelay(int emissionDelay) {
		this.emissionDelay = emissionDelay;
	}

	public void setIgnoreTrainingCases(boolean ignoreTrainingCases) {
		this.ignoreTrainingCases = ignoreTrainingCases;
	}

	public void setInclusionDecisionStrategy(InclusionDecisionStrategy inclDecStrat) {
		this.inclDecStrat = inclDecStrat;
	}

	public void setInclusionDecisionThreshold(double inclusionDecisionThreshold) {
		this.inclusionDecisionThreshold = inclusionDecisionThreshold;
	}

	public void setLookAheadInclusionDecisionStrategy(LookAheadInclusionDecisionStrategy lookAheadInclDec) {
		this.lookAheadInclDec = lookAheadInclDec;
	}

	public void setLookaheadInclusionDecisionThreshold(double lookaheadInclusionDecisionThreshold) {
		this.lookaheadInclusionDecisionThreshold = lookaheadInclusionDecisionThreshold;
	}

	public void setMaxLookAhead(byte maxLookAhead) {
		this.maxLookAhead = maxLookAhead;
	}

}
