/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.framework.util;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class ThreadInfoReport extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ThreadInfoReport(String failedMonitor) {
		super(getThreadDump(failedMonitor));
	}

	public static String getThreadDump(String failedMonitor) {
		long currentId = Thread.currentThread().getId();
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		StringBuilder dump = new StringBuilder("Thread dump"); //$NON-NLS-1$
		ThreadInfo[] infos = threadMXBean.dumpAllThreads(threadMXBean.isObjectMonitorUsageSupported(), threadMXBean.isSynchronizerUsageSupported());
		for (ThreadInfo info : infos) {
			dumpThreadIDNameState(info, dump);
			dumpLockInfo(currentId, failedMonitor, info, dump);
			dumpStackTrace(info, dump);
		}
		return dump.toString();
	}

	private static void dumpThreadIDNameState(ThreadInfo info, StringBuilder dump) {
		dump.append('\n').append('\n');
		dump.append("ThreadId: ").append(info.getThreadId()); //$NON-NLS-1$
		dump.append(" ThreadName: ").append(info.getThreadName()); //$NON-NLS-1$
		dump.append(" ThreadState: ").append(info.getThreadState()); //$NON-NLS-1$
	}

	private static void dumpLockInfo(long currentId, String failedMonitor, ThreadInfo info, StringBuilder dump) {
		dump.append('\n');
		dump.append("  Blocked On: "); //$NON-NLS-1$
		LockInfo blockedOn = info.getLockInfo();
		if (blockedOn == null) {
			if (currentId == info.getThreadId() && failedMonitor != null) {
				dump.append(failedMonitor);
			} else {
				dump.append("none"); //$NON-NLS-1$
			}
		} else {
			dump.append(blockedOn.toString());
			dump.append(" LockOwnerId: ").append(info.getLockOwnerId()); //$NON-NLS-1$
			dump.append(" LockOwnerName: ").append(info.getLockOwnerName()); //$NON-NLS-1$
		}
		dump.append('\n');

		dump.append("  Synchronizers Locked: "); //$NON-NLS-1$
		LockInfo[] synchronizers = info.getLockedSynchronizers();
		if (synchronizers.length == 0) {
			dump.append("none"); //$NON-NLS-1$
		} else {
			for (LockInfo sync : synchronizers) {
				dump.append('\n');
				dump.append("    ").append(sync.toString()); //$NON-NLS-1$
			}
		}
		dump.append('\n');

		dump.append("  Monitors Locked: "); //$NON-NLS-1$
		MonitorInfo[] monitors = info.getLockedMonitors();
		if (monitors.length == 0) {
			dump.append("none"); //$NON-NLS-1$
		}
		for (MonitorInfo monitor : monitors) {
			dump.append('\n');
			dump.append("    ").append(monitor.toString()); //$NON-NLS-1$
		}
		dump.append('\n');
	}

	private static void dumpStackTrace(ThreadInfo info, StringBuilder dump) {
		dump.append("  Stack Trace: "); //$NON-NLS-1$
		for (StackTraceElement e : info.getStackTrace()) {
			dump.append('\n').append("    ").append(e); //$NON-NLS-1$
		}
	}
}
