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
package org.eclipse.equinox.bidi.custom;

import org.eclipse.equinox.bidi.*;
import org.eclipse.equinox.bidi.internal.BidiComplexImpl;

/**
 *  Generic processor which can be used as superclass (base class)
 *  for specific complex expression processors.
 *  <p>
 *  Here are some guidelines about how to write complex expression
 *  processors.
 *  <ul>
 *    <li>Processor instances may be accessed simultaneously by
 *        several threads. They should have no instance variables.</li>
 *    <li>Each use of a processor is initialized with the {@link #init init}
 *        or the {@link #updateEnvironment updateEnvironment} methods.
 *        Both methods have a {@link BidiComplexHelper} argument named <code>caller</code>.
 *        All calls to methods of <code>BidiComplexProcessor</code> have a
 *        <code>caller</code> argument.
 *        If a processor needs to retain some data across invocations, it may
 *        access the field {@link BidiComplexImpl#processorData caller.impl.processorData}.
 *        It is guaranteed that this field will not be modified by any code
 *        except the processor itself.</li>
 *    <li>The behavior of a processor is governed by 3 factors:
 *        <ul>
 *          <li>The {@link BidiComplexFeatures#operators operators} field of its
 *              {@link BidiComplexFeatures features} determines how submitted
 *              complex expressions are split into tokens.</li>
 *          <li>The tokens are displayed one after the other according
 *              to the appropriate {@link BidiComplexFeatures#dirArabic direction}.
 *          <li>The counter {@link BidiComplexFeatures#specialsCount specialsCount}
 *              determines how many special cases need to be handled by
 *              code specific to that processor.
 *  </ul>
 *
 *  @author Matitiahu Allouche
 */
public class BidiComplexProcessor implements IBidiComplexProcessor {

	/**
	 *  In <code>BidiComplexProcessor</code> this method returns a
	 *  {@link BidiComplexFeatures#DEFAULT DEFAULT} value which
	 *  directs the processor to do nothing.
	 */
	public BidiComplexFeatures init(BidiComplexHelper caller, BidiComplexEnvironment environment) {
		return BidiComplexFeatures.DEFAULT;
	}

	/**
	 *  In <code>BidiComplexProcessor</code> this method simply calls the
	 *  {@link #init init} method. If this is good enough, i.e.
	 *  if the {@link BidiComplexFeatures features} of the processor are
	 *  determined identically for initialization and after a
	 *  change in the {@link BidiComplexEnvironment environment}, subclasses of
	 *  <code>BidiComplexProcessor</code> don't need to override this
	 *  method.
	 */
	public BidiComplexFeatures updateEnvironment(BidiComplexHelper caller, BidiComplexEnvironment environment) {
		return init(caller, environment);
	}

	/**
	 *  In <code>BidiComplexProcessor</code> this method throws an
	 *  <code>IllegalStateException</code>. This is appropriate behavior
	 *  (and does not need to be overridden) for processors whose
	 *  {@link BidiComplexFeatures#specialsCount specialsCount} is zero, which
	 *  means that <code>indexOfSpecial</code> should never be called
	 *  for them.
	 */
	public int indexOfSpecial(BidiComplexHelper caller, int caseNumber, String srcText, int fromIndex) {
		// This method must be overridden by all subclasses with special cases.
		throw new IllegalStateException();
	}

	/**
	 *  In <code>BidiComplexProcessor</code> this method throws an
	 *  <code>IllegalStateException</code>. This is appropriate behavior
	 *  (and does not need to be overridden) for processors whose
	 *  {@link BidiComplexFeatures#specialsCount specialsCount} is zero, which
	 *  means that <code>processSpecial</code> should never be called
	 *  for them.
	 */
	public int processSpecial(BidiComplexHelper caller, int caseNumber, String srcText, int operLocation) {
		// This method must be overridden by all subclasses with any special case.
		throw new IllegalStateException();
	}

}
