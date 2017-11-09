package org.processmining.streambasedeventfilter.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.processmining.eventstream.core.interfaces.XSEvent;
import org.processmining.framework.util.Pair;
import org.processmining.streambasedeventfilter.algorithms.abstr.AbstractXSEventFilterImpl;
import org.processmining.streambasedeventfilter.parameters.AdjustmentMethod;
import org.processmining.streambasedeventfilter.parameters.ConditionalProbabilitiesBasedXSEventFilterParametersImpl;
import org.processmining.streambasedeventfilter.parameters.FilteringMethod;
import org.processmining.streambasedeventfilter.util.XSEventUtils;
import org.processmining.streambasedeventlog.algorithms.NaiveEventCollectorImpl;
import org.processmining.streambasedeventlog.parameters.StreamBasedEventLogParametersImpl;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class ConditionalProbabilitiesBasedXSEventFilterImpl
		extends AbstractXSEventFilterImpl<ConditionalProbabilitiesBasedXSEventFilterParametersImpl> {

	// cases that are no longer complete in the store.
	private final Collection<String> alteredCases = new HashSet<>();
	private final String ARTIFICIAL_START_SYMBOL = "__ARTIFICIAL_START__" + System.currentTimeMillis();
	private final NaiveEventCollectorImpl<StreamBasedEventLogParametersImpl> collector;
	private final Map<Collection<String>, TObjectIntMap<String>> followsRelation = new HashMap<>();
	private final Map<String, TObjectIntMap<Collection<String>>> precedesRelation = new HashMap<>();
	// registry that keeps track of indices in stored traces that relate to noise 
	private final Map<String, int[]> noise = new HashMap<>();



	@Deprecated // -> make publicly available via event collector...
	private final StreamBasedEventLogParametersImpl storageParams;

	public ConditionalProbabilitiesBasedXSEventFilterImpl(
			ConditionalProbabilitiesBasedXSEventFilterParametersImpl filterParameters,
			StreamBasedEventLogParametersImpl storageParams) {
		super("spurious_event_filter_conditional_probs", filterParameters);
		collector = new NaiveEventCollectorImpl<StreamBasedEventLogParametersImpl>(storageParams);
		this.storageParams = storageParams;
	}

	private boolean classifyNewEventAsNoise(final String caseId, final List<String> trace) {
		FilteringMethod filtermethod = getFilterParameters().getFiltermethod();
		switch (filtermethod) {
		case Any: 
			return evaluateFollowsRelations(caseId, trace) || evaluatePrecedesRelation(caseId, trace);
			
		case BothDirections:
			return evaluateFollowsRelations(caseId, trace) || evaluatePrecedesRelation(caseId, trace);
			
		case Forward: 
			return evaluateFollowsRelations(caseId, trace);
			
		case Backward:
			return evaluatePrecedesRelation(caseId, trace);
			

		default:
			return evaluateFollowsRelations(caseId, trace) || evaluatePrecedesRelation(caseId, trace);
		} 
		
		
	}

	private List<String> constructNoiseAwarePrefix(final String caseId, final List<String> trace, final int length) {
		if (noise.containsKey(caseId)) {
			List<String> loopVar = new ArrayList<>(trace.subList(0, trace.size() - 1));
			List<String> prefix = new ArrayList<>();
			while (prefix.size() < length && loopVar.size() > 0) {
				if (!ArrayUtils.contains(noise.get(caseId), loopVar.size() - 1)) {
					prefix.add(0, loopVar.get(loopVar.size() - 1));
				}
				int b = loopVar.size() - 1;
				loopVar = new ArrayList<>(loopVar.subList(0, b));
			}
			return prefix;
		} else {
			return trace.subList(trace.size() - length - 1, trace.size() - 1);
		}

	}

	private Pair<String, List<String>> constructNoiseAwareSuffixAndFollowingActivity(final String caseId,
			final List<String> trace, final int length) {
		if (noise.containsKey(caseId)) {
			List<String> loopVar = new ArrayList<>(trace.subList(0, trace.size() - 1));
			List<String> suffix = new ArrayList<>(trace.subList(trace.size() - 1, trace.size()));
			int loopVarSize = loopVar.size();
			while (suffix.size() < length && loopVarSize > 0) {
				if (!ArrayUtils.contains(noise.get(caseId), loopVar.size() - 1)) {
					suffix.add(0, loopVar.get(loopVar.size() - 1));
				}
				int b = loopVar.size() - 1;
				loopVar = new ArrayList<>(loopVar.subList(0, b));
				loopVarSize = loopVar.size();
			}
			String act = null;
			while (loopVar.size() > 0) {
				if (!ArrayUtils.contains(noise.get(caseId), loopVar.size() - 1)) {
					act = loopVar.get(loopVar.size() - 1);
					break;
				}
				int b = loopVar.size() - 1;
				loopVar = loopVar.subList(0, b);
				loopVar = new ArrayList<>(loopVar.subList(0, b));
			}
			return new Pair<>(act, suffix);
		} else {
			return new Pair<>(trace.get(trace.size() - length - 1), trace.subList(trace.size() - length, trace.size()));
		}

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
	private boolean evaluateFollowsRelations(final String caseId, final List<String> trace) {
		final String newActivity = trace.get(trace.size() - 1);
		for (int i = 1; i <= getFilterParameters().getMaxPatternLength(); i++) {
			if (trace.size() > i) {
				List<String> prefix = constructNoiseAwarePrefix(caseId, trace, i);
				if (prefix.size() == i) {
					TObjectIntMap<String> distribution = followsRelation.get(prefix);
					int max = getMaximalFollowsValue(distribution);
					int Sum = getSumFollowsValue(distribution);
					int Count= getNZeroCountFollowsValue(distribution);
					double NZAvg=0;
					if (Count>0)
						NZAvg= (Sum*1.0)/Count;
					if (max == -1 || !distribution.containsKey(newActivity)) {
						
						return true;
					}else  {
						AdjustmentMethod adjustmethod= getFilterParameters().getAdjustmentmethod();
						switch (adjustmethod) {
						case None: if (distribution.get(newActivity) <= getFilterParameters().getCutoffThreshold() * (Sum)){
							return true;
							}else { return false;}
						case Max: if (distribution.get(newActivity) <= getFilterParameters().getCutoffThreshold() * (max)){
							return true;
							}else { return false;}
						case MaxNZAvg: if (distribution.get(newActivity) <= getFilterParameters().getCutoffThreshold() * (max- NZAvg)){
							return true;
							}else { return false;}				
		
						} // Switch
						 
					} // main computation
				} else {
					return false;
				}
			} else {
				return false;
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
	private boolean evaluatePrecedesRelation(final String caseId, final List<String> trace) {
		for (int i = 1; i <= getFilterParameters().getMaxPatternLength(); i++) {
			if (trace.size() > i) {
				Pair<String, List<String>> actSuffix = constructNoiseAwareSuffixAndFollowingActivity(caseId, trace, i);
				String act = actSuffix.getFirst();
				List<String> suffix = actSuffix.getSecond();
				if (act != null) {
					TObjectIntMap<Collection<String>> distribution = precedesRelation.get(act);
					int max = getMaximalPrecedesValue(distribution, suffix.size());
					int Sum=  getSumPrecedesValue(distribution, suffix.size());
					int Count = getNZeroCountPrecedesValue(distribution, suffix.size());
					double NZAvg=0;
					if (Count>0)
						NZAvg=(Sum*1.0)/Count;
					if (max == -1 || !distribution.containsKey(suffix)) {
						return true;
					} else {
						AdjustmentMethod adjustmethod= getFilterParameters().getAdjustmentmethod();
						switch (adjustmethod) {
						case None: if (distribution.get(suffix) <= getFilterParameters().getCutoffThreshold() * (Sum)){
							return true;
								}else { return false;}
						case Max: if (distribution.get(suffix) <= getFilterParameters().getCutoffThreshold() * (max)){
							return true;
								}else { return false;}
						case MaxNZAvg: if (distribution.get(suffix) <= getFilterParameters().getCutoffThreshold() * (max- NZAvg)){
							return true;
								}else { return false;}				
							}//switch
					} //main computation
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
		return false;
	}

	private void filter(final XSEvent event, final String caseId, final List<String> trace) {
		boolean isNoise = classifyNewEventAsNoise(caseId, trace);
		if (!isNoise) {
			write(event);
		} else {
			if (!noise.containsKey(caseId)) {
				noise.put(caseId, new int[0]);
			}
			int[] indices = Arrays.copyOf(noise.get(caseId), noise.get(caseId).length + 1);
			indices[indices.length - 1] = trace.size() - 1;
			noise.put(caseId, indices);
		}
		if (getFilterParameters().isExperiment()) {
			updateExperimentVariables(event, isNoise);
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
	private int getSumFollowsValue(final TObjectIntMap<String> distribution) {
		int Sum = 0;
		if (distribution != null) {
			for (String k : distribution.keySet()) {
				Sum = Sum + distribution.get(k);
			}
		}
		
		return Sum;
	}
	private int getNZeroCountFollowsValue(final TObjectIntMap<String> distribution) {
		
		int Count=0;
		if (distribution != null) {
			for (String k : distribution.keySet()) {
				if (distribution.get(k)>0)
					Count++;
			}
		}
		
		return Count;
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

	private int getNZeroCountPrecedesValue(final TObjectIntMap<Collection<String>> distribution, final int patternLength) {
		int Count = 0;
		if (distribution != null) {
			for (Collection<String> coll : distribution.keySet()) {
				if (coll.size() == patternLength) { // we have to make sure that we only look at suffixes of the same length here!
					if( distribution.get(coll) >0);
					Count++;
				}
			}
		}
		return Count;
	}

	@Override
	protected void handleNextPacket(XSEvent event) {
		final String caseId = event.get(storageParams.getCaseIdentifier()).toString();
		collector.triggerPacketHandle(event);
		List<String> trace = XSEventUtils.convertByKey(collector.getCases().get(caseId),
				storageParams.getActivityIdentifier().toString());
		if (!alteredCases.contains(caseId)) {
			trace.add(0, ARTIFICIAL_START_SYMBOL);
		}
		updateConditionalProbabilityStructure(trace);
		if (collector.getSlidingWindow().size() >= storageParams.getSlidingWindowSize()
				&& (!getFilterParameters().isIgnoreTrainingCases()
						|| (getFilterParameters().isIgnoreTrainingCases() && !getTrainingCases().contains(caseId)))) {
			filter(event, caseId, trace);
		} else if (getFilterParameters().isIgnoreTrainingCases()) {
			getTrainingCases().add(caseId);
		}
	}

	private void incrementallyIncreaseFollowsRelation(final List<String> trace) {
		for (int i = 1; i <= getFilterParameters().getMaxPatternLength(); i++) {
			if (trace.size() > i) {
				List<String> prefix = trace.subList(trace.size() - i - 1, trace.size() - 1);
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
				List<String> suffix = trace.subList(trace.size() - i, trace.size());
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
					List<String> prefix = alteredTrace.subList(0, i);
					followsRelation.get(prefix).adjustValue(alteredTrace.get(i), -1);
				}
			}
		} else {
			for (int i = 0; i < diff; i++) {
				for (int j = i + 1; j <= i + getFilterParameters().getMaxPatternLength(); j++) {
					if (j < alteredTrace.size()) {
						List<String> prefix = alteredTrace.subList(i, j);
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
					List<String> suffix = alteredTrace.subList(i, i + 1);
					precedesRelation.get(act).adjustValue(suffix, -1);
				}
			}
		} else {
			for (int i = 0; i < diff; i++) {
				String act = alteredTrace.get(i);
				for (int j = i + 1; j <= i + getFilterParameters().getMaxPatternLength(); j++) {
					if (j < alteredTrace.size()) {
						List<String> suffix = alteredTrace.subList(i + 1, j + 1);
						precedesRelation.get(act).adjustValue(suffix, -1);
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
			final List<String> alteredTrace = XSEventUtils.convertByKey(me.getValue().getFirst(),
					storageParams.getActivityIdentifier().toString());
			final List<String> newTrace = XSEventUtils.convertByKey(me.getValue().getSecond(),
					storageParams.getActivityIdentifier().toString());
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
			if (noise.containsKey(alteredCaseId)) {
				updateNoiseDataStructureAfterRemoval(alteredCaseId, isFirstRemovalForCase);
			}
		}
	}

	private void updateNoiseDataStructureAfterRemoval(final String alteredCaseId, final boolean isFirstRemovalForCase) {
		boolean shift = false;
		for (int i = 0; i < noise.get(alteredCaseId).length; i++) {
			noise.get(alteredCaseId)[i]--;
			if (isFirstRemovalForCase) {
				noise.get(alteredCaseId)[i]--;
			}
			if (noise.get(alteredCaseId)[i] < 0) {
				shift = true;
			}
		}
		if (shift) {
			if (noise.get(alteredCaseId).length > 1) {
				noise.put(alteredCaseId,
						Arrays.copyOfRange(noise.get(alteredCaseId), 1, noise.get(alteredCaseId).length));
			} else {
				noise.remove(alteredCaseId);
			}
			if (isFirstRemovalForCase && noise.containsKey(alteredCaseId)) {
				if (noise.get(alteredCaseId)[0] < 0) {
					if (noise.get(alteredCaseId).length > 1) {
						noise.put(alteredCaseId,
								Arrays.copyOfRange(noise.get(alteredCaseId), 1, noise.get(alteredCaseId).length));
					} else {
						noise.remove(alteredCaseId);
					}
				}
			}
		}
	}

}
