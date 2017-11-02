
package org.processmining.streambasedeventfilter.util;

import java.util.ArrayList;
import java.util.List;

import org.processmining.eventstream.core.interfaces.XSEvent;

public class XSEventUtils {

	public static List<String> convertByKey(final List<XSEvent> list, final String key) {
		List<String> result = new ArrayList<>();
		for (XSEvent e : list) {
			result.add(e.get(key).toString());
		}
		return result;
	}

}
