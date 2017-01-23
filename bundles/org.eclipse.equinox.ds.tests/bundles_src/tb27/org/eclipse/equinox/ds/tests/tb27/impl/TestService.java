package org.eclipse.equinox.ds.tests.tb27.impl;

import java.util.AbstractCollection;
import java.util.Collections;
import java.util.Iterator;

public class TestService<T> extends AbstractCollection<T> {

	@Override
	public Iterator<T> iterator() {
		return (Iterator<T>) Collections.emptyList().iterator();
	}

	@Override
	public int size() {
		return 0;
	}

}
