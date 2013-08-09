package org.eclipse.equinox.ds.tests.tb26.impl;

import org.eclipse.equinox.ds.tests.tb26.Component;

public class Component2 extends Component {
	public String getName() {
		return getClass().getName();
	}
	
	public void update() throws Exception {
		replaceCurrentComponentXmlWith("component1.xml");
	}
}
