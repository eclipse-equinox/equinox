package org.eclipse.equinox.ds.tests.tb4;

import org.eclipse.equinox.ds.tests.tbc.TestHelper;
import org.osgi.service.component.ComponentContext;


public class AnotherComponent {

  public void activate(ComponentContext ctxt) {
    TestHelper.setActivatedStandAlone(true);
  }
  
  public void deactivate(ComponentContext ctxt) {
    TestHelper.setActivatedStandAlone(false);
  }
}
