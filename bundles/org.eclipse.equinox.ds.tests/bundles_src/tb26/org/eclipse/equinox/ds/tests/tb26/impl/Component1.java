package org.eclipse.equinox.ds.tests.tb26.impl;

import org.eclipse.equinox.ds.tests.tb26.Component;

public class Component1 extends Component {
	public String getName() {
		return getClass().getName();
	}
	
	public void update() throws Exception {
		replaceCurrentComponentXmlWith("component2.xml");
	}
}
