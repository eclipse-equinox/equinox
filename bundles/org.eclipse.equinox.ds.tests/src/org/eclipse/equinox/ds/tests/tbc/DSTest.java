/*******************************************************************************
 * Copyright (c) 1997, 2018 by ProSyst Software GmbH and others.
 * http://www.prosyst.com
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds.tests.tbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.equinox.ds.tests.BundleInstaller;
import org.eclipse.equinox.ds.tests.DSTestsActivator;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.permissionadmin.PermissionInfo;
import org.osgi.util.tracker.ServiceTracker;

public class DSTest {

  private static final String NAMED_CLASS = "org.eclipse.equinox.ds.tests.tb4.NamedService";

  private static final String EXTENDED_CLASS = "org.eclipse.equinox.ds.tests.tb1.BindUnbindSuccessor";

  private static final String SAC_CLASS = "org.eclipse.equinox.ds.tests.tb1.impl.AnotherComponent";

  private static final String SC_CLASS = "org.eclipse.equinox.ds.tests.tb1.impl.BaseComp";

  private static final String DYN_SERVICE_CLASS = "org.eclipse.equinox.ds.tests.tb4.DynamicService";

  private static final String BSRC_CLASS = "org.eclipse.equinox.ds.tests.tb4.BoundReplacer";

  private static final String MBSRC_CLASS = "org.eclipse.equinox.ds.tests.tb4.AdvancedBounder";

  private static final String SECURITY_CLASS = "org.eclipse.equinox.ds.tests.tb5.impl.SecurityTester";

  private static final String BLOCK_ACTIVE_CLASS = "org.eclipse.equinox.ds.tests.tb2.impl.Blocker";

  private static final String BLOCK_BIND_CLASS = "org.eclipse.equinox.ds.tests.tb3.impl.BindBlocker";

  private static final String STATIC_CLASS = "org.eclipse.equinox.ds.tests.tb6.StaticComp";

  private static final String REFERENCED_CLASS = "org.eclipse.equinox.ds.tests.tb6.ReferencedComp";

  private static final String NS_CLASS = "org.eclipse.equinox.ds.tests.tbc.NamespaceProvider";

  private static final String COMP_OPTIONAL_100 = "org.eclipse.equinox.ds.tests.tb11.optionalNS100";

  private static final String COMP_OPTIONAL_110 = "org.eclipse.equinox.ds.tests.tb11.optionalNS110";

  private static final String COMP_REQUIRE_100 = "org.eclipse.equinox.ds.tests.tb11.requireNS100";

  private static final String COMP_REQUIRE_110 = "org.eclipse.equinox.ds.tests.tb11.requireNS110";

  private static final String COMP_IGNORE_100 = "org.eclipse.equinox.ds.tests.tb11.ignoreNS100";

  private static final String COMP_IGNORE_110 = "org.eclipse.equinox.ds.tests.tb11.ignoreNS110";

  private static final String COMP_NOTSET_100 = "org.eclipse.equinox.ds.tests.tb11.notsetNS100";

  private static final String COMP_NOTSET_110 = "org.eclipse.equinox.ds.tests.tb11.notsetNS110";

  private static final String MOD_NOTSET_NS100 = "org.eclipse.equinox.ds.tests.tb21.notsetNS100";

  private static final String MOD_NOTSET_NS110 = "org.eclipse.equinox.ds.tests.tb21.notsetNS110";

  private static final String MOD_NOARGS_NS100 = "org.eclipse.equinox.ds.tests.tb21.NoArgs100";

  private static final String MOD_NOARGS_NS110 = "org.eclipse.equinox.ds.tests.tb21.NoArgs110";

  private static final String MOD_CC_NS100 = "org.eclipse.equinox.ds.tests.tb21.CcNS100";

  private static final String MOD_CC_NS110 = "org.eclipse.equinox.ds.tests.tb21.CcNS110";

  private static final String MOD_BC_NS100 = "org.eclipse.equinox.ds.tests.tb21.BcNS100";

  private static final String MOD_BC_NS110 = "org.eclipse.equinox.ds.tests.tb21.BcNS110";

  private static final String MOD_MAP_NS100 = "org.eclipse.equinox.ds.tests.tb21.MapNS100";

  private static final String MOD_MAP_NS110 = "org.eclipse.equinox.ds.tests.tb21.MapNS110";

  private static final String MOD_CC_BC_MAP_NS100 = "org.eclipse.equinox.ds.tests.tb21.CcBcMapNS100";

  private static final String MOD_CC_BC_MAP_NS110 = "org.eclipse.equinox.ds.tests.tb21.CcBcMapNS110";

  private static final String MOD_NOT_EXIST_NS110 = "org.eclipse.equinox.ds.tests.tb21.NotExistNS110";

  private static final String MOD_THROW_EX_NS110 = "org.eclipse.equinox.ds.tests.tb21.ThrowExNS110";
  
  private static final String COMP_OPTIONAL = "org.eclipse.equinox.ds.tests.tb24.optional";
  
  private static final String COMP_REQUIRE = "org.eclipse.equinox.ds.tests.tb24.require";

  private static final String COMP_IGNORE = "org.eclipse.equinox.ds.tests.tb24.ignore";


  private static int timeout = 1000;

  private Bundle tb1;

  private ServiceTracker trackerNamedService;

  private ServiceTracker trackerNamedServiceFactory;

  private ServiceTracker trackerCM;

  private ServiceTracker trackerExtendedClass;

  private ServiceTracker trackerSAC;

  private ServiceTracker trackerSC;

  private ServiceTracker trackerDynService;

  private ServiceTracker trackerDynServiceFactory;

  private ServiceTracker trackerBSRC;

  private ServiceTracker trackerMBSRC;

  private ServiceTracker trackerSecurity;

  private ServiceTracker trackerBAS;

  private ServiceTracker trackerBBS;

  private ServiceTracker trackerStatic;

  private ServiceTracker trackerReferenced;

  private ServiceTracker trackerNS;

  private ServiceTracker trackerBoundServiceCounterFactory;

  private ServiceTracker trackerBoundServiceCounterHelperFactory;

  private ServiceTracker trackerStaticServiceCounterFactory;

  private ServiceTracker trackerBaseService;

  private Hashtable registeredServices = new Hashtable();

  private int scr_restart_timeout = 33000;

  private boolean synchronousBuild = false;

  private BundleInstaller installer;

  @Before
  public void setUp() throws Exception {
    DSTestsActivator.activateSCR();

    timeout = getSystemProperty("scr.test.timeout", timeout);
    scr_restart_timeout = getSystemProperty("scr.restart.timeout", scr_restart_timeout);

    String synchronousBuildProp = System.getProperty("equinox.ds.synchronous_build");
    synchronousBuild = (synchronousBuildProp == null) || !synchronousBuildProp.equalsIgnoreCase("false");

    clearConfigurations();
    // init trackers
    BundleContext bc = getContext();

    installer = new BundleInstaller("/scr_test/", bc);

    // install test bundles
    tb1 = installBundle("tb1");

    // start them
    tb1.start();
    waitBundleStart();

    trackerNamedService = new ServiceTracker(bc, NAMED_CLASS, null);
    Filter filter = bc.createFilter("(&(" + ComponentConstants.COMPONENT_FACTORY + '=' + NAMED_CLASS + ")("
        + Constants.OBJECTCLASS + '=' + ComponentFactory.class.getName() + "))");
    trackerNamedServiceFactory = new ServiceTracker(bc, filter, null);
    trackerCM = new ServiceTracker(bc, ConfigurationAdmin.class.getName(), null);
    trackerExtendedClass = new ServiceTracker(bc, EXTENDED_CLASS, null);
    trackerSAC = new ServiceTracker(bc, SAC_CLASS, null);
    trackerSC = new ServiceTracker(bc, SC_CLASS, null);
    trackerDynService = new ServiceTracker(bc, DYN_SERVICE_CLASS, null);
    filter = bc.createFilter("(&(" + ComponentConstants.COMPONENT_FACTORY + '=' + DYN_SERVICE_CLASS + ")("
        + Constants.OBJECTCLASS + '=' + ComponentFactory.class.getName() + "))");
    trackerDynServiceFactory = new ServiceTracker(bc, filter, null);
    trackerBSRC = new ServiceTracker(bc, BSRC_CLASS, null);
    trackerMBSRC = new ServiceTracker(bc, MBSRC_CLASS, null);
    trackerSecurity = new ServiceTracker(bc, SECURITY_CLASS, null);
    trackerBAS = new ServiceTracker(bc, BLOCK_ACTIVE_CLASS, null);
    trackerBBS = new ServiceTracker(bc, BLOCK_BIND_CLASS, null);
    trackerStatic = new ServiceTracker(bc, STATIC_CLASS, null);
    trackerReferenced = new ServiceTracker(bc, REFERENCED_CLASS, null);
    trackerNS = new ServiceTracker(bc, NS_CLASS, null);
    filter = bc.createFilter("(&(" + ComponentConstants.COMPONENT_FACTORY + '=' + "CountFactory" + ")("
        + Constants.OBJECTCLASS + '=' + ComponentFactory.class.getName() + "))");
    trackerBoundServiceCounterFactory = new ServiceTracker(bc, filter, null);
    filter = bc.createFilter("(&(" + ComponentConstants.COMPONENT_FACTORY + '=' + "CountHelperFactory" + ")("
        + Constants.OBJECTCLASS + '=' + ComponentFactory.class.getName() + "))");
    trackerBoundServiceCounterHelperFactory = new ServiceTracker(bc, filter, null);
    filter = bc.createFilter("(&(" + ComponentConstants.COMPONENT_FACTORY + '=' + "StaticServiceCountFactory" + ")("
        + Constants.OBJECTCLASS + '=' + ComponentFactory.class.getName() + "))");
    trackerStaticServiceCounterFactory = new ServiceTracker(bc, filter, null);
    trackerBaseService = new ServiceTracker(bc, PropertiesProvider.class.getName(), null);

    // start listening
    trackerNamedService.open(true);
    trackerNamedServiceFactory.open(true);
    trackerCM.open(true);
    trackerExtendedClass.open(true);
    trackerSAC.open(true);
    trackerSC.open(true);
    trackerDynService.open(true);
    trackerDynServiceFactory.open(true);
    trackerBSRC.open(true);
    trackerMBSRC.open(true);
    trackerSecurity.open(true);
    trackerBAS.open(true);
    trackerBBS.open(true);
    trackerStatic.open(true);
    trackerReferenced.open(true);
    trackerNS.open(true);
    trackerBoundServiceCounterFactory.open(true);
    trackerBoundServiceCounterHelperFactory.open(true);
    trackerStaticServiceCounterFactory.open(true);
    trackerBaseService.open(true);
  }

  /**
   * This methods takes care of the configurations related to this test
   * 
   * @throws IOException
   * @throws InvalidSyntaxException
   * @throws InterruptedException
   */
  private void clearConfigurations() throws IOException, InvalidSyntaxException {
    ServiceReference cmSR = getContext().getServiceReference(ConfigurationAdmin.class.getName());
    if (cmSR == null)
      return;
    ConfigurationAdmin cm = (ConfigurationAdmin) getContext().getService(cmSR);
    // clean configurations from previous tests
    // clean factory configs for named service
    clearConfiguration(cm, "(service.factoryPid=" + NAMED_CLASS + ")");
    // clean configs for named service
    clearConfiguration(cm, "(service.pid=" + NAMED_CLASS + ")");
    // clean configs for stand alone component
    clearConfiguration(cm, "(service.pid=" + SAC_CLASS + ")");
    // clean configs for optionalNS100
    clearConfiguration(cm, "(service.pid=" + COMP_OPTIONAL_100 + ")");
    clearConfiguration(cm, "(service.factoryPid=" + COMP_OPTIONAL_100 + ")");
    // clean configs for optionalNS110
    clearConfiguration(cm, "(service.pid=" + COMP_OPTIONAL_110 + ")");
    clearConfiguration(cm, "(service.factoryPid=" + COMP_OPTIONAL_110 + ")");
    // clean configs for requireNS100
    clearConfiguration(cm, "(service.pid=" + COMP_REQUIRE_100 + ")");
    clearConfiguration(cm, "(service.factoryPid=" + COMP_REQUIRE_100 + ")");
    // clean configs for requireNS110
    clearConfiguration(cm, "(service.pid=" + COMP_REQUIRE_110 + ")");
    clearConfiguration(cm, "(service.factoryPid=" + COMP_REQUIRE_110 + ")");
    // clean configs for ignoreNS100
    clearConfiguration(cm, "(service.pid=" + COMP_IGNORE_100 + ")");
    clearConfiguration(cm, "(service.factoryPid=" + COMP_IGNORE_100 + ")");
    // clean configs for ignoreNS110
    clearConfiguration(cm, "(service.pid=" + COMP_IGNORE_110 + ")");
    clearConfiguration(cm, "(service.factoryPid=" + COMP_IGNORE_110 + ")");
    // clean configs for notsetNS100
    clearConfiguration(cm, "(service.pid=" + COMP_NOTSET_100 + ")");
    clearConfiguration(cm, "(service.factoryPid=" + COMP_NOTSET_100 + ")");
    // clean configs for notsetNS110
    clearConfiguration(cm, "(service.pid=" + COMP_NOTSET_110 + ")");
    clearConfiguration(cm, "(service.factoryPid=" + COMP_NOTSET_110 + ")");

    clearConfiguration(cm, "(service.pid=" + MOD_NOTSET_NS100 + ")");
    clearConfiguration(cm, "(service.pid=" + MOD_NOTSET_NS110 + ")");
    clearConfiguration(cm, "(service.pid=" + MOD_NOARGS_NS100 + ")");
    clearConfiguration(cm, "(service.pid=" + MOD_NOARGS_NS110 + ")");
    clearConfiguration(cm, "(service.pid=" + MOD_CC_NS100 + ")");
    clearConfiguration(cm, "(service.pid=" + MOD_CC_NS110 + ")");
    clearConfiguration(cm, "(service.pid=" + MOD_BC_NS100 + ")");
    clearConfiguration(cm, "(service.pid=" + MOD_BC_NS110 + ")");
    clearConfiguration(cm, "(service.pid=" + MOD_MAP_NS100 + ")");
    clearConfiguration(cm, "(service.pid=" + MOD_MAP_NS110 + ")");
    clearConfiguration(cm, "(service.pid=" + MOD_CC_BC_MAP_NS100 + ")");
    clearConfiguration(cm, "(service.pid=" + MOD_CC_BC_MAP_NS110 + ")");
    clearConfiguration(cm, "(service.pid=" + MOD_NOT_EXIST_NS110 + ")");
    clearConfiguration(cm, "(service.pid=" + MOD_THROW_EX_NS110 + ")");

    clearConfiguration(cm, "(service.pid=" + COMP_IGNORE + ")");
    clearConfiguration(cm, "(service.pid=" + COMP_OPTIONAL + ")");
    clearConfiguration(cm, "(service.pid=" + COMP_REQUIRE + ")");
    getContext().ungetService(cmSR);
  }

  private void clearConfiguration(ConfigurationAdmin cm, String filter) throws IOException, InvalidSyntaxException {
    Configuration[] configs = cm.listConfigurations(filter);
    for (int i = 0; configs != null && i < configs.length; i++) {
      Configuration configuration = configs[i];
      configuration.delete();
    }
  }

  /**
   * @param propertyKey
   */
  private int getSystemProperty(String propertyKey, int defaultValue) {
    String propertyString = System.getProperty(propertyKey);
    int sleepTime = defaultValue;
    if (propertyString != null) {
      try {
        sleepTime = Integer.parseInt(propertyString);
      } catch (Exception e) {
        e.printStackTrace();
        System.out.println("Error while parsing sleep value! The default one will be used : " + defaultValue);
      }
      if (sleepTime < 100) {
        log("The sleep value is too low : " + sleepTime + " ! The default one will be used : " + defaultValue);
        return defaultValue;
      }
      return sleepTime;
    }
    return defaultValue;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.equinox.ds.tests.tbc.DSTest.#tearDown()
   */
  public void tearDown() throws Exception {
    unregisterAllServices();

    trackerNamedService.close();
    trackerNamedServiceFactory.close();
    trackerExtendedClass.close();
    trackerSAC.close();
    trackerSC.close();
    trackerDynService.close();
    trackerDynServiceFactory.close();
    trackerBSRC.close();
    trackerMBSRC.close();
    trackerSecurity.close();
    trackerBAS.close();
    trackerBBS.close();
    trackerStatic.close();
    trackerReferenced.close();
    trackerNS.close();
    trackerBoundServiceCounterFactory.close();
    trackerBoundServiceCounterHelperFactory.close();
    trackerBaseService.close();

    if (installer != null) {
      BundleInstaller bi = installer;
      installer = null;
      bi.shutdown();
    }

    clearConfigurations();
  }

  @Test
  public void testBindUnbind() throws Exception {

    assertEquals("TestBundle1 must be running.", Bundle.ACTIVE, tb1.getState());

    Bundle tb1a = installBundle("tb1a");
    tb1a.start();
    waitBundleStart();
    ServiceReference sr1 = getServiceReference("org.eclipse.equinox.ds.tests.tb1a.Comp1");
    assertNotNull("Incorrect components should be ignored and the component Comp1 should be available", sr1);
    getContext().ungetService(sr1);
    uninstallBundle(tb1a);

    Object s = trackerExtendedClass.getService();
    assertNotNull("The BindUnbindSuccessor component should be available", s);

    assertTrue("The bind method on BindUnbindSuccessor component should be called to save the service reference",
        ((BoundTester) s).getBoundObjectsCount() > 0);

    // disable the referenced component to trigger unbind event
    ComponentManager enabler = (ComponentManager) s;
    enabler.enableComponent(SAC_CLASS, false);
    Thread.sleep(timeout);

    assertNull("The SAC component should be disabled (unavailable)", trackerSAC.getServiceReference());

    assertTrue("The unbind method on BindUnbindSuccessor component should be called to reset the service reference",
        ((BoundTester) s).getBoundObjectsCount() < 1);

    // enable the referenced component
    enabler = (ComponentManager) trackerExtendedClass.getService();
    enabler.enableComponent(SAC_CLASS, true);
    Thread.sleep(timeout);
    assertNotNull("The SAC component should be available", trackerSAC.getServiceReference());
  }

  @Test
  public void testUniqueComponentContext() throws Exception {
    Bundle bundle = installBundle("tb4");
    bundle.start();
    waitBundleStart();

    Hashtable props;
    ComponentFactory factory = (ComponentFactory) trackerNamedServiceFactory.getService();
    assertNotNull("The NamedService component factory should be available", factory);

    // create the first service
    props = new Hashtable();
    props.put("name", "hello");

    ComponentInstance ci1 = factory.newInstance(props);
    ComponentInstance ci2 = factory.newInstance(props);

    ComponentContextProvider cce1 = (ComponentContextProvider) ci1.getInstance();
    ComponentContextProvider cce2 = (ComponentContextProvider) ci2.getInstance();

    assertNotSame("The two instances created must be different", cce1, cce2);

    ComponentContext cc1 = cce1.getComponentContext();
    ComponentContext cc2 = cce2.getComponentContext();

    assertNotSame("The two component contexts must be not the same", cc1, cc2);

    uninstallBundle(bundle);
  }

  @Test
  public void testComponentContextMethods() throws Exception {

    Object extendedClass = trackerExtendedClass.getService();
    // check that the BindUnbindSuccessor component is available
    assertNotNull("BindUnbindSuccessor component should be available", extendedClass);

    ComponentContext ctxt = ((ComponentContextProvider) extendedClass).getComponentContext();
    assertNotNull("The BindUnbindSuccessor component should be activated properly", ctxt);

    assertNotNull("The AnotherComponent should be available before we disable it", trackerSAC.getServiceReferences());
    assertTrue("The AnotherComponent should be available before we disable it",
        trackerSAC.getServiceReferences().length > 0);
    assertNotNull("The Worker should be available before we disable it", trackerSC.getServiceReferences());
    assertTrue("The Worker should be available before we disable it", trackerSC.getServiceReferences().length > 0);

    try {
      ((ComponentManager) extendedClass).enableComponent("InvalidParameter", true); // test
      // for
      // disabling
      // unexistent
    } catch (Exception e) {
      // unexpected exception
      fail("Unexpected exception " + e.getMessage());
    }

    ((ComponentManager) extendedClass).enableComponent(SAC_CLASS, false);
    Thread.sleep(timeout); // let the SCR to unregister the service
    assertNull("The service must not be available after we had disabled the component (AnotherComponent)", trackerSAC
        .getServiceReferences());

    ((ComponentManager) extendedClass).enableComponent(SC_CLASS, false);
    Thread.sleep(timeout); // let the SCR to unregister the service
    assertNull("The service must not be available after we had disabled the component (Worker)", trackerSC
        .getServiceReferences());

    // *** test enableComponent() method
    ((ComponentManager) extendedClass).enableComponent(SAC_CLASS, true);
    Thread.sleep(timeout); // let the SCR to register the service
    assertNotNull("The service must be available after we had enabled the component", trackerSAC.getServiceReferences());
    assertTrue("The service must be available after we had enabled the component",
        trackerSAC.getServiceReferences().length > 0);

    ((ComponentManager) extendedClass).enableComponent(null, true);
    Thread.sleep(timeout);
    assertNotNull("The enableComponent() with passed null parameter, must enable the remaining disabled components",
        trackerSC.getServiceReferences());
    assertTrue("The enableComponent() with passed null parameter, must enable the remaining disabled components",
        trackerSC.getServiceReferences().length > 0);

    // *** test getBundleContext()
    BundleContextProvider sacBCE = (BundleContextProvider) trackerSAC.getService();
    assertNotNull("AnotherComponent should be available", sacBCE);
    assertSame("The two bundle context (this from the activator and from the ComponentContext object must be the same",
        sacBCE.getBundleContext(), ((ComponentContextProvider) extendedClass).getComponentContext().getBundleContext());

    // *** test getComponentInstance()
    Bundle bundle = installBundle("tb4");
    assertNotNull("Installing tb4.jar should succeed", bundle);
    bundle.start();
    waitBundleStart();

    Hashtable props;
    ComponentFactory factory = (ComponentFactory) trackerNamedServiceFactory.getService();
    assertNotNull("NamedService component factory should be available", factory);

    props = new Hashtable();
    props.put("name", "hello");

    ComponentInstance ci = factory.newInstance(props);
    assertNotNull("newInstance() should not return null", ci);
    ComponentContextProvider cce = (ComponentContextProvider) ci.getInstance();
    assertNotNull("getInstance() should not return null if we haven't disposed the component", cce);
    assertNotNull("the component instance should be initialized correctly", cce.getComponentContext());
    ComponentInstance ctxtInstance = cce.getComponentContext().getComponentInstance();
    assertSame(
        "The ComponentInstance object retrieved from the factory and from the ComponentContext must be the same", ci,
        ctxtInstance);
    // dispose the instance
    ci.dispose();
    assertNull("getInstance() should return null when disposed", ci.getInstance());

    // *** test getUsingBundle()
    ComponentContextProvider simpleComponentCCE = (ComponentContextProvider) trackerSC.getService();
    assertNotNull("Worker should be available", simpleComponentCCE);
    assertNotNull("Worker's context should be not null", simpleComponentCCE.getComponentContext());
    assertNotNull("At least this bundle (TBC) must be using the Worker service", simpleComponentCCE
        .getComponentContext().getUsingBundle());

    // *** test getProperties()
    Dictionary p = simpleComponentCCE.getComponentContext().getProperties();
    assertNotNull("Worker properties must be not null", p);
    assertEquals("The properties must contain the custom property defined in the component description", p
        .get("custom"), "customvalue");
    assertEquals("The properties must contain the component.name property", p.get(ComponentConstants.COMPONENT_NAME),
        SC_CLASS);
    assertNotNull("The properties must contain the component.id property", p.get(ComponentConstants.COMPONENT_ID));
    assertEquals("The component.id property must be of type java.lang.Long", p.get(ComponentConstants.COMPONENT_ID)
        .getClass().getName(), Long.class.getName());

    // *** test getServiceReference()
    ServiceReference ctxtServiceReference = ctxt.getServiceReference();
    ServiceReference bcServiceReference = trackerExtendedClass.getServiceReference();
    assertEquals("The two ServiceReference should be equal", ctxtServiceReference, bcServiceReference);

    // *** test locateService(String)
    Object locateSac = ctxt.locateService("StandAloneComp");
    assertNotNull("The locateService() method should return non-null object", locateSac);
    assertEquals("The object must implement " + SAC_CLASS, locateSac.getClass().getName(), SAC_CLASS);

    // test illegal call
    assertNull("Trying to get invalid reference should return null", ctxt.locateService("InvalidReference"));

    ((ComponentManager) extendedClass).enableComponent(SAC_CLASS, false); // disable
    // component
    // to
    // test
    // that
    // the
    // locateService()
    // don't
    // return
    // disabled
    // components
    Thread.sleep(timeout);

    assertEquals("Check that the component is correctly disabled", 0, countAvailableServices(trackerSAC));

    locateSac = ctxt.locateService("StandAloneComp");
    assertNull("The reference shouldn't be available with optional cardinality and disabled component", locateSac);

    ((ComponentManager) extendedClass).enableComponent(SAC_CLASS, true);
    Thread.sleep(timeout);
    assertTrue("Check that the component is correctly enabled", countAvailableServices(trackerSAC) > 0);

    // *** test locateServices(String)
    Object[] boundObjects = ctxt.locateServices("StandAloneComp");
    int boundCount = ((BoundTester) extendedClass).getBoundObjectsCount();
    assertNotNull("The returned array of bound services should not be null", boundObjects);
    assertEquals("The returned array of bound services should have the length equal to the internal count", boundCount,
        boundObjects.length);
    for (int i = 0; i < boundObjects.length; i++) {
      assertNotNull("There shouldn't be null element in the bound objects array (" + i + ")", boundObjects[i]);
    }

    assertNull("The locateServices() method should return null on invalid reference name", ctxt
        .locateServices("InvalidReference"));

    // *** test locateService(String, ServiceReference)
    assertTrue("There must be at least one bound element", ((BoundTester) extendedClass).getBoundObjectsCount() > 0);

    ServiceReference sr1 = ((BoundTester) extendedClass).getBoundServiceRef(0);
    assertNotNull("The ServiceReference bound to the BindUnbindSuccessor components should not be null", sr1);
    Object fromSR1 = ctxt.getBundleContext().getService(sr1);
    Object fromCtxt = ctxt.locateService("StandAloneComp", sr1);
    try {
      assertNotNull("The service object from BundleContext must not be null", fromSR1);
      assertNotNull("The service object from locateService() must not be null", fromCtxt);
      assertSame(
          "The two objects retrieved from BundleContext and from locateService(String, ServiceReference) method must be the same",
          fromSR1, fromCtxt);
    } finally {
      ctxt.getBundleContext().ungetService(sr1);
    }

// null is not allowed for ServiceReference
//    assertNull("locateService() must return null when passed ServiceReference is null", ctxt.locateService(
//        "StandAloneComp", null));

    assertNull("locateService() must return null when passed ServiceReference isn't bound to the component", ctxt
        .locateService("StandAloneComp", trackerExtendedClass.getServiceReference()));

    assertNull(
        "locateService() must return null when the referenceName isn't correct even if the service reference is correct",
        ctxt.locateService("InvalidReference", sr1));

    uninstallBundle(bundle);
  }

  @Test
  public void testPropertiesHandling() throws Exception {
    ConfigurationAdmin cm = (ConfigurationAdmin) trackerCM.getService();
    if (cm == null)
    	return; // cannot test without CM
    ServiceReference ref;

    // update the properties
    Hashtable props = new Hashtable(10);
    props.put("test.property.value", "setFromCM");
    props.put("test.property.list", "setFromCM");
    props.put("component.name", "setFromCM");
    props.put("component.id", Long.valueOf(-1));
    // the line below will create the configuration if it doesn't exists!
    // see CM api for details

    Configuration config = cm.getConfiguration(SAC_CLASS, null);
    assertNotNull("The Configuration object should be created if don't exist", config);
    config.update(props);

    // let SCR & CM to complete it's job
    Thread.sleep(timeout * 2);

    ref = trackerSAC.getServiceReference();
    // check the correctness of the properties
    assertNotNull("The AnotherComponent's reference should be available", ref);
    assertEquals("Properties not overriden from later ones should not be lost", "setFromFile", ref
        .getProperty("test.property.array"));
    assertEquals("Properties set through the CM should take precedence before those set from file", "setFromCM", ref
        .getProperty("test.property.value"));
    assertEquals("Properties overriden from later ones in definition should take precedence", "setFromDefinition", ref
        .getProperty("test.property.name"));
    assertEquals("Properties set through the CM should take precedence before those set from definition", "setFromCM",
        ref.getProperty("test.property.list"));
    assertEquals("Properties not overriden from later ones should not be lost", "setFromDefinition", ref
        .getProperty("test.property.cont"));
    assertEquals("Must not allow overriding the component.name property", SAC_CLASS, ref.getProperty("component.name"));
    assertNotNull("component.id property should be present", ref.getProperty(ComponentConstants.COMPONENT_ID));
    assertTrue("Must not allow overriding the component.id property", ((Long) ref.getProperty("component.id"))
        .longValue() > 0);

    Bundle bundle = installBundle("tb4");
    bundle.start();
    waitBundleStart();

    Configuration c = cm.getConfiguration(NAMED_CLASS, null);
    assertNotNull("The Configuration should be created properly", c);
    Hashtable cmProps = new Hashtable();
    cmProps.put("override.property.3", "setFromCM");
    c.update(cmProps);

    // let the config update reach the SCR
    Thread.sleep(timeout * 2);

    ComponentFactory factory = (ComponentFactory) trackerNamedServiceFactory.getService();
    assertNotNull("The NamedService ComponentFactory should be available", factory);

    Hashtable newProps = new Hashtable();
    newProps.put("override.property.1", "setFromMethod");
    newProps.put("override.property.2", "setFromMethod");
    newProps.put("override.property.3", "setFromMethod");
    newProps.put(ComponentConstants.COMPONENT_NAME, "setFromMethod");
    newProps.put(ComponentConstants.COMPONENT_ID, Long.valueOf(-1));
    newProps.put("name", "test");

    List<ComponentInstance> cis = new ArrayList<ComponentInstance>();
    ComponentInstance ci = factory.newInstance(newProps);
    assertNotNull("newInstance() method shouldn't return null", ci);
    cis.add(ci);
    ci = factory.newInstance(newProps);
    assertNotNull("newInstance() method shouldn't return null", ci);
    cis.add(ci);
    ci = factory.newInstance(newProps);
    assertNotNull("newInstance() method shouldn't return null", ci);
    cis.add(ci);

    ServiceReference[] refs = trackerNamedService.getServiceReferences();
    boolean serviceFound = false;
    for (int i = 0; refs != null && i < refs.length; i++) {
      ServiceReference current = refs[i];
      if ("test".equals(current.getProperty("name"))) {
        serviceFound = true;
        assertEquals("Properties set through newInstance method must override those from definition", "setFromMethod",
            current.getProperty("override.property.1"));
        assertEquals("Properties set through newInstance method must override those from file", "setFromMethod",
            current.getProperty("override.property.2"));
        assertEquals("Properties set through newInstance method must override those from ConfigurationAdmin",
            "setFromMethod", current.getProperty("override.property.3"));
        assertEquals("Must not override " + ComponentConstants.COMPONENT_NAME, current
            .getProperty(ComponentConstants.COMPONENT_NAME), NAMED_CLASS);
        assertTrue("Must not override " + ComponentConstants.COMPONENT_ID, ((Long) current
            .getProperty(ComponentConstants.COMPONENT_ID)).longValue() > 0);
      }
    }
    assertTrue("Must have found service", serviceFound);

    for (ComponentInstance i :cis) {
    	i.dispose();
    }
    c.delete();
    //bundle.stop();

    factory = (ComponentFactory) trackerNamedServiceFactory.getService();
    ci = factory.newInstance(newProps);
    assertNotNull("newInstance() method shouldn't return null", ci);

    // test the conflict between factory and factoryPID
    c = cm.createFactoryConfiguration(NAMED_CLASS, null);
    assertNotNull("CM should not return null Configuration from createFactoryConfiguration()", c);
    c.update(cmProps);
    Thread.sleep(timeout);

    bundle.start();
    waitBundleStart();

 // TODO Equinox DS behaves differently than Felix SCR here. The specification is vague
 // Equinox DS will disable the component factory in this error case
 // Felix will keep the component factory enabled and ignore the CM factory configuration
    assertNotNull("The named service ComponentFactory should be available even when there is factory configuration for it",
        trackerNamedServiceFactory.getService());

    c.delete();
    Thread.sleep(timeout * 2);

    // create factory configs for Worker
    Configuration scConfig1 = cm.createFactoryConfiguration(SC_CLASS, null);
    Hashtable scProps1 = new Hashtable();
    scProps1.put("name", "instance1");
    scConfig1.update(scProps1);

    Configuration scConfig2 = cm.createFactoryConfiguration(SC_CLASS, null);
    Hashtable scProps2 = new Hashtable();
    scProps2.put("name", "instance2");
    scConfig2.update(scProps2);

    Thread.sleep(timeout * 2);

    try {// test factory configuration for normal component
      assertEquals("The Worker should have two instances", 2, countAvailableServices(trackerSC));
    } finally {
      scConfig1.delete();
      scConfig2.delete();
    }
    Thread.sleep(timeout * 2);

    assertEquals("The Worker should have one instance", 1, countAvailableServices(trackerSC));
    ServiceReference scRef = trackerSC.getServiceReference();
    assertNull("The Worker only instance shouldn't have \"name\" property", scRef.getProperty("name"));

    uninstallBundle(bundle);
  }

  @Test
  public void testBoundServiceReplacement() throws Exception {
    int beforeCount, afterCount;
    Hashtable mandatoryProperty = new Hashtable();
    mandatoryProperty.put("mandatory.property", "true");

    Bundle tb4 = installBundle("tb4");
    tb4.start();
    waitBundleStart();
    assertEquals("tb4.jar should be ACTIVE", Bundle.ACTIVE, tb4.getState());

    ComponentFactory namedFactory = (ComponentFactory) trackerNamedServiceFactory.getService();
    assertNotNull("NamedService component factory should be available", namedFactory);
    ComponentFactory dynFactory = (ComponentFactory) trackerDynServiceFactory.getService();
    assertNotNull("DynamicWorker component factory should be available", dynFactory);

    // create the mandatory elements
    ComponentInstance namedServiceInstance = namedFactory.newInstance((Dictionary) mandatoryProperty.clone());
    assertNotNull("NamedService component instance should not be null", namedServiceInstance);
    Object namedService = namedServiceInstance.getInstance();
    assertNotNull("NamedService should be created properly", namedService);
    ComponentInstance dynServiceInstance = dynFactory.newInstance((Dictionary) mandatoryProperty.clone());
    assertNotNull("DynamicWorker component instance should not be null", dynServiceInstance);
    Object dynService = dynServiceInstance.getInstance();
    assertNotNull("DynamicWorker should be created properly", dynService);

    Object bsrc = trackerBSRC.getService();
    assertNotNull("BoundReplacer should be available", bsrc);
    assertSame("NamedService bound should be our first instance", ((BoundMainProvider) bsrc)
        .getBoundService(BoundMainProvider.NAMED_SERVICE), namedService);
    assertSame("DynamicWorker bound should be our first instance", ((BoundMainProvider) bsrc)
        .getBoundService(BoundMainProvider.DYNAMIC_SERVICE), dynService);

    // provide second dynamic service
    ComponentInstance dynServiceInstance2 = dynFactory.newInstance((Dictionary) mandatoryProperty.clone());
    assertNotNull("Second DynamicWorker component instance should not be null", dynServiceInstance2);
    Object dynService2 = dynServiceInstance2.getInstance();
    assertNotNull("Second DynamicWorker instance should be available", dynService2);

    // reset the events
    ((DSEventsProvider) bsrc).resetEvents();
    // destroy the first instance of dynamic service
    dynServiceInstance.dispose();

    // check that service is replaced
    assertNotSame("The bound dynamic service shouldn't be our first instance", ((BoundMainProvider) bsrc)
        .getBoundService(BoundMainProvider.DYNAMIC_SERVICE), dynService);
    assertSame("The bound dynamic service should be our second instance", ((BoundMainProvider) bsrc)
        .getBoundService(BoundMainProvider.DYNAMIC_SERVICE), dynService2);

    // check the correct order of replacing
    DSEvent[] replacedBoundDynamicServicesEvents = ((DSEventsProvider) bsrc).getEvents();
    assertEquals("There should two events after we have disposed the bound service", 2,
        replacedBoundDynamicServicesEvents.length);
    assertEquals("The first event should be bind event", DSEvent.ACT_BOUND, replacedBoundDynamicServicesEvents[0]
        .getAction());
    assertSame("The first event should have associated object the second instance", dynService2,
        replacedBoundDynamicServicesEvents[0].getObject());

    assertEquals("The second event should be unbind event", DSEvent.ACT_UNBOUND, replacedBoundDynamicServicesEvents[1]
        .getAction());
    assertSame("The second event should have associated object the first instance", dynService,
        replacedBoundDynamicServicesEvents[1].getObject());

    // destroy and the second service
    dynServiceInstance2.dispose();

    // check that the inspected service is deactivated
    assertNull("The BoundReplacer should not be available as the destroyed service hasn't replacement", trackerBSRC
        .getService());

    // restore the BSRC
    assertNotNull("The DynamicWorker component instance should be created properly", dynFactory
        .newInstance((Dictionary) mandatoryProperty.clone()));

    Object bsrcObject = trackerBSRC.getService();
    assertNotNull("The BoundReplacer should be available again", bsrcObject);
    ComponentContext bsrcCtxt1 = ((ComponentContextProvider) bsrcObject).getComponentContext();
    assertNotNull("The BoundReplacer should be activated and ComponentContext available", bsrcCtxt1);

    // prepare second static service instance
    ComponentInstance namedServiceInstance2 = namedFactory.newInstance((Dictionary) mandatoryProperty.clone());
    assertNotNull("Second NamedService instance should be created properly", namedServiceInstance2);
    Object namedService2 = namedServiceInstance2.getInstance();
    assertNotNull("Second NamedService instance should be created properly", namedService2);

    // destroy the first instance
    beforeCount = countAvailableServices(trackerNamedService);
    namedServiceInstance.dispose();
    afterCount = countAvailableServices(trackerNamedService);
    assertEquals("The NamedService instance should be removed from the registry", beforeCount - 1, afterCount);

    // check that the BSRC has been reactivated
    Object bsrcObject2 = trackerBSRC.getService(); // the BSRC object can be
    // recreated
    assertNotNull("The BoundReplacer should not be null", bsrcObject2);
    ComponentContext bsrcCtxt2 = ((ComponentContextProvider) bsrcObject2).getComponentContext();
    assertNotNull("The second ComponentContext should not be null", bsrcCtxt2);
    assertNotSame("The second ComponentContext should be different than the first one", bsrcCtxt1, bsrcCtxt2);

    // destroy the second instance
    namedServiceInstance2.dispose();

    assertNull("The BSRC should be disabled", trackerBSRC.getService());

    uninstallBundle(tb4);
  }

  /**
   * Returns the number of available services for the passed tracker
   * 
   * @param tracker
   * @return
   */
  private int countAvailableServices(ServiceTracker tracker) {
    if (tracker == null)
      return -1;
    ServiceReference[] refs = tracker.getServiceReferences();
    return refs != null ? refs.length : 0;
  }

  @Test
  public void testBoundServiceReplacementOnModification() throws Exception {
    BundleContext bc = getContext();
    Hashtable initialProps = new Hashtable();
    Hashtable modifiedProps = new Hashtable();
    initialProps.put("mandatory.property", "true");
    modifiedProps.put("mandatory.property", "false");

    ServiceRegistration dynRegistration1 = registerService(DynamicWorker.class.getName(), new DynamicWorker(),
        (Dictionary) initialProps.clone());

    ServiceRegistration staticRegistration1 = registerService(StaticWorker.class.getName(), new StaticWorker(),
        (Dictionary) initialProps.clone());

    Bundle tb4 = installBundle("tb4");
    tb4.start();
    waitBundleStart();
    assertEquals("tb4.jar should be ACTIVE", Bundle.ACTIVE, tb4.getState());

    // assure the MBSRC is available
    assertTrue("The AdvancedBounder must be available", countAvailableServices(trackerMBSRC) > 0);
    Object bsrc = trackerMBSRC.getService();
    assertNotNull("MBSRC isntance should be not null", bsrc);

    // register the second instances
    ServiceRegistration dynRegistration2 = registerService(DynamicWorker.class.getName(), new DynamicWorker(),
        (Dictionary) initialProps.clone());

    // reset the bound services events
    ((DSEventsProvider) bsrc).resetEvents();
    // change the first instance of dynamic service
    dynRegistration1.setProperties(modifiedProps);

    Object instance1 = bc.getService(dynRegistration1.getReference());
    Object instance2 = bc.getService(dynRegistration2.getReference());
    try {
      // check that service is replaced
      assertNotSame("The bound dynamic service shouldn't be our first instance", ((BoundMainProvider) bsrc)
          .getBoundService(BoundMainProvider.DYNAMIC_SERVICE), instance1);
      assertSame("The bound dynamic service should be our second instance", ((BoundMainProvider) bsrc)
          .getBoundService(BoundMainProvider.DYNAMIC_SERVICE), instance2);

      // check the correct order of replacing
      DSEvent[] replacedBoundDynamicServicesEvents = ((DSEventsProvider) bsrc).getEvents();
      assertEquals("There should two events after we have disposed the bound service", 2,
          replacedBoundDynamicServicesEvents.length);

      assertEquals("The first event should be bind event", DSEvent.ACT_BOUND, replacedBoundDynamicServicesEvents[0]
          .getAction());
      assertSame("The first event should have associated object the second instance", instance2,
          replacedBoundDynamicServicesEvents[0].getObject());

      assertEquals("The second event should be unbind event", DSEvent.ACT_UNBOUND,
          replacedBoundDynamicServicesEvents[1].getAction());
      assertSame("The second event should have associated object the first instance", instance1,
          replacedBoundDynamicServicesEvents[1].getObject());

    } finally {
      bc.ungetService(dynRegistration1.getReference());
      bc.ungetService(dynRegistration2.getReference());
    }
    instance1 = instance2 = null;

    ComponentContext bsrcCtxt1 = ((ComponentContextProvider) bsrc).getComponentContext();
    assertNotNull("ComponentContext object should be available", bsrcCtxt1);

    ServiceRegistration staticRegistration2 = registerService(StaticWorker.class.getName(), new StaticWorker(),
        (Dictionary) initialProps.clone());
    // change the first instance
    staticRegistration1.setProperties((Dictionary) modifiedProps.clone());

    Object bsrcObject2 = trackerMBSRC.getService(); // the BSRC object can be
    // recreated
    assertNotNull("The BoundReplacer should not be null", bsrcObject2);
    ComponentContext bsrcCtxt2 = ((ComponentContextProvider) bsrcObject2).getComponentContext();
    assertNotNull("The second ComponentContext should not be null", bsrcCtxt2);
    assertNotSame("The second ComponentContext should be different than the first one", bsrcCtxt1, bsrcCtxt2);
    
    //test the modification of static service reference which is not bound
    unregisterService(staticRegistration1);
    unregisterService(staticRegistration2);
    assertTrue("The AdvancedBounder must not be available", countAvailableServices(trackerMBSRC) == 0);
    staticRegistration1 = registerService(StaticWorker.class.getName(), new StaticWorker(),
        (Dictionary) initialProps.clone());    
    // assure the MBSRC is available
    assertTrue("The AdvancedBounder must be available", countAvailableServices(trackerMBSRC) > 0);
    staticRegistration2 = registerService(StaticWorker.class.getName(), new StaticWorker(),
        (Dictionary) initialProps.clone());
    bsrcCtxt2 = ((ComponentContextProvider)trackerMBSRC.getService()).getComponentContext();
    //modify the service which is not bound
    staticRegistration2.setProperties((Dictionary) modifiedProps.clone());
    bsrcCtxt1 = ((ComponentContextProvider)trackerMBSRC.getService()).getComponentContext();
    //check if the component is reactivated when a service is modified but it is not bound to the component
    assertEquals("The component context must not be changed", bsrcCtxt2, bsrcCtxt1);
    
    // test the modification of static service reference which is bound. 
    // The service reference does still satisfy the component reference after the
    // modification
    unregisterService(staticRegistration1);
    unregisterService(staticRegistration2);
    assertTrue("The AdvancedBounder must not be available", countAvailableServices(trackerMBSRC) == 0);
    staticRegistration1 = registerService(StaticWorker.class.getName(), new StaticWorker(),
        (Dictionary) initialProps.clone());    
    // assure the MBSRC is available
    assertTrue("The AdvancedBounder must be available", countAvailableServices(trackerMBSRC) > 0);
    bsrcCtxt1 = ((ComponentContextProvider)trackerMBSRC.getService()).getComponentContext();
    //modify the service which is bound
    Hashtable modified = (Hashtable) initialProps.clone();
    modified.put("a", "a");
    staticRegistration1.setProperties(modified);
    bsrcCtxt2 = ((ComponentContextProvider)trackerMBSRC.getService()).getComponentContext();
    //check if the component is reactivated when a bound service is modified but still satisfies the component reference
    assertEquals("The component context must not be changed", bsrcCtxt1, bsrcCtxt2);
    
    // test the modification of dynamic service reference which is bound. 
    // The service reference does still satisfy the component reference after the
    // modification
    unregisterService(dynRegistration1);
    unregisterService(dynRegistration2);
    assertTrue("The AdvancedBounder must not be available", countAvailableServices(trackerMBSRC) == 0);
    dynRegistration1 = registerService(DynamicWorker.class.getName(), new DynamicWorker(),
        (Dictionary) initialProps.clone());    
    // assure the MBSRC is available
    assertTrue("The AdvancedBounder must be available", countAvailableServices(trackerMBSRC) > 0);
    bsrc = trackerMBSRC.getService();
    // reset the bound services events
    ((DSEventsProvider) bsrc).resetEvents();
    //modify the service which is bound
    modified = (Hashtable) initialProps.clone();
    modified.put("a", "a");
    dynRegistration1.setProperties(modified);
    assertEquals(
        "There must be no unbind/bind activity when modifying reference which still satsfies the component reference",
        0, ((DSEventsProvider) bsrc).getEvents().length);

    uninstallBundle(tb4);

    unregisterService(dynRegistration1);
    unregisterService(dynRegistration2);
    unregisterService(staticRegistration1);
    unregisterService(staticRegistration2);
  }

  @Test
  public void testSecurity() throws Exception {
    // the method below sets the permissions of a bundle before installing it
    // to simplify the test case
    if (System.getSecurityManager() == null) {
      // the security is off
      return;
    }
    BundleContext bc = getContext();
    ServiceReference padmRef = bc.getServiceReference(PermissionAdmin.class.getName());
    assertNotNull("Permission Admin service not available.", padmRef);

    PermissionAdmin padm = (PermissionAdmin) bc.getService(padmRef);
    assertNotNull("Permission Admin service should be available", padm);

    assertEquals("TestBundle1 must be running.", Bundle.ACTIVE, tb1.getState());

    PermissionInfo registerServiceInfo = new PermissionInfo(ServicePermission.class.getName(),
        "org.eclipse.equinox.ds.tests.tb5.impl.SecurityTester", ServicePermission.REGISTER);
    PermissionInfo getServiceInfo = new PermissionInfo(ServicePermission.class.getName(),
        "org.eclipse.equinox.ds.tests.tb1.impl.AnotherComponent", ServicePermission.GET);

    PermissionInfo importPackage = new PermissionInfo(PackagePermission.class.getName(),
        "org.eclipse.equinox.ds.tests.tb1.impl", PackagePermission.IMPORT);

    // install the bundle to get the location
    Bundle tb5 = installBundle("tb5");
    tb5.start();
    waitBundleStart();
    final String bundleLocation = tb5.getLocation();

    uninstallBundle(tb5);

    // do the test

    // set all permission needed for correct operation
    padm.setPermissions(bundleLocation, new PermissionInfo[] { registerServiceInfo, getServiceInfo, importPackage });

    // install
    tb5 = installBundle("tb5");
    tb5.start();
    waitBundleStart();
    assertEquals("The bundle location should be the same as the first one registered", bundleLocation, tb5
        .getLocation());

    // check that the component is available
    assertTrue("The SecurityTester should be present because all needed permissions are set",
        countAvailableServices(trackerSecurity) > 0);

    // uninstall
    uninstallBundle(tb5);

    // remove the register permission - the service shouldn't be available
    padm.setPermissions(bundleLocation, new PermissionInfo[] { importPackage, getServiceInfo });

    // install
    tb5 = installBundle("tb5");
    tb5.start();
    waitBundleStart();
    assertEquals("The bundle location should be the same as the first one registered", bundleLocation, tb5
        .getLocation());

    // check that the service is unavailable
    assertEquals("The SecurityTester shouldn't be present due to missing ServicePermission.REGISTER", 0,
        countAvailableServices(trackerSecurity));

    // uninstall
    uninstallBundle(tb5);

    // remove the get permission and bring back the register permission
    padm.setPermissions(bundleLocation, new PermissionInfo[] { importPackage, registerServiceInfo });

    // install
    tb5 = installBundle("tb5");
    tb5.start();
    waitBundleStart();
    assertEquals("The bundle location should be the same as the first one registered", bundleLocation, tb5
        .getLocation());

    // check that the component is unavailable
    assertEquals("The SecurityTester shouldn't be present due to missing ServicePermission.GET", 0,
        countAvailableServices(trackerSecurity));

    // uninstall
    uninstallBundle(tb5);

    // reset the permissions
    padm.setPermissions(bundleLocation, null);
    // release the PermissionAdmin service
    bc.ungetService(padmRef);
    padm = null;
  }

  @Test
  public void testImmediateComponents() throws Exception {
    Bundle tb4 = installBundle("tb4");
    tb4.start();
    waitBundleStart();

    // check that the ServiceProvider is registered
    assertNotNull("The ServiceProvider should be registered as service", getServiceReference(
        "org.eclipse.equinox.ds.tests.tb4.ServiceProvider"));
    // check that the ServiceProvider is activated
    assertTrue("The ServiceProvider should be activated", TestHelper.isActivatedServiceProvider());

    // check that the Stand Alone Component is activated
    assertTrue("The AnotherComponent should be activated", TestHelper.isActivatedStandAlone());

    uninstallBundle(tb4);
  }

  @Test
  public void testRowReference() throws Exception {
    final String TAIL_CLASS = "org.eclipse.equinox.ds.tests.tb4.Component3";
    final String MIDDLE_CLASS = "org.eclipse.equinox.ds.tests.tb4.Component2";
    final String HEAD_CLASS = "org.eclipse.equinox.ds.tests.tb4.Component1";

    Bundle tb4 = installBundle("tb4");
    tb4.start();
    waitBundleStart();

    // check that all the components are present
    assertTrue("The Component3 should be available", checkAvailability(TAIL_CLASS));
    assertTrue("The Component2 should be available", checkAvailability(MIDDLE_CLASS));
    assertTrue("The Component1 should be available", checkAvailability(HEAD_CLASS));

    BundleContext bc = getContext();
    // get ComponentContext
    ServiceReference cceRef = bc.getServiceReference("org.eclipse.equinox.ds.tests.tbc.ComponentContextProvider");
    assertNotNull("The GiveMeContext should be available", cceRef);
    assertEquals("The GiveMeContext should be the implementation present in tb4.jar",
        "org.eclipse.equinox.ds.tests.tb4.GiveMeContext", cceRef.getProperty("component.name"));

    ComponentContextProvider cce = (ComponentContextProvider) bc.getService(cceRef);
    assertNotNull("The service object should be retrieved correctly", cce);
    ComponentContext ctxt = cce.getComponentContext();
    assertNotNull("The ComponentContext object should not be null", ctxt);

    // disable the tail component
    ctxt.disableComponent(TAIL_CLASS);
    Thread.sleep(timeout);

    // check that no component is available
    assertTrue("The Component3 shouldn't be available", !checkAvailability(TAIL_CLASS));
    assertTrue("The Component2 shouldn't be available", !checkAvailability(MIDDLE_CLASS));
    assertTrue("The Component1 shouldn't be available", !checkAvailability(HEAD_CLASS));

    // enable the tail component
    ctxt.enableComponent(TAIL_CLASS);
    Thread.sleep(timeout);

    // check that the components are back online
    assertTrue("The Component3 should be available", checkAvailability(TAIL_CLASS));
    assertTrue("The Component2 should be available", checkAvailability(MIDDLE_CLASS));
    assertTrue("The Component1 should be available", checkAvailability(HEAD_CLASS));

    // release the GiveMeContext
    bc.ungetService(cceRef);

    // remove the bundle
    uninstallBundle(tb4);
  }

  private boolean checkAvailability(String service) {
    ServiceReference ref = getServiceReference(service);
    return ref != null;
  }

  private ServiceReference getServiceReference(String service) {
    BundleContext bc = getContext();
    try {
		ServiceReference[] refs = bc.getAllServiceReferences(service, null);
		return refs == null ? null : refs[0];
	} catch (InvalidSyntaxException e) {
		return null;
	}
  }

  private boolean checkFactoryAvailability(String factory) throws InvalidSyntaxException {
    BundleContext bc = getContext();
    ServiceReference[] refs = bc.getServiceReferences(ComponentFactory.class.getName(), "("
        + ComponentConstants.COMPONENT_FACTORY + "=" + factory + ")");
    return refs != null && refs.length > 0;
  }

  @Test
  public void testBlockingComponents() throws Exception {
    final Bundle tb2 = installBundle("tb2");
    final Bundle tb3 = installBundle("tb3");
    final Bundle tb4 = installBundle("tb4");

    new Thread() {
      @Override
      public void run() {
        try {
          tb2.start(); // start the blocking service
        } catch (BundleException e) {
        }
      }
    }.start();
    sleep0(scr_restart_timeout + timeout * 2);

    new Thread() {
      @Override
      public void run() {
        try {
          tb4.start(); // start the other
        } catch (BundleException e) {
        }
      }
    }.start();

    sleep0(timeout * 2); // sleep until the services are activated

    // check that the first service is missing, and the second is available
    assertEquals("The blocking service should not be available", 0, countAvailableServices(trackerBAS));
    assertTrue("The service in the bundle should be available",
        checkAvailability("org.eclipse.equinox.ds.tests.tbc.ComponentContextProvider"));

    tb2.stop();
    tb4.stop();

    // check that AnotherComponent is available
    assertTrue("The AnotherComponent should be available",
        checkAvailability("org.eclipse.equinox.ds.tests.tb1.impl.AnotherComponent"));

    // start the other blocking bundle
    new Thread() {
      @Override
      public void run() {
        try {
          tb3.start();
        } catch (BundleException e) {
        }
      }
    }.start();

    sleep0(scr_restart_timeout + timeout * 2);

    // start the non-blocking bundle
    new Thread() {
      @Override
      public void run() {
        try {
          tb4.start(); // start the other
        } catch (BundleException e) {
        }
      }
    }.start();

    sleep0(timeout * 2); // sleep until the services are activated

    assertEquals("The blocking service should not be available", 0, countAvailableServices(trackerBBS));
    assertTrue("The service in the bundle should be available",
        checkAvailability("org.eclipse.equinox.ds.tests.tbc.ComponentContextProvider"));

    uninstallBundle(tb2);
    uninstallBundle(tb3);
    uninstallBundle(tb4);
  }

  @Test
  public void testStaticPolicyBinding() throws Exception {
    Bundle tb6 = installBundle("tb6");
    tb6.start();
    waitBundleStart();

    // check initial conditions
    assertTrue("The StaticComp should be available", checkAvailability(STATIC_CLASS));
    assertTrue("The ReferencedComp shouldn't be available (disabled)", !checkAvailability(REFERENCED_CLASS));

    // reset the events list
    Object initialStatic = trackerStatic.getService();
    assertNotNull(STATIC_CLASS + " component should be non-null", initialStatic);
    ComponentContext initialCtxt = ((ComponentContextProvider) initialStatic).getComponentContext();
    ((DSEventsProvider) initialStatic).resetEvents();
    assertEquals("There shouldn't be bound service to StaticComp", 0, ((BoundTester) initialStatic)
        .getBoundObjectsCount());

    // enable the ReferencedComp
    initialCtxt.enableComponent(REFERENCED_CLASS);
    Thread.sleep(timeout);

    // check the availability after enablement
    assertTrue("The StaticComp should be available", checkAvailability(STATIC_CLASS));
    assertTrue("The ReferencedComp should be available", checkAvailability(REFERENCED_CLASS));

    Object enabledStatic = trackerStatic.getService();
    assertNotNull(STATIC_CLASS + " component should be non-null", enabledStatic);
    ComponentContext enabledCtxt = ((ComponentContextProvider) enabledStatic).getComponentContext();
    assertSame("The StaticComp must not have been restarted", initialCtxt, enabledCtxt);
    assertEquals("There should be no bound service to StaticComp", 0, ((BoundTester) enabledStatic)
        .getBoundObjectsCount());

    // disable the referenced component
    enabledCtxt.disableComponent(REFERENCED_CLASS);
    Thread.sleep(timeout);

    // check the availability
    assertTrue("The StaticComp should be available", checkAvailability(STATIC_CLASS));
    assertTrue("The ReferencedComp shouldn't be available", !checkAvailability(REFERENCED_CLASS));

    // check that the SCR did not restarted the component with static reference
    Object staticRefComp = trackerStatic.getService();
    assertNotNull(STATIC_CLASS + " component should be non-null", staticRefComp);
    ComponentContext ctxt = ((ComponentContextProvider) staticRefComp).getComponentContext();
    assertSame("The StaticComp must not have been restarted", enabledCtxt, ctxt);
    assertEquals("There should be none bound service to StaticComp", 0, ((BoundTester) staticRefComp)
        .getBoundObjectsCount());

    uninstallBundle(tb6);
  }

  @Test
  public void testCircularityHandling() throws Exception {
    Bundle tb7 = installBundle("tb7");
    tb7.start();
    waitBundleStart();

    final String UNBREAKABLE = "org.eclipse.equinox.ds.tests.tb7.UnbreakableCircuit";
    final String DYN_BREAKABLE = "org.eclipse.equinox.ds.tests.tb7.DynamicCircuit";
    final String STATIC_BREAKABLE = "org.eclipse.equinox.ds.tests.tb7.StaticCircuit";

    // check that the unbreakable circuit isn't available
    assertTrue("The first service from the unbreakable circularity shouldn't be available",
        !checkAvailability(UNBREAKABLE + "1"));
    assertTrue("The second service from the unbreakable circularity shouldn't be available",
        !checkAvailability(UNBREAKABLE + "2"));

    // check that the breakable circuit with dynamic optional reference is
    // available
    assertTrue("The first service from the breakable circularity with dynamic optional reference should be available",
        checkAvailability(DYN_BREAKABLE + "1"));
    assertTrue("The second service from the breakable circularity with dynamic optional reference should be available",
        checkAvailability(DYN_BREAKABLE + "2"));

    
    // check that the breakable circuit with dynamic optional reference has
    // bound correctly
    ServiceReference dynBreak1Ref = getServiceReference(DYN_BREAKABLE + "1");
    Object dynBreak1 = getContext().getService(dynBreak1Ref);
    ServiceReference dynBreak2Ref = getServiceReference(DYN_BREAKABLE + "2");
    Object dynBreak2 = getContext().getService(dynBreak2Ref);
    sleep0(timeout * 2); // sleep to allow the delayed bind for the broken reference cycle to be done
    try {
    	assertNotNull("The DynamicCircuit1 component should be available", dynBreak1);
    	assertEquals("The DynamicCircuit2 component should have one bound object", 1, ((BoundTester) dynBreak2)
    			.getBoundObjectsCount());
    	assertNotNull("The DynamicCircuit2 component should have one non-null bound object", ((BoundTester) dynBreak2)
    			.getBoundService(0));
    } finally {
      getContext().ungetService(dynBreak2Ref);
    }
    // check that the breakable circuit with static optional reference isn't
    // available
    assertTrue("The first service from the breakable circularity with static optional reference should be available",
        checkAvailability(STATIC_BREAKABLE + "1"));
    assertTrue("The second service from the breakable circularity with static optional reference should be available",
        checkAvailability(STATIC_BREAKABLE + "2"));

    // check that the optional reference isn't satisfied
    ServiceReference staticBreak2Ref = getServiceReference(STATIC_BREAKABLE + "2");
    Object staticBreak2 = getContext().getService(staticBreak2Ref);
    try {
      assertEquals("The StaticCircuit2 component shouldn't have bound objects", 0, ((BoundTester) staticBreak2)
          .getBoundObjectsCount());
    } finally {
      getContext().ungetService(staticBreak2Ref);
    }

    // check that the worker hasn't been blocked
    Bundle tb5 = installBundle("tb5");
    tb5.start();
    waitBundleStart();

    // check that the AnotherComponent service is available
    assertTrue("The AnotherComponent should be available",
        checkAvailability("org.eclipse.equinox.ds.tests.tb1.impl.AnotherComponent"));
    assertTrue("The service in TB5 should be available which means that the working thread of the SCR isn't blocked",
        checkAvailability("org.eclipse.equinox.ds.tests.tb5.impl.SecurityTester"));

    uninstallBundle(tb5);
    uninstallBundle(tb7);
  }

  // tests namespace handling in xml component description parser
  // TODO Felix handles improper XML differently, not sure this is spec'ed behavior.
  // disable @Test
  public void testNamespaceHandling() throws Exception {
    Bundle tb8 = installBundle("tb8");
    tb8.start();
    waitBundleStart();

    // check the root component handling
    assertTrue("The root1 component should be available", isNSComponentAvailable(101));
    assertTrue("The root2 component should be available", isNSComponentAvailable(102));
    assertTrue("The root3 component should not be available", !isNSComponentAvailable(103));
    assertTrue("The root4 component should be available", isNSComponentAvailable(104));
    assertTrue("The root5 component should not be available", !isNSComponentAvailable(105));
    // check the non root component handling
    assertTrue("The nonroot1 component should not be available", !isNSComponentAvailable(111));
    assertTrue("The nonroot2 component should be available", isNSComponentAvailable(112));
    assertTrue("The nonroot3 component should not be available", !isNSComponentAvailable(113));
    assertTrue("The nonroot4 component should be available", isNSComponentAvailable(114));
    assertTrue("The nonroot5 component should not be available", !isNSComponentAvailable(115));
    assertTrue("The nonroot6 component should be available", isNSComponentAvailable(116));
    assertTrue("The nonroot7 component should not be available", !isNSComponentAvailable(117));
    assertTrue("The nonroot8 component should not be available", !isNSComponentAvailable(118));
    assertTrue("The nonroot9 component should not be available", !isNSComponentAvailable(119));
    assertTrue("The nonroot10 component should not be available", !isNSComponentAvailable(120));
    assertTrue("The nonroot11 component should be available", isNSComponentAvailable(121));
    assertTrue("The nonroot12 component should not be available", !isNSComponentAvailable(122));
    assertTrue("The nonroot13 component should not be available", !isNSComponentAvailable(123));
    assertTrue("The nonroot14 component should be available", isNSComponentAvailable(124));
    assertTrue("The nonroot15 component should be available", isNSComponentAvailable(125));
    assertTrue("The nonroot16 component should be available", isNSComponentAvailable(126));
    assertTrue("The nonroot17 component should be available", isNSComponentAvailable(127));

    uninstallBundle(tb8);
  }

  private boolean isNSComponentAvailable(int nsid) {
    Object[] services = trackerNS.getServices();
    if (services == null) {
      return false;
    }
    for (Object service : services) {
        if (service instanceof NamespaceProvider) {
           NamespaceProvider s = (NamespaceProvider) service;
            if (s.getComponentNSID() == nsid) {
                return true;
            }
        }
    }

    return false;
  }

  // tests wildcard handling in mf (e.g. Service-Component: OSGI-INF/*.xml)
  @Test
  public void testWildcardHandling() throws Exception {
    Bundle tb9 = installBundle("tb9");
    tb9.start();
    waitBundleStart();

    final String WILD = "org.eclipse.equinox.ds.tests.tb9.Wildcard";

    // check that the both components are available
    assertTrue("The first Wildcard component should be available", checkAvailability(WILD + "1"));
    assertTrue("The second Wildcard component should be available", checkAvailability(WILD + "2"));

    uninstallBundle(tb9);
  }

  @Test
  public void testDynamicComponentFactoryServiceBinding() throws Exception {
    Bundle tb10 = installBundle("tb10");
    assertNotNull("Failed to install test bundle tb10.jar", tb10);
    tb10.start();
    waitBundleStart();

    // assert that the required components are available
    assertTrue("The referenced simple component should be available", checkAvailability(SC_CLASS));
    assertTrue("The referenced factory component should be available", checkFactoryAvailability("CountHelperFactory"));
    assertTrue("The dependent dynamic factory component should be available", checkFactoryAvailability("CountFactory"));

    // retrieve the helper factory
    ComponentFactory helperFactory = (ComponentFactory) trackerBoundServiceCounterHelperFactory.getService();
    assertNotNull("The helper factory must be retrievable", helperFactory);

    // retrieve the observed factory
    ComponentFactory observedFactory = (ComponentFactory) trackerBoundServiceCounterFactory.getService();
    assertNotNull("The observed factory must be retrievable", observedFactory);

    // instantiate observed components
    ComponentInstance observedInstance1 = observedFactory.newInstance(null);
    assertNotNull("Cannot instantiate component from observed factory [1]", observedInstance1);
    ComponentInstance observedInstance2 = observedFactory.newInstance(null);
    assertNotNull("Cannot instantiate component from observed factory [2]", observedInstance2);

    BoundCountProvider observedComponent1 = (BoundCountProvider) observedInstance1.getInstance();
    BoundCountProvider observedComponent2 = (BoundCountProvider) observedInstance2.getInstance();
    assertNotNull("The observed component must be non-null [1]", observedComponent1);
    assertNotNull("The observed component must be non-null [2]", observedComponent2);

    // check the bound services before instantiating helper factory components -
    // both components must have 1 service bound
    assertEquals("The bound service count must be 1 - only the simple component is available [1]", 1,
        observedComponent1.getBoundServiceCount(null));
    assertEquals("The bound service count must be 1 - only the simple component is available [2]", 1,
        observedComponent2.getBoundServiceCount(null));

    // instantiate three helper components and check the service count again
    for (int i = 0; i < 3; i++) {
      helperFactory.newInstance(null); // don't keep track of the created
      // instances, they will be disposed when
      // the bundle is stopped and uninstalled
    }

    // check the bound services count again - both components must have 4
    // services bound -> 1 simple component and 3 helper factory
    assertEquals("The bound service count must be 4 - 1 simple component, 3 helper factory components [1]", 4,
        observedComponent1.getBoundServiceCount(null));
    assertEquals("The bound service count must be 4 - 1 simple component, 3 helper factory components [2]", 4,
        observedComponent2.getBoundServiceCount(null));

    uninstallBundle(tb10);
  }

  @Test
  public void testStaticComponentFactoryServiceBinding() throws Exception {
    Bundle tb10 = installBundle("tb10");
    assertNotNull("Failed to install test bundle tb10.jar", tb10);
    tb10.start();
    waitBundleStart();

    // assert that the required components are available
    assertTrue("The referenced simple component should be available", checkAvailability(SC_CLASS));
    assertTrue("The referenced factory component should be available", checkFactoryAvailability("CountHelperFactory"));
    assertTrue("The dependent static factory component should be available",
        checkFactoryAvailability("StaticServiceCountFactory"));

    // retrieve the helper and observer factories
    ComponentFactory helperFactory = (ComponentFactory) trackerBoundServiceCounterHelperFactory.getService();
    ComponentFactory observedFactory = (ComponentFactory) trackerStaticServiceCounterFactory.getService();
    assertNotNull("The helper factory must be retrievable", helperFactory);
    assertNotNull("The observed factory must be retrievable", observedFactory);

    // instantiate observed components
    ComponentInstance observedInstance1 = observedFactory.newInstance(null);
    ComponentInstance observedInstance2 = observedFactory.newInstance(null);
    assertNotNull("Cannot instantiate component from observed factory [1]", observedInstance1);
    assertNotNull("Cannot instantiate component from observed factory [2]", observedInstance2);

    BoundCountProvider observedComponent1 = (BoundCountProvider) observedInstance1.getInstance();
    BoundCountProvider observedComponent2 = (BoundCountProvider) observedInstance2.getInstance();
    assertNotNull("The observed component must be non-null [1]", observedComponent1);
    assertNotNull("The observed component must be non-null [2]", observedComponent2);

    // check the bound services before instantiating helper factory components -
    // both components must have 1 service bound
    assertEquals("The bound service count must be 1 - only the simple component is available [1]", 1,
        observedComponent1.getBoundServiceCount(null));
    assertEquals("The bound service count must be 1 - only the simple component is available [2]", 1,
        observedComponent2.getBoundServiceCount(null));

    // instantiate three helper components and check the service count again
    for (int i = 0; i < 3; i++) {
      helperFactory.newInstance(null); // don't keep track of the created
      // instances, they will be disposed when
      // the bundle is stopped and uninstalled
    }

    // check whether the factory is the same, it shouldn't be disposed
    assertSame(observedFactory, trackerStaticServiceCounterFactory.getService());
    observedFactory = (ComponentFactory) trackerStaticServiceCounterFactory.getService();

    // check that the components are reinstantiated
    observedInstance1 = observedFactory.newInstance(null);
    observedInstance2 = observedFactory.newInstance(null);
    assertNotNull("Cannot instantiate new observed component instance [1]", observedInstance1);
    assertNotNull("Cannot instantiate new observed component instance [2]", observedInstance2);

    // get the new instances
    observedComponent1 = (BoundCountProvider) observedInstance1.getInstance();
    observedComponent2 = (BoundCountProvider) observedInstance2.getInstance();
    assertNotNull("The observed component instance must be non-null [1]", observedComponent1);
    assertNotNull("The observed component instance must be non-null [2]", observedComponent2);

    // check the bound services count again - both components must have 4
    // services bound -> 1 simple component and 3 helper factory
    assertEquals("The bound service count must be 4 - 1 simple component, 3 helper factory components [1]", 4,
        observedComponent1.getBoundServiceCount(null));
    assertEquals("The bound service count must be 4 - 1 simple component, 3 helper factory components [2]", 4,
        observedComponent2.getBoundServiceCount(null));

    uninstallBundle(tb10);
  }

  @Test
  public void testConfigurationPolicy() throws Exception {
    ConfigurationAdmin cm = (ConfigurationAdmin) trackerCM.getService();
    if (cm == null)
    	return;

    Bundle tb11 = installBundle("tb11");
    tb11.start();
    waitBundleStart();

    Hashtable props = new Hashtable(10);
    props.put("config.base.data", Integer.valueOf(1));

    // component notsetNS100 - property not set by Configuration Admin; XML NS
    // 1.0.0
    assertEquals("Configuration data should not be available for notsetNS100", 0, getBaseConfigData(COMP_NOTSET_100));
    // component notsetNS100 - property set by Configuration Admin; XML NS 1.0.0
    Configuration config = cm.getConfiguration(COMP_NOTSET_100, null);
    config.update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Configuration data should be available for notsetNS100 and equal to 1", 1,
        getBaseConfigData(COMP_NOTSET_100));

    // component notsetNS110 - property not set by Configuration Admin; XML NS
    // 1.1.0
    assertEquals("Configuration data should not be available for notsetNS110", 0, getBaseConfigData(COMP_NOTSET_110));
    // component notsetNS110 - property set by Configuration Admin; XML NS 1.1.0
    config = cm.getConfiguration(COMP_NOTSET_110, null);
    config.update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Configuration data should be available for notsetNS110 and equal to 1", 1,
        getBaseConfigData(COMP_NOTSET_110));

    // component optionalNS100 - property not set by Configuration Admin; XML NS
    // 1.0.0 - INVALID COMPONENT
    assertEquals("Component optionalNS100 should not be activated", -1, getBaseConfigData(COMP_OPTIONAL_100));
    // component optionalNS100 - property set by Configuration Admin; XML NS
    // 1.0.0 - INVALID COMPONENT
    config = cm.getConfiguration(COMP_OPTIONAL_100, null);
    config.update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Component optionalNS100 should not be activated", -1, getBaseConfigData(COMP_OPTIONAL_100));

    // component optionalNS110 - property not set by Configuration Admin; XML NS
    // 1.1.0
    assertEquals("Configuration data should not be available for optionalNS110", 0,
        getBaseConfigData(COMP_OPTIONAL_110));
    // component optionalNS110 - property set by Configuration Admin; XML NS
    // 1.1.0
    config = cm.getConfiguration(COMP_OPTIONAL_110, null);
    config.update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Configuration data should be available for optionalNS110 and equal to 1", 1,
        getBaseConfigData(COMP_OPTIONAL_110));

    // component requireNS100 - property not set by Configuration Admin; XML NS
    // 1.0.0 - INVALID COMPONENT
    assertEquals("Component requireNS100 should not be activated", -1, getBaseConfigData(COMP_REQUIRE_100));
    // component requireNS100 - property set by Configuration Admin; XML NS
    // 1.0.0 - INVALID COMPONENT
    config = cm.getConfiguration(COMP_REQUIRE_100, null);
    config.update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Component requireNS100 should not be activated", -1, getBaseConfigData(COMP_REQUIRE_100));

    // component requireNS110 - property not set by Configuration Admin; XML NS
    // 1.1.0
    assertEquals("Configuration data should not be available for requireNS110, and it should not be satisfied", -1,
        getBaseConfigData(COMP_REQUIRE_110));
    // component requireNS110 - property set by Configuration Admin; XML NS
    // 1.1.0
    config = cm.getConfiguration(COMP_REQUIRE_110, null);
    config.update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Configuration data should be available for requireNS110 and equal to 1", 1,
        getBaseConfigData(COMP_REQUIRE_110));

    // component ignoreNS100 - property not set by Configuration Admin; XML NS
    // 1.0.0 - INVALID COMPONENT
    assertEquals("Component ignoreNS100 should not be activated", -1, getBaseConfigData(COMP_IGNORE_100));
    // component ignoreNS100 - property set by Configuration Admin; XML NS 1.0.0
    // - INVALID COMPONENT
    config = cm.getConfiguration(COMP_IGNORE_100, null);
    config.update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Component ignoreNS100 should not be activated", -1, getBaseConfigData(COMP_IGNORE_100));

    // component ignoreNS110 - property not set by Configuration Admin; XML NS
    // 1.1.0
    assertEquals("Configuration data should not be available for ignoreNS110, but it should be satisfied", 0,
        getBaseConfigData(COMP_IGNORE_110));
    // component ignoreNS110 - property set by Configuration Admin; XML NS 1.1.0
    config = cm.getConfiguration(COMP_IGNORE_110, null);
    config.update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Configuration data should not be available for ignoreNS110, but it should be satisfied", 0,
        getBaseConfigData(COMP_IGNORE_110));

    uninstallBundle(tb11);
  }

  // tests configuration-policy for factory configuration objects
  @Test
  public void testConfigurationPolicyFactoryConf() throws Exception {
    ConfigurationAdmin cm = (ConfigurationAdmin) trackerCM.getService();
    if (cm == null)
    	return;

    Bundle tb11 = installBundle("tb11");
    tb11.start();
    waitBundleStart();

    Hashtable props = new Hashtable(10);
    props.put("config.base.data", Integer.valueOf(1));

    // component notsetNS100 - property not set by Configuration Admin; XML NS
    // 1.0.0
    assertEquals("Configuration data should not be available for notsetNS100", 0, getBaseConfigData(COMP_NOTSET_100));
    // component notsetNS100 - property set by Configuration Admin; XML NS 1.0.0
    Configuration config = cm.createFactoryConfiguration(COMP_NOTSET_100, null);
    config.update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Configuration data should be available for notsetNS100 and equal to 1", 1,
        getBaseConfigData(COMP_NOTSET_100));

    // component notsetNS110 - property not set by Configuration Admin; XML NS
    // 1.1.0
    assertEquals("Configuration data should not be available for notsetNS110", 0, getBaseConfigData(COMP_NOTSET_110));
    // component notsetNS110 - property set by Configuration Admin; XML NS 1.1.0
    config = cm.createFactoryConfiguration(COMP_NOTSET_110, null);
    config.update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Configuration data should be available for notsetNS110 and equal to 1", 1,
        getBaseConfigData(COMP_NOTSET_110));

    // component optionalNS100 - property not set by Configuration Admin; XML NS
    // 1.0.0 - INVALID COMPONENT
    assertEquals("Component optionalNS100 should not be activated", -1, getBaseConfigData(COMP_OPTIONAL_100));
    // component optionalNS100 - property set by Configuration Admin; XML NS
    // 1.0.0 - INVALID COMPONENT
    config = cm.createFactoryConfiguration(COMP_OPTIONAL_100, null);
    config.update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Component optionalNS100 should not be activated", -1, getBaseConfigData(COMP_OPTIONAL_100));

    // component optionalNS110 - property not set by Configuration Admin; XML NS
    // 1.1.0
    assertEquals("Configuration data should not be available for optionalNS110", 0,
        getBaseConfigData(COMP_OPTIONAL_110));
    // component optionalNS110 - property set by Configuration Admin; XML NS
    // 1.1.0
    config = cm.createFactoryConfiguration(COMP_OPTIONAL_110, null);
    config.update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Configuration data should be available for optionalNS110 and equal to 1", 1,
        getBaseConfigData(COMP_OPTIONAL_110));

    // component requireNS100 - property not set by Configuration Admin; XML NS
    // 1.0.0 - INVALID COMPONENT
    assertEquals("Component requireNS100 should not be activated", -1, getBaseConfigData(COMP_REQUIRE_100));
    // component requireNS100 - property set by Configuration Admin; XML NS
    // 1.0.0 - INVALID COMPONENT
    config = cm.createFactoryConfiguration(COMP_REQUIRE_100, null);
    config.update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Component requireNS100 should not be activated", -1, getBaseConfigData(COMP_REQUIRE_100));

    // component requireNS110 - property not set by Configuration Admin; XML NS
    // 1.1.0
    assertEquals("Configuration data should not be available for requireNS110, and it should not be satisfied", -1,
        getBaseConfigData(COMP_REQUIRE_110));
    // component requireNS110 - property set by Configuration Admin; XML NS
    // 1.1.0
    config = cm.createFactoryConfiguration(COMP_REQUIRE_110, null);
    config.update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Configuration data should be available for requireNS110 and equal to 1", 1,
        getBaseConfigData(COMP_REQUIRE_110));

    // component ignoreNS100 - property not set by Configuration Admin; XML NS
    // 1.0.0 - INVALID COMPONENT
    assertEquals("Component ignoreNS100 should not be activated", -1, getBaseConfigData(COMP_IGNORE_100));
    // component ignoreNS100 - property set by Configuration Admin; XML NS 1.0.0
    // - INVALID COMPONENT
    config = cm.createFactoryConfiguration(COMP_IGNORE_100, null);
    config.update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Component ignoreNS100 should not be activated", -1, getBaseConfigData(COMP_IGNORE_100));

    // component ignoreNS110 - property not set by Configuration Admin; XML NS
    // 1.1.0
    assertEquals("Configuration data should not be available for ignoreNS110, but it should be satisfied", 0,
        getBaseConfigData(COMP_IGNORE_110));
    // component ignoreNS110 - property set by Configuration Admin; XML NS 1.1.0
    config = cm.createFactoryConfiguration(COMP_IGNORE_110, null);
    config.update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Configuration data should not be available for ignoreNS110, but it should be satisfied", 0,
        getBaseConfigData(COMP_IGNORE_110));

    uninstallBundle(tb11);
  }

  @Test
  public void testActivateDeactivate() throws Exception {
    Bundle tb12 = installBundle("tb12");
    tb12.start();
    waitBundleStart();

    final String NOTSET_NS100 = "org.eclipse.equinox.ds.tests.tb12.notsetNS100";
    final String NOTSET_NS110 = "org.eclipse.equinox.ds.tests.tb12.notsetNS110";
    final String NOARGS_NS100 = "org.eclipse.equinox.ds.tests.tb12.NoArgs100";
    final String NOARGS_NS110 = "org.eclipse.equinox.ds.tests.tb12.NoArgs110";
    final String CC_NS100 = "org.eclipse.equinox.ds.tests.tb12.CcNS100";
    final String CC_NS110 = "org.eclipse.equinox.ds.tests.tb12.CcNS110";
    final String BC_NS100 = "org.eclipse.equinox.ds.tests.tb12.Bc100";
    final String BC_NS110 = "org.eclipse.equinox.ds.tests.tb12.Bc110";
    final String MAP_NS100 = "org.eclipse.equinox.ds.tests.tb12.MapNS100";
    final String MAP_NS110 = "org.eclipse.equinox.ds.tests.tb12.MapNS110";
    final String CC_BC_MAP_NS100 = "org.eclipse.equinox.ds.tests.tb12.CcBcMapNS100";
    final String CC_BC_MAP_NS110 = "org.eclipse.equinox.ds.tests.tb12.CcBcMapNS110";
    final String INT_NS110 = "org.eclipse.equinox.ds.tests.tb12.IntNS110";
    final String CC_BC_MAP_INT_NS110 = "org.eclipse.equinox.ds.tests.tb12.CcBcMapIntNS110";

    PropertiesProvider bs = getBaseService(NOTSET_NS100);
    ComponentContext cc = (bs instanceof ComponentContextProvider) ? ((ComponentContextProvider) bs)
        .getComponentContext() : null;
    assertNotNull("Component context should be available", cc);

    assertEquals("Activate method of " + NOTSET_NS100 + " should be called", 1 << 0, (1 << 0) & getBaseConfigData(bs));
    cc.disableComponent(NOTSET_NS100);
    Thread.sleep(timeout);
    assertEquals("Deactivate method of " + NOTSET_NS100 + " should be called", 1 << 1, (1 << 1) & getBaseConfigData(bs));

    bs = getBaseService(NOTSET_NS110);
    assertNotNull(bs);
    assertEquals("Activate method of " + NOTSET_NS110 + " should be called", 1 << 0, (1 << 0) & getBaseConfigData(bs));
    cc.disableComponent(NOTSET_NS110);
    Thread.sleep(timeout);
    assertEquals("Deactivate method of " + NOTSET_NS110 + " should be called", 1 << 1, (1 << 1) & getBaseConfigData(bs));

    bs = getBaseService(NOARGS_NS100); // INVALID COMPONENT FOR XML NS 1.0.0
    assertEquals("Component " + NOARGS_NS100 + " should not be activated", -1, getBaseConfigData(bs));

    bs = getBaseService(NOARGS_NS110);
    assertNotNull(bs);
    assertEquals("Activate method of " + NOARGS_NS110 + " should be called", 1 << 2, (1 << 2) & getBaseConfigData(bs));
    cc.disableComponent(NOARGS_NS110);
    Thread.sleep(timeout);
    assertEquals("Deactivate method of " + NOARGS_NS110 + " should be called", 1 << 3, (1 << 3) & getBaseConfigData(bs));

    bs = getBaseService(CC_NS100); // INVALID COMPONENT FOR XML NS 1.0.0
    assertEquals("Component " + CC_NS100 + " should not be activated", -1, getBaseConfigData(bs));

    bs = getBaseService(CC_NS110);
    assertNotNull(bs);
    assertEquals("Activate method of " + CC_NS110 + " should be called", 1 << 4, (1 << 4) & getBaseConfigData(bs));
    cc.disableComponent(CC_NS110);
    Thread.sleep(timeout);
    assertEquals("Deactivate method of " + CC_NS110 + " should be called", 1 << 5, (1 << 5) & getBaseConfigData(bs));

    bs = getBaseService(BC_NS100); // INVALID COMPONENT FOR XML NS 1.0.0
    assertEquals("Component " + BC_NS100 + " should not be activated", -1, getBaseConfigData(bs));

    bs = getBaseService(BC_NS110);
    assertNotNull(bs);
    assertEquals("Activate method of " + BC_NS110 + " should be called", 1 << 6, (1 << 6) & getBaseConfigData(bs));
    cc.disableComponent(BC_NS110);
    Thread.sleep(timeout);
    assertEquals("Deactivate method of " + BC_NS110 + " should be called", 1 << 7, (1 << 7) & getBaseConfigData(bs));

    bs = getBaseService(MAP_NS100); // INVALID COMPONENT FOR XML NS 1.0.0
    assertEquals("Component " + MAP_NS100 + " should not be activated", -1, getBaseConfigData(bs));

    bs = getBaseService(MAP_NS110);
    assertNotNull(bs);
    assertEquals("Activate method of " + MAP_NS110 + " should be called", 1 << 8, (1 << 8) & getBaseConfigData(bs));
    cc.disableComponent(MAP_NS110);
    Thread.sleep(timeout);
    assertEquals("Deactivate method of " + MAP_NS110 + " should be called", 1 << 9, (1 << 9) & getBaseConfigData(bs));

    bs = getBaseService(CC_BC_MAP_NS100); // INVALID COMPONENT FOR XML NS 1.0.0
    assertEquals("Component " + CC_BC_MAP_NS100 + " should not be activated", -1, getBaseConfigData(bs));

    bs = getBaseService(CC_BC_MAP_NS110);
    assertNotNull(bs);
    assertEquals("Activate method of " + CC_BC_MAP_NS110 + " should be called", 1 << 10, (1 << 10)
        & getBaseConfigData(bs));
    cc.disableComponent(CC_BC_MAP_NS110);
    Thread.sleep(timeout);
    assertEquals("Deactivate method of " + CC_BC_MAP_NS110 + " should be called", 1 << 11, (1 << 11)
        & getBaseConfigData(bs));

    bs = getBaseService(INT_NS110);
    assertNotNull(bs);
    cc.disableComponent(INT_NS110);
    Thread.sleep(timeout);
    assertEquals("Deactivate method of " + INT_NS110 + " should be called", 1 << 12, (1 << 12) & getBaseConfigData(bs));

    bs = getBaseService(CC_BC_MAP_INT_NS110);
    assertNotNull(bs);
    cc.disableComponent(CC_BC_MAP_INT_NS110);
    Thread.sleep(timeout);
    int data = getBaseConfigData(bs);
    assertEquals("Deactivate method of " + CC_BC_MAP_INT_NS110 + " should be called", 1 << 13, (1 << 13) & data);

    // // Testing Deactivation reasons ////
    assertEquals("Deactivation reason shall be DEACTIVATION_REASON_DISABLED", 1, 0xFF & (data >> 16));

    final String CONT_EXP = "org.eclipse.equinox.ds.tests.tb12.ContextExp";

    cc.enableComponent(CC_BC_MAP_INT_NS110);
    Thread.sleep(timeout);
    bs = getBaseService(CC_BC_MAP_INT_NS110);
    assertNotNull(bs);
    cc.disableComponent(CONT_EXP);
    Thread.sleep(timeout);
    assertEquals("Deactivation reason shall be DEACTIVATION_REASON_REFERENCE", 2, 0xFF & (getBaseConfigData(bs) >> 16));

    cc.enableComponent(CONT_EXP);
    Thread.sleep(timeout);

    bs = getBaseService(CC_BC_MAP_INT_NS110);
    assertNotNull(bs);
    ConfigurationAdmin cm = (ConfigurationAdmin) trackerCM.getService();
    if (cm == null)
    	return;
    Configuration config = cm.getConfiguration(CC_BC_MAP_INT_NS110, null);
    Dictionary properties = config.getProperties();
    if (properties == null) {
      properties = new Hashtable();
    }
    properties.put("configuration.dummy", "dummy");
    config.update(properties);
    Thread.sleep(timeout * 2);
    assertEquals("Deactivation reason shall be DEACTIVATION_REASON_CONFIGURATION_MODIFIED", 3,
        0xFF & (getBaseConfigData(bs) >> 16));

    cc.enableComponent(CC_BC_MAP_INT_NS110);
    Thread.sleep(timeout );

    bs = getBaseService(CC_BC_MAP_INT_NS110);
    assertNotNull(bs);
    config = cm.getConfiguration(CC_BC_MAP_INT_NS110, null);
    config.delete();
    Thread.sleep(timeout * 2);
    assertEquals("Deactivation reason shall be DEACTIVATION_REASON_CONFIGURATION_DELETED", 4,
        0xFF & (getBaseConfigData(bs) >> 16));

    cc.enableComponent(CC_BC_MAP_INT_NS110);
    Thread.sleep(timeout);

    cc.enableComponent(INT_NS110);
    Thread.sleep(timeout);

    bs = getBaseService(INT_NS110);
    assertNotNull(bs);
    ComponentContext ccIntNS110 = (bs instanceof ComponentContextProvider) ? ((ComponentContextProvider) bs)
        .getComponentContext() : null;
    assertNotNull("Component context should be available for " + INT_NS110, ccIntNS110);
    ccIntNS110.getComponentInstance().dispose();
    assertEquals("Deactivation reason shall be DEACTIVATION_REASON_DISPOSED", 5, 0xFF & (getBaseConfigData(bs) >> 16));

    bs = getBaseService(CC_BC_MAP_INT_NS110);
    assertNotNull(bs);
    tb12.stop();
    assertEquals("Deactivation reason shall be DEACTIVATION_REASON_BUNDLE_STOPPED", 6,
        0xFF & (getBaseConfigData(bs) >> 16));

    uninstallBundle(tb12);
  }

  @Test
  public void testBindUnbindParams() throws Exception {
    Bundle tb13 = installBundle("tb13");
    tb13.start();
    waitBundleStart();

    final String SR_NS100 = "org.eclipse.equinox.ds.tests.tb13.SrNS100";
    final String SR_NS110 = "org.eclipse.equinox.ds.tests.tb13.SrNS110";
    final String CE_NS100 = "org.eclipse.equinox.ds.tests.tb13.CeNS100";
    final String CE_NS110 = "org.eclipse.equinox.ds.tests.tb13.CeNS110";
    final String CE_MAP_NS100 = "org.eclipse.equinox.ds.tests.tb13.CeMapNS100";
    final String CE_MAP_NS110 = "org.eclipse.equinox.ds.tests.tb13.CeMapNS110";

    ServiceReference ref = getContext().getServiceReference(ComponentManager.class.getName());
    assertNotNull("Component Enabler Service Reference should be available", ref);
    ComponentManager enabler = (ComponentManager) getContext().getService(ref);
    assertNotNull("Component Enabler Service should be available", enabler);

    PropertiesProvider bs = getBaseService(SR_NS100);
    assertNotNull("Component " + SR_NS100 + " should be activated", bs);
    assertEquals("Bind method of " + SR_NS100 + " should be called", 1 << 0, (1 << 0) & getBaseConfigData(bs));
    enabler.enableComponent(SR_NS100, false);
    Thread.sleep(timeout);
    assertEquals("Unbind method of " + SR_NS100 + " should be called", 1 << 1, (1 << 1) & getBaseConfigData(bs));

    bs = getBaseService(SR_NS110);
    assertNotNull("Component " + SR_NS110 + " should be activated", bs);
    assertEquals("Bind method of " + SR_NS110 + " should be called", 1 << 0, (1 << 0) & getBaseConfigData(bs));
    enabler.enableComponent(SR_NS110, false);
    Thread.sleep(timeout);
    assertEquals("Unbind method of " + SR_NS110 + " should be called", 1 << 5, (1 << 5) & getBaseConfigData(bs));

    bs = getBaseService(CE_NS100);
    assertNotNull("Component " + CE_NS100 + " should be activated", bs);
    assertEquals("Bind method of " + CE_NS100 + " should be called", 1 << 2, (1 << 2) & getBaseConfigData(bs));
    enabler.enableComponent(CE_NS100, false);
    Thread.sleep(timeout);
    assertEquals("Unbind method of " + CE_NS100 + " should be called", 1 << 3, (1 << 3) & getBaseConfigData(bs));

    bs = getBaseService(CE_NS110);
    assertNotNull("Component " + CE_NS110 + " should be activated", bs);
    assertEquals("Bind method of " + CE_NS110 + " should be called", 1 << 2, (1 << 2) & getBaseConfigData(bs));
    enabler.enableComponent(CE_NS110, false);
    Thread.sleep(timeout);
    assertEquals("Unbind method of " + CE_NS110 + " should be called", 1 << 3, (1 << 3) & getBaseConfigData(bs));

    bs = getBaseService(CE_MAP_NS100);
    assertNotNull("Component " + CE_MAP_NS100 + " should be activated", bs);

    bs = getBaseService(CE_MAP_NS110);
    assertNotNull("Component " + CE_MAP_NS110 + " should be activated", bs);
    assertEquals("Bind method of " + CE_MAP_NS110 + " should be called", 1 << 4, (1 << 4) & getBaseConfigData(bs));
    enabler.enableComponent(CE_MAP_NS110, false);
    Thread.sleep(timeout);
    assertEquals("Unbind method of " + CE_MAP_NS110 + " should be called", 1 << 5, (1 << 5) & getBaseConfigData(bs));

    getContext().ungetService(ref);
    uninstallBundle(tb13);
  }

  @Test
  public void testOptionalNames() throws Exception {
    Bundle tb14 = installBundle("tb14");
    tb14.start();
    waitBundleStart();
    PropertiesProvider bs;

    final String OPT_NAME_100 = "org.eclipse.equinox.ds.tests.tb14.Optional";
    final String OPT_NAME_110 = "org.eclipse.equinox.ds.tests.tb14.Optional2";
    final String OPT_REF_100 = "org.eclipse.equinox.ds.tests.tb14.OptRef100";
    final String OPT_REF_110 = "org.eclipse.equinox.ds.tests.tb14.OptRef110";

    assertNull("Component " + OPT_NAME_100 + " should not be activated", getBaseService(OPT_NAME_100));
    assertNotNull("Component " + OPT_NAME_110 + " should be activated", getBaseService(OPT_NAME_110));

    assertNull("Component " + OPT_REF_100 + " should not be activated", getBaseService(OPT_REF_100));
    assertNotNull("Component " + OPT_REF_110 + " should be activated", bs = getBaseService(OPT_REF_110));

    ComponentContext cc = (bs instanceof ComponentContextProvider) ? ((ComponentContextProvider) bs)
        .getComponentContext() : null;
    assertNotNull("Component context should be available", cc);
    assertNotNull("Optional reference name should be set to interface attribute", cc
        .locateService(ComponentContextProvider.class.getName()));

    uninstallBundle(tb14);
  }

  @Test
  public void testDisposingMultipleDependencies() throws Exception {
    Bundle tb15 = installBundle("tb15");
    tb15.start();
    waitBundleStart();

    final String C1 = "org.eclipse.equinox.ds.tests.tb15.Component1";
    final String C2 = "org.eclipse.equinox.ds.tests.tb15.Component2";
    final String C3 = "org.eclipse.equinox.ds.tests.tb15.Component3";

    PropertiesProvider serviceC1 = getBaseService(C1);
    assertNotNull("Component " + C1 + " should be activated", serviceC1);
    PropertiesProvider serviceC2 = getBaseService(C2);
    assertNotNull("Component " + C2 + " should be activated", serviceC2);
    PropertiesProvider serviceC3 = getBaseService(C3);
    assertNotNull("Component " + C3 + " should be activated", serviceC3);

    ComponentContext cc = (serviceC1 instanceof ComponentContextProvider) ? ((ComponentContextProvider) serviceC1)
        .getComponentContext() : null;
    assertNotNull("Component context should be available", cc);

    cc.disableComponent(C1);
    Thread.sleep(timeout);

    assertEquals("Component " + C3 + " should be deactivated first", 0, getBaseConfigData(serviceC3));
    assertEquals("Component " + C2 + " should be deactivated second", 1, getBaseConfigData(serviceC2));
    assertEquals("Component " + C1 + " should be deactivated third", 2, getBaseConfigData(serviceC1));

    uninstallBundle(tb15);
  }

  @Test
  public void testReferenceTargetProperty() throws Exception {
    Bundle tb16 = installBundle("tb16");
    tb16.start();
    waitBundleStart();

    final String EXPOSER = "org.eclipse.equinox.ds.tests.tb16.Exposer";
    final String C1 = "org.eclipse.equinox.ds.tests.tb16.C1";
    final String C2 = "org.eclipse.equinox.ds.tests.tb16.C2";

    PropertiesProvider bs = getBaseService(EXPOSER);
    ComponentContext cc = (bs instanceof ComponentContextProvider) ? ((ComponentContextProvider) bs)
        .getComponentContext() : null;
    assertNotNull("Component context should be available", cc);

    PropertiesProvider serviceC2 = getBaseService(C2);
    // target property of referenced service of component Component2 should not
    // be satisfied
    assertNull("Component " + C2 + " should not be activated because of unsatisfied reference", serviceC2);

    cc.enableComponent(C1);
    Thread.sleep(timeout);
    assertNotNull("Component " + C1 + " should be available", getBaseService(C1));

    serviceC2 = getBaseService(C2);
    // target property of referenced service of component Component2 should now
    // be satisfied
    assertNotNull("Component " + C2 + " should be activated", serviceC2);

    uninstallBundle(tb16);
  }

  class OverloadManager extends Thread {
    private String compPrefix;
    private int firstComp;
    private int lastComp;

    public OverloadManager(String compPrefix, int first, int last) {
      this.compPrefix = compPrefix;
      this.firstComp = first;
      this.lastComp = last;
    }

    @Override
    public void run() {
      ConfigurationAdmin cm = (ConfigurationAdmin) trackerCM.getService();
      assertNotNull("The ConfigurationAdmin should be available", cm);

      try {
        for (int i = firstComp; i <= lastComp; i++) {
          Configuration config = cm.getConfiguration(compPrefix + i, null);
          Dictionary properties = config.getProperties();
          if (properties == null) {
            properties = new Hashtable();
          }
          properties.put("component.index", Integer.valueOf(i));
          config.update(properties);

          sleep0(100);
        }
      } catch (IOException e) {
        return;
      }
    }

    public boolean isAllComponentsRunning() {
      for (int i = firstComp; i <= lastComp; i++) {
        if (getBaseService(compPrefix + i) == null) {
          return false;
        }
      }
      return true;
    }
  }

  @Test
  public void testOverload() throws Exception {
    Bundle tb17 = installBundle("tb17");
    Bundle tb18 = installBundle("tb18");
    Bundle tb19 = installBundle("tb19");
    tb17.start();
    tb18.start();
    tb19.start();
    waitBundleStart();

    final String SCR = "org.eclipse.equinox.ds.tests";
    final int startComp = 1;
    final int lastComp = 10;
    final int OVERLOAD_TIMEOUT = 60000; // max time allowed for processing in ms

    OverloadManager manager17 = new OverloadManager(SCR + ".tb17.C", startComp, lastComp);
    OverloadManager manager18 = new OverloadManager(SCR + ".tb18.C", startComp, lastComp);
    OverloadManager manager19 = new OverloadManager(SCR + ".tb19.C", startComp, lastComp);

    long startTime = System.currentTimeMillis();
    manager17.start();
    manager18.start();
    manager19.start();

    manager17.join();
    manager18.join();
    manager19.join();

    // waiting SCR to process events
    int successCount = 0;
    while ((successCount < 5) && (System.currentTimeMillis() - startTime < OVERLOAD_TIMEOUT)) {
      Thread.sleep(100);
      if (!manager17.isAllComponentsRunning() || !manager18.isAllComponentsRunning()
          || !manager19.isAllComponentsRunning()) {
        successCount = 0;
        continue;
      }
      successCount++;
    }

    long elapsed = System.currentTimeMillis() - startTime;
    log("testOverload(): Overload processing finished for " + elapsed + " ms.");

    assertTrue("All components of tb17 should be activated", manager17.isAllComponentsRunning());
    assertTrue("All components of tb18 should be activated", manager18.isAllComponentsRunning());
    assertTrue("All components of tb19 should be activated", manager19.isAllComponentsRunning());

    uninstallBundle(tb17);
    uninstallBundle(tb18);
    uninstallBundle(tb19);
  }

  @Test
  public void testLazyBundles() throws Exception {
    Bundle tb20 = installBundle("tb20");
    // lazy bundle
    tb20.start(Bundle.START_ACTIVATION_POLICY);
    waitBundleStart();

    final String COMP = "org.eclipse.equinox.ds.tests.tb20.component";
    assertTrue("Provided service of Component " + COMP + " should be available.", trackerBaseService.size() > 0);
    uninstallBundle(tb20);
  }

  // Testing modified attribute for XML NS 1.0.0
  @Test
  public void testModified100() throws Exception {
    ConfigurationAdmin cm = (ConfigurationAdmin) trackerCM.getService();
    if (cm == null)
    	return;

    Bundle tb21 = installBundle("tb21");

    Hashtable props = new Hashtable(10);
    props.put("config.dummy.data", Integer.valueOf(1));
    cm.getConfiguration(MOD_NOTSET_NS100, null).update(props);
    cm.getConfiguration(MOD_NOARGS_NS100, null).update(props);
    cm.getConfiguration(MOD_CC_NS100, null).update(props);
    cm.getConfiguration(MOD_BC_NS100, null).update(props);
    cm.getConfiguration(MOD_MAP_NS100, null).update(props);
    cm.getConfiguration(MOD_CC_BC_MAP_NS100, null).update(props);

    Thread.sleep(timeout * 2);

    tb21.start();
    waitBundleStart();

    props.put("config.dummy.data", Integer.valueOf(2));
    Hashtable unsatisfyingProps = new Hashtable(10);
    unsatisfyingProps.put("ref.target", "(component.name=org.eclipse.equinox.ds.tests.tb21.unexisting.provider)");

    PropertiesProvider bs = getBaseService(MOD_NOTSET_NS100);
    assertNotNull(bs);
    cm.getConfiguration(MOD_NOTSET_NS100, null).update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Modified method of " + MOD_NOTSET_NS100 + " should not be called", 0, (1 << 0)
        & getBaseConfigData(bs));
    assertEquals("Deactivate method of " + MOD_NOTSET_NS100 + " should be called", 1 << 7, (1 << 7)
        & getBaseConfigData(bs));
    bs = getBaseService(MOD_NOTSET_NS100);
    cm.getConfiguration(MOD_NOTSET_NS100, null).update(unsatisfyingProps);
    Thread.sleep(timeout * 2);
    assertEquals("Modified method of " + MOD_NOTSET_NS100 + " should not be called", 0, (1 << 0)
        & getBaseConfigData(bs));
    assertEquals("Deactivate method of " + MOD_NOTSET_NS100 + " should be called", 1 << 7, (1 << 7)
        & getBaseConfigData(bs));

    // INVALID COMPONENTS for XML NS 1.0.0 - modified attribute is set
    bs = getBaseService(MOD_NOARGS_NS100);
    assertEquals("Component " + MOD_NOARGS_NS100 + " should not be activated", null, bs);
    bs = getBaseService(MOD_CC_NS100);
    assertEquals("Component " + MOD_CC_NS100 + " should not be activated", null, bs);
    bs = getBaseService(MOD_BC_NS100);
    assertEquals("Component " + MOD_BC_NS100 + " should not be activated", null, bs);
    bs = getBaseService(MOD_MAP_NS100);
    assertEquals("Component " + MOD_MAP_NS100 + " should not be activated", null, bs);
    bs = getBaseService(MOD_CC_BC_MAP_NS100);
    assertEquals("Component " + MOD_CC_BC_MAP_NS100 + " should not be activated", null, bs);

    uninstallBundle(tb21);
  }

  // Testing modified attribute for XML NS 1.1.0
  @Test
  public void testModified110() throws Exception {
    ConfigurationAdmin cm = (ConfigurationAdmin) trackerCM.getService();
    if (cm == null)
    	return;

    Bundle tb21a = installBundle("tb21a");

    Hashtable props = new Hashtable(10);
    props.put("config.dummy.data", Integer.valueOf(1));
    cm.getConfiguration(MOD_NOTSET_NS110, null).update(props);
    cm.getConfiguration(MOD_NOARGS_NS110, null).update(props);
    cm.getConfiguration(MOD_CC_NS110, null).update(props);
    cm.getConfiguration(MOD_BC_NS110, null).update(props);
    cm.getConfiguration(MOD_MAP_NS110, null).update(props);
    cm.getConfiguration(MOD_CC_BC_MAP_NS110, null).update(props);

    Thread.sleep(timeout * 2);

    tb21a.start();
    waitBundleStart();

    props.put("config.dummy.data", Integer.valueOf(2));
    Hashtable unsatisfyingProps = new Hashtable(10);
    unsatisfyingProps.put("ref.target", "(component.name=org.eclipse.equinox.ds.tests.tb21.unexisting.provider)");

    PropertiesProvider bs = getBaseService(MOD_NOTSET_NS110);
    cm.getConfiguration(MOD_NOTSET_NS110, null).update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Modified method of " + MOD_NOTSET_NS110 + " should not be called", 0, (1 << 0)
        & getBaseConfigData(bs));
    assertEquals("Deactivate method of " + MOD_NOTSET_NS110 + " should be called", 1 << 7, (1 << 7)
        & getBaseConfigData(bs));
    bs = getBaseService(MOD_NOTSET_NS110);
    cm.getConfiguration(MOD_NOTSET_NS110, null).update(unsatisfyingProps);
    Thread.sleep(timeout * 2);
    assertEquals("Modified method of " + MOD_NOTSET_NS110 + " should not be called", 0, (1 << 0)
        & getBaseConfigData(bs));
    assertEquals("Deactivate method of " + MOD_NOTSET_NS110 + " should be called", 1 << 7, (1 << 7)
        & getBaseConfigData(bs));
    // Re-activating
    bs = getBaseService(MOD_NOTSET_NS110);
    assertEquals("Activate method of " + MOD_NOTSET_NS110 + " should be called", 1 << 6, (1 << 6)
        & getBaseConfigData(bs));

    bs = getBaseService(MOD_NOARGS_NS110);
    cm.getConfiguration(MOD_NOARGS_NS110, null).update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Modified method of " + MOD_NOARGS_NS110 + " should be called", 1 << 1, (1 << 1)
        & getBaseConfigData(bs));
    assertEquals("Deactivate method of " + MOD_NOARGS_NS110 + " should not be called", 0, (1 << 7)
        & getBaseConfigData(bs));
    cm.getConfiguration(MOD_NOARGS_NS110, null).update(unsatisfyingProps);
    Thread.sleep(timeout * 2);
    assertEquals("Deactivate method of " + MOD_NOARGS_NS110 + " should be called", 1 << 7, (1 << 7)
        & getBaseConfigData(bs));
    // Re-activating
    bs = getBaseService(MOD_NOARGS_NS110);
    assertEquals("Activate method of " + MOD_NOARGS_NS110 + " should be called", 1 << 6, (1 << 6)
        & getBaseConfigData(bs));

    bs = getBaseService(MOD_CC_NS110);
    cm.getConfiguration(MOD_CC_NS110, null).update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Modified method of " + MOD_CC_NS110 + " should be called", 1 << 2, (1 << 2) & getBaseConfigData(bs));
    assertEquals("Deactivate method of " + MOD_CC_NS110 + " should not be called", 0, (1 << 7) & getBaseConfigData(bs));
    cm.getConfiguration(MOD_CC_NS110, null).update(unsatisfyingProps);
    Thread.sleep(timeout * 2);
    assertEquals("Deactivate method of " + MOD_CC_NS110 + " should be called", 1 << 7, (1 << 7) & getBaseConfigData(bs));
    // Re-activating
    bs = getBaseService(MOD_CC_NS110);
    assertEquals("Activate method of " + MOD_CC_NS110 + " should be called", 1 << 6, (1 << 6) & getBaseConfigData(bs));

    bs = getBaseService(MOD_BC_NS110);
    cm.getConfiguration(MOD_BC_NS110, null).update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Modified method of " + MOD_BC_NS110 + " should be called", 1 << 3, (1 << 3) & getBaseConfigData(bs));
    assertEquals("Deactivate method of " + MOD_BC_NS110 + " should not be called", 0, (1 << 7) & getBaseConfigData(bs));
    cm.getConfiguration(MOD_BC_NS110, null).update(unsatisfyingProps);
    Thread.sleep(timeout * 2);
    assertEquals("Deactivate method of " + MOD_BC_NS110 + " should be called", 1 << 7, (1 << 7) & getBaseConfigData(bs));
    // Re-activating
    bs = getBaseService(MOD_BC_NS110);
    assertEquals("Activate method of " + MOD_BC_NS110 + " should be called", 1 << 6, (1 << 6) & getBaseConfigData(bs));

    bs = getBaseService(MOD_MAP_NS110);
    cm.getConfiguration(MOD_MAP_NS110, null).update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Modified method of " + MOD_MAP_NS110 + " should be called", 1 << 4, (1 << 4) & getBaseConfigData(bs));
    assertEquals("Deactivate method of " + MOD_MAP_NS110 + " should not be called", 0, (1 << 7) & getBaseConfigData(bs));
    cm.getConfiguration(MOD_MAP_NS110, null).update(unsatisfyingProps);
    Thread.sleep(timeout * 2);
    assertEquals("Deactivate method of " + MOD_MAP_NS110 + " should be called", 1 << 7, (1 << 7)
        & getBaseConfigData(bs));
    // Re-activating
    bs = getBaseService(MOD_MAP_NS110);
    assertEquals("Activate method of " + MOD_MAP_NS110 + " should be called", 1 << 6, (1 << 6) & getBaseConfigData(bs));

    bs = getBaseService(MOD_CC_BC_MAP_NS110);
    cm.getConfiguration(MOD_CC_BC_MAP_NS110, null).update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Modified method of " + MOD_CC_BC_MAP_NS110 + " should be called", 1 << 5, (1 << 5)
        & getBaseConfigData(bs));
    assertEquals("Deactivate method of " + MOD_CC_BC_MAP_NS110 + " should not be called", 0, (1 << 7)
        & getBaseConfigData(bs));
    cm.getConfiguration(MOD_CC_BC_MAP_NS110, null).update(unsatisfyingProps);
    Thread.sleep(timeout * 2);
    assertEquals("Deactivate method of " + MOD_CC_BC_MAP_NS110 + " should be called", 1 << 7, (1 << 7)
        & getBaseConfigData(bs));
    // Re-activating
    bs = getBaseService(MOD_CC_BC_MAP_NS110);
    assertEquals("Activate method of " + MOD_CC_BC_MAP_NS110 + " should be called", 1 << 6, (1 << 6)
        & getBaseConfigData(bs));

    uninstallBundle(tb21a);
  }

  // Testing modified attribute - special cases
  @Test
  public void testModifiedSpecialCases() throws Exception {
    ConfigurationAdmin cm = (ConfigurationAdmin) trackerCM.getService();
    if (cm == null)
    	return;

    Bundle tb21a = installBundle("tb21a");

    Hashtable props = new Hashtable(10);
    props.put("config.dummy.data", Integer.valueOf(1));
    cm.getConfiguration(MOD_CC_NS110, null).update(props);
    cm.getConfiguration(MOD_NOT_EXIST_NS110, null).update(props);
    cm.getConfiguration(MOD_THROW_EX_NS110, null).update(props);
    cm.getConfiguration(MOD_BC_NS110, null).update(props);
    Thread.sleep(timeout * 2);

    tb21a.start();
    waitBundleStart();

    // Verifying correctness of updated component properties
    PropertiesProvider bs = getBaseService(MOD_CC_NS110);
    props.put("config.dummy.data", Integer.valueOf(2));
    cm.getConfiguration(MOD_CC_NS110, null).update(props);
    Thread.sleep(timeout * 2);
    Object val = ((ComponentContextProvider) bs).getComponentContext().getProperties().get("config.dummy.data");
    assertEquals("Modified method of " + MOD_CC_NS110 + " should be called", 1 << 2, (1 << 2) & getBaseConfigData(bs));
    assertTrue("Component properties should be updated properly for " + MOD_CC_NS110, (Integer.valueOf(2)).equals(val));

    // Specified modified method doesn't exist, deactivate() should be called
    // instead of modified
    bs = getBaseService(MOD_NOT_EXIST_NS110);
    cm.getConfiguration(MOD_NOT_EXIST_NS110, null).update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Deactivate method of " + MOD_NOT_EXIST_NS110 + " should be called", 1 << 7, (1 << 7)
        & getBaseConfigData(bs));
    // Re-activating
    bs = getBaseService(MOD_NOT_EXIST_NS110);
    assertEquals("Activate method of " + MOD_NOT_EXIST_NS110 + " should be called", 1 << 6, (1 << 6)
        & getBaseConfigData(bs));

    // Specified modified method throws exception. Normal workflow should
    // continue, deactivate() should not be called
    bs = getBaseService(MOD_THROW_EX_NS110);
    cm.getConfiguration(MOD_THROW_EX_NS110, null).update(props);
    Thread.sleep(timeout * 2);
    assertEquals("Deactivate method of " + MOD_THROW_EX_NS110 + " should not be called", 0, (1 << 7)
        & getBaseConfigData(bs));

    // Deleting component configuration
    bs = getBaseService(MOD_BC_NS110);
    cm.getConfiguration(MOD_BC_NS110, null).delete();
    Thread.sleep(timeout * 2);
    assertEquals("Modified method of " + MOD_BC_NS110 + " should not be called", 0, (1 << 5) & getBaseConfigData(bs));
    assertEquals("Deactivate method of " + MOD_BC_NS110 + " should be called", 1 << 7, (1 << 7) & getBaseConfigData(bs));
    // Re-activating
    bs = getBaseService(MOD_BC_NS110);
    assertEquals("Activate method of " + MOD_BC_NS110 + " should be called", 1 << 6, (1 << 6) & getBaseConfigData(bs));

    uninstallBundle(tb21a);
  }

  @Test
  public void testPrivateProperties() throws Exception {
    Bundle tb22 = installBundle("tb22");
    tb22.start();
    waitBundleStart();

    final String COMP = "org.eclipse.equinox.ds.tests.tb22.component";

    ServiceReference ref = trackerBaseService.getServiceReference();
    assertNotNull("Provided service of " + COMP + " should be available", ref);
    String[] keys = ref.getPropertyKeys();
    for (String key : keys) {
        assertTrue("Private properties should not be propagated", !key.startsWith("."));
    }

    uninstallBundle(tb22);
  }

  // Testing situation when bind method throws exception
  @Test
  public void testBindException() throws Exception {
    Bundle tb23 = installBundle("tb23");

    final String MANDATORY_REF_COMP = "org.eclipse.equinox.ds.tests.tb23.mandatory";
    final String OPTIONAL_REF_COMP = "org.eclipse.equinox.ds.tests.tb23.optional";
    tb23.start();
    waitBundleStart();

    PropertiesProvider bs = getBaseService(MANDATORY_REF_COMP);
    assertNotNull("Component " + MANDATORY_REF_COMP + " should not be activated", bs);
    bs = getBaseService(OPTIONAL_REF_COMP);
    assertEquals("Component " + OPTIONAL_REF_COMP + " should be activated", 1 << 2, (1 << 2) & getBaseConfigData(bs));

    uninstallBundle(tb23);
  }
  
  // Testing config admin appear/disappear situations
  @Test
  public void testConfigAdminOnOff() throws Exception {
    ConfigurationAdmin cm = (ConfigurationAdmin) trackerCM.getService();
    if (cm == null)
    	return;
    
    Hashtable props = new Hashtable(11);
    props.put("config.base.data", Integer.valueOf(1));
    //create the configurations for the test DS components
    Configuration config = cm.getConfiguration(COMP_OPTIONAL, null);
    config.update(props);
    config = cm.getConfiguration(COMP_REQUIRE, null);
    config.update(props);
    config = cm.getConfiguration(COMP_IGNORE, null);
    config.update(props);
    //wait for CM to process the configuration updates
    Thread.sleep(timeout * 2);
    
    //stop the config admin bundle
    Bundle cmBundle = trackerCM.getServiceReference().getBundle();
    cmBundle.stop();
    Bundle tb24 = installBundle("tb24");
    try {
      tb24.start();
      waitBundleStart();

      // component with optional configuration should be available and not initialized by configuration
      assertEquals("Component with optional configuration should be activated", 0, getBaseConfigData(COMP_OPTIONAL));
      // component with ignored configuration should be available and not initialized by configuration
      assertEquals("Component with ignored configuration should be activated", 0, getBaseConfigData(COMP_IGNORE));
      // component with required configuration should NOT be available
      assertEquals("Component with required configuration should NOT be activated", -1, getBaseConfigData(COMP_REQUIRE));
      
      //start again the config admin 
      cmBundle.start();
      
      //wait for processing components that depend on configurations
      Thread.sleep(timeout * 2);

      // component with optional configuration should be available and initialized by configuration
      assertEquals("Component with optional configuration should be activated and inited by configuration", 1, getBaseConfigData(COMP_OPTIONAL));
      // component with ignored configuration should be available and not initialized by configuration
      assertEquals("Component with ignored configuration should be activated", 0, getBaseConfigData(COMP_IGNORE));
      // component with required configuration should be available
      assertEquals("Component with required configuration should be activated", 1, getBaseConfigData(COMP_REQUIRE));
      
      //stop again the config admin 
      cmBundle.stop();

      /*The components should remain activated when Configuration Admin service disappears*/
      // component with optional configuration should be available and initialized by configuration
      assertEquals("Component with optional configuration should be activated", 1, getBaseConfigData(COMP_OPTIONAL));
      // component with ignored configuration should be available and not initialized by configuration
      assertEquals("Component with ignored configuration should be activated", 0, getBaseConfigData(COMP_IGNORE));
      // component with required configuration should be available
      assertEquals("Component with required configuration should be activated", 1, getBaseConfigData(COMP_REQUIRE));

    } finally {
      uninstallBundle(tb24);
      cmBundle.start();
    }
  }

  // Tests update of service properties
  @Test
  public void testServicePropertiesUpdate() throws Exception {
    Bundle tb25 = installBundle("tb25");
    ServiceRegistration sr = null;
    try {
      final String COMP_NAME = "org.eclipse.equinox.ds.tests.tb25.ServicePropertiesComp";
      final String PROP = "test.property";
      final String PROP_STATIC = "serviceUpdatedStatic";
      final String PROP_DYNAMIC = "serviceUpdatedDynamic";
      tb25.start();
      waitBundleStart();

      Hashtable props = new Hashtable();
      props.put("service.provider", "service.properties.update.test");
      props.put(PROP, Boolean.FALSE);

      // register service referenced by the component
      sr = registerService(PropertiesProvider.class.getName(), new DefaultPropertiesProvider(null),
          (Dictionary) props.clone());
      Thread.sleep(timeout);

      PropertiesProvider bs = getBaseService(COMP_NAME);
      assertNotNull("Component " + COMP_NAME + " should be activated", bs);

      // update service properties
      props.put(PROP, Boolean.TRUE);
      sr.setProperties((Dictionary) props.clone());
      Thread.sleep(timeout);

      bs = getBaseService(COMP_NAME);
      assertNotNull("Component " + COMP_NAME + " should still be active", bs);
      Dictionary compProps = bs.getProperties();
      assertNotNull("Component properties should be available.", compProps);
      assertEquals("Test property should be updated.", Boolean.TRUE, compProps.get(PROP));
      assertEquals("Updated method for static reference should be called.", Boolean.TRUE, compProps.get(PROP_STATIC));
      assertEquals("Updated method for dynamic reference should be called.", Boolean.TRUE, compProps.get(PROP_DYNAMIC));
    } finally {
      uninstallBundle(tb25);
      if (sr != null) {
        unregisterService(sr);
      }
    }
  }

  // Tests Reluctant policy option of service references
  @Test
  public void testPolicyOptionReluctant() throws Exception {
    Bundle tb25 = installBundle("tb25");
    ServiceRegistration sr = null;
    ServiceRegistration srLower = null;
    ServiceRegistration srHigher = null;
    try {
      final String COMP_NAME_STATIC = "org.eclipse.equinox.ds.tests.tb25.PolicyReluctantStaticComp";
      final String COMP_NAME_DYNAMIC = "org.eclipse.equinox.ds.tests.tb25.PolicyReluctantDynamicComp";
      final String PROP_BIND_01 = "bind01";
      final String PROP_BIND_11 = "bind11";
      final String PROP_BIND_0n = "bind0n";
      final String PROP_BIND_1n = "bind1n";
      final Integer RANK_1 = Integer.valueOf(1);
      final Integer RANK_2 = Integer.valueOf(2);
      final Integer RANK_3 = Integer.valueOf(3);
      tb25.start();
      waitBundleStart();

      Hashtable props = new Hashtable();
      props.put("service.provider", "reluctant.policy.option.test");
      props.put(Constants.SERVICE_RANKING, RANK_2);

      // register service referenced by the component
      sr = registerService(PropertiesProvider.class.getName(), new DefaultPropertiesProvider(null),
          (Dictionary) props.clone());
      Thread.sleep(timeout);

      PropertiesProvider bsStatic = getBaseService(COMP_NAME_STATIC);
      assertNotNull("Component " + COMP_NAME_STATIC + " should be activated", bsStatic);
      PropertiesProvider bsDynamic = getBaseService(COMP_NAME_DYNAMIC);
      assertNotNull("Component " + COMP_NAME_DYNAMIC + " should be activated", bsDynamic);

      // register service with lower ranking
      props.put(Constants.SERVICE_RANKING, RANK_1);
      srLower = registerService(PropertiesProvider.class.getName(), new DefaultPropertiesProvider(null),
          (Dictionary) props.clone());
      Thread.sleep(timeout);

      // check bound references
      bsStatic = getBaseService(COMP_NAME_STATIC);
      assertNotNull("Component " + COMP_NAME_STATIC + " should still be active", bsStatic);
      Dictionary compProps = bsStatic.getProperties();
      assertNotNull("Component properties should be available.", compProps);
      assertEqualElements("New target service with lower ranking should be ignored for static 0..1.", toList(RANK_2),
          (List) compProps.get(PROP_BIND_01));
      assertEqualElements("New target service with lower ranking should be ignored for static 1..1.", toList(RANK_2),
          (List) compProps.get(PROP_BIND_11));
      assertEqualElements("New target service with lower ranking should be ignored for static 0..n.", toList(RANK_2),
          (List) compProps.get(PROP_BIND_0n));
      assertEqualElements("New target service with lower ranking should be ignored for static 1..n.", toList(RANK_2),
          (List) compProps.get(PROP_BIND_1n));
      bsDynamic = getBaseService(COMP_NAME_DYNAMIC);
      assertNotNull("Component " + COMP_NAME_DYNAMIC + " should still be active", bsDynamic);
      compProps = bsDynamic.getProperties();
      assertNotNull("Component properties should be available.", compProps);
      assertEqualElements("New target service with lower ranking should be ignored for dynamic 0..1.", toList(RANK_2),
          (List) compProps.get(PROP_BIND_01));
      assertEqualElements("New target service with lower ranking should be ignored for dynamic 1..1.", toList(RANK_2),
          (List) compProps.get(PROP_BIND_11));
      assertEqualElements("New target service with lower ranking should be bound for dynamic 0..n.",
          toList(RANK_1, RANK_2), (List) compProps.get(PROP_BIND_0n));
      assertEqualElements("New target service with lower ranking should be bound for dynamic 1..n.",
          toList(RANK_1, RANK_2), (List) compProps.get(PROP_BIND_1n));

      // register service with higher ranking
      props.put(Constants.SERVICE_RANKING, RANK_3);
      srHigher = registerService(PropertiesProvider.class.getName(), new DefaultPropertiesProvider(null),
          (Dictionary) props.clone());
      Thread.sleep(timeout);

      // check bound references
      bsStatic = getBaseService(COMP_NAME_STATIC);
      assertNotNull("Component " + COMP_NAME_STATIC + " should still be active", bsStatic);
      compProps = bsStatic.getProperties();
      assertNotNull("Component properties should be available.", compProps);
      assertEqualElements("New target service with higher ranking should be ignored for static 0..1.", toList(RANK_2),
          (List) compProps.get(PROP_BIND_01));
      assertEqualElements("New target service with higher ranking should be ignored for static 1..1.", toList(RANK_2),
          (List) compProps.get(PROP_BIND_11));
      assertEqualElements("New target service with higher ranking should be ignored for static 0..n.", toList(RANK_2),
          (List) compProps.get(PROP_BIND_0n));
      assertEqualElements("New target service with higher ranking should be ignored for static 1..n.", toList(RANK_2),
          (List) compProps.get(PROP_BIND_1n));
      bsDynamic = getBaseService(COMP_NAME_DYNAMIC);
      assertNotNull("Component " + COMP_NAME_DYNAMIC + " should still be active", bsDynamic);
      compProps = bsDynamic.getProperties();
      assertNotNull("Component properties should be available.", compProps);
      assertEqualElements("New target service with higher ranking should be ignored for dynamic 0..1.", toList(RANK_2),
          (List) compProps.get(PROP_BIND_01));
      assertEqualElements("New target service with higher ranking should be ignored for dynamic 1..1.", toList(RANK_2),
          (List) compProps.get(PROP_BIND_11));
      assertEqualElements("New target service with higher ranking should be bound for dynamic 0..n.",
          toList(RANK_1, RANK_2, RANK_3), (List) compProps.get(PROP_BIND_0n));
      assertEqualElements("New target service with higher ranking should be bound for dynamic 1..n.",
          toList(RANK_1, RANK_2, RANK_3), (List) compProps.get(PROP_BIND_1n));
    } finally {
      uninstallBundle(tb25);
      if (sr != null) {
        unregisterService(sr);
      }
      if (srLower != null) {
        unregisterService(srLower);
      }
      if (srHigher != null) {
        unregisterService(srHigher);
      }
    }
  }

  // Tests Greedy policy option of service references
  @Test
  public void testPolicyOptionGreedy() throws Exception {
    Bundle tb25 = installBundle("tb25");
    ServiceRegistration sr = null;
    ServiceRegistration srLower = null;
    ServiceRegistration srHigher = null;
    try {
      final String COMP_NAME_STATIC = "org.eclipse.equinox.ds.tests.tb25.PolicyGreedyStaticComp";
      final String COMP_NAME_DYNAMIC = "org.eclipse.equinox.ds.tests.tb25.PolicyGreedyDynamicComp";
      final String PROP_BIND_01 = "bind01";
      final String PROP_BIND_11 = "bind11";
      final String PROP_BIND_0n = "bind0n";
      final String PROP_BIND_1n = "bind1n";
      final Integer RANK_1 = 1;
      final Integer RANK_2 = 2;
      final Integer RANK_3 = 3;
      tb25.start();
      waitBundleStart();

      Hashtable props = new Hashtable();
      props.put("service.provider", "greedy.policy.option.test");
      props.put(Constants.SERVICE_RANKING, RANK_2);

      // register service referenced by the component
      sr = registerService(PropertiesProvider.class.getName(), new DefaultPropertiesProvider(null),
          (Dictionary) props.clone());
      Thread.sleep(timeout);

      PropertiesProvider bsStatic = getBaseService(COMP_NAME_STATIC);
      assertNotNull("Component " + COMP_NAME_STATIC + " should be activated", bsStatic);
      PropertiesProvider bsDynamic = getBaseService(COMP_NAME_DYNAMIC);
      assertNotNull("Component " + COMP_NAME_DYNAMIC + " should be activated", bsDynamic);

      // register service with lower ranking
      props.put(Constants.SERVICE_RANKING, RANK_1);
      srLower = registerService(PropertiesProvider.class.getName(), new DefaultPropertiesProvider(null),
          (Dictionary) props.clone());
      Thread.sleep(timeout);

      // check bound references
      bsStatic = getBaseService(COMP_NAME_STATIC);
      assertNotNull("Component " + COMP_NAME_STATIC + " should still be active", bsStatic);
      Dictionary compProps = bsStatic.getProperties();
      assertNotNull("Component properties should be available.", compProps);
      assertEqualElements("New target service with lower ranking should be ignored for static 0..1.", toList(RANK_2),
          (List) compProps.get(PROP_BIND_01));
      assertEqualElements("New target service with lower ranking should be ignored for static 1..1.", toList(RANK_2),
          (List) compProps.get(PROP_BIND_11));
      assertEqualElements("New target service with lower ranking should be bound for static 0..n.",
          toList(RANK_1, RANK_2), (List) compProps.get(PROP_BIND_0n));
      assertEqualElements("New target service with lower ranking should be bound for static 1..n.",
          toList(RANK_1, RANK_2), (List) compProps.get(PROP_BIND_1n));
      bsDynamic = getBaseService(COMP_NAME_DYNAMIC);
      assertNotNull("Component " + COMP_NAME_DYNAMIC + " should still be active", bsDynamic);
      compProps = bsDynamic.getProperties();
      assertNotNull("Component properties should be available.", compProps);
      assertEqualElements("New target service with lower ranking should be ignored for dynamic 0..1.", toList(RANK_2),
          (List) compProps.get(PROP_BIND_01));
      assertEqualElements("New target service with lower ranking should be ignored for dynamic 1..1.", toList(RANK_2),
          (List) compProps.get(PROP_BIND_11));
      assertEqualElements("New target service with lower ranking should be bound for dynamic 0..n.",
          toList(RANK_1, RANK_2), (List) compProps.get(PROP_BIND_0n));
      assertEqualElements("New target service with lower ranking should be bound for dynamic 1..n.",
          toList(RANK_1, RANK_2), (List) compProps.get(PROP_BIND_1n));

      // register service with higher ranking
      props.put(Constants.SERVICE_RANKING, RANK_3);
      srHigher = registerService(PropertiesProvider.class.getName(), new DefaultPropertiesProvider(null),
          (Dictionary) props.clone());
      Thread.sleep(timeout);

      // check bound references
      bsStatic = getBaseService(COMP_NAME_STATIC);
      assertNotNull("Component " + COMP_NAME_STATIC + " should still be active", bsStatic);
      compProps = bsStatic.getProperties();
      assertNotNull("Component properties should be available.", compProps);
      assertEqualElements("New target service with higher ranking should be bound for static 0..1.", toList(RANK_3),
          (List) compProps.get(PROP_BIND_01));
      assertEqualElements("New target service with higher ranking should be bound for static 1..1.", toList(RANK_3),
          (List) compProps.get(PROP_BIND_11));
      assertEqualElements("New target service with higher ranking should be bound for static 0..n.",
          toList(RANK_1, RANK_2, RANK_3), (List) compProps.get(PROP_BIND_0n));
      assertEqualElements("New target service with higher ranking should be bound for static 1..n.",
          toList(RANK_1, RANK_2, RANK_3), (List) compProps.get(PROP_BIND_1n));
      bsDynamic = getBaseService(COMP_NAME_DYNAMIC);
      assertNotNull("Component " + COMP_NAME_DYNAMIC + " should still be active", bsDynamic);
      compProps = bsDynamic.getProperties();
      assertNotNull("Component properties should be available.", compProps);
      assertEqualElements("New target service with higher ranking should be bound for dynamic 0..1.",
          toList(RANK_2, RANK_3), (List) compProps.get(PROP_BIND_01));
      assertEqualElements("New target service with higher ranking should be bound for dynamic 1..1.",
          toList(RANK_2, RANK_3), (List) compProps.get(PROP_BIND_11));
      assertEqualElements("New target service with higher ranking should be bound for dynamic 0..n.",
          toList(RANK_1, RANK_2, RANK_3), (List) compProps.get(PROP_BIND_0n));
      assertEqualElements("New target service with higher ranking should be bound for dynamic 1..n.",
          toList(RANK_1, RANK_2, RANK_3), (List) compProps.get(PROP_BIND_1n));
    } finally {
      uninstallBundle(tb25);
      if (sr != null) {
        unregisterService(sr);
      }
      if (srLower != null) {
        unregisterService(srLower);
      }
      if (srHigher != null) {
        unregisterService(srHigher);
      }
    }
  }

  // Tests PID of Component configuration
  @Test
  public void testComponentConfigurationPID() throws Exception {
    ConfigurationAdmin cm = (ConfigurationAdmin) trackerCM.getService();
    if (cm == null) {
      return;
    }

    Bundle tb25 = installBundle("tb25");
    Configuration config = null;
    try {
      final String COMP_NAME = "org.eclipse.equinox.ds.tests.tb25.ConfigPIDComp";
      final String CONFIG_PID = "test.changed.configuration.pid";
      final String PROP = "test.property";

      // set component configuration
      config = cm.getConfiguration(CONFIG_PID, null);
      Dictionary configProperties = config.getProperties();
      if (configProperties == null) {
        configProperties = new Hashtable();
      }
      configProperties.put(PROP, Boolean.TRUE);
      config.update(configProperties);
      Thread.sleep(timeout);

      tb25.start();
      waitBundleStart();

      // check component properties
      PropertiesProvider bs = getBaseService(COMP_NAME);
      assertNotNull("Component " + COMP_NAME + " should be activated", bs);
      Dictionary compProps = bs.getProperties();
      assertNotNull("Component properties should be available.", compProps);
      assertEquals("Test property should be set.", Boolean.TRUE, compProps.get(PROP));
    } finally {
      uninstallBundle(tb25);
      if (config != null) {
        config.delete();
      }
    }
  }

  /**
   * Asserts that two lists contain equal elements (the order doesn't matter).
   */
  private static void assertEqualElements(String message, List list1, List list2) {
    if (list1 == null || list2 == null) {
      fail(message);
    }
    if (list1.size() != list2.size()) {
      fail(message);
    }
    List tmp = new ArrayList(list2);
    for (Iterator it = list1.iterator(); it.hasNext();) {
      Object el = it.next();
      if (!tmp.contains(el)) {
        fail(message);
      }
      tmp.remove(el);
    }
  }

  private List toList(Object el) {
    List list = new ArrayList();
    list.add(el);
    return list;
  }

  private List toList(Object el1, Object el2) {
    List list = new ArrayList();
    list.add(el1);
    list.add(el2);
    return list;
  }

  private List toList(Object el1, Object el2, Object el3) {
    List list = new ArrayList();
    list.add(el1);
    list.add(el2);
    list.add(el3);
    return list;
  }

  /**
   * Searches for component with name componentName which provides
   * PropertiesProvider. Returns value of its "config.base.data" property.
   * 
   * @param componentName
   *          - the name of the component to get data
   * @return the value of property "config.base.data", provided by
   *         PropertiesProvider.getProperties(). Returned value is -1 when
   *         component which provides PropertiesProvider and has specified name
   *         is not activated. Returned value is 0 when component with specified
   *         name is active but doesn't have property "config.base.data".
   */
  private int getBaseConfigData(String componentName) {
    PropertiesProvider s = getBaseService(componentName);
    return getBaseConfigData(s);
  }

  private int getBaseConfigData(PropertiesProvider s) {
    Dictionary props = null;
    int value = -1;
    if (s != null) {
      value = 0;
      if ((props = s.getProperties()) != null) {
        Object prop = props.get("config.base.data");
        if (prop instanceof Integer) {
          value = ((Integer) prop).intValue();
        }
      }
    }
    return value;
  }

  private PropertiesProvider getBaseService(String componentName) {
    Object[] services = trackerBaseService.getServices();
    if (services == null) {
      return null;
    }
    for (Object service : services) {
        if (service instanceof PropertiesProvider) {
            PropertiesProvider s = (PropertiesProvider) service;
            Dictionary props = s.getProperties();
            if (props != null && ((String) props.get(ComponentConstants.COMPONENT_NAME)).equals(componentName)) {
                return s;
                }
            }
        }
    return null;
  }

  private BundleContext getContext() {
    return DSTestsActivator.getContext();
  }

  private Bundle installBundle(String bundle) throws BundleException {
    Bundle b = installer.installBundle(bundle);
    return b;
  }
  
  private Bundle installBundleAsDirectory(String bundle) throws Exception {
	  BundleContext context = getContext();
	  String location = installer.getBundleLocation(bundle);
	  String reference = "reference:";
	  if (location.startsWith(reference))
		  // Remove the "reference" protocol from the URL.
		  // (1) So that when running from a workspace, the test will modify a
		  // copy of the component.xml.
		  // (2) When running from a server, to get a readable input stream when 
		  // extracting the JAR into a directory.
		  location = location.substring(location.indexOf(':') + 1);
	  if (!location.endsWith(".jar"))
		  // If the bundle is already a directory, go ahead and install it in
		  // the typical fashion. Leave the "reference" protocol out.
		  return context.installBundle(location);
	  // The bundle is a JAR file and needs to be extracted and copied as a
	  // directory to the data storage area of the test harness bundle.
	  File file = context.getBundle().getDataFile(bundle);
	  ZipInputStream in = new ZipInputStream(new URL(location).openStream());
	  try {
		  for (ZipEntry ze = in.getNextEntry(); ze != null; ze = in.getNextEntry()) {
			  String name = ze.getName();
			  // Is the entry a directory?
			  if (ze.isDirectory())
				  // If so, continue to the next entry. Directories will be
				  // created later.
				  continue;
			  // If not, the contents of the file must be copied.
			  File destination;
			  // Does the file entry contain a directory?
			  int index = name.lastIndexOf('/');
			  if (index == -1)
				  // If not, just create the destination file.
				  destination = new File(file, name);
			  else {
				  // If so, make sure the directory exists.
				  File dir = new File(file, name.substring(0, index));
				  dir.mkdirs();
				  // Then create the destination file.
				  destination = new File(dir, name.substring(index));
			  }
			  // Now copy the contents of the file entry to the destination.
			  byte[] bytes = new byte[1024];
			  int read;
			  FileOutputStream out = new FileOutputStream(destination);
			  try {
				  while ((read = in.read(bytes)) != -1)
					  out.write(bytes, 0, read);
			  }
			  finally {
				  out.close();
			  }
			  in.closeEntry();
		  }
	  }
	  finally {
		  in.close();
	  }
	  // Add the "reference" protocol back when running from a server so the
	  // framework sees the modified component.xml in the bundle data storage.
	  return context.installBundle(reference + file.toURI());
  }

  private void uninstallBundle(Bundle bundle) throws BundleException {
    installer.uninstallBundle(bundle);
  }

  private ServiceRegistration registerService(String className, Object service, Dictionary props) {
    ServiceRegistration sr = getContext().registerService(className, service, props);

    registeredServices.put(service, sr);

    return sr;
  }

  private void unregisterService(Object service) {
    ServiceRegistration sr = (ServiceRegistration) registeredServices.get(service);
    if (sr != null) {
      sr.unregister();
      registeredServices.remove(service);
    }
  }

  private void unregisterService(ServiceRegistration reg) {
    Enumeration e = registeredServices.keys();
    while (e.hasMoreElements()) {
      Object service = e.nextElement();
      if (reg == null || registeredServices.get(service) == reg) {
        unregisterService(service);
      }
    }
  }

  private void unregisterAllServices() {
    Enumeration e = registeredServices.keys();
    while (e.hasMoreElements()) {
      Object service = e.nextElement();
      unregisterService(service);
    }
  }

  private void log(String msg) {
    // System.out.println("[Declarative Service TC] " + msg);
  }

  public void sleep0(long millisToSleep) {
    long start = System.currentTimeMillis();
    do {
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
      }
    } while (System.currentTimeMillis() - start < millisToSleep);
  }

  /**
   * Waits for the processing of the bundle declarative components by SCR. In
   * case the building of the DS components is synchronous the method does not
   * wait
   */
  private void waitBundleStart() {
    if (!synchronousBuild) {
      sleep0(2 * timeout);
    }
  }
  
  /*
   * This test requires the bundle to be installed as a directory using a 
   * reference URL. Otherwise, necessary file updates will not occur, and the 
   * test will no longer be valid.
   */
  public void disableTestComponentDefinitionReloadedOnBundleUpdateInDevModeWhenUsingWildcardInServiceComponentHeader() throws Exception {
	  // Enable component caching in DS.
	  System.setProperty("equinox.ds.dbstore", Boolean.TRUE.toString());
	  // Set dev mode.
	  System.setProperty("osgi.checkConfiguration", Boolean.TRUE.toString());
	  
	  String serviceName = "org.eclipse.equinox.ds.tests.tb26.Component";
	  // component.xml = conmponent1.xml initially.
	  Bundle b = installBundleAsDirectory("tb26");
	  try {
		  b.start();
		  waitBundleStart();
		  
		  // The component service should be Component1.
		  BundleContext context = getContext();
		  ServiceReference<?> ref = getServiceReference(serviceName);
		  assertNotNull("Component service not registered on start", ref);
		  Object service = context.getService(ref);
		  Class clazz = service.getClass();
		  assertEquals("Wrong Component service", "org.eclipse.equinox.ds.tests.tb26.impl.Component1", clazz.getName());
		  
		  // Update component.xml with component2.xml.
		  clazz.getMethod("update", (Class[])null).invoke(service, (Object[])null);
		  
		  // Force a component update in DS.
		  b.stop();
		  b.start();
		  waitBundleStart();
		  
		  // The component service should now be Component2.
		  ref = context.getServiceReference(serviceName);
		  assertNotNull("Component service not registered on restart", ref);
		  service = context.getService(ref);
		  clazz = service.getClass();
		  assertEquals("Wrong component service", "org.eclipse.equinox.ds.tests.tb26.impl.Component2", clazz.getName());
	  }
	  finally {
		  uninstallBundle(b);
	  }
  }
}
