/*******************************************************************************
 * Copyright (c) 2008, 2011 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.eclipse.virgo.util.math;

/**
 * Defines an ordered pair of elements of types <code>F</code> and <code>S</code> respectively.
 * <p/>
 * The elements may be null, in which case they are without type.
 * <br/>For example:
 * <pre>
 *        ( new OrderedPair<Long,Long  >(42,null) )
 * .equals( new OrderedPair<Long,String>(42,null) )</pre>
 * returns <code>true</code>.
 * <p/>
 * 
 * <strong>Concurrent Semantics</strong><br/>
 * Implementation is immutable.<br/> 
 * <strong>Note:</strong> the elements are <code>final</code> but that doesn't stop
 * them being modified after the pair is constructed. The <code>hashCode()</code> of the <code>OrderedPair</code> may then change.
 * 
 * @param <F> type of first element
 * @param <S> type of second element
 * 
 */
public final class OrderedPair<F, S> {

	private final F first;

	private final S second;

	/**
	 * Creates a new <code>OrderedPair</code>.
	 * 
	 * @param first
	 *            the first member of the <code>OrderedPair</code>.
	 * @param second
	 *            the second member of the <code>OrderedPair</code>.
	 */
	public OrderedPair(F first, S second) {
		this.first = first;
		this.second = second;
	}

	/**
	 * Gets the first member of the <code>OrderedPair</code>.
	 * 
	 * @return the first member of the <code>OrderedPair</code>.
	 */
	public F getFirst() {
		return this.first;
	}

	/**
	 * Gets the second member of the <code>OrderedPair</code>.
	 * 
	 * @return the second member of the <code>OrderedPair</code>.
	 */
	public S getSecond() {
		return this.second;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = this.first == null ? 0 : this.first.hashCode();
		result = prime * result + (this.second == null ? 0 : this.second.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		@SuppressWarnings("unchecked")
		final OrderedPair<F, S> other = (OrderedPair<F, S>) obj;

		return (this.first == null ? other.first == null : this.first.equals(other.first)) && (this.second == null ? other.second == null : this.second.equals(other.second));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "(" + String.valueOf(this.first) + ", " + String.valueOf(this.second) + ")";
	}

}
