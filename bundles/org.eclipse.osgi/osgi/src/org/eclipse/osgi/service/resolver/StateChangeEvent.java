package org.eclipse.osgi.service.resolver;

import java.util.EventObject;

public class StateChangeEvent extends EventObject {
	private StateDelta delta;
	public StateChangeEvent(StateDelta delta) {
		super(delta.getState());
		this.delta = delta;
	}
	/**
	 * Returns a delta detailing changes to a state object.
	 * 
	 * @return a state delta
	 */
	public StateDelta getDelta() {
		return delta;
	}
}