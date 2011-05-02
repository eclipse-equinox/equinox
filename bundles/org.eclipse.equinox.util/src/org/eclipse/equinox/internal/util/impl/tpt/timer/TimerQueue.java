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
package org.eclipse.equinox.internal.util.impl.tpt.timer;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

class TimerQueue {

	static final int POOL_SIZE = 20;
	QueueElement[] pool = new QueueElement[POOL_SIZE];
	int filled = 0;

	QueueElement first;
	QueueElement lastInserted;

	TimerQueue() {
	}

	/**
	 * Adds a new task to the priority queue.
	 */
	void add(TimerQueueNode task) {
		QueueElement toAdd = getQueueElement();
		toAdd.node = task;

		if (first == null) {
			first = toAdd;
		} else {
			insertElement(toAdd);
		}
	}

	/**
	 * Return the "head task" of the priority queue. (The head task is an task
	 * with the lowest nextExecutionTime.)
	 */
	TimerQueueNode getMin() {
		return (first != null) ? first.node : null;
	}

	/**
	 * Remove the head task from the priority queue.
	 */
	void removeMin() {
		if (first != null) {
			if (lastInserted == first) {
				lastInserted = null;
			}
			if (filled < POOL_SIZE) {
				QueueElement toFree = first;
				first = first.next;
				freeQueueElement(toFree);
			} else {
				first = first.next;
			}
		}
	}

	/**
	 * Sets the nextExecutionTime associated with the head task to the specified
	 * value, and adjusts priority queue accordingly.
	 */
	void rescheduleMin(long newTime) {
		first.node.runOn = newTime;
		if (first.next != null) {
			if (lastInserted == first) {
				lastInserted = null;
			}
			QueueElement el = first;
			first = first.next;
			el.next = null;
			insertElement(el);
		}
	}

	/**
	 * Returns true if the priority queue contains no elements.
	 */
	boolean isEmpty() {
		return first == null;
	}

	/**
	 * Removes all elements from the priority queue.
	 */
	void clear() {
		while (first != null) {
			first.node.returnInPool();
			first = first.next;
		}
		lastInserted = null;
	}

	void insertElement(QueueElement newElement) {
		if (first.node.runOn >= newElement.node.runOn) {
			/* private case - insert in the beginning of the queue */
			newElement.next = first;
			first = newElement;
		} else if (lastInserted != null) {
			if (lastInserted.node.runOn == newElement.node.runOn) {
				QueueElement tmp = lastInserted.next;
				lastInserted.next = newElement;
				newElement.next = tmp;
			} else if (lastInserted.node.runOn > newElement.node.runOn) {
				// System.out.println("insert 1");
				doInsertElement(first, newElement);
			} else {
				// System.out.println("insert 2");
				doInsertElement(lastInserted, newElement);
			}
		} else {
			// System.out.println("insert 3");
			doInsertElement(first, newElement);
		}
	}

	/**
	 * elToInsert should not be placed before firstEl, because firstEl may not
	 * be the first element of the queue
	 */
	void doInsertElement(QueueElement firstEl, QueueElement elToInsert) {
		QueueElement tmp = firstEl;
		QueueElement prev = firstEl;
		while (tmp != null && tmp.node.runOn < elToInsert.node.runOn) {
			prev = tmp;
			tmp = tmp.next;
		}
		if (tmp == null) {
			/* reached the end of the queue */
			prev.next = elToInsert;
		} else {
			prev.next = elToInsert;
			elToInsert.next = tmp;
		}
		lastInserted = elToInsert;
	}

	void removeTimerNode(TimerQueueNode node) {
		QueueElement tmp = first;
		QueueElement prev = null;
		while (tmp != null) {
			if (node.listener == tmp.node.listener && node.event == tmp.node.event) {
				if (prev != null) {
					if (lastInserted == tmp) {
						lastInserted = prev;
					}
					prev.next = tmp.next;
					if (filled < POOL_SIZE) {
						freeQueueElement(tmp);
					}
				} else {
					/* removing the first element */
					if (lastInserted == first) {
						lastInserted = null;
					}
					if (filled < POOL_SIZE) {
						QueueElement toFree = first;
						first = first.next;
						freeQueueElement(toFree);
					} else {
						first = first.next;
					}
				}
				break;
			}
			prev = tmp;
			tmp = tmp.next;
		}
	}

	private QueueElement getQueueElement() {
		return (filled > 0) ? pool[--filled] : new QueueElement();
	}

	private void freeQueueElement(QueueElement toFree) {
		if (filled < POOL_SIZE) {
			pool[filled] = toFree;
			filled++;
			toFree.next = null;
			toFree.node = null;
		}
	}

	private class QueueElement {
		QueueElement next;
		TimerQueueNode node;

		public QueueElement() {
		}
	}

}
