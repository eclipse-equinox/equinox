package org.eclipse.osgi.internal.serviceregistry;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockExt extends ReentrantLock {
	private static final long serialVersionUID = 161034782199227436L;

	public ReentrantLockExt() {
		super();
	}

	public ReentrantLockExt(boolean fair) {
		super(fair);
	}

	/**
	 * Returns a string identifying this lock, as well as its lock state. The state,
	 * in brackets, includes either the String {@code "Unlocked"} or the String
	 * {@code "Locked by"} followed by the {@linkplain Thread#getName name} of the
	 * owning thread.
	 *
	 * @return a string identifying this lock, as well as its lock state
	 */
	@SuppressWarnings("nls")
	@Override
	public String toString() {
		Thread o = getOwner();

		if (o != null) {
			try {
				ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
				ThreadInfo threadInfo = threadMXBean.getThreadInfo(o.getId(), Integer.MAX_VALUE);
				StackTraceElement[] trace = threadInfo.getStackTrace();
					StringBuilder sb = new StringBuilder("\"" + o.getName() + "\"" + (o.isDaemon() ? " daemon" : "")
						+ " prio=" + o.getPriority() + " Id=" + o.getId() + " " + o.getState());

				for (StackTraceElement traceElement : trace)
					sb.append("\tat " + traceElement + "\n");

				return super.toString() + "[Locked by thread " + o.getName() + "], Details:\n" + sb.toString();
			} catch (Exception e) {
				// do nothing and fall back to just the default, thread might be gone
			}
		}
		return super.toString() + "[Unlocked]";
	}
}