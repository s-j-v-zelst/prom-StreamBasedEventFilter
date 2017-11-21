package org.processmining.streambasedeventfilter.parameters;

import org.processmining.basicutils.parameters.impl.PluginParametersImpl;

public class XSEventFilterParametersImpl extends PluginParametersImpl {

	private String classificationNoiseLabelKey = "xsevent:data:noise";

	private String classificationNoiseLabelValue = "true";

	private boolean contextAware = true;

	private boolean ignoreTrainingCases = true;

	private boolean isExperiment = true;

	private long delay = 0l;

	public String getNoiseClassificationLabelKey() {
		return classificationNoiseLabelKey;
	}

	public String getNoiseClassificationLabelValue() {
		return classificationNoiseLabelValue;
	}

	public boolean isContextAware() {
		return contextAware;
	}

	public boolean isExperiment() {
		return isExperiment;
	}

	public long getDelay() {
		return delay;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public boolean isIgnoreTrainingCases() {
		return ignoreTrainingCases;
	}

	public void setContextAware(boolean contextAware) {
		this.contextAware = contextAware;
	}

	public void setExperiment(boolean isExperiment) {
		this.isExperiment = isExperiment;
	}

	public void setIgnoreTrainingCases(boolean ignoreTrainingCases) {
		this.ignoreTrainingCases = ignoreTrainingCases;
	}

	public void setNoiseClassificationLabelKey(final String key) {
		this.classificationNoiseLabelKey = key;
	}

	public void setNoiseClassificationLabelValue(final String value) {
		this.classificationNoiseLabelValue = value;
	}

}
