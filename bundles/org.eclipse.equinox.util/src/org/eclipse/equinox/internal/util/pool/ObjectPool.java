/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.util.pool;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class ObjectPool {

	private Class template;

	protected Object[][] buff;

	protected int nextFree;

	protected int size;

	protected int minimumFill;

	protected int factor;

	protected int minfSize;

	protected int minffactor;

	// protected int usageGet = 0;

	// protected int usageReleased = 0;

	protected boolean dontExtend = false;

	protected ObjectCreator oc;

	public Object getInstance() throws Exception {
		return oc != null ? oc.getInstance() : template.newInstance();
	}

	public ObjectPool(ObjectCreator oc, int size, int factor) {
		this(null, oc, size, factor, size * factor);
	}

	public ObjectPool(Class template, int size, int factor) {
		this(template, null, size, factor, size * factor);
	}

	public ObjectPool(Class template, int size, int factor, int minimumFill) {
		this(template, null, size, factor, (minimumFill = (minimumFill > (size * factor)) ? (size * factor) : minimumFill));
	}

	public ObjectPool(ObjectCreator oc, int size, int factor, int minimumFill) {
		this(null, oc, size, factor, (minimumFill = minimumFill > (size * factor) ? (size * factor) : minimumFill));
	}

	protected ObjectPool(Class template, ObjectCreator oc, int size, int factor, int minimumFill) {

		if (size <= 1 || factor < 1) {
			throw new IllegalArgumentException(size + " is less or equal to 1");
		}

		this.minimumFill = minimumFill < 1 ? 1 : minimumFill;
		this.oc = oc;
		if (template != null) {
			try {
				template.getConstructor(new Class[0]);
				this.template = template;
			} catch (NoSuchMethodException nsm) {
				throw new IllegalArgumentException(template + " don't have default constructor!");
			}
		}
		buff = new Object[size][];
		this.size = size;
		this.factor = factor;
		minfSize = this.minimumFill / factor;
		minffactor = this.minimumFill % factor;
		// System.out.println("minimumFill " + this.minimumFill);
		// System.out.println("minfSize " + minfSize);
		// System.out.println("minffactor " + minffactor);
		if (minimumFill <= 1) {
			nextFree = -1;
		} else {
			for (int i = 0; i < (minfSize == 0 ? 1 : minfSize); i++) {
				buff[i] = new Object[factor];
			}
			fill();
		}
		// start();
	}

	private void fill() {
		int i = 0;
		synchronized (buff) {
			for (; (i < minfSize); i++) {
				put(i, factor);
			}
			if (minffactor > 0) {
				put(i, minffactor);
			}
		}
		nextFree = minimumFill - 1;
	}

	private void put(int i, int count) {
		for (int j = 0; j < count; j++) {
			try {
				if (buff[i] == null) {
					buff[i] = new Object[factor];
				}
				buff[i][j] = getInstance();
				nextFree = i * j;
			} catch (Throwable t) {
				throw new RuntimeException("buffer fill failed: " + t);
			}
		}
	}

	public void clear() {
		dontExtend = true;
		shrink(-1);
	}

	protected void shrink(int count) {
		synchronized (buff) {
			for (; nextFree > count; nextFree--) {
				buff[(nextFree / factor)][nextFree % factor] = null;
			}
		}
	}

	public void shrink() {
		dontExtend = true;
		shrink(minimumFill);
		dontExtend = false;
	}

	public Object getObject() {
		Object tmp = null;
		synchronized (buff) {
			if (nextFree < 0) {
				if (dontExtend) {
					throw new RuntimeException();
				}
				if (minimumFill <= 1) {
					try {
						return getInstance();
					} catch (Throwable e) {
						throw new RuntimeException("buffer fill failed: " + e);
					}
				}
				fill();
			}
			tmp = buff[(nextFree / factor)][(nextFree % factor)];
			buff[(nextFree / factor)][nextFree % factor] = null;
			nextFree--;
			// usageGet++;
			return tmp;
		}
	}

	public boolean releaseObject(Object obj) {
		// usageReleased++;
		if (dontExtend) {
			return false;
		}
		synchronized (buff) {
			int tmp = nextFree + 1;
			int telement = tmp / factor;
			if ((telement) < size) {
				if (buff[telement] == null) {
					buff[telement] = new Object[factor];
				}
				buff[telement][tmp % factor] = obj;
				nextFree = tmp;
				return true;
			}
			return false;
		}
	}
}
