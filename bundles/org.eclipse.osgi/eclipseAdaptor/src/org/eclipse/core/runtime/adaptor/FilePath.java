/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;

import java.io.File;

/** 
 * Helper class for manipulating absolute paths.
 */
public class FilePath {
	private final static String CURRENT_DIR = "."; //$NON-NLS-1$
	/** 
	 * Device separator character constant ":" used in paths.
	 */
	public static final char DEVICE_SEPARATOR = ':';
	private static final byte HAS_LEADING = 1;
	private static final byte HAS_TRAILING = 4;
	/** Constant value indicating no segments */
	private static final String[] NO_SEGMENTS = new String[0];
	private final static String PARENT_DIR = ".."; //$NON-NLS-1$
	private final static char SEPARATOR = '/';
	private final static String UNC_SLASHES = "//"; //$NON-NLS-1$
	// if UNC, device will be \\host\share, otherwise, it will be letter/name + colon
	private String device;
	private byte flags;
	private String[] segments;

	public FilePath(File location) {
		initialize(location.getPath());
		if (location.isDirectory())
			flags |= HAS_TRAILING;
		else
			flags &= ~HAS_TRAILING;
	}

	public FilePath(String original) {
		initialize(original);
	}

	/**
	 * Returns the number of segments in the given path
	 */
	private int computeSegmentCount(String path) {
		int len = path.length();
		if (len == 0 || (len == 1 && path.charAt(0) == SEPARATOR))
			return 0;
		int count = 1;
		int prev = -1;
		int i;
		while ((i = path.indexOf(SEPARATOR, prev + 1)) != -1) {
			if (i != prev + 1 && i != len)
				++count;
			prev = i;
		}
		if (path.charAt(len - 1) == SEPARATOR)
			--count;
		return count;
	}

	private String[] computeSegments(String path) {
		int maxSegmentCount = computeSegmentCount(path);
		if (maxSegmentCount == 0)
			return NO_SEGMENTS;
		String[] newSegments = new String[maxSegmentCount];
		int len = path.length();
		// allways absolute
		int firstPosition = isAbsolute() ? 1 : 0;
		int lastPosition = hasTrailingSlash() ? len - 2 : len - 1;
		// for non-empty paths, the number of segments is 
		// the number of slashes plus 1, ignoring any leading
		// and trailing slashes
		int next = firstPosition;
		int actualSegmentCount = 0;
		for (int i = 0; i < maxSegmentCount; i++) {
			int start = next;
			int end = path.indexOf(SEPARATOR, next);
			next = end + 1;
			String segment = path.substring(start, end == -1 ? lastPosition + 1 : end);
			if (CURRENT_DIR.equals(segment))
				continue;
			if (PARENT_DIR.equals(segment)) {
				if (actualSegmentCount > 0)
					actualSegmentCount--;
				continue;
			}
			newSegments[actualSegmentCount++] = segment;
		}
		if (actualSegmentCount == newSegments.length)
			return newSegments;
		if (actualSegmentCount == 0)
			return NO_SEGMENTS;
		String[] actualSegments = new String[actualSegmentCount];
		System.arraycopy(newSegments, 0, actualSegments, 0, actualSegments.length);
		return actualSegments;
	}

	private boolean hasTrailingSlash() {
		return (flags & HAS_TRAILING) != 0;
	}

	private void initialize(String original) {
		original = original.indexOf('\\') == -1 ? original : original.replace('\\', SEPARATOR);
		int deviceSeparatorPos = original.indexOf(DEVICE_SEPARATOR);
		if (deviceSeparatorPos >= 0) {
			//extract device if any				
			//remove leading slash from device part to handle output of URL.getFile()
			int start = original.charAt(0) == SEPARATOR ? 1 : 0;
			device = original.substring(start, deviceSeparatorPos + 1);
			original = original.substring(deviceSeparatorPos + 1, original.length());
		} else if (original.startsWith(UNC_SLASHES)) {
			// handle UNC paths
			int uncPrefixEnd = original.indexOf(SEPARATOR, 2);
			if (uncPrefixEnd >= 0)
				uncPrefixEnd = original.indexOf(SEPARATOR, uncPrefixEnd + 1);
			if (uncPrefixEnd >= 0) {
				device = original.substring(0, uncPrefixEnd);
				original = original.substring(uncPrefixEnd, original.length());
			} else
				// not a valid UNC
				throw new IllegalArgumentException("Not a valid UNC: " + original); //TODO add message
		}
		// device names letters and UNCs properly stripped off
		if (original.charAt(0) == SEPARATOR)
			flags |= HAS_LEADING;
		if (original.charAt(original.length() - 1) == SEPARATOR)
			flags |= HAS_TRAILING;
		segments = computeSegments(original);
	}

	boolean isAbsolute() {
		return (flags & HAS_LEADING) != 0;
	}

	public String makeRelative(FilePath location) {
		if (location.device != null && !location.device.equalsIgnoreCase(this.device))
			return location.toString();
		int baseCount = this.segments.length;
		int count = this.matchingFirstSegments(location);
		if (baseCount == count && count == location.segments.length)
			return location.hasTrailingSlash() ? ("." + SEPARATOR) : "."; //$NON-NLS-1$ //$NON-NLS-2$
		StringBuffer relative = new StringBuffer(); //$NON-NLS-1$	
		for (int j = 0; j < baseCount - count; j++)
			relative.append(PARENT_DIR + SEPARATOR); //$NON-NLS-1$
		for (int i = 0; i < location.segments.length - count; i++) {
			relative.append(location.segments[count + i]);
			relative.append(SEPARATOR);
		}
		if (!location.hasTrailingSlash())
			relative.deleteCharAt(relative.length() - 1);
		return relative.toString();
	}

	public int matchingFirstSegments(FilePath anotherPath) {
		int anotherPathLen = anotherPath.segments.length;
		int max = Math.min(segments.length, anotherPathLen);
		int count = 0;
		for (int i = 0; i < max; i++) {
			if (!segments[i].equals(anotherPath.segments[i]))
				return count;
			count++;
		}
		return count;
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		if (device != null)
			result.append(device);
		if (isAbsolute())
			result.append(SEPARATOR);
		for (int i = 0; i < segments.length; i++) {
			result.append(segments[i]);
			result.append(SEPARATOR);
		}
		if (segments.length > 0 && !hasTrailingSlash())
			result.deleteCharAt(result.length() - 1);
		return result.toString();
	}
}
