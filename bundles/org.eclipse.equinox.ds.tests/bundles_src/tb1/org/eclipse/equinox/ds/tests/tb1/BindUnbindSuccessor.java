package org.eclipse.equinox.ds.tests.tb1;

import java.util.Dictionary;

import org.eclipse.equinox.ds.tests.tb1.impl.BindUnbind;
import org.eclipse.equinox.ds.tests.tbc.ComponentContextProvider;
import org.eclipse.equinox.ds.tests.tbc.ComponentManager;
import org.osgi.service.component.ComponentContext;


public class BindUnbindSuccessor extends BindUnbind implements ComponentManager, ComponentContextProvider {

  private ComponentContext ctxt;
  
  public void activate(ComponentContext ctxt) {
   this.ctxt = ctxt;
  }
  
  public void deactivate(ComponentContext ctxt) {
    this.ctxt = null;
  }
  
  public void doNothing() {
    
  }
  
  public ComponentContext getContext() {
    return ctxt;
  }

  public void enableComponent(String name, boolean flag) {
    if (flag) ctxt.enableComponent(name);
    else      ctxt.disableComponent(name);
  }

  public Dictionary getProperties() {
    return ctxt.getProperties();
  }

  public ComponentContext getComponentContext() {
    return ctxt;
  }
}
