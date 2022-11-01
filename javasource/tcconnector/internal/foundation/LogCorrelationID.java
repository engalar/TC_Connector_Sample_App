// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.internal.foundation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Stack;

/**
 * The LogCorrelationID class is used to create unique IDs that can be passed
 * from tier to tier with a service request, so that any log messages on each
 * tier that processes the service request can be correlated with logs on other
 * tiers.
 *
 * By default the ID will have the host name and process/thread ID, the SOA
 * Framework will add information to uniquely identify each service request, so
 * the ID sent with the service request to the server will have the form:
 * 
 * <pre>
 * MxTcc.process.sessionID.request
 * where:
 *  process:    Unique Id for the process/thread
 *  connection: Index of the Connection instance associated with this request
 *  sessionID:  A unique ID for the current client session
 *  request:    Counter of service request for this Connection
 *
 * example:
 *  MxTcC.45089.5673356.00124
 * </pre>
 *
 *
 */
public class LogCorrelationID {
	private static Hashtable<Thread, LogCorrelationID> LOG_IDS = new Hashtable<Thread, LogCorrelationID>();

	/**
	 * Push information onto the end of the ID string. Each element is delimited by
	 * a '.' in the ID string.
	 *
	 * @param id Element to add to the end of the ID string
	 */
	public static void push(String id) {
		LogCorrelationID.getCurrent().pushID(id);
	}

	/**
	 * Pop the last element from the ID string. Each element is delimited by a '.'
	 * in the ID string.
	 *
	 * @return The value of the element being removed from the ID string
	 */
	public static String pop() {
		return LogCorrelationID.getCurrent().popID();
	}

	/**
	 * Get the ID string
	 *
	 * @return the ID string
	 */
	public static String getId() {
		return LogCorrelationID.getCurrent().extractID();
	}

	private static synchronized LogCorrelationID getCurrent() {
		Thread key = Thread.currentThread();

		LogCorrelationID currentLog = LogCorrelationID.LOG_IDS.get(key);
		if (currentLog == null) {
			currentLog = new LogCorrelationID();
			LogCorrelationID.LOG_IDS.put(key, currentLog);
			LogCorrelationID.removeStaleIDs();
		}

		return currentLog;
	}

	private static void removeStaleIDs() {

		List<Thread> toRemove = new ArrayList<Thread>();
		for (Thread threadKey : LogCorrelationID.LOG_IDS.keySet()) {
			if (!threadKey.isAlive()) {
				toRemove.add(threadKey);
			}
		}
		for (Thread staleThread : toRemove) {
			LogCorrelationID.LOG_IDS.remove(staleThread);
		}
	}

	private Stack<String> stack = new Stack<String>();
	private int extraElements = 0;

	private LogCorrelationID() {
		Random r = new Random();
		DecimalFormat df = new DecimalFormat("00000");
		stack.push("MxTcC");
		stack.push(df.format(r.nextInt(100000)));
	}

	/**
	 * Push information onto the end of the ID string. Each element is delimited by
	 * a '.' in the ID string.
	 *
	 * @param id Element to add to the end of the ID string
	 */
	private void pushID(String id) {
		// Don't let the stack grow uncontrollably, cap it at 10
		if (stack.size() < 10) {
			stack.push(id);
		} else
			extraElements++;
	}

	/**
	 * Pop the last element from the ID string. Each element is delimited by a '.'
	 * in the ID string.
	 *
	 * @return The value of the element being removed from the ID string
	 */
	public String popID() {
		if (stack.size() > 0) {
			if (extraElements == 0)
				return stack.pop();
			extraElements--;
			return "";
		}
		Constants.LOGGER.warn("Tried top pop past the end of the LogCorrelationID stack.");
		return "";
	}

	/**
	 * Get the ID string
	 *
	 * @return the ID string
	 */
	public String extractID() {
		StringBuffer id = new StringBuffer();
		for (String element : stack) {
			if (id.length() > 0) {
				id.append(".");
			}
			id.append(element);
		}
		return id.toString();
	}
}
