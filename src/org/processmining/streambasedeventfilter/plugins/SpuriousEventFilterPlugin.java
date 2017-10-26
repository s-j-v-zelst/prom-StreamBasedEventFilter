package org.processmining.streambasedeventfilter.plugins;

import java.util.ArrayList;
import java.util.List;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.eventstream.connections.XSEventXSAuthorXSStreamConnectionImpl;
import org.processmining.eventstream.core.factories.XSEventStreamFactory;
import org.processmining.eventstream.core.interfaces.XSEventStream;
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
import org.processmining.streambasedeventlog.algorithms.ConditionalProbabilitiesBasedXSEventFilterImpl;
import org.processmining.streambasedeventlog.help.StreamBasedEventLogHelp;
import org.processmining.streambasedeventlog.parameters.ConditionalProbabilitiesBasedXSEventFilterParametersImpl;
import org.processmining.streambasedeventlog.parameters.StreamBasedEventLogParametersImpl;
import org.processmining.streambasedeventlog.parameters.StreamBasedEventStorageParametersImpl;

@Plugin(name = "Spurious Event Filter", parameterLabels = { "Event Stream", "Parameters" }, returnLabels = { "Hub",
		"Event Stream" }, returnTypes = { XSEventHub.class, XSEventStream.class }, help = StreamBasedEventLogHelp.TEXT)
public class SpuriousEventFilterPlugin {

	@PluginVariant(variantLabel = "Spurious Event Filter, stream / parameters", requiredParameterLabels = { 0, 1 })
	public Object[] run(final PluginContext context, final XSEventStream stream,
			final StreamBasedEventStorageParametersImpl parameters) {
		//		XSEventHub hub = new TrieBasedSpuriousEventFilterStatisticsImpl(parameters,
		//				new TrieBasedSpurioiusEventFilterParametersImpl());
		XSEventHub hub = new ConditionalProbabilitiesBasedXSEventFilterImpl(
				new ConditionalProbabilitiesBasedXSEventFilterParametersImpl(),
				new StreamBasedEventLogParametersImpl());
		XSEventStream out = XSEventStreamFactory.createXSEventStream(CommunicationType.SYNC);
		out.start();
		out.connect(hub);
		hub.start();
		stream.connect(hub);
		return new Object[] { hub, out };
	}

	@PluginVariant(variantLabel = "Spurious Event Filter, stream", requiredParameterLabels = { 0 })
	public Object[] runDefault(PluginContext context, XSEventStream stream) {
		StreamBasedEventStorageParametersImpl parameters = new StreamBasedEventStorageParametersImpl();
		return run(context, stream, parameters);
	}

	@UITopiaVariant(affiliation = "Eindhoven University of Technology", author = "Sebastiaan J. van Zelst", email = "s.j.v.zelst@tue.nl")
	@PluginVariant(variantLabel = "Spurious Event Filter, UI", requiredParameterLabels = { 0 })
	public Object[] runUI(final UIPluginContext context, final XSEventStream stream) {
		return run(context, stream, new StreamBasedEventStorageParametersImpl());
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
					return run(context, dialog.getSelectedStream(), new StreamBasedEventStorageParametersImpl());
				}
			}
		} catch (ConnectionCannotBeObtained e) {
		}
		context.getFutureResult(0).cancel(true);
		return null;
	}

}
