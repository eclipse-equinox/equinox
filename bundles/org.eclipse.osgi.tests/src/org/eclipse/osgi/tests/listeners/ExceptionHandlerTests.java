package org.eclipse.osgi.tests.listeners;

import java.io.IOException;
import java.net.MalformedURLException;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.eclipse.osgi.tests.OSGiTests;
import org.osgi.framework.*;

public class ExceptionHandlerTests extends TestCase {
	//These tests exercise the code change for bug 73111.
	
	class FrameworkEventListenerWithResult implements FrameworkListener {
		FrameworkEvent event = null;
		
		public synchronized void frameworkEvent(FrameworkEvent newEvent) {
			if (newEvent.getType() != FrameworkEvent.ERROR)
				return;
			
			if (this.event != null)
				return;
			event = newEvent;
			notify();
		}
		
		public synchronized FrameworkEvent getResult(long timeout) {
			if (event != null) {
				FrameworkEvent tmp = event;
				event = null;
				return tmp;
			}
			try {
				wait(timeout);
			} catch (InterruptedException e) {
			}
			FrameworkEvent tmp = event;
			event = null;
			return tmp;
		}
	};

	
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	
	public void testNonFatalException() {	
		FrameworkEventListenerWithResult fwkListener = new FrameworkEventListenerWithResult();
		OSGiTests.getContext().addFrameworkListener(fwkListener);

		BundleListener npeGenerator = new BundleListener() {
			public void bundleChanged(BundleEvent event) {
				throw new NullPointerException("Generated exception");
			}
		};	
		OSGiTests.getContext().addBundleListener(npeGenerator);
		
		try {
			BundleTestingHelper.installBundle(OSGiTests.getContext(), OSGiTests.TEST_FILES_ROOT + "internal/plugins/installTests/bundle09");
			FrameworkEvent eventReceived = fwkListener.getResult(60000);
			Assert.assertEquals(FrameworkEvent.ERROR, eventReceived.getType());
			Assert.assertEquals(true, eventReceived.getThrowable() instanceof NullPointerException);
		} catch (MalformedURLException e) {
			//Does not happen
		} catch (BundleException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		OSGiTests.getContext().removeFrameworkListener(fwkListener);
		OSGiTests.getContext().removeBundleListener(npeGenerator);
	}
	
	
	public void testFatalException() {	
		FrameworkEventListenerWithResult fwkListener = new FrameworkEventListenerWithResult();
		OSGiTests.getContext().addFrameworkListener(fwkListener);

		BundleListener fatalException = new BundleListener() {
			public void bundleChanged(BundleEvent event) {
				throw new OutOfMemoryError("Generated exception");
			}
		};
		OSGiTests.getContext().addBundleListener(fatalException);
		
	
		try {
			System.setProperty("eclipse.exitOnError","false"); //Here we set the value to false, because otherwise we would simply exit
			BundleTestingHelper.installBundle(OSGiTests.getContext(), OSGiTests.TEST_FILES_ROOT + "internal/plugins/installTests/bundle10");
			FrameworkEvent eventReceived = fwkListener.getResult(10000);
			Assert.assertEquals(FrameworkEvent.ERROR, eventReceived.getType());
			Assert.assertEquals(true, eventReceived.getThrowable() instanceof VirtualMachineError);
			System.setProperty("eclipse.exitOnError","true");
		} catch (MalformedURLException e) {
			//Does not happen
		} catch (BundleException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		OSGiTests.getContext().removeFrameworkListener(fwkListener);
		OSGiTests.getContext().removeBundleListener(fatalException);
	}
 
}