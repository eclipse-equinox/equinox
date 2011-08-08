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

import org.eclipse.equinox.bidi.advanced.STextEnvironment;
import org.eclipse.equinox.bidi.advanced.STextProcessorMultipassNew;
import org.eclipse.equinox.bidi.custom.STextProcessor;

public class STextProcessorMultipassImplNew extends STextProcessorImplNew implements STextProcessorMultipassNew {

	/**
	 *  Constant to use in the first element of the <code>state</code>
	 *  argument when calling most methods of this class
	 *  to indicate that there is no context of previous lines which
	 *  should be initialized before performing the operation.
	 */
	public static final int STATE_INITIAL = 0;

	private int[] state;

	public STextProcessorMultipassImplNew(STextProcessor structuredTextDescriptor, STextEnvironment environment) {
		super(structuredTextDescriptor, environment);
		reset();
	}

	public String leanToFullText(String text) {
		return STextImpl.leanToFullText(structuredTextDescriptor, environment, text, state);
	}

	public int[] leanToFullMap(String text) {
		return STextImpl.leanToFullMap(structuredTextDescriptor, environment, text, state);
	}

	public int[] leanBidiCharOffsets(String text) {
		return STextImpl.leanBidiCharOffsets(structuredTextDescriptor, environment, text, state);
	}

	public String fullToLeanText(String text) {
		return STextImpl.fullToLeanText(structuredTextDescriptor, environment, text, state);
	}

	public int[] fullToLeanMap(String text) {
		return STextImpl.fullToLeanMap(structuredTextDescriptor, environment, text, state);
	}

	public int[] fullBidiCharOffsets(String text) {
		return STextImpl.fullBidiCharOffsets(structuredTextDescriptor, environment, text, state);
	}

	public int getCurDirection(String text) {
		return structuredTextDescriptor.getDirection(environment, text);
	}

	public void reset() {
		state = new int[] {STATE_INITIAL};
	}

}
