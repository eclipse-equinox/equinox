/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Patrick Tasse - Add extra constructor to Path class (bug 454959)
 *******************************************************************************/
package org.eclipse.core.runtime;

import java.io.File;
import java.util.Arrays;

/**
 * The standard implementation of the <code>IPath</code> interface. Paths are
 * always maintained in canonicalized form. That is, parent references (i.e.,
 * <code>../../</code>) and duplicate separators are resolved. For example,
 *
 * <pre>
 * new Path("/a/b").append("../foo/bar")
 * </pre>
 *
 * will yield the path
 *
 * <pre>
 *      /a/foo/bar
 * </pre>
 * <p>
 * This class can be used without OSGi running.
 * </p>
 * <p>
 * This class is not intended to be subclassed by clients but may be
 * instantiated.
 * </p>
 *
 * @see IPath
 * @noextend This class is not intended to be subclassed by clients.
 */
public final class Path implements IPath, Cloneable {
	/* masks for flag values: */
	/**
	 * if HAS_LEADING is set then Path starts with leading slash i.e. it is
	 * absolute, but the slash is not included in segments.
	 */
	private static final int HAS_LEADING = 1;
	private static final int IS_UNC = 2;
	private static final int HAS_TRAILING = 4;
	private static final int IS_FOR_WINDOWS = 8;

	private static final int ALL_SEPARATORS = HAS_LEADING | IS_UNC | HAS_TRAILING;

	/**
	 * Carrier that ensures that the contained constants are always initialized,
	 * regardless of if the Path class or IPath interface is initialized fist.
	 */
	static class Constants {
		/** Constant value indicating if the current platform is Windows */
		static final boolean RUNNING_ON_WINDOWS = java.io.File.separatorChar == '\\';

		/** Constant value indicating no segments */
		static final String[] NO_SEGMENTS = new String[0];

		/**
		 * We have cycle : Path implements IPath and IPath uses Path object instances in
		 * interface constants.
		 *
		 * Constants and methods below are needed to resolve init order issues between
		 * IPath and Path classes - depending on which is loaded first, constants
		 * defined in one of the classes and pointing to other one will see not
		 * initialized state. See https://github.com/eclipse-equinox/equinox/pull/279
		 */
		private static Path empty = new Path(""); //$NON-NLS-1$
		private static Path root = new Path("/"); //$NON-NLS-1$

		static synchronized Path empty() {
			if (empty == null) {
				empty = new Path(""); //$NON-NLS-1$
			}
			return empty;
		}

		static synchronized Path root() {
			if (root == null) {
				root = new Path("/"); //$NON-NLS-1$
			}
			return root;
		}
	}

	/**
	 * Constant value containing the empty path with no device on the local file
	 * system.
	 * <p>
	 * Instead of referencing this constants it is recommended to use
	 * {@link IPath#EMPTY} instead.
	 * </p>
	 *
	 * @see IPath#EMPTY
	 */
	public static final Path EMPTY = Constants.empty();

	/**
	 * Constant value containing the root path with no device on the local file
	 * system.
	 * <p>
	 * Instead of referencing this constants it is recommended to use
	 * {@link IPath#ROOT} instead.
	 * </p>
	 *
	 * @see IPath#ROOT
	 */
	public static final Path ROOT = Constants.root();

	/** The device id string. May be null if there is no device. */
	private final String device;

	// Private implementation note: the segments array and flag bitmap
	// are never modified, so that they can be shared between path instances

	/** The path segments */
	private final String[] segments;

	/** cached hash code */
	private int hash;

	/**
	 * flags indicating separators (has leading, is UNC, has trailing, is for
	 * Windows)
	 */
	private final byte flags;

	/**
	 * Constructs a new path from the given string path. The string path must
	 * represent a valid file system path on the local file system. The path is
	 * canonicalized and double slashes are removed except at the beginning. (to
	 * handle UNC paths). All forward slashes ('/') are treated as segment
	 * delimiters, and any segment and device delimiters for the local file system
	 * are also respected.
	 * <p>
	 * Instead of calling this method it is recommended to call
	 * {@link IPath#fromOSString(String)} instead.
	 * </p>
	 *
	 * @param pathString the operating-system specific string path
	 * @return the IPath representing the given OS specific string path
	 * @see IPath#toPortableString()
	 * @see IPath#fromOSString(String)
	 * @since 3.1
	 */
	public static IPath fromOSString(String pathString) {
		return IPath.fromOSString(pathString);
	}

	/**
	 * Constructs a new path from the given path string. The path string must have
	 * been produced by a previous call to <code>IPath.toPortableString</code>.
	 * <p>
	 * Instead of calling this method it is recommended to call
	 * {@link IPath#fromPortableString(String)} instead.
	 * </p>
	 *
	 * @param pathString the portable path string
	 * @return the IPath representing the given portable string path
	 * @see IPath#toPortableString()
	 * @see IPath#fromPortableString(String)
	 * @since 3.1
	 */
	public static IPath fromPortableString(String pathString) {
		return IPath.fromPortableString(pathString);
	}

	static IPath parsePortableString(String pathString) {
		int firstMatch = pathString.indexOf(DEVICE_SEPARATOR) + 1;
		// no extra work required if no device characters
		if (firstMatch <= 0) {
			return new Path(null, pathString, Constants.RUNNING_ON_WINDOWS);
		}
		// if we find a single colon, then the path has a device
		String devicePart = null;
		int pathLength = pathString.length();
		if (firstMatch == pathLength || pathString.charAt(firstMatch) != DEVICE_SEPARATOR) {
			devicePart = pathString.substring(0, firstMatch);
			pathString = pathString.substring(firstMatch, pathLength);
		}
		// optimize for no colon literals
		if (pathString.indexOf(DEVICE_SEPARATOR) == -1) {
			return new Path(devicePart, pathString, Constants.RUNNING_ON_WINDOWS);
		}
		// contract colon literals
		char[] chars = pathString.toCharArray();
		int readOffset = 0, writeOffset = 0, length = chars.length;
		while (readOffset < length) {
			if (chars[readOffset] == DEVICE_SEPARATOR && ++readOffset >= length) {
				break;
			}
			chars[writeOffset++] = chars[readOffset++];
		}
		return new Path(devicePart, new String(chars, 0, writeOffset), Constants.RUNNING_ON_WINDOWS);
	}

	/**
	 * Constructs a new POSIX path from the given string path. The string path must
	 * represent a valid file system path on a POSIX file system. The path is
	 * canonicalized and double slashes are removed except at the beginning (to
	 * handle UNC paths). All forward slashes ('/') are treated as segment
	 * delimiters. This factory method should be used if the string path is for a
	 * POSIX file system.
	 * <p>
	 * Instead of calling this method it is recommended to call
	 * {@link IPath#forPosix(String)} instead.
	 * </p>
	 *
	 * @param fullPath the string path
	 * @return the IPath representing the given POSIX string path
	 * @see #isValidPosixPath(String)
	 * @see IPath#forPosix(String)
	 * @since 3.7
	 */
	public static Path forPosix(String fullPath) {
		return (Path) IPath.forPosix(fullPath);
	}

	/**
	 * Constructs a new Windows path from the given string path. The string path
	 * must represent a valid file system path on the Windows file system. The path
	 * is canonicalized and double slashes are removed except at the beginning (to
	 * handle UNC paths). All forward slashes ('/') are treated as segment
	 * delimiters, and any segment ('\') and device (':') delimiters for the Windows
	 * file system are also respected. This factory method should be used if the
	 * string path is for the Windows file system.
	 * <p>
	 * Instead of calling this method it is recommended to call
	 * {@link IPath#forWindows(String)} instead.
	 * </p>
	 *
	 * @param fullPath the string path
	 * @return the IPath representing the given Windows string path
	 * @see #isValidWindowsPath(String)
	 * @see IPath#forWindows(String)
	 * @since 3.7
	 */
	public static Path forWindows(String fullPath) {
		return (Path) IPath.forWindows(fullPath);
	}

	/**
	 * Constructs a new path from the given string path. The string path must
	 * represent a valid file system path on the local file system. The path is
	 * canonicalized and double slashes are removed except at the beginning. (to
	 * handle UNC paths). All forward slashes ('/') are treated as segment
	 * delimiters, and any segment and device delimiters for the local file system
	 * are also respected (such as colon (':') and backslash ('\') on some file
	 * systems). This constructor should be used if the string path if for the local
	 * file system.
	 *
	 * @param fullPath the string path
	 * @see #isValidPath(String)
	 */
	public Path(String fullPath) {
		this(fullPath, Constants.RUNNING_ON_WINDOWS);
	}

	/**
	 * Constructs a new path from the given device id and string path. The given
	 * string path must be valid. The path is canonicalized and double slashes are
	 * removed except at the beginning (to handle UNC paths). All forward slashes
	 * ('/') are treated as segment delimiters, and any segment delimiters for the
	 * local file system are also respected (such as backslash ('\') on some file
	 * systems).
	 *
	 * @param device the device id
	 * @param path   the string path
	 * @see #isValidPath(String)
	 * @see #setDevice(String)
	 */
	public Path(String device, String path) {
		this(device, backslashToForward(path, Constants.RUNNING_ON_WINDOWS), Constants.RUNNING_ON_WINDOWS);
	}

	private static String backslashToForward(String path, boolean forWindows) {
		if (forWindows) {
			// convert backslash to forward slash
			return path.replace('\\', SEPARATOR);
		}
		return path;
	}

	/**
	 * Constructs a new path from the given string path. The string path must
	 * represent a valid file system path on the specified file system. The path is
	 * canonicalized and double slashes are removed except at the beginning (to
	 * handle UNC paths). All forward slashes ('/') are treated as segment
	 * delimiters, and any segment and device delimiters for the specified file
	 * system are also respected (such as colon (':') and backslash ('\') on
	 * Windows).
	 *
	 * @param fullPath   the string path
	 * @param forWindows true if the string path is for the Windows file system
	 * @since 3.7
	 */
	Path(String fullPath, boolean forWindows) {
		String devicePart = null;
		if (forWindows) {
			// convert backslash to forward slash
			fullPath = fullPath.replace('\\', SEPARATOR);
			// extract device
			int i = fullPath.indexOf(DEVICE_SEPARATOR);
			if (i != -1) {
				int start = 0;
				if (fullPath.startsWith("//?/")) { //$NON-NLS-1$
					// Paths prefixed with "//?/" are local paths. For details:
					// https://learn.microsoft.com/en-us/windows/win32/fileio/naming-a-file#win32-file-namespaces
					start = 4;
				} else if (fullPath.charAt(0) == SEPARATOR) {
					// remove leading slash from device part to handle output of URL.getFile()
					start = 1;
				}
				devicePart = fullPath.substring(start, i + 1);
				fullPath = fullPath.substring(i + 1, fullPath.length());
			}
		}

		// inlined Path(String devicePart, String fullPath, booleanforWindows)
		// because calling other constructor has to be first statement:

		Assert.isNotNull(fullPath);
		String collapsedPath = collapseSlashes(devicePart, fullPath);
		int flag = computeFlags(collapsedPath, forWindows);
		// compute segments and ensure canonical form
		String[] canonicalSegments = canonicalize((flag & HAS_LEADING) != 0, computeSegments(collapsedPath));
		if (canonicalSegments.length == 0) {
			// paths of length 0 have no trailing separator
			flag &= ~HAS_TRAILING;
		}
		this.device = devicePart;
		this.segments = canonicalSegments;
		this.flags = (byte) flag;
	}

