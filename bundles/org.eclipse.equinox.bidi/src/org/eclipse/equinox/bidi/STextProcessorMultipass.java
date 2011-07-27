/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.bidi;

import org.eclipse.equinox.bidi.custom.STextProcessor;
import org.eclipse.equinox.bidi.internal.STextImpl;

// TBD experimental
public class STextProcessorMultipass extends STextProcessor {

	private int[] state = new int[] {0};

	public STextProcessorMultipass() {

	}

	public String leanToFullText(STextProcessor processor, STextEnvironment environment, String text) {
		return STextImpl.leanToFullText(processor, environment, text, state);
	}

	public int[] leanToFullMap(STextProcessor processor, STextEnvironment environment, String text) {
		return STextImpl.leanToFullMap(processor, environment, text, state);
	}

	public int[] leanBidiCharOffsets(STextProcessor processor, STextEnvironment environment, String text) {
		return STextImpl.leanBidiCharOffsets(processor, environment, text, state);
	}

	public String fullToLeanText(STextProcessor processor, STextEnvironment environment, String text) {
		return STextImpl.fullToLeanText(processor, environment, text, state);
	}

	public int[] fullToLeanMap(STextProcessor processor, STextEnvironment environment, String text) {
		return STextImpl.fullToLeanMap(processor, environment, text, state);
	}

	public int[] fullBidiCharOffsets(STextProcessor processor, STextEnvironment environment, String text) {
		return STextImpl.fullBidiCharOffsets(processor, environment, text, state);
	}
}
