/*******************************************************************************
 * Copyright (c) 2011 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SpringSource, a division of VMware - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.eclipse.equinox.internal.region.hook;

import java.util.*;
import org.eclipse.equinox.region.*;

/**
 * {@link RegionDigraphVisitorBase} is an abstract base class for {@link RegionDigraphVisitor} implementations in the
 * framework hooks.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * This class is thread safe.
 */
abstract class RegionDigraphVisitorBase<C> implements RegionDigraphVisitor {

	private final Collection<C> allCandidates;

	private final Stack<Set<C>> allowedStack = new Stack<Set<C>>();
	private final Stack<Collection<C>> filteredStack = new Stack<Collection<C>>();

	private Object monitor = new Object();

	private Set<C> allowed;

	protected RegionDigraphVisitorBase(Collection<C> candidates) {
		this.allCandidates = candidates;
		synchronized (this.monitor) {
			this.allowed = new HashSet<C>();
		}
	}

	Collection<C> getAllowed() {
		synchronized (this.monitor) {
			return this.allowed;
		}
	}

	private void allow(C candidate) {
		synchronized (this.monitor) {
			this.allowed.add(candidate);
		}
	}

	private void allow(Collection<C> candidates) {
		synchronized (this.monitor) {
			this.allowed.addAll(candidates);
		}
	}

	private void pushAllowed() {
		synchronized (this.monitor) {
			this.allowedStack.push(this.allowed);
			this.allowed = new HashSet<C>();
		}
	}

	private Collection<C> popAllowed() {
		synchronized (this.monitor) {
			Collection<C> a = this.allowed;
			this.allowed = this.allowedStack.pop();
			return a;
		}
	}

	private void pushFiltered(Collection<C> filtered) {
		synchronized (this.monitor) {
			this.filteredStack.push(filtered);
		}
	}

	private Collection<C> popFiltered() {
		synchronized (this.monitor) {
			return this.filteredStack.isEmpty() ? allCandidates : this.filteredStack.pop();
		}
	}

	private Collection<C> peekFiltered() {
		synchronized (this.monitor) {
			return this.filteredStack.isEmpty() ? allCandidates : this.filteredStack.peek();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(Region region) {
		Collection<C> candidates = peekFiltered();
		for (C candidate : candidates) {
			if (contains(region, candidate)) {
				allow(candidate);
			}
		}
		if (allowed.containsAll(candidates)) {
			// there is no need to traverse edges of this region,
			// it contains all the remaining filtered candidates
			return false;
		}
		return true;
	}

	/**
	 * Determines whether the given region contains the given candidate.
	 * 
	 * @param region the {@link Region}
	 * @param candidate the candidate
	 * @return <code>true</code> if and only if the given region contains the given candidate
	 */
	protected abstract boolean contains(Region region, C candidate);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean preEdgeTraverse(RegionFilter regionFilter) {
		// Find the candidates filtered by the previous edge
		Collection<C> candidates = new ArrayList<C>(peekFiltered());
		// remove any candidates contained in the current region
		candidates.removeAll(allowed);
		// apply the filter across remaining candidates
		filter(candidates, regionFilter);
		if (candidates.isEmpty())
			return false; // this filter does not apply; avoid traversing this edge
		// push the filtered candidates for the next region
		pushFiltered(candidates);
		// push the allowed
		pushAllowed();
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void postEdgeTraverse(RegionFilter regionFilter) {
		popFiltered();
		Collection<C> candidates = popAllowed();
		allow(candidates);
	}

	private void filter(Collection<C> candidates, RegionFilter filter) {
		Iterator<C> i = candidates.iterator();
		while (i.hasNext()) {
			C candidate = i.next();
			if (!isAllowed(candidate, filter)) {
				i.remove();
			}
		}
	}

	/**
	 * Determines whether the given candidate is allowed by the given {@link RegionFilter}.
	 * 
	 * @param candidate the candidate
	 * @param filter the filter
	 * @return <code>true</code> if and only if the given candidate is allowed by the given filter
	 */
	protected abstract boolean isAllowed(C candidate, RegionFilter filter);
}