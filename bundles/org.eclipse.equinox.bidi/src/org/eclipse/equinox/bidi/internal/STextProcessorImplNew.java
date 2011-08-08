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
import org.eclipse.equinox.bidi.advanced.STextProcessorNew;
import org.eclipse.equinox.bidi.custom.STextProcessor;

public class STextProcessorImplNew implements STextProcessorNew {

	protected STextProcessor structuredTextDescriptor;
	protected STextEnvironment environment;

	private final static int[] initialState = new int[] {0};

	public STextProcessorImplNew(STextProcessor structuredTextDescriptor, STextEnvironment environment) {
		this.structuredTextDescriptor = structuredTextDescriptor;
		this.environment = environment;
	}

	public String leanToFullText(String text) {
		return STextImpl.leanToFullText(structuredTextDescriptor, environment, text, initialState);
	}

	public int[] leanToFullMap(String text) {
		return STextImpl.leanToFullMap(structuredTextDescriptor, environment, text, initialState);
	}

	public int[] leanBidiCharOffsets(String text) {
		return STextImpl.leanBidiCharOffsets(structuredTextDescriptor, environment, text, initialState);
	}

	public String fullToLeanText(String text) {
		return STextImpl.fullToLeanText(structuredTextDescriptor, environment, text, initialState);
	}

	public int[] fullToLeanMap(String text) {
		return STextImpl.fullToLeanMap(structuredTextDescriptor, environment, text, initialState);
	}

	public int[] fullBidiCharOffsets(String text) {
		return STextImpl.fullBidiCharOffsets(structuredTextDescriptor, environment, text, initialState);
	}

	public int getCurDirection(String text) {
		return structuredTextDescriptor.getDirection(environment, text);
	}

}
