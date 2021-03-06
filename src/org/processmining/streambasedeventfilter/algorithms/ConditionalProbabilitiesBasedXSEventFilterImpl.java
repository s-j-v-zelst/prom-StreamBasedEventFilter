package org.processmining.streambasedeventfilter.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.lang3.ArrayUtils;
import org.processmining.eventstream.core.interfaces.XSEvent;
import org.processmining.eventstream.core.interfaces.XSStaticXSEventStream;
import org.processmining.framework.util.Pair;
import org.processmining.framework.util.collection.HashMultiSet;
import org.processmining.streambasedeventfilter.algorithms.abstr.AbstractXSEventFilterImpl;
import org.processmining.streambasedeventfilter.parameters.ConditionalProbabilitiesBasedXSEventFilterParametersImpl;
import org.processmining.streambasedeventfilter.parameters.ConditionalProbabilitiesBasedXSEventFilterParametersImpl.AdjustmentMethod;
import org.processmining.streambasedeventfilter.parameters.ConditionalProbabilitiesBasedXSEventFilterParametersImpl.FilteringMethod;
import org.processmining.streambasedeventfilter.util.XSEventUtils;
import org.processmining.streambasedeventstorage.algorithms.XSEventStoreSlidingWindowImpl;
import org.processmining.streambasedeventstorage.parameters.XSEventStoreSlidingWindowParametersImpl;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class ConditionalProbabilitiesBasedXSEventFilterImpl
		extends AbstractXSEventFilterImpl<ConditionalProbabilitiesBasedXSEventFilterParametersImpl> {

	// cases that are no longer complete in the store.
	private final Collection<String> alteredCases = new HashSet<>();
	private final String ARTIFICIAL_START_SYMBOL = "__ARTIFICIAL_START__" + System.currentTimeMillis();
	//	private final NaiveEventCollectorImpl<StreamBasedEventLogParametersImpl> collector;

	private final XSEventStoreSlidingWindowImpl collector;

	private final Map<Collection<String>, TObjectIntMap<String>> followsRelation = new HashMap<>();
	// registry that keeps track of indices in stored traces that relate to noise 
	private final Map<String, Pair<List<String>, int[]>> noise = new HashMap<>();
	private final Map<String, TObjectIntMap<Collection<String>>> precedesRelation = new HashMap<>();
	private final Collection<XSEvent> resultingStream = new ArrayList<>();
	private final Queue<XSEvent> eventQueue = new LinkedList<>();
	private final Queue<String> caseQueue = new LinkedList<>();
	private final Queue<List<String>> traceQueue = new LinkedList<>();

	public ConditionalProbabilitiesBasedXSEventFilterImpl(
			ConditionalProbabilitiesBasedXSEventFilterParametersImpl filterParameters,
			XSEventStoreSlidingWindowParametersImpl storageParams) {
		super("spurious_event_filter_conditional_probs", filterParameters);
		collector = new XSEventStoreSlidingWindowImpl(storageParams);
	}

	private boolean classifyNewEventAsNoise(final List<String> trace, final int[] noiseIndices) {
		FilteringMethod filtermethod = getFilterParameters().getFiltermethod();
		switch (filtermethod) {
			case ANY :
				return evaluateFollowsRelations(trace, noiseIndices) || evaluatePrecedesRelation(trace, noiseIndices);
			case BOTH_DIRECTIONS :
				return evaluateFollowsRelations(trace, noiseIndices) && evaluatePrecedesRelation(trace, noiseIndices);
			case FORWARD :
				return evaluateFollowsRelations(trace, noiseIndices);
			case BACKWARD :
				return evaluatePrecedesRelation(trace, noiseIndices);
			default :
				return evaluateFollowsRelations(trace, noiseIndices) || evaluatePrecedesRelation(trace, noiseIndices);
		}

	}

	private List<String> constructNoiseAwarePrefix(final List<String> trace, final int length,
			final int[] noiseIndices) {
		if (noiseIndices != null) {
			List<String> loopVar = new ArrayList<>(trace.subList(0, trace.size() - 1));
			List<String> prefix = new ArrayList<>();
			while (prefix.size() < length && loopVar.size() > 0) {
				if (!ArrayUtils.contains(noiseIndices, loopVar.size() - 1)) {
					prefix.add(0, loopVar.get(loopVar.size() - 1));
				}
				int b = loopVar.size() - 1;
				loopVar = new ArrayList<>(loopVar.subList(0, b));
			}
			return prefix;
		} else {
			return new ArrayList<>(trace.subList(trace.size() - length - 1, trace.size() - 1));
		}

	}

	private Pair<String, Collection<String>> constructNoiseAwareSuffixAndFollowingActivity(final List<String> trace,
			final int length, final int[] noiseIndices) {
		if (noiseIndices != null) {
			List<String> loopVar = new ArrayList<>(trace.subList(0, trace.size() - 1));
			List<String> suffix = new ArrayList<>(trace.subList(trace.size() - 1, trace.size()));
			int loopVarSize = loopVar.size();
			while (suffix.size() < length && loopVarSize > 0) {
				if (!ArrayUtils.contains(noiseIndices, loopVar.size() - 1)) {
					suffix.add(0, loopVar.get(loopVar.size() - 1));
				}
				int b = loopVar.size() - 1;
				loopVar = new ArrayList<>(loopVar.subList(0, b));
				loopVarSize = loopVar.size();
			}
			String act = null;
			while (loopVar.size() > 0) {
				if (!ArrayUtils.contains(noiseIndices, loopVar.size() - 1)) {
					act = loopVar.get(loopVar.size() - 1);
					break;
				}
				int b = loopVar.size() - 1;
				loopVar = new ArrayList<>(loopVar.subList(0, b));
			}
			return new Pair<>(act, getAbstraction(suffix));
		} else {
			return new Pair<>(trace.get(trace.size() - length - 1),
					getAbstraction(new ArrayList<>(trace.subList(trace.size() - length, trace.size()))));
		}

	}

	private boolean evaluateFollowsByAbstraction(final Collection<String> abstraction, final String newActivity) {
		TObjectIntMap<String> distribution = followsRelation.get(abstraction);
		int max = getMaximalFollowsValue(distribution);
		int sum = getSumFollowsValue(distribution);
		int count = getNZeroCountFollowsValue(distribution);
		double NZAvg = 0;
		if (count > 0)
			NZAvg = (sum * 1.0) / count;
		if (max == -1 || !distribution.containsKey(newActivity)) {
			return true;
		} else {
			AdjustmentMethod adjustmethod = getFilterParameters().getAdjustmentmethod();
			switch (adjustmethod) {
				case NONE :
					if (distribution.get(newActivity) <= getFilterParameters().getCutoffThreshold() * (sum)) {
						return true;
					}
					break;
				case MAX :
					if (distribution.get(newActivity) <= getFilterParameters().getCutoffThreshold() * (max)) {
						return true;
					}
					break;
				case MAX_NZ_AVG :
					if (distribution.get(newActivity) <= getFilterParameters().getCutoffThreshold() * (max - NZAvg)) {
						return true;
					}
					break;
			}
		}
		return false;
	}

	/**
	 * checks follows relation(s) returns true iff noise is detected, false
	 * otherwise.
	 * 
	 * @param caseId
	 * @param trace
	 * @param newActivity
	 * @return
	 */
	private boolean evaluateFollowsRelations(final List<String> trace, final int[] noiseIndices) {
		for (int i = 1; i <= getFilterParameters().getMaxPatternLength(); i++) {
			if (trace.size() > i) {
				Collection<String> prefix = constructNoiseAwarePrefix(trace, i, noiseIndices);
				if (prefix.size() == i) {
					if (evaluateFollowsByAbstraction(getAbstraction((List<String>) prefix),
							trace.get(trace.size() - 1))) {
						return true;
					}
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
		return false;
	}

	private boolean evaluatePrecedesByAbstraction(final Collection<String> abstraction, final String activity) {
		TObjectIntMap<Collection<String>> distribution = precedesRelation.get(activity);
		int max = getMaximalPrecedesValue(distribution, abstraction.size());
		int sum = getSumPrecedesValue(distribution, abstraction.size());
		int count = getNZeroCountPrecedesValue(distribution, abstraction.size());
		double NZAvg = 0;
		if (count > 0)
			NZAvg = (sum * 1.0) / count;
		if (max == -1 || !distribution.containsKey(abstraction)) {
			return true;
		} else {
			AdjustmentMethod adjustmethod = getFilterParameters().getAdjustmentmethod();
			switch (adjustmethod) {
				case NONE :
					if (distribution.get(abstraction) <= getFilterParameters().getCutoffThreshold() * (sum)) {
						return true;
					}
					break;
				case MAX :
					if (distribution.get(abstraction) <= getFilterParameters().getCutoffThreshold() * (max)) {
						return true;
					}
					break;
				case MAX_NZ_AVG :
					if (distribution.get(abstraction) <= getFilterParameters().getCutoffThreshold() * (max - NZAvg)) {
						return true;
					}
					break;
			}
		}
		return false;
	}

	/**
	 * checks precedes relation, returns true iff noise is detected
	 * 
	 * @param caseId
	 * @param trace
	 * @return
	 */
	private boolean evaluatePrecedesRelation(final List<String> trace, final int[] noiseIndices) {
		for (int i = 1; i <= getFilterParameters().getMaxPatternLength(); i++) {
			if (trace.size() > i) {
				Pair<String, Collection<String>> actAbstr = constructNoiseAwareSuffixAndFollowingActivity(trace, i,
						noiseIndices);
				String act = actAbstr.getFirst();
				Collection<String> abstr = actAbstr.getSecond();
				if (act != null) {
					if (evaluatePrecedesByAbstraction(abstr, act)) {
						return true;
					}
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
		return false;
	}

	public XSStaticXSEventStream fetchFilteredStream() {
		return XSStaticXSEventStream.Factory.createArrayListBasedXSStaticXSEventStream(resultingStream);
	}

	private void filter(final XSEvent event, final String caseId, final List<String> trace) {
		Pair<List<String>, int[]> formerFilteredTraceAndNoise = noise.get(caseId);
		List<String> prevTrace = formerFilteredTraceAndNoise == null ? new ArrayList<String>()
				: formerFilteredTraceAndNoise.getFirst();
		int[] noiseIndices = formerFilteredTraceAndNoise == null ? new int[0] : formerFilteredTraceAndNoise.getSecond();
		if (trace.size() <= prevTrace.size()) {
			noiseIndices = shiftNoiseArray(noiseIndices, prevTrace.size() - trace.size());
		}
		boolean isNoise = classifyNewEventAsNoise(trace, noiseIndices);
		if (!isNoise) {
			if (getFilterParameters().isContextAware()) {
				write(event);
			} else {
				resultingStream.add(event);
			}
		} else {
			noiseIndices = Arrays.copyOf(noiseIndices, noiseIndices.length + 1);
			noiseIndices[noiseIndices.length - 1] = trace.size() - 1;
		}
		noise.put(caseId, new Pair<List<String>, int[]>(new ArrayList<String>(trace), noiseIndices));
		if (getFilterParameters().isExperiment()) {
			updateExperimentVariables(event, isNoise);
		}
	}

	private Collection<String> getAbstraction(List<String> trace) {
		switch (getFilterParameters().getAbstraction()) {
			case MULTISET :
				return new HashMultiSet<>(trace);
			case SET :
				return new HashSet<>(trace);
			case SEQUENCE :
			default :
				return trace;
		}
	}

	private int getMaximalFollowsValue(final TObjectIntMap<String> distribution) {
		int max = -1;
		if (distribution != null) {
			for (String k : distribution.keySet()) {
				max = Math.max(max, distribution.get(k));
			}
		}
		return max;
	}

	private int getMaximalPrecedesValue(final TObjectIntMap<Collection<String>> distribution, final int patternLength) {
		int max = -1;
		if (distribution != null) {
			for (Collection<String> coll : distribution.keySet()) {
				if (coll.size() == patternLength) { // we have to make sure that we only look at suffixes of the same length here!
					max = Math.max(max, distribution.get(coll));
				}
			}
		}
		return max;
	}

	private int getNZeroCountFollowsValue(final TObjectIntMap<String> distribution) {
		int Count = 0;
		if (distribution != null) {
			for (String k : distribution.keySet()) {
				if (distribution.get(k) > 0)
					Count++;
			}
		}
		return Count;
	}

	private int getNZeroCountPrecedesValue(final TObjectIntMap<Collection<String>> distribution,
			final int patternLength) {
		int Count = 0;
		if (distribution != null) {
			for (Collection<String> coll : distribution.keySet()) {
				if (coll.size() == patternLength) { // we have to make sure that we only look at suffixes of the same length here!
					if (distribution.get(coll) > 0)
						;
					Count++;
				}
			}
		}
		return Count;
	}

	private int getSumFollowsValue(final TObjectIntMap<String> distribution) {
		int Sum = 0;
		if (distribution != null) {
			for (String k : distribution.keySet()) {
				Sum = Sum + distribution.get(k);
			}
		}
		return Sum;
	}

	private int getSumPrecedesValue(final TObjectIntMap<Collection<String>> distribution, final int patternLength) {
		int Sum = 0;
		if (distribution != null) {
			for (Collection<String> coll : distribution.keySet()) {
				if (coll.size() == patternLength) { // we have to make sure that we only look at suffixes of the same length here!
					Sum = Sum + distribution.get(coll);
				}
			}
		}
		return Sum;
	}

	@Override
	protected void handleNextPacket(XSEvent event) {
		final String caseId = event.get(collector.getParameters().getCaseIdentifier()).toString();
		collector.triggerPacketHandle(event);
		List<String> trace = XSEventUtils.convertByKey(collector.project(caseId),
				collector.getParameters().getActivityIdentifier().toString());
		if (!alteredCases.contains(caseId)) {
			trace.add(0, ARTIFICIAL_START_SYMBOL);
		}
		updateConditionalProbabilityStructure(trace);
		if (collector.getWindow().size() >= collector.getParameters().getSize()
				&& (!getFilterParameters().isIgnoreTrainingCases()
						|| (getFilterParameters().isIgnoreTrainingCases() && !getTrainingCases().contains(caseId)))) {
			eventQueue.add(event);
			caseQueue.add(caseId);
			traceQueue.add(new ArrayList<>(trace));
			if (eventQueue.size() > getFilterParameters().getDelay()) {
				filter(eventQueue.poll(), caseQueue.poll(), traceQueue.poll());
			}
		} else if (getFilterParameters().isIgnoreTrainingCases()) {
			getTrainingCases().add(caseId);
		}
	}

	private Map<String, Pair<List<XSEvent>, List<XSEvent>>> computeDelta() {
		Map<String, Pair<List<XSEvent>, List<XSEvent>>> delta = new HashMap<>();
		TObjectIntMap<String> added = new TObjectIntHashMap<>();
		for (XSEvent e : collector.getOutFlux()) {
			String caseId = e.get(collector.getParameters().getCaseIdentifier()).toString();
			if (!delta.containsKey(caseId)) {
				List<XSEvent> trace = new ArrayList<>(collector.project(caseId));
				trace.add(0, e);
				delta.put(caseId, new Pair<List<XSEvent>, List<XSEvent>>(trace, collector.project(caseId)));
				added.adjustOrPutValue(caseId, 1, 1);
			} else {
				delta.get(caseId).getFirst().add(added.get(caseId), e);
				added.adjustOrPutValue(caseId, 1, 1);
			}
		}
		return delta;
	}

	private void incrementallyIncreaseFollowsRelation(final List<String> trace) {
		for (int i = 1; i <= getFilterParameters().getMaxPatternLength(); i++) {
			if (trace.size() > i) {
				Collection<String> prefix = getAbstraction(
						new ArrayList<>(trace.subList(trace.size() - i - 1, trace.size() - 1)));
				if (!followsRelation.containsKey(prefix)) {
					followsRelation.put(prefix, new TObjectIntHashMap<String>());
				}
				followsRelation.get(prefix).adjustOrPutValue(trace.get(trace.size() - 1), 1, 1);
			}
		}
	}

	private void incrementallyIncreasePrecedenceRelation(final List<String> trace) {
		for (int i = 1; i <= getFilterParameters().getMaxPatternLength(); i++) {
			if (trace.size() > i) {
				Collection<String> suffix = getAbstraction(
						new ArrayList<>(trace.subList(trace.size() - i, trace.size())));
				String act = trace.get(trace.size() - i - 1);
				if (!precedesRelation.containsKey(act)) {
					precedesRelation.put(act, new TObjectIntHashMap<Collection<String>>());
				}
				precedesRelation.get(act).adjustOrPutValue(suffix, 1, 1);
			}
		}
	}

	private void incrementallyReduceFollowsRelation(final List<String> alteredTrace, final List<String> newTrace) {
		int diff = alteredTrace.size() - newTrace.size();
		if (diff == 0) { // can't be > 0
			for (int i = 1; i <= getFilterParameters().getMaxPatternLength(); i++) {
				if (i < alteredTrace.size()) {
					Collection<String> prefix = getAbstraction(new ArrayList<>(alteredTrace.subList(0, i)));
					followsRelation.get(prefix).adjustValue(alteredTrace.get(i), -1);
				}
			}
		} else {
			for (int i = 0; i < diff; i++) {
				for (int j = i + 1; j <= i + getFilterParameters().getMaxPatternLength(); j++) {
					if (j < alteredTrace.size()) {
						Collection<String> prefix = getAbstraction(new ArrayList<>(alteredTrace.subList(i, j)));
						followsRelation.get(prefix).adjustValue(alteredTrace.get(j), -1);
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
					Collection<String> suffix = getAbstraction(new ArrayList<>(alteredTrace.subList(i, i + 1)));
					precedesRelation.get(act).adjustValue(suffix, -1);
				}
			}
		} else {
			for (int i = 0; i < diff; i++) {
				String act = alteredTrace.get(i);
				for (int j = i + 1; j <= i + getFilterParameters().getMaxPatternLength(); j++) {
					if (j < alteredTrace.size()) {
						Collection<String> suffix = getAbstraction(new ArrayList<>(alteredTrace.subList(i + 1, j + 1)));
						precedesRelation.get(act).adjustValue(suffix, -1);
					}
				}
			}
		}
	}

	public void processEvent(XSEvent e) {
		handleNextPacket(e);
	}

	private void updateConditionalProbabilityStructure(final List<String> trace) {
		incrementallyIncreaseFollowsRelation(trace);
		incrementallyIncreasePrecedenceRelation(trace);
//		Map<String, Pair<List<XSEvent>, List<XSEvent>>> delta = collector.getDelta();
		Map<String, Pair<List<XSEvent>, List<XSEvent>>> delta = computeDelta();
		for (Map.Entry<String, Pair<List<XSEvent>, List<XSEvent>>> me : delta.entrySet()) {
			final List<String> alteredTrace = XSEventUtils.convertByKey(me.getValue().getFirst(),
					collector.getParameters().getActivityIdentifier().toString());
			final List<String> newTrace = XSEventUtils.convertByKey(me.getValue().getSecond(),
					collector.getParameters().getActivityIdentifier().toString());
			final String alteredCaseId = me.getKey();
			boolean isFirstRemovalForCase = false;
			if (!alteredCases.contains(me.getKey())) {
				alteredTrace.add(0, ARTIFICIAL_START_SYMBOL);
				isFirstRemovalForCase = true;
			}
			incrementallyReduceFollowsRelation(alteredTrace, newTrace);
			incrementallyReducePrecedenceRelation(alteredTrace, newTrace);
			alteredCases.add(me.getKey());
			if (me.getValue().getSecond().isEmpty()) {
				alteredCases.remove(me.getKey());
			}
			//			if (noise.containsKey(alteredCaseId)) {
			//				updateNoiseDataStructureAfterRemoval(alteredCaseId, isFirstRemovalForCase);
			//			}
		}
	}

	private int[] shiftNoiseArray(final int[] originalNoiseArray, final int indexShift) {
		int[] noise = Arrays.copyOf(originalNoiseArray, originalNoiseArray.length);
		int negative = 0;
		for (int b = 0; b <= indexShift; b++) {
			for (int i = 0; i < noise.length; i++) {
				noise[i]--;
				if (noise[i] == -1) {
					negative++;
				}
			}
		}
		return Arrays.copyOfRange(noise, negative, noise.length);
	}

	//	private void updateNoiseDataStructureAfterRemoval(final String alteredCaseId, final boolean isFirstRemovalForCase) {
	//		boolean shift = false;
	//		for (int i = 0; i < noise.get(alteredCaseId).length; i++) {
	//			noise.get(alteredCaseId)[i]--;
	//			if (isFirstRemovalForCase) {
	//				noise.get(alteredCaseId)[i]--;
	//			}
	//			if (noise.get(alteredCaseId)[i] < 0) {
	//				shift = true;
	//			}
	//		}
	//		if (shift) {
	//			if (noise.get(alteredCaseId).length > 1) {
	//				noise.put(alteredCaseId,
	//						Arrays.copyOfRange(noise.get(alteredCaseId), 1, noise.get(alteredCaseId).length));
	//			} else {
	//				noise.remove(alteredCaseId);
	//			}
	//			if (isFirstRemovalForCase && noise.containsKey(alteredCaseId)) {
	//				if (noise.get(alteredCaseId)[0] < 0) {
	//					if (noise.get(alteredCaseId).length > 1) {
	//						noise.put(alteredCaseId,
	//								Arrays.copyOfRange(noise.get(alteredCaseId), 1, noise.get(alteredCaseId).length));
	//					} else {
	//						noise.remove(alteredCaseId);
	//					}
	//				}
	//			}
	//		}
	//	}

}