	/*
	 * (Intentionally not included in javadoc) Private constructor.
	 */
	private Path(String device, String[] segments, int flags) {
		int flag = flags;
		if (segments.length == 0) {
			// paths of length 0 have no trailing separator
			flag &= ~HAS_TRAILING;
		}
		// no segment validations are done for performance reasons
		this.segments = segments;
		this.device = device;
		this.flags = (byte) flag;
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#addFileExtension
	 */
	@Override
	public IPath addFileExtension(String extension) {
		if (isRoot() || isEmpty() || hasTrailingSeparator()) {
			return this;
		}
		String[] s = getSegments();
		int len = s.length;
		String[] newSegments = Arrays.copyOf(s, len);
		newSegments[len - 1] = s[len - 1] + '.' + extension;
		return new Path(device, newSegments, flags);
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#addTrailingSeparator
	 */
	@Override
	public IPath addTrailingSeparator() {
		if (hasTrailingSeparator() || isRoot()) {
			return this;
		}
		// XXX workaround, see 1GIGQ9V
		String[] s = getSegments();
		if (isEmpty()) {
			return new Path(device, s, (flags & IS_FOR_WINDOWS) | HAS_LEADING);
		}
		return new Path(device, s, flags | HAS_TRAILING);
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#append(IPath)
	 */
	@Override
	public IPath append(IPath tail) {
		// optimize some easy cases
		if (tail == null || tail.segmentCount() == 0) {
			return this;
		}
		// these call chains look expensive, but in most cases they are no-ops
		// the tail must be for the same platform as this instance
		if (this.isEmpty() && ((flags & IS_FOR_WINDOWS) == 0) == tail.isValidSegment(":")) { //$NON-NLS-1$
			return tail.setDevice(device).makeRelative().makeUNC(isUNC());
		}
		if (this.isRoot() && ((flags & IS_FOR_WINDOWS) == 0) == tail.isValidSegment(":")) { //$NON-NLS-1$
			return tail.setDevice(device).makeAbsolute().makeUNC(isUNC());
		}

		// concatenate the two segment arrays
		String[] s = getSegments();
		int myLen = s.length;
		int tailLen = tail.segmentCount();
		String[] newSegments = Arrays.copyOf(s, myLen + tailLen);
		for (int i = 0; i < tailLen; i++) {
			newSegments[myLen + i] = tail.segment(i);
		}
		// use my leading separators and the tail's trailing separator
		String tailFirstSegment = newSegments[myLen];
		if (tailFirstSegment.equals("..") || tailFirstSegment.equals(".")) { //$NON-NLS-1$ //$NON-NLS-2$
			newSegments = canonicalize(isAbsolute(), newSegments);
		}
		return new Path(device, newSegments,
				(flags & (HAS_LEADING | IS_UNC | IS_FOR_WINDOWS)) | (tail.hasTrailingSeparator() ? HAS_TRAILING : 0));
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#append(java.lang.String)
	 */
	@Override
	public IPath append(String tail) {
		// optimize addition of a single segment
		if (tail.indexOf(SEPARATOR) == -1 && tail.indexOf('\\') == -1 && tail.indexOf(DEVICE_SEPARATOR) == -1) {
			int tailLength = tail.length();
			if (tailLength < 3) {
				// some special cases
				if (tailLength == 0 || ".".equals(tail)) { //$NON-NLS-1$
					return this;
				}
				if ("..".equals(tail)) { //$NON-NLS-1$
					return removeLastSegments(1);
				}
			}
			// just add the segment
			String[] s = getSegments();
			int myLen = s.length;
			String[] newSegments = Arrays.copyOf(s, myLen + 1);
			newSegments[myLen] = tail;
			return new Path(device, newSegments, flags & ~HAS_TRAILING);
		}
		// go with easy implementation
		return append(new Path(tail, (flags & IS_FOR_WINDOWS) != 0));
	}

	/**
	 * Destructively converts this path to its canonical form.
	 * <p>
	 * In its canonical form, a path does not have any "." segments, and parent
	 * references ("..") are collapsed where possible.
	 * </p>
	 *
	 * @return true if the path was modified, and false otherwise.
	 */
	private static String[] canonicalize(boolean isAbsolute, String[] segments) {
		// look for segments that need canonicalizing
		for (String segment : segments) {
			if (segment.charAt(0) == '.' && (segment.equals("..") || segment.equals("."))) { //$NON-NLS-1$ //$NON-NLS-2$
				// path needs to be canonicalized
				return collapseParentReferences(isAbsolute, segments);
			}
		}
		return segments;
	}

	/*
	 * (Intentionally not included in javadoc) Clones this object.
	 */
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	/**
	 * Destructively removes all occurrences of ".." segments from this path.
	 */
	private static String[] collapseParentReferences(boolean isAbsolute, String[] segments) {
		int segmentCount = segments.length;
		String[] stack = new String[segmentCount];
		int stackPointer = 0;
		for (int i = 0; i < segmentCount; i++) {
			String segment = segments[i];
			if (segment.equals("..")) { //$NON-NLS-1$
				if (stackPointer == 0) {
					// if the stack is empty we are going out of our scope
					// so we need to accumulate segments. But only if the original
					// path is relative. If it is absolute then we can't go any higher than
					// root so simply toss the .. references.
					if (!isAbsolute) {
						stack[stackPointer++] = segment; // stack push
					}
				} else {
					// if the top is '..' then we are accumulating segments so don't pop
					if ("..".equals(stack[stackPointer - 1])) { //$NON-NLS-1$
						stack[stackPointer++] = ".."; //$NON-NLS-1$
					} else {
						stackPointer--;
						// stack pop
					}
				}
				// collapse current references
			} else if (!segment.equals(".") || segmentCount == 1) { //$NON-NLS-1$
				stack[stackPointer++] = segment; // stack push
			}
		}
		// if the number of segments hasn't changed, then no modification needed
		if (stackPointer == segmentCount) {
			return segments;
		}
		// build the new segment array backwards by popping the stack
		return Arrays.copyOf(stack, stackPointer);
	}

	/**
	 * Removes duplicate slashes from the given path, with the exception of leading
	 * double slash which represents a UNC path.
	 */
	private static String collapseSlashes(String device, String path) {
		int length = path.length();
		// if the path is only 0, 1 or 2 chars long then it could not possibly have
		// illegal
		// duplicate slashes.
		if (length < 3) {
			return path;
		}
		// check for an occurrence of // in the path. Start at index 1 to ensure we skip
		// leading UNC //
		// If there are no // then there is nothing to collapse so just return.
		if (path.indexOf("//", 1) == -1) { //$NON-NLS-1$
			return path;
		}
		// We found an occurrence of // in the path so do the slow collapse.
		char[] result = new char[path.length()];
		int count = 0;
		boolean hasPrevious = false;
		char[] characters = path.toCharArray();
		for (int index = 0; index < characters.length; index++) {
			char c = characters[index];
			if (c == SEPARATOR) {
				if (hasPrevious) {
					// skip double slashes, except for beginning of UNC.
					// note that a UNC path can't have a device.
					if (device == null && index == 1) {
						result[count] = c;
						count++;
					}
				} else {
					hasPrevious = true;
					result[count] = c;
					count++;
				}
			} else {
				hasPrevious = false;
				result[count] = c;
				count++;
			}
		}
		return new String(result, 0, count);
	}

	/*
	 * (Intentionally not included in javadoc) Computes the hash code for this
	 * object.
	 */
	private static int computeHashCode(String device, String[] segments) {
		int hash = device == null ? 17 : device.hashCode();
		int segmentCount = segments.length;
		for (int i = 0; i < segmentCount; i++) {
			// this function tends to given a fairly even distribution
			hash = hash * 37 + segments[i].hashCode();
		}
		return hash;
	}

	/*
	 * (Intentionally not included in javadoc) Returns the size of the string that
	 * will be created by toString or toOSString.
	 */
	private int computeLength() {
		int length = 0;
		if (device != null) {
			length += device.length();
		}
		if ((flags & HAS_LEADING) != 0) {
			length++;
		}
		if ((flags & IS_UNC) != 0) {
			length++;
		}
		// add the segment lengths
		String[] s = getSegments();
		int max = s.length;
		if (max > 0) {
			for (int i = 0; i < max; i++) {
				length += s[i].length();
			}
			// add the separator lengths
			length += max - 1;
		}
		if ((flags & HAS_TRAILING) != 0) {
			length++;
		}
		return length;
	}

	/*
	 * (Intentionally not included in javadoc) Returns the number of segments in the
	 * given path
	 */
	private static int computeSegmentCount(String path) {
		int len = path.length();
		if (len == 0 || (len == 1 && path.charAt(0) == SEPARATOR)) {
			return 0;
		}
		int count = 1;
		int prev = -1;
		int i;
		while ((i = path.indexOf(SEPARATOR, prev + 1)) != -1) {
			if (i != prev + 1 && i != len) {
				++count;
			}
			prev = i;
		}
		if (path.charAt(len - 1) == SEPARATOR) {
			--count;
		}
		return count;
	}

	/**
	 * Computes the segment array for the given canonicalized path.
	 */
	private static String[] computeSegments(String path) {
		// performance sensitive --- avoid creating garbage
		int segmentCount = computeSegmentCount(path);
		if (segmentCount == 0) {
			return Constants.NO_SEGMENTS;
		}
		String[] newSegments = new String[segmentCount];
		int len = path.length();
		// check for initial slash
		int firstPosition = (path.charAt(0) == SEPARATOR) ? 1 : 0;
		// check for UNC
		if (firstPosition == 1 && len > 1 && (path.charAt(1) == SEPARATOR)) {
			firstPosition = 2;
		}
		int lastPosition = (path.charAt(len - 1) != SEPARATOR) ? len - 1 : len - 2;
		// for non-empty paths, the number of segments is
		// the number of slashes plus 1, ignoring any leading
		// and trailing slashes
		int next = firstPosition;
		for (int i = 0; i < segmentCount; i++) {
			int start = next;
			int end = path.indexOf(SEPARATOR, next);
			if (end == -1) {
				newSegments[i] = path.substring(start, lastPosition + 1);
			} else {
				newSegments[i] = path.substring(start, end);
			}
			next = end + 1;
		}
		return newSegments;
	}

	/**
	 * Returns the platform-neutral encoding of the given segment onto the given
	 * string buffer. This escapes literal colon characters with double colons.
	 */
	private void encodeSegment(String string, StringBuilder buf) {
		int len = string.length();
		for (int i = 0; i < len; i++) {
			char c = string.charAt(i);
			buf.append(c);
			if (c == DEVICE_SEPARATOR) {
				buf.append(DEVICE_SEPARATOR);
			}
		}
	}

	/*
	 * (Intentionally not included in javadoc) Compares objects for equality.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Path target)) {
			return false;
		}
		// check leading separators
		if ((flags & (HAS_LEADING | IS_UNC)) != (target.flags & (HAS_LEADING | IS_UNC))) {
			return false;
		}
		String[] targetSegments = target.getSegments();
		String[] s = getSegments();
		int i = s.length;
		// check segment count
		if (i != targetSegments.length) {
			return false;
		}
		// check segments in reverse order - later segments more likely to differ
		while (--i >= 0) {
			if (!s[i].equals(targetSegments[i])) {
				return false;
			}
		}
		// check device last (least likely to differ)
		return device == target.device || (device != null && device.equals(target.device));
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#getDevice
	 */
	@Override
	public String getDevice() {
		return device;
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#getFileExtension
	 */
	@Override
	public String getFileExtension() {
		if (hasTrailingSeparator()) {
			return null;
		}
		String lastSegment = lastSegment();
		if (lastSegment == null) {
			return null;
		}
		int index = lastSegment.lastIndexOf('.');
		if (index == -1) {
			return null;
		}
		return lastSegment.substring(index + 1);
	}

	/*
	 * (Intentionally not included in javadoc) Computes the hash code for this
	 * object.
	 */
	@Override
	public int hashCode() {
		int h = hash;
		if (h == 0) {
			hash = h = computeHashCode(device, getSegments());
		}
		return h;
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#hasTrailingSeparator2
	 */
	@Override
	public boolean hasTrailingSeparator() {
		return (flags & HAS_TRAILING) != 0;
	}

	/*
	 * Initialize the current path with the given string. (Intentionally not
	 * included in javadoc) Private constructor.
	 */
	private Path(String deviceString, String path, boolean forWindows) {
		Assert.isNotNull(path);
		String collapsedPath = collapseSlashes(deviceString, path);
		int flag = computeFlags(collapsedPath, forWindows);
		// compute segments and ensure canonical form
		String[] canonicalSegments = canonicalize((flag & HAS_LEADING) != 0, computeSegments(collapsedPath));
		if (canonicalSegments.length == 0) {
			// paths of length 0 have no trailing separator
			flag &= ~HAS_TRAILING;
		}
		this.device = deviceString;
		this.segments = canonicalSegments;
		this.flags = (byte) flag;
	}

	private static int computeFlags(String path, boolean forWindows) {
		int len = path.length();
		int flags;
		// compute the flags bitmap
		if (len < 2) {
			if (len == 1 && path.charAt(0) == SEPARATOR) {
				flags = HAS_LEADING;
			} else {
				flags = 0;
			}
		} else {
			boolean hasLeading = path.charAt(0) == SEPARATOR;
			boolean isUNC = hasLeading && path.charAt(1) == SEPARATOR;
			// UNC path of length two has no trailing separator
			boolean hasTrailing = !(isUNC && len == 2) && path.charAt(len - 1) == SEPARATOR;
			flags = hasLeading ? HAS_LEADING : 0;
			if (isUNC) {
				flags |= IS_UNC;
			}
			if (hasTrailing) {
				flags |= HAS_TRAILING;
			}
		}
		if (forWindows) {
			flags |= IS_FOR_WINDOWS;
		}
		return flags;
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#isAbsolute
	 */
	@Override
	public boolean isAbsolute() {
		// it's absolute if it has a leading separator
		return (flags & HAS_LEADING) != 0;
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#isEmpty
	 */
	@Override
	public boolean isEmpty() {
		// true if no segments and no leading prefix
		return getSegments().length == 0 && ((flags & ALL_SEPARATORS) != HAS_LEADING);
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#isPrefixOf
	 */
	@Override
	public boolean isPrefixOf(IPath anotherPath) {
		if (device == null) {
			if (anotherPath.getDevice() != null) {
				return false;
			}
		} else {
			if (!device.equalsIgnoreCase(anotherPath.getDevice())) {
				return false;
			}
		}
		if (isEmpty() || (isRoot() && anotherPath.isAbsolute())) {
			return true;
		}
		String[] s = getSegments();
		int len = s.length;
		if (len > anotherPath.segmentCount()) {
			return false;
		}
		for (int i = 0; i < len; i++) {
			if (!s[i].equals(anotherPath.segment(i))) {
				return false;
			}
		}
		return true;
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#isRoot
	 */
	@Override
	public boolean isRoot() {
		// must have no segments, a leading separator, and not be a UNC path.
		return this == ROOT || (getSegments().length == 0 && ((flags & ALL_SEPARATORS) == HAS_LEADING));
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#isUNC
	 */
	@Override
	public boolean isUNC() {
		if (device != null) {
			return false;
		}
		return (flags & IS_UNC) != 0;
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#isValidPath(String)
	 */
	@Override
	public boolean isValidPath(String path) {
		return isValidPath(path, (flags & IS_FOR_WINDOWS) != 0);
	}

	/**
	 * Returns whether the given string is syntactically correct as a path on a
	 * POSIX file system. The path is correct if each of the segments in its
	 * canonicalized form is valid.
	 *
	 * @param path the path to check
	 * @return <code>true</code> if the given string is a valid path, and
	 *         <code>false</code> otherwise
	 * @see #isValidPosixSegment(String)
	 * @since 3.7
	 */
	public static boolean isValidPosixPath(String path) {
		return isValidPath(path, false);
	}

	/**
	 * Returns whether the given string is syntactically correct as a path on the
	 * Windows file system. The device id is the prefix up to and including the
	 * device separator (':'); the path proper is everything to the right of it, or
	 * the entire string if there is no device separator. The device id is not
	 * checked for validity; the path proper is correct if each of the segments in
	 * its canonicalized form is valid.
	 *
	 * @param path the path to check
	 * @return <code>true</code> if the given string is a valid path, and
	 *         <code>false</code> otherwise
	 * @see #isValidWindowsSegment(String)
	 * @since 3.7
	 */
	public static boolean isValidWindowsPath(String path) {
		return isValidPath(path, true);
	}

	/**
	 * Returns whether the given string is syntactically correct as a path on the
	 * specified file system. The device id is the prefix up to and including the
	 * device separator for the specified file system; the path proper is everything
	 * to the right of it, or the entire string if there is no device separator.
	 * When the specified platform is a file system with no meaningful device
	 * separator, the entire string is treated as the path proper. The device id is
	 * not checked for validity; the path proper is correct if each of the segments
	 * in its canonicalized form is valid.
	 *
	 * @param path       the path to check
	 * @param forWindows true if the path is for the Windows file system
	 * @return <code>true</code> if the given string is a valid path, and
	 *         <code>false</code> otherwise
	 * @see #isValidSegment(String, boolean)
	 * @since 3.7
	 */
	private static boolean isValidPath(String path, boolean forWindows) {
		Path test = new Path(path, forWindows);
		for (int i = 0, max = test.segmentCount(); i < max; i++) {
			if (!Path.isValidSegment(test.segment(i), forWindows)) {
				return false;
			}
		}
		return true;
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#isValidSegment(String)
	 */
	@Override
	public boolean isValidSegment(String segment) {
		return isValidSegment(segment, (flags & IS_FOR_WINDOWS) != 0);
	}

	/**
	 * Returns whether the given string is valid as a segment in a path on a POSIX
	 * file system. The rules for valid segments are as follows:
	 * <ul>
	 * <li>the empty string is not valid
	 * <li>any string containing the slash character ('/') is not valid
	 * </ul>
	 *
	 * @param segment the path segment to check
	 * @return <code>true</code> if the given path segment is valid, and
	 *         <code>false</code> otherwise
	 * @since 3.7
	 */
	public static boolean isValidPosixSegment(String segment) {
		return isValidSegment(segment, false);
	}

	/**
	 * Returns whether the given string is valid as a segment in a path on the
	 * Windows file system. The rules for valid segments are as follows:
	 * <ul>
	 * <li>the empty string is not valid
	 * <li>any string containing the slash character ('/') is not valid
	 * <li>any string containing segment ('\') or device (':') separator characters
	 * is not valid
	 * </ul>
	 *
	 * @param segment the path segment to check
	 * @return <code>true</code> if the given path segment is valid, and
	 *         <code>false</code> otherwise
	 * @since 3.7
	 */
	public static boolean isValidWindowsSegment(String segment) {
		return isValidSegment(segment, true);
	}

	/**
	 * Returns whether the given string is valid as a segment in a path on the
	 * specified file system. The rules for valid segments are as follows:
	 * <ul>
	 * <li>the empty string is not valid
	 * <li>any string containing the slash character ('/') is not valid
	 * <li>any string containing segment or device separator characters on the
	 * specified file system, such as the backslash ('\') and colon (':') on
	 * Windows, is not valid
	 * </ul>
	 *
	 * @param segment    the path segment to check
	 * @param forWindows true if the path is for the Windows file system
	 * @return <code>true</code> if the given path segment is valid, and
	 *         <code>false</code> otherwise
	 * @since 3.7
	 */
	private static boolean isValidSegment(String segment, boolean forWindows) {
		int size = segment.length();
		if (size == 0) {
			return false;
		}
		for (int i = 0; i < size; i++) {
			char c = segment.charAt(i);
			if (c == '/') {
				return false;
			}
			if (forWindows && (c == '\\' || c == ':')) {
				return false;
			}
		}
		return true;
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#lastSegment()
	 */
	@Override
	public String lastSegment() {
		String[] s = getSegments();
		int len = s.length;
		return len == 0 ? null : s[len - 1];
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#makeAbsolute()
	 */
	@Override
	public IPath makeAbsolute() {
		if (isAbsolute()) {
			return this;
		}
		String[] newSegments = getSegments();
		// may need canonicalizing if it has leading ".." or "." segments
		if (newSegments.length > 0) {
			String first = newSegments[0];
			if (first.equals("..") || first.equals(".")) { //$NON-NLS-1$ //$NON-NLS-2$
				newSegments = canonicalize(true, newSegments);
			}
		}
		return new Path(device, newSegments, flags | HAS_LEADING);
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#makeRelative()
	 */
	@Override
	public IPath makeRelative() {
		if (!isAbsolute()) {
			return this;
		}
		return new Path(device, getSegments(), flags & (HAS_TRAILING | IS_FOR_WINDOWS));
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since org.eclipse.equinox.common 3.5
	 */
	@Override
	public IPath makeRelativeTo(IPath base) {
		// can't make relative if devices are not equal
		if (device != base.getDevice() && (device == null || !device.equalsIgnoreCase(base.getDevice()))) {
			return this;
		}
		int commonLength = matchingFirstSegments(base);
		final int differenceLength = base.segmentCount() - commonLength;
		final int newSegmentLength = differenceLength + segmentCount() - commonLength;
		if (newSegmentLength == 0) {
			return Path.EMPTY;
		}
		String[] newSegments = new String[newSegmentLength];
		// add parent references for each segment different from the base
		Arrays.fill(newSegments, 0, differenceLength, ".."); //$NON-NLS-1$
		// append the segments of this path not in common with the base
		System.arraycopy(getSegments(), commonLength, newSegments, differenceLength, newSegmentLength - differenceLength);
		return new Path(null, newSegments, flags & (HAS_TRAILING | IS_FOR_WINDOWS));
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#makeUNC(boolean)
	 */
	@Override
	public IPath makeUNC(boolean toUNC) {
		// if we are already in the right form then just return
		if (!(toUNC ^ isUNC())) {
			return this;
		}

		int newSeparators = this.flags;
		if (toUNC) {
			newSeparators |= HAS_LEADING | IS_UNC;
		} else {
			// mask out the UNC bit
			newSeparators &= HAS_LEADING | HAS_TRAILING | IS_FOR_WINDOWS;
		}
		return new Path(toUNC ? null : device, getSegments(), newSeparators);
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#matchingFirstSegments(IPath)
	 */
	@Override
	public int matchingFirstSegments(IPath anotherPath) {
		Assert.isNotNull(anotherPath);
		int anotherPathLen = anotherPath.segmentCount();
		String[] s = getSegments();
		int max = Math.min(s.length, anotherPathLen);
		int count = 0;
		for (int i = 0; i < max; i++) {
			if (!s[i].equals(anotherPath.segment(i))) {
				return count;
			}
			count++;
		}
		return count;
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#removeFileExtension()
	 */
	@Override
	public IPath removeFileExtension() {
		String extension = getFileExtension();
		if (extension == null || extension.equals("")) { //$NON-NLS-1$
			return this;
		}
		String lastSegment = lastSegment();
		int index = lastSegment.lastIndexOf(extension) - 1;
		return removeLastSegments(1).append(lastSegment.substring(0, index));
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#removeFirstSegments(int)
	 */
	@Override
	public IPath removeFirstSegments(int count) {
		if (count == 0) {
			return this;
		}
		String[] s = getSegments();
		if (count >= s.length) {
			return new Path(device, Constants.NO_SEGMENTS, flags & IS_FOR_WINDOWS);
		}
		Assert.isLegal(count > 0);
		int newSize = s.length - count;
		String[] newSegments = Arrays.copyOfRange(s, count, newSize + count);

		// result is always a relative path
		return new Path(device, newSegments, flags & (HAS_TRAILING | IS_FOR_WINDOWS));
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#removeLastSegments(int)
	 */
	@Override
	public IPath removeLastSegments(int count) {
		if (count == 0) {
			return this;
		}
		String[] s = getSegments();
		if (count >= s.length) {
			// result will have no trailing separator
			return new Path(device, Constants.NO_SEGMENTS, flags & (HAS_LEADING | IS_UNC | IS_FOR_WINDOWS));
		}
		Assert.isLegal(count > 0);
		int newSize = s.length - count;
		String[] newSegments = Arrays.copyOf(s, newSize);
		return new Path(device, newSegments, flags);
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#removeTrailingSeparator()
	 */
	@Override
	public IPath removeTrailingSeparator() {
		if (!hasTrailingSeparator()) {
			return this;
		}
		return new Path(device, getSegments(), flags & (HAS_LEADING | IS_UNC | IS_FOR_WINDOWS));
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#segment(int)
	 */
	@Override
	public String segment(int index) {
		String[] s = getSegments();
		if (index >= s.length) {
			return null;
		}
		return s[index];
	}

	private String[] getSegments() {
		return segments;
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#segmentCount()
	 */
	@Override
	public int segmentCount() {
		return getSegments().length;
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#segments()
	 */
	@Override
	public String[] segments() {
		String[] s = getSegments();
		return Arrays.copyOf(s, s.length);
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#setDevice(String)
	 */
	@Override
	public IPath setDevice(String value) {
		if (value != null) {
			Assert.isTrue(value.indexOf(IPath.DEVICE_SEPARATOR) == (value.length() - 1),
					"Last character should be the device separator"); //$NON-NLS-1$
		}
		// return the receiver if the device is the same
		if (value == device || (value != null && value.equals(device))) {
			return this;
		}

		return new Path(value, getSegments(), flags);
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#toFile()
	 */
	@Override
	public File toFile() {
		return new File(toOSString());
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#toOSString()
	 */
	@Override
	public String toOSString() {
		// Note that this method is identical to toString except
		// it uses the OS file separator instead of the path separator
		int resultSize = computeLength();
		if (resultSize <= 0) {
			return ""; //$NON-NLS-1$
		}
		char FILE_SEPARATOR = File.separatorChar;
		char[] result = new char[resultSize];
		int offset = 0;
		if (device != null) {
			int size = device.length();
			device.getChars(0, size, result, offset);
			offset += size;
		}
		if ((flags & HAS_LEADING) != 0) {
			result[offset++] = FILE_SEPARATOR;
		}
		if ((flags & IS_UNC) != 0) {
			result[offset++] = FILE_SEPARATOR;
		}
		String[] s = getSegments();
		int len = s.length - 1;
		if (len >= 0) {
			// append all but the last segment, with file separators
			for (int i = 0; i < len; i++) {
				int size = s[i].length();
				s[i].getChars(0, size, result, offset);
				offset += size;
				result[offset++] = FILE_SEPARATOR;
			}
			// append the last segment
			int size = s[len].length();
			s[len].getChars(0, size, result, offset);
			offset += size;
		}
		if ((flags & HAS_TRAILING) != 0) {
			result[offset++] = FILE_SEPARATOR;
		}
		return new String(result);
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#toPortableString()
	 */
	@Override
	public String toPortableString() {
		int resultSize = computeLength();
		if (resultSize <= 0) {
			return ""; //$NON-NLS-1$
		}
		StringBuilder result = new StringBuilder(resultSize);
		if (device != null) {
			result.append(device);
		}
		if ((flags & HAS_LEADING) != 0) {
			result.append(SEPARATOR);
		}
		if ((flags & IS_UNC) != 0) {
			result.append(SEPARATOR);
		}
		String[] s = getSegments();
		int len = s.length;
		// append all segments with separators
		for (int i = 0; i < len; i++) {
			if (s[i].indexOf(DEVICE_SEPARATOR) >= 0) {
				encodeSegment(s[i], result);
			} else {
				result.append(s[i]);
			}
			if (i < len - 1 || (flags & HAS_TRAILING) != 0) {
				result.append(SEPARATOR);
			}
		}
		return result.toString();
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#toString()
	 */
	@Override
	public String toString() {
		int resultSize = computeLength();
		if (resultSize <= 0) {
			return ""; //$NON-NLS-1$
		}
		char[] result = new char[resultSize];
		int offset = 0;
		if (device != null) {
			int size = device.length();
			device.getChars(0, size, result, offset);
			offset += size;
		}
		if ((flags & HAS_LEADING) != 0) {
			result[offset++] = SEPARATOR;
		}
		if ((flags & IS_UNC) != 0) {
			result[offset++] = SEPARATOR;
		}
		String[] s = getSegments();
		int len = s.length - 1;
		if (len >= 0) {
			// append all but the last segment, with separators
			for (int i = 0; i < len; i++) {
				int size = s[i].length();
				s[i].getChars(0, size, result, offset);
				offset += size;
				result[offset++] = SEPARATOR;
			}
			// append the last segment
			int size = s[len].length();
			s[len].getChars(0, size, result, offset);
			offset += size;
		}
		if ((flags & HAS_TRAILING) != 0) {
			result[offset++] = SEPARATOR;
		}
		return new String(result);
	}

	/*
	 * (Intentionally not included in javadoc)
	 *
	 * @see IPath#uptoSegment(int)
	 */
	@Override
	public IPath uptoSegment(int count) {
		if (count == 0) {
			return new Path(device, Constants.NO_SEGMENTS, flags & (HAS_LEADING | IS_UNC | IS_FOR_WINDOWS));
		}
		String[] s = getSegments();
		if (count >= s.length) {
			return this;
		}
		Assert.isTrue(count > 0, "Invalid parameter to Path.uptoSegment"); //$NON-NLS-1$
		String[] newSegments = Arrays.copyOf(s, count);
		return new Path(device, newSegments, flags);
	}
}
