package org.processmining.streambasedeventfilter.algorithms.abstr;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.processmining.basicutils.parameters.PluginParameters;
import org.processmining.eventstream.core.interfaces.XSEvent;
import org.processmining.eventstream.models.XSEventHub;
import org.processmining.framework.util.Pair;
import org.processmining.stream.core.abstracts.AbstractXSHub;
import org.processmining.stream.core.abstracts.AbstractXSVisualization;
import org.processmining.stream.core.enums.CommunicationType;
import org.processmining.stream.core.interfaces.XSVisualization;
import org.processmining.streambasedeventfilter.parameters.XSEventFilterParametersImpl;

public class AbstractXSEventFilterImpl<P extends XSEventFilterParametersImpl> extends AbstractXSHub<XSEvent, XSEvent>
		implements XSEventHub {

	private final Set<String> trainingCases = new HashSet<>();

	public Set<String> getTrainingCases() {
		return trainingCases;
	}

	public P getFilterParameters() {
		return filterParameters;
	}

	private long truePositives, falsePositives, trueNegatives, falseNegatives = 0;
	private final P filterParameters;

	public AbstractXSEventFilterImpl(final String name, final P filterParameters) {
		super(name, CommunicationType.SYNC);
		this.filterParameters = filterParameters;
	}

	public XSEvent getCurrentResult() {
		return null;
	}

	public long getFalseNegatives() {
		return falseNegatives;
	}

	public long getFalsePositives() {
		return falsePositives;
	}

	public Class<XSEvent> getTopic() {
		return XSEvent.class;
	}

	public long getTrueNegatives() {
		return trueNegatives;
	}

	public long getTruePositives() {
		return truePositives;
	}

	public XSVisualization<?> getVisualization() {
		return new AbstractXSVisualization<XSEvent>("") {

			public JComponent asComponent() {
				return new JPanel();
			}

			public void update(Pair<Date, String> message) {
			}

			public void update(String object) {
			}

			public void updateVisualization(Pair<Date, XSEvent> newArtifact) {
			}

			public void updateVisualization(XSEvent newArtifact) {
			}

			protected void workPackage() {
				interrupt();
			}
		};
	}

	protected boolean isNoiseAccordingToLabel(final XSEvent event) {
		return event.containsKey(filterParameters.getNoiseClassificationLabelKey())
				&& event.get(filterParameters.getNoiseClassificationLabelKey()).toString()
						.equals(filterParameters.getNoiseClassificationLabelValue());
	}

	private void setFalseNegatives(long falseNegatives) {
		this.falseNegatives = falseNegatives;
	}

	private void setFalsePositives(long falsePositives) {
		this.falsePositives = falsePositives;
	}

	private void setTrueNegatives(long trueNegatives) {
		this.trueNegatives = trueNegatives;
	}

	private void setTruePositives(long truePositives) {
		this.truePositives = truePositives;
	}

	protected XSEvent transform(XSEvent packet) {
		return packet;
	}

	protected void updateExperimentVariables(final XSEvent event, boolean isClassifiedAsNoise) {
		if (isClassifiedAsNoise) {
			if (isNoiseAccordingToLabel(event)) {
				setTruePositives(getTruePositives() + 1);
				if (getFilterParameters().getMessageLevel() == PluginParameters.DEBUG) {
					getFilterParameters().displayMessage("tp: " + event.toString());
				}
			} else {
				setFalsePositives(getFalsePositives() + 1);
				if (getFilterParameters().getMessageLevel() == PluginParameters.DEBUG) {
					getFilterParameters().displayMessage("fp: " + event.toString());
				}
			}
		} else {
			if (!isNoiseAccordingToLabel(event)) {
				setTrueNegatives(getTrueNegatives() + 1);
				if (getFilterParameters().getMessageLevel() == PluginParameters.DEBUG) {
					getFilterParameters().displayMessage("tn: " + event.toString());
				}
			} else {
				setFalseNegatives(getFalseNegatives() + 1);
				if (getFilterParameters().getMessageLevel() == PluginParameters.DEBUG) {
					getFilterParameters().displayMessage("fn: " + event.toString());
				}
			}
		}
		if (getFilterParameters().getMessageLevel() == PluginParameters.DEBUG) {
			System.out.println(getTruePositives() + "\t" + getFalsePositives() + "\t" + getTrueNegatives() + "\t"
					+ getFalseNegatives());
		}
	}

}
