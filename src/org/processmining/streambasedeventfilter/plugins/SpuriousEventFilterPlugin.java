package org.processmining.streambasedeventfilter.plugins;

import java.util.ArrayList;
import java.util.List;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.eventstream.connections.XSEventXSAuthorXSStreamConnectionImpl;
import org.processmining.eventstream.core.factories.XSEventStreamFactory;
import org.processmining.eventstream.core.interfaces.XSEvent;
import org.processmining.eventstream.core.interfaces.XSEventStream;
import org.processmining.eventstream.core.interfaces.XSStaticXSEventStream;
import org.processmining.eventstream.dialogs.XSEventStreamConnectionDialogImpl;
import org.processmining.eventstream.models.XSEventAuthor;
import org.processmining.eventstream.models.XSEventHub;
import org.processmining.framework.connections.Connection;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.stream.connections.XSAuthorXSStreamConnectionImpl;
import org.processmining.stream.core.enums.CommunicationType;
import org.processmining.streambasedeventfilter.algorithms.ConditionalProbabilitiesBasedXSEventFilterImpl;
import org.processmining.streambasedeventfilter.parameters.ConditionalProbabilitiesBasedXSEventFilterParametersImpl;
import org.processmining.streambasedeventstorage.parameters.XSEventStoreSlidingWindowParametersImpl;

@Plugin(name = "Spurious Event Filter", parameterLabels = { "Event Stream", "Parameters" }, returnLabels = { "Hub",
		"Event Stream" }, returnTypes = { XSEventHub.class, XSEventStream.class }, help = "Spurious Event Filter")
public class SpuriousEventFilterPlugin {

	@PluginVariant(variantLabel = "Spurious Event Filter, stream / parameters", requiredParameterLabels = { 0, 1 })
	public Object[] run(final PluginContext context, final XSEventStream stream,
			final XSEventStoreSlidingWindowParametersImpl parameters) {
		ConditionalProbabilitiesBasedXSEventFilterParametersImpl filterParams = new ConditionalProbabilitiesBasedXSEventFilterParametersImpl();
		XSEventStoreSlidingWindowParametersImpl storageParams = new XSEventStoreSlidingWindowParametersImpl();
		XSEventHub hub = new ConditionalProbabilitiesBasedXSEventFilterImpl(filterParams, storageParams);
		XSEventStream out = XSEventStreamFactory.createXSEventStream(CommunicationType.SYNC);
		out.start();
		out.connect(hub);
		hub.start();
		stream.connect(hub);
		return new Object[] { hub, out };
	}

	@UITopiaVariant(affiliation = "Eindhoven University of Technology", author = "Sebastiaan J. van Zelst", email = "s.j.v.zelst@tue.nl")
	@PluginVariant(variantLabel = "Spurious Event Filter, static stream", requiredParameterLabels = { 0 })
	public Object[] runStatic(final PluginContext context, final XSStaticXSEventStream stream) {
		ConditionalProbabilitiesBasedXSEventFilterParametersImpl filterParams = new ConditionalProbabilitiesBasedXSEventFilterParametersImpl();
		XSEventStoreSlidingWindowParametersImpl storageParams = new XSEventStoreSlidingWindowParametersImpl();
		filterParams.setContextAware(false);
		filterParams.setExperiment(false);
		ConditionalProbabilitiesBasedXSEventFilterImpl filter = new ConditionalProbabilitiesBasedXSEventFilterImpl(
				filterParams, storageParams);
		for (XSEvent e : stream) {
			//			System.out.println("Delivery of: " + e.toString());
			filter.processEvent(e);
		}
		System.out.println("Filter done");
		System.out.println("Original number of events: " + stream.size());
		System.out.println("After filtering: " + filter.fetchFilteredStream().size());
		return new Object[2];

	}

	@PluginVariant(variantLabel = "Spurious Event Filter, stream", requiredParameterLabels = { 0 })
	public Object[] runDefault(PluginContext context, XSEventStream stream) {
		XSEventStoreSlidingWindowParametersImpl parameters = new XSEventStoreSlidingWindowParametersImpl();
		return run(context, stream, parameters);
	}

	@UITopiaVariant(affiliation = "Eindhoven University of Technology", author = "Sebastiaan J. van Zelst", email = "s.j.v.zelst@tue.nl")
	@PluginVariant(variantLabel = "Spurious Event Filter, UI", requiredParameterLabels = { 0 })
	public Object[] runUI(final UIPluginContext context, final XSEventStream stream) {
		//TODO: make ProM-user dialog...
		return run(context, stream, new XSEventStoreSlidingWindowParametersImpl());
	}

	@UITopiaVariant(affiliation = "Eindhoven University of Technology", author = "Sebastiaan J. van Zelst", email = "s.j.v.zelst@tue.nl")
	@PluginVariant(variantLabel = "Spurious Event Filter, UI", requiredParameterLabels = { 0 })
	public Object[] runUI(final UIPluginContext context, final XSEventAuthor author) {
		List<XSEventStream> availableStreamsOfAuthor = new ArrayList<>();
		try {
			for (Connection c : context.getConnectionManager()
					.getConnections(XSEventXSAuthorXSStreamConnectionImpl.class, context, author)) {
				XSEventStream stream = c.getObjectWithRole(XSAuthorXSStreamConnectionImpl.KEY_STREAM);
				availableStreamsOfAuthor.add(stream);
			}
			if (!availableStreamsOfAuthor.isEmpty()) {
				XSEventStreamConnectionDialogImpl dialog = new XSEventStreamConnectionDialogImpl(
						availableStreamsOfAuthor);
				if (context.showWizard("Select Stream", true, true, dialog).equals(InteractionResult.FINISHED)) {
					return run(context, dialog.getSelectedStream(), new XSEventStoreSlidingWindowParametersImpl());
				}
			}
		} catch (ConnectionCannotBeObtained e) {
		}
		context.getFutureResult(0).cancel(true);
		return null;
	}

}
