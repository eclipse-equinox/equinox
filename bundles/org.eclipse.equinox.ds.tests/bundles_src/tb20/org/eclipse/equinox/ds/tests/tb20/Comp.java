package org.eclipse.equinox.ds.tests.tb20;

import java.util.Dictionary;

import org.eclipse.equinox.ds.tests.tbc.PropertiesProvider;
import org.osgi.service.component.ComponentContext;

public class Comp implements PropertiesProvider {
  private ComponentContext ctxt;

  protected void activate(ComponentContext ctxt) {
    this.ctxt = ctxt;
  }

  protected void deactivate(ComponentContext ctxt) {

  }

  public Dictionary getProperties() {
    if (ctxt == null)
      return null;

    return ctxt.getProperties();
  }
}
