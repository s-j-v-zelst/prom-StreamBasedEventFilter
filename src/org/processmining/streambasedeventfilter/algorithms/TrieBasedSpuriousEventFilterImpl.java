package org.processmining.streambasedeventfilter.algorithms;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

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
import org.processmining.streambasedeventlog.models.IncrementalPayloadTrie;
import org.processmining.streambasedeventlog.parameters.StreamBasedEventStorageParametersImpl;

@Deprecated // not using this implementation at the moment, keeping the code for possible future experiments
public class TrieBasedSpuriousEventFilterImpl extends AbstractXSHub<XSEvent, XSEvent> implements XSEventHub {

	private final Collection<String> casesObservedDuringWarmup = new HashSet<>();
	private final Queue<XSEvent> emissionDelay = new LinkedList<>();
	private final TrieBasedSpuriousEventFilterEventCollectorImpl<EventPayload, StreamBasedEventStorageParametersImpl> eventCollector;
	private final TrieBasedSpurioiusEventFilterParametersImpl filterParameters;
	private final StreamBasedEventStorageParametersImpl storageParameters;
	private long tp, fp, tn, fn = 0;

	public TrieBasedSpuriousEventFilterImpl(final StreamBasedEventStorageParametersImpl storageParameters,
			final TrieBasedSpurioiusEventFilterParametersImpl filterParameters) {
		super("spurious event filter", CommunicationType.SYNC);
		eventCollector = new TrieBasedSpuriousEventFilterEventCollectorImpl<EventPayload, StreamBasedEventStorageParametersImpl>(
				storageParameters, new EventPayload.FactoryNaiveImpl(), filterParameters);
		this.storageParameters = storageParameters;
		this.filterParameters = filterParameters;
	}

	public XSEvent getCurrentResult() {
		return null;
	}

	public long getFalseNegatives() {
		return fn;
	}

	public long getFalsePositives() {
		return fp;
	}

	public Class<XSEvent> getTopic() {
		return XSEvent.class;
	}

	public long getTrueNegatives() {
		return tn;
	}

	public long getTruePositives() {
		return tp;
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

	@Override
	protected void handleNextPacket(XSEvent event) {
		//		eventCollector.handleNextPacket(event);
		//TODO test if this works:
		eventCollector.deliver(event);
		if (eventCollector.getSlidingWindow().size() >= storageParameters.getSlidingWindowSize()) {
			emissionDelay.add(event);
			if (emissionDelay.size() > filterParameters.getEmissionDelay()) {
				XSEvent toEmit = emissionDelay.poll();
				String caseId = toEmit.get(storageParameters.getCaseIdentifier()).toString();
				if (!casesObservedDuringWarmup.contains(caseId)) {
					IncrementalPayloadTrie.Edge<EventPayload> currContinuation = eventCollector.getContinuationEdges()
							.get(caseId);
					eventCollector.applyContinuation(toEmit);
					IncrementalPayloadTrie.Edge<EventPayload> nextContinuation = eventCollector.getContinuationEdges()
							.get(caseId);
					if ((currContinuation == null && !(nextContinuation.equals(eventCollector.getTrie().getRootEdge())))
							|| (currContinuation != null && !currContinuation.equals(nextContinuation))) {
						write(toEmit);
						if (filterParameters.isExperiment()) {
							if (!isNoise(toEmit)) {
								tn++;
							} else {
								fn++;
							}
						}
					} else {
						if (filterParameters.isExperiment()) {
							if (isNoise(toEmit)) {
								tp++;
								System.out.println("tp: " + event.toString());
							} else {
								fp++;
								System.out.println("fp: " + event.toString());
							}
						}
					}
				}
			}
			//			System.out.println(getTruePositives() + "," + getFalsePositives() + "," + getTrueNegatives() + ","
			//					+ getFalseNegatives());
		} else {
			if (filterParameters.isIgnoreTrainingCases()) {
				casesObservedDuringWarmup.add(event.get(storageParameters.getCaseIdentifier()).toString());
			}
		}
	}

	private boolean isNoise(final XSEvent event) {
		return event.containsKey(filterParameters.getNoiseClassificationLabelKey())
				&& event.get(filterParameters.getNoiseClassificationLabelKey()).toString()
						.equals(filterParameters.getNoiseClassificationLabelValue());
	}

	protected XSEvent transform(XSEvent packet) {
		return packet;
	}

}
