package org.processmining.streambasedeventfilter.algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.processmining.eventstream.core.interfaces.XSEvent;
import org.processmining.framework.util.Pair;
import org.processmining.framework.util.collection.HashMultiSet;
import org.processmining.framework.util.collection.MultiSet;
import org.processmining.streambasedeventfilter.algorithms.abstr.AbstractXSEventFilterImpl;
import org.processmining.streambasedeventfilter.parameters.ConditionalProbabilitiesBasedXSEventFilterParametersImpl;
import org.processmining.streambasedeventlog.algorithms.NaiveEventCollectorImpl;
import org.processmining.streambasedeventlog.parameters.StreamBasedEventLogParametersImpl;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class ConditionalProbabilitiesBasedXSEventFilterImpl
		extends AbstractXSEventFilterImpl<ConditionalProbabilitiesBasedXSEventFilterParametersImpl> {

	private final String ARTIFICIAL_START_SYMBOL = "__ARTIFICIAL_START__" + System.currentTimeMillis();
	private final Map<Collection<String>, TObjectIntMap<String>> followsRelation = new HashMap<>();
	private final Map<String, TObjectIntMap<Collection<String>>> precedesRelation = new HashMap<>();
	private final NaiveEventCollectorImpl<StreamBasedEventLogParametersImpl> collector;
	private final StreamBasedEventLogParametersImpl storageParams;
	private final Collection<String> alteredCases = new HashSet<>();

	public ConditionalProbabilitiesBasedXSEventFilterImpl(
			ConditionalProbabilitiesBasedXSEventFilterParametersImpl filterParameters,
			StreamBasedEventLogParametersImpl storageParams) {
		super("spurious_event_filter_conditional_probs", filterParameters);
		collector = new NaiveEventCollectorImpl<StreamBasedEventLogParametersImpl>(storageParams);
		this.storageParams = storageParams;
	}

	@Override
	protected void handleNextPacket(XSEvent event) {
		final String caseId = event.get(storageParams.getCaseIdentifier()).toString();
		collector.triggerPacketHandle(event);
		List<String> trace = translateToStringList(collector.getCases().get(caseId));
		if (!alteredCases.contains(caseId))
			trace.add(0, ARTIFICIAL_START_SYMBOL);
		updateConditionalProbabilityStructure(trace);
		if (collector.getSlidingWindow().size() >= storageParams.getSlidingWindowSize()) {
			boolean isNoise = classifyNewEventAsNoise(trace);
			if (!isNoise) {
				write(event);
			}
			if (getFilterParameters().isExperiment()) {
				updateExperimentVariables(event, isNoise);
			}
		}
	}

	private void incrementallyIncreaseFollowsRelation(final List<String> trace) {
		for (int i = 1; i <= getFilterParameters().getMaxPatternLength(); i++) {
			if (trace.size() > i) {
				List<String> prefix = trace.subList(trace.size() - i - 1, trace.size() - 1);
				MultiSet<String> mset = new HashMultiSet<>(prefix);
				if (!followsRelation.containsKey(mset)) {
					followsRelation.put(mset, new TObjectIntHashMap<String>());
				}
				followsRelation.get(mset).adjustOrPutValue(trace.get(trace.size() - 1), 1, 1);
			}
		}
	}

	private void incrementallyIncreasePrecedenceRelation(final List<String> trace) {
		for (int i = 1; i <= getFilterParameters().getMaxPatternLength(); i++) {
			if (trace.size() > i) {
				List<String> suffix = trace.subList(trace.size() - i, trace.size());
				MultiSet<String> mset = new HashMultiSet<>(suffix);
				String act = trace.get(trace.size() - i - 1);
				if (!precedesRelation.containsKey(act)) {
					precedesRelation.put(act, new TObjectIntHashMap<Collection<String>>());
				}
				precedesRelation.get(act).adjustOrPutValue(mset, 1, 1);
			}
		}
	}

	private void incrementallyReduceFollowsRelation(final List<String> alteredTrace, final List<String> newTrace) {
		int diff = alteredTrace.size() - newTrace.size();
		if (diff == 0) { // can't be > 0
			for (int i = 1; i <= getFilterParameters().getMaxPatternLength(); i++) {
				if (i < alteredTrace.size()) {
					List<String> prefix = alteredTrace.subList(0, i);
					MultiSet<String> mset = new HashMultiSet<>(prefix);
					followsRelation.get(mset).adjustValue(alteredTrace.get(i), -1);
				}
			}
		} else {
			for (int i = 0; i < diff; i++) {
				for (int j = i + 1; j <= i + getFilterParameters().getMaxPatternLength(); j++) {
					if (j < alteredTrace.size()) {
						List<String> prefix = alteredTrace.subList(i, j);
						MultiSet<String> mset = new HashMultiSet<>(prefix);
						followsRelation.get(mset).adjustValue(alteredTrace.get(j), -1);
					} else {
						break;
					}
				}
			}
		}
	}

	private void incrementallyReducePrecedenceRelation(final List<String> alteredTrace, final List<String> newTrace) {
		int diff = alteredTrace.size() - newTrace.size();
		if (diff == 0) {
			String act = alteredTrace.get(0);
			for (int i = 1; i <= getFilterParameters().getMaxPatternLength(); i++) {
				if (i < alteredTrace.size()) {
					List<String> suffix = alteredTrace.subList(i, i + 1);
					MultiSet<String> mset = new HashMultiSet<>(suffix);
					precedesRelation.get(act).adjustValue(mset, -1);
				}
			}
		} else {
			for (int i = 0; i < diff; i++) {
				String act = alteredTrace.get(i);
				for (int j = i + 1; j <= i + getFilterParameters().getMaxPatternLength(); j++) {
					if (j < alteredTrace.size()) {
						List<String> suffix = alteredTrace.subList(i + 1, j + 1);
						MultiSet<String> mset = new HashMultiSet<>(suffix);
						precedesRelation.get(act).adjustValue(mset, -1);
					}
				}
			}
		}
	}

	private void updateConditionalProbabilityStructure(final List<String> trace) {
		incrementallyIncreaseFollowsRelation(trace);
		incrementallyIncreasePrecedenceRelation(trace);
		Map<String, Pair<List<XSEvent>, List<XSEvent>>> delta = collector.getDelta();
		for (Map.Entry<String, Pair<List<XSEvent>, List<XSEvent>>> me : delta.entrySet()) {
			final List<String> alteredTrace = translateToStringList(me.getValue().getFirst());
			final List<String> newTrace = translateToStringList(me.getValue().getSecond());
			if (!alteredCases.contains(me.getKey())) {
				alteredTrace.add(0, ARTIFICIAL_START_SYMBOL);
			}
			incrementallyReduceFollowsRelation(alteredTrace, newTrace);
			incrementallyReducePrecedenceRelation(alteredTrace, newTrace);
			alteredCases.add(me.getKey());
			if (me.getValue().getSecond().isEmpty()) {
				alteredCases.remove(me.getKey());
			}
		}
	}

	private List<String> translateToStringList(final List<XSEvent> input) {
		List<String> result = new ArrayList<>();
		for (XSEvent e : input) {
			result.add(e.get(storageParams.getActivityIdentifier()).toString());
		}
		return result;
	}

	private boolean classifyNewEventAsNoise(final List<String> trace) {
		final String newActivity = trace.get(trace.size() - 1);
		boolean noise = false;
		for (int i = 1; i <= getFilterParameters().getMaxPatternLength(); i++) {
			if (trace.size() > i) {
				List<String> prefix = trace.subList(trace.size() - i - 1, trace.size() - 1);
				MultiSet<String> mset = new HashMultiSet<>(prefix);
				TObjectIntMap<String> dist = followsRelation.get(mset);
				// do some test on the distribution
				int max = Integer.MIN_VALUE;
				for (String k : dist.keySet()) {
					max = Math.max(max, dist.get(k));
				}
				if (!dist.containsKey(newActivity)
						|| dist.get(newActivity) <= getFilterParameters().getCutoffThreshold() * max) {
					noise = true;
					break;
				}
			} else {
				break;
			}
		}
		if (!noise) {
			for (int i = 1; i <= getFilterParameters().getMaxPatternLength(); i++) {
				if (trace.size() > i) {
					List<String> suffix = trace.subList(trace.size() - i, trace.size());
					MultiSet<String> mset = new HashMultiSet<>(suffix);
					String act = trace.get(trace.size() - i - 1);
					TObjectIntMap<Collection<String>> distribution = precedesRelation.get(act);
					int max = Integer.MIN_VALUE;
					for (Collection<String> coll : distribution.keySet()) {
						max = Math.max(max, distribution.get(coll));
					}
					if (!distribution.containsKey(mset)
							|| distribution.get(mset) <= getFilterParameters().getCutoffThreshold() * max) {
						noise = true;
						break;
					}
				} else {
					break;
				}
			}
		}
		return noise;
	}

}
