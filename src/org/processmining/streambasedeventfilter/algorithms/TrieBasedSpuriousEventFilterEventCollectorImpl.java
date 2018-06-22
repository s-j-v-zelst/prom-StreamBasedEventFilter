//package org.processmining.streambasedeventfilter.algorithms;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import org.processmining.eventstream.core.interfaces.XSEvent;
//import org.processmining.streambasedeventfilter.parameters.TrieBasedSpurioiusEventFilterParametersImpl;
//import org.processmining.streambasedeventlog.algorithms.TrieBasedEventCollectorImpl;
//import org.processmining.streambasedeventlog.models.EventPayload;
//import org.processmining.streambasedeventlog.models.EventPayload.Factory;
//import org.processmining.streambasedeventlog.models.IncrementalPayloadTrie;
//import org.processmining.streambasedeventlog.models.IncrementalPayloadTrie.Edge;
//import org.processmining.streambasedeventlog.parameters.StreamBasedEventStorageParametersImpl;
//
//import gnu.trove.map.TObjectIntMap;
//import gnu.trove.map.hash.TObjectIntHashMap;
//
///**
// * has an additional set of pointers called "continuation pointers", stored as a
// * Map<String, Edge> continuationEdges.
// * 
// * @author svzelst
// *
// * @param <E>
// * @param <P>
// */
//
//@Deprecated // not using this implementation at the moment, keeping the code for possible future experiments
//public class TrieBasedSpuriousEventFilterEventCollectorImpl<E extends EventPayload, P extends StreamBasedEventStorageParametersImpl>
//		extends TrieBasedEventCollectorImpl<E, P> {
//
//	private final Map<String, IncrementalPayloadTrie.Edge<E>> continuationEdges = new HashMap<>();
//	private final TrieBasedSpurioiusEventFilterParametersImpl filterParameters;
//
//	public TrieBasedSpuriousEventFilterEventCollectorImpl(P parameters, Factory<E> eventPayloadFactory,
//			final TrieBasedSpurioiusEventFilterParametersImpl filterParameters) {
//		super(parameters, eventPayloadFactory);
//		this.filterParameters = filterParameters;
//	}
//
//	public void applyContinuation(XSEvent event) {
//		String caseId = event.get(getParameters().getCaseIdentifier()).toString();
//		String activity = event.get(getParameters().getActivityIdentifier()).toString();
//		if (!continuationEdges.containsKey(caseId)) {
//			continuationEdges.put(caseId, getTrie().getRootEdge());
//		}
//		IncrementalPayloadTrie.Edge<E> currentContinuation = continuationEdges.get(caseId);
//		int maxVal = Integer.MIN_VALUE;
//		for (Edge<E> c : currentContinuation.getChildren()) {
//			maxVal = Math.max(maxVal, c.getPayload().getActiveCaseIdentifiers().size());
//		}
//		double threshold = filterParameters.getInclusionDecisionThreshold() * maxVal;
//		boolean advanced = false;
//		for (Edge<E> c : currentContinuation.getChildren()) {
//			if (c.getPayload().getActiveCaseIdentifiers().size() >= threshold
//					&& c.getPayload().getActivity().equals(activity)) {
//				continuationEdges.put(caseId, c);
//				advanced = true;
//				break;
//			}
//		}
//		if (!advanced) {
//			// check distribution
//			TObjectIntMap<String> distr = constructLookAheadDistribution(currentContinuation,
//					new TObjectIntHashMap<String>(), (byte) 1);
//			int max = Integer.MIN_VALUE;
//			for (int i : distr.values()) {
//				if (i > max)
//					max = i;
//			}
//			if (distr.containsKey(activity)
//					&& distr.get(activity) >= filterParameters.getLookaheadInclusionDecisionThreshold() * max) {
//				//shift anyway...
//				for (Edge<E> c : currentContinuation.getChildren()) {
//					if (c.getPayload().getActivity().equals(activity)) {
//						continuationEdges.put(caseId, c);
//						break;
//					}
//				}
//			}
//		}
//	}
//
//	private TObjectIntMap<String> constructLookAheadDistribution(final IncrementalPayloadTrie.Edge<E> edge,
//			final TObjectIntMap<String> distribution, byte depth) {
//		if (depth <= filterParameters.getMaxLookAhead()) {
//			for (IncrementalPayloadTrie.Edge<E> c : edge.getChildren()) {
//				int adjVal = c.getPayload().getActiveCaseIdentifiers().size();
//				distribution.adjustOrPutValue(c.getPayload().getActivity(), adjVal, adjVal);
//				constructLookAheadDistribution(c, distribution, depth++);
//			}
//		}
//		return distribution;
//	}
//
//	public Map<String, IncrementalPayloadTrie.Edge<E>> getContinuationEdges() {
//		return continuationEdges;
//	}
//}
