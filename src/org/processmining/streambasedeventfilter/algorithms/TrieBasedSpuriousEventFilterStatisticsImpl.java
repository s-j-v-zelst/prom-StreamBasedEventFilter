package org.processmining.streambasedeventfilter.algorithms;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.processmining.eventstream.core.interfaces.XSEvent;
import org.processmining.eventstream.models.XSEventHub;
import org.processmining.framework.util.Pair;
import org.processmining.stream.core.abstracts.AbstractXSHub;
import org.processmining.stream.core.abstracts.AbstractXSVisualization;
import org.processmining.stream.core.enums.CommunicationType;
import org.processmining.stream.core.interfaces.XSVisualization;
import org.processmining.streambasedeventfilter.parameters.TrieBasedSpurioiusEventFilterParametersImpl;
import org.processmining.streambasedeventlog.models.EventPayload;
import org.processmining.streambasedeventlog.models.IncrementalPayloadTrie.Edge;
import org.processmining.streambasedeventlog.parameters.StreamBasedEventStorageParametersImpl;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class TrieBasedSpuriousEventFilterStatisticsImpl extends AbstractXSHub<XSEvent, XSEvent> implements XSEventHub {

	private final Collection<String> casesObservedDuringWarmup = new HashSet<>();
	//	private final TObjectIntMap<String> noiseDetected = new TObjectIntHashMap<>();
	private final TrieBasedSpuriousEventFilterEventCollectorImpl<EventPayload, StreamBasedEventStorageParametersImpl> eventCollector;
	private final TrieBasedSpurioiusEventFilterParametersImpl filterParameters;
	private final StreamBasedEventStorageParametersImpl storageParameters;
	private long tp, fp, tn, fn = 0;

	public TrieBasedSpuriousEventFilterStatisticsImpl(final StreamBasedEventStorageParametersImpl storageParameters,
			final TrieBasedSpurioiusEventFilterParametersImpl filterParameters) {
		super("spurious event filter (statistics)", CommunicationType.SYNC);
		eventCollector = new TrieBasedSpuriousEventFilterEventCollectorImpl<EventPayload, StreamBasedEventStorageParametersImpl>(
				storageParameters, new EventPayload.FactoryNaiveImpl(), filterParameters);
		this.storageParameters = storageParameters;
		this.filterParameters = filterParameters;
	}

	@Override
	protected void handleNextPacket(XSEvent event) {
		//		eventCollector.handleNextPacket(event);
		eventCollector.deliver(event);
		String caseId = event.get(storageParameters.getCaseIdentifier()).toString();
		if (eventCollector.getSlidingWindow().size() >= storageParameters.getSlidingWindowSize()) {
			if (!casesObservedDuringWarmup.contains(caseId)) {
				Edge<EventPayload> newEdge = eventCollector.getToEdges().get(caseId);
				int d = newEdge.getDepth();
				TObjectDoubleMap<String> distribution = new TObjectDoubleHashMap<String>();
				for (Edge<EventPayload> e : eventCollector.getTrie().getEdges()) {
					if (e.getDepth() >= d - 2 && e.getDepth() <= d + 2) {
						double count = e.getPayload().getActiveCaseIdentifiers().size()
								* Math.pow(0.75, Math.abs(e.getDepth() - d));
						distribution.adjustOrPutValue(e.getPayload().getActivity(), count, count);
					}
				}

				// avg
				int sum = 0;
				for (String key : distribution.keySet()) {
					sum += distribution.get(key);
				}
				double avg = sum / (double) distribution.keySet().size();
				if (distribution.get(newEdge.getPayload().getActivity()) / avg < 0.5) {
					if (filterParameters.isExperiment()) {
						if (isNoise(event)) {
							tp++;
							System.out.println("tp: " + event.toString());
						} else {
							fp++;
							System.out.println("fp: " + event.toString());
						}
					}
				} else {
					write(event);
					if (filterParameters.isExperiment()) {
						if (!isNoise(event)) {
							tn++;
							System.out.println("tn: " + event.toString());
						} else {
							fn++;
							System.out.println("fn: " + event.toString());
						}
					}
				}
			}
			System.out.println(getTruePositives() + "\t" + getFalsePositives() + "\t" + getTrueNegatives() + "\t"
					+ getFalseNegatives());
		} else {
			if (filterParameters.isIgnoreTrainingCases()) {
				casesObservedDuringWarmup.add(caseId);
			}
		}
	}

	public long getFalseNegatives() {
		return fn;
	}

	public long getFalsePositives() {
		return fp;
	}

	public long getTrueNegatives() {
		return tn;
	}

	public long getTruePositives() {
		return tp;
	}

	private boolean isNoise(final XSEvent event) {
		return event.containsKey(filterParameters.getNoiseClassificationLabelKey())
				&& event.get(filterParameters.getNoiseClassificationLabelKey()).toString()
						.equals(filterParameters.getNoiseClassificationLabelValue());
	}

	public XSEvent getCurrentResult() {
		return null;
	}

	public Class<XSEvent> getTopic() {
		return XSEvent.class;
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

	protected XSEvent transform(XSEvent packet) {
		return packet;
	}

}
