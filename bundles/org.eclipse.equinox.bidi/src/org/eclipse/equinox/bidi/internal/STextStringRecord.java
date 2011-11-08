/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.bidi.internal;

import java.lang.ref.SoftReference;
import org.eclipse.equinox.bidi.STextTypeHandlerFactory;

/**
 * Records strings which contain structured text. Several static
 * methods in this class allow to record such strings in a pool, and to find if
 * a given string is member of the pool.
 * <p>
 * Instances of this class are the records which are members of the pool.
 * </p><p>
 * The pool is managed as a cyclic list. When the pool is full,
 * each new element overrides the oldest element in the list.
 * </p><p>
 * A string may be itself entirely a structured text, or it may contain
 * segments each of which is a structured text of a given type. Each such
 * segment is identified by its starting and ending offsets within the
 * string, and by the handler which is appropriate to handle it.
 */
public class STextStringRecord {
	/**
	 * Number of entries in the pool of recorded strings
	 */
	public static final int POOLSIZE = 100;

	// maximum index allowed
	private static final int MAXINDEX = POOLSIZE - 1;

	// index of the last entered record
	private static int last = -1;

	// flag indicating that the pool has wrapped around
	private static boolean wrapAround;

	// the pool
	private static SoftReference[] recordRefs = new SoftReference[POOLSIZE];

	// hash code of the recorded strings
	private static int[] hashArray = new int[POOLSIZE];

	// total number of segments in the record
	private int totalSegmentCount;

	// number of used segments in the record
	private int usedSegmentCount;

	// reference to the recorded string
	private String string;

	// reference to the handlers of the STT segments in the recorded string
	private String[] handlers;

	// reference to the boundaries of the STT segments in the recorded string
	// (entries 0, 2, 4, ... are start offsets; entries 1, 3, 5, ... are
	// ending offsets)
	private short[] boundaries;

	/**
	 * Constructor
	 */
	private STextStringRecord() {
		// inhibit creation of new instances by customers
	}

	/**
	 * Records a string in the pool. The caller must specify the number
	 * of segments in the record (at least 1), and the handler, starting
	 * and ending offsets for the first segment.
	 *
	 * @param  string the string to record.
	 * @param  segmentCount number of segments allowed in this string.
	 *         This number must be >= 1.
	 * @param  handlerID identifier for the handler appropriate to handle 
	 *         the type of structured text present in the first segment.
	 *         It may be one of the predefined identifiers in 
	 *         {@link STextTypeHandlerFactory}, or it may be an identifier
	 *         for a type handler created by a plug-in or by the application.
	 * @param  start offset in the string of the starting character of the first
	 *         segment. It must be >= 0 and less than the length of the string.
	 * @param  limit offset of the character following the first segment. It
	 *         must be greater than the <code>start</code> argument and
	 *         not greater than the length of the string.
	 *
	 * @return an instance of STextRecordString which represents this record.
	 *         This instance may be used to specify additional segments with
	 *         {@link #addSegment addSegment}.
	 *
	 * @throws IllegalArgumentException if <code>string</code> is null or
	 *         if <code>segmentCount</code> is less than 1.
	 * @throws also the same exceptions as {@link #addSegment addSegment}.
	 */
	public static STextStringRecord addRecord(String string, int segmentCount, String handlerID, int start, int limit) {
		if (string == null)
			throw new IllegalArgumentException("The string argument must not be null!"); //$NON-NLS-1$
		if (segmentCount < 1)
			throw new IllegalArgumentException("The segment count must be at least 1!"); //$NON-NLS-1$
		synchronized (recordRefs) {
			if (last < MAXINDEX)
				last++;
			else {
				wrapAround = true;
				last = 0;
			}
		}
		STextStringRecord record = null;
		if (recordRefs[last] != null)
			record = (STextStringRecord) recordRefs[last].get();
		if (record == null) {
			record = new STextStringRecord();
			recordRefs[last] = new SoftReference(record);
		}
		hashArray[last] = string.hashCode();
		for (int i = 0; i < record.usedSegmentCount; i++)
			record.handlers[i] = null;
		if (segmentCount > record.totalSegmentCount) {
			record.handlers = new String[segmentCount];
			record.boundaries = new short[segmentCount * 2];
			record.totalSegmentCount = segmentCount;
		}
		record.usedSegmentCount = 0;
		record.string = string;
		record.addSegment(handlerID, start, limit);
		return record;
	}

	/**
	 * Adds a second or further segment to a record.
	 *
	 * @param  handlerID identifier for the handler appropriate to handle 
	 *         the type of structured text present in the first segment.
	 *         It may be one of the predefined identifiers in 
	 *         {@link STextTypeHandlerFactory}, or it may be an identifier
	 *         for a type handler created by a plug-in or by the application.
	 * @param  start offset in the string of the starting character of the
	 *         segment. It must be >= 0 and less than the length of the string.
	 * @param  limit offset of the character following the segment. It must be
	 *         greater than the <code>start</code> argument and not greater
	 *         than the length of the string.
	 *
	 * @throws IllegalArgumentException if <code>handlerID</code> is null,
	 *         or if <code>start</code> or <code>limit</code> have invalid
	 *         values.
	 * @throws IllegalStateException if the current segment exceeds the
	 *         number of segments specified by <code>segmentCount</code>
	 *         in the call to {@link #addRecord addRecord} which created
	 *         the STextStringRecord instance.
	 */
	public void addSegment(String handlerID, int start, int limit) {
		if (handlerID == null)
			throw new IllegalArgumentException("The handlerID argument must not be null!"); //$NON-NLS-1$
		if (start < 0 || start >= string.length())
			throw new IllegalArgumentException("The start position must be at least 0 and less than the length of the string!"); //$NON-NLS-1$
		if (limit <= start || limit > string.length())
			throw new IllegalArgumentException("The limit position must be greater than the start position but no greater than the length of the string!"); //$NON-NLS-1$
		if (usedSegmentCount >= totalSegmentCount)
			throw new IllegalStateException("All segments of the record are already used!"); //$NON-NLS-1$
		handlers[usedSegmentCount] = handlerID;
		boundaries[usedSegmentCount * 2] = (short) start;
		boundaries[usedSegmentCount * 2 + 1] = (short) limit;
		usedSegmentCount++;
	}

