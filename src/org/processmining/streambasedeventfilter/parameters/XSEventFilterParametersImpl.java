package org.processmining.streambasedeventfilter.parameters;

import org.processmining.basicutils.parameters.impl.PluginParametersImpl;

public class XSEventFilterParametersImpl extends PluginParametersImpl {
	private String classificationNoiseLabelKey = "xsevent:data:noise";

	private String classificationNoiseLabelValue = "true";

	private boolean isExperiment = true;

	public String getNoiseClassificationLabelKey() {
		return classificationNoiseLabelKey;
	}

	public String getNoiseClassificationLabelValue() {
		return classificationNoiseLabelValue;
	}

	public void setNoiseClassificationLabelKey(final String key) {
		this.classificationNoiseLabelKey = key;
	}

	public void setNoiseClassificationLabelValue(final String value) {
		this.classificationNoiseLabelValue = value;
	}

	public boolean isExperiment() {
		return isExperiment;
	}

	public void setExperiment(boolean isExperiment) {
		this.isExperiment = isExperiment;
	}

}