	/**
	 * Checks if a string is recorded and retrieves its record.
	 *
	 * @param  string the string to check.
	 *
	 * @return <code>null</code> if the string is not recorded in the pool;
	 *         otherwise, return the <code>STextStringRecord</code> instance 
	 *         which records this string.<br>
	 *         Once a record has been found, the number of its segments can
	 *         be retrieved using {@link #getSegmentCount getSegmentCount},
	 *         its handler ID can
	 *         be retrieved using {@link #getHandler getHandler},
	 *         its starting offset can
	 *         be retrieved using {@link #getStart getStart},
	 *         its ending offset can
	 *         be retrieved using {@link #getLimit getLimit},
	 */
	public static STextStringRecord getRecord(String string) {
		if (last < 0) // no records at all
			return null;
		if (string == null || string.length() < 1)
			return null;
		STextStringRecord record;
		int myLast = last;
		int hash = string.hashCode();
		for (int i = myLast; i >= 0; i--) {
			if (hash != hashArray[i])
				continue;
			record = (STextStringRecord) recordRefs[i].get();
			if (record == null)
				continue;
			if (string.equals(record.string))
				return record;
		}
		if (!wrapAround) // never recorded past myLast
			return null;
		for (int i = MAXINDEX; i > myLast; i--) {
			if (hash != hashArray[i])
				continue;
			record = (STextStringRecord) recordRefs[i].get();
			if (record == null)
				continue;
			if (string.equals(record.string))
				return record;
		}
		return null;
	}

	/**
	 * Retrieves the number of segments in a record.
	 * 
	 * @return the number of segments in the current record
	 */
	public int getSegmentCount() {
		return usedSegmentCount;
	}

	private void checkSegmentNumber(int segmentNumber) {
		if (segmentNumber >= usedSegmentCount)
			throw new IllegalArgumentException("The segment number " + segmentNumber + " is greater than the total number of segments = " + usedSegmentCount + "!"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Retrieves the handler ID of a given segment.
	 *
	 * @param  segmentNumber number of the segment about which information
	 *         is required. It must be >= 0 and less than the number of
	 *         segments returned  by {@link #getSegmentCount}.
	 *
	 * @return the handler ID of the handler appropriate to 
	 *         process the structured text in the segment
	 *         specified by <code>segmentNumber</code>.
	 *
	 * @throws IllegalArgumentException if <code>segmentNumber</code>
	 *         has an invalid value.
	 */
	public String getHandler(int segmentNumber) {
		checkSegmentNumber(segmentNumber);
		return handlers[segmentNumber];
	}

	/**
	 * Retrieves the starting offset of a given segment.
	 *
	 * @param  segmentNumber number of the segment about which information
	 *         is required. It must be >= 0 and less than the number of
	 *         segments returned  by {@link #getSegmentCount}.
	 *
	 * @return the starting offset within the string of the segment
	 *         specified by <code>segmentNumber</code>.
	 *
	 * @throws IllegalArgumentException if <code>segmentNumber</code>
	 *         has an invalid value.
	 */
	public int getStart(int segmentNumber) {
		checkSegmentNumber(segmentNumber);
		return boundaries[segmentNumber * 2];
	}

	/**
	 * Retrieves the ending offset of a given segment.
	 *
	 * @param  segmentNumber number of the segment about which information
	 *         is required. It must be >= 0 and less than the number of
	 *         segments returned  by {@link #getSegmentCount}.
	 *
	 * @return the offset of the position following the segment
	 *         specified by <code>segmentNumber</code>.
	 *
	 * @throws IllegalArgumentException if <code>segmentNumber</code>
	 *         has an invalid value.
	 */
	public int getLimit(int segmentNumber) {
		checkSegmentNumber(segmentNumber);
		return boundaries[segmentNumber * 2 + 1];
	}

	/**
	 * Clears the pool. All elements of the pool are erased and any associated
	 * memory is freed.
	 */
	public static synchronized void clear() {
		for (int i = 0; i <= MAXINDEX; i++) {
			hashArray[i] = 0;
			SoftReference softRef = recordRefs[i];
			if (softRef == null)
				continue;
			STextStringRecord record = (STextStringRecord) softRef.get();
			if (record == null)
				continue;
			record.boundaries = null;
			record.handlers = null;
			record.totalSegmentCount = 0;
			record.usedSegmentCount = 0;
			recordRefs[i].clear();
		}
		last = -1;
		wrapAround = false;
	}

}
