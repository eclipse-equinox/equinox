/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.core;

import java.io.IOException;
import java.security.*;
import java.util.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.eventmgr.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.startlevel.StartLevel;

/**
 * StartLevel service implementation for the OSGi specification.
 *
 * Framework service which allows management of framework and bundle startlevels.
 *
 * This class also acts as the StartLevel service factory class, providing StartLevel objects
 * to those requesting org.osgi.service.startlevel.StartLevel service. 
 * 
 * If present, there will only be a single instance of this service
 * registered in the framework.
 */
public class StartLevelManager implements EventDispatcher<Object, Object, StartLevelEvent>, StartLevel {
	protected static EventManager eventManager;
	protected static Map<Object, Object> startLevelListeners;

	/** The initial bundle start level for newly installed bundles */
	protected int initialBundleStartLevel = 1;
	// default value is 1 for compatibility mode

	/** The currently active framework start level */
	private int activeSL = 0;

	/** An object used to lock the active startlevel while it is being referenced */
	private final Object lock = new Object();
	private final Framework framework;

	/** This constructor is called by the Framework */
	protected StartLevelManager(Framework framework) {
		this.framework = framework;
	}

	protected void initialize() {
		initialBundleStartLevel = framework.adaptor.getInitialBundleStartLevel();

		// create an event manager and a start level listener
		// note that we do not pass the ContextFinder because it is set each time doSetStartLevel is called
		eventManager = new EventManager("Start Level Event Dispatcher"); //$NON-NLS-1$
		startLevelListeners = new CopyOnWriteIdentityMap<Object, Object>();
		startLevelListeners.put(this, this);
	}

	protected void cleanup() {
		eventManager.close();
		eventManager = null;
		startLevelListeners.clear();
		startLevelListeners = null;
	}

	/**
	 * Return the initial start level value that is assigned
	 * to a Bundle when it is first installed.
	 *
	 * @return The initial start level value for Bundles.
	 * @see #setInitialBundleStartLevel
	 */
	public int getInitialBundleStartLevel() {
		return initialBundleStartLevel;
	}

	/**
	 * Set the initial start level value that is assigned
	 * to a Bundle when it is first installed.
	 *
	 * <p>The initial bundle start level will be set to the specified start level. The
	 * initial bundle start level value will be persistently recorded
	 * by the Framework.
	 *
	 * <p>When a Bundle is installed via <tt>BundleContext.installBundle</tt>,
	 * it is assigned the initial bundle start level value.
	 *
	 * <p>The default initial bundle start level value is 1
	 * unless this method has been
	 * called to assign a different initial bundle
	 * start level value.
	 *
	 * <p>This method does not change the start level values of installed
	 * bundles.
	 *
	 * @param startlevel The initial start level for newly installed bundles.
	 * @throws IllegalArgumentException If the specified start level is less than or
	 * equal to zero.
	 * @throws SecurityException if the caller does not have the
	 * <tt>AdminPermission</tt> and the Java runtime environment supports
	 * permissions.
	 */
	public void setInitialBundleStartLevel(int startlevel) {
		framework.checkAdminPermission(framework.systemBundle, AdminPermission.STARTLEVEL);
		if (startlevel <= 0) {
			throw new IllegalArgumentException();
		}
		initialBundleStartLevel = startlevel;
		framework.adaptor.setInitialBundleStartLevel(startlevel);
	}

	/**
	 * Return the active start level value of the Framework.
	 *
	 * If the Framework is in the process of changing the start level
	 * this method must return the active start level if this
	 * differs from the requested start level.
	 *
	 * @return The active start level value of the Framework.
	 */
	public int getStartLevel() {
		return activeSL;
	}

	/**
	 * Modify the active start level of the Framework.
	 *
	 * <p>The Framework will move to the requested start level. This method
	 * will return immediately to the caller and the start level
	 * change will occur asynchronously on another thread.
	 *
	 * <p>If the specified start level is
	 * higher than the active start level, the
	 * Framework will continue to increase the start level
	 * until the Framework has reached the specified start level,
	 * starting bundles at each
	 * start level which are persistently marked to be started as described in the
	 * <tt>Bundle.start</tt> method.
	 *
	 * At each intermediate start level value on the
	 * way to and including the target start level, the framework must:
	 * <ol>
	 * <li>Change the active start level to the intermediate start level value.
	 * <li>Start bundles at the intermediate start level in
	 * ascending order by <tt>Bundle.getBundleId</tt>.
	 * </ol>
	 * When this process completes after the specified start level is reached,
	 * the Framework will broadcast a Framework event of
	 * type <tt>FrameworkEvent.STARTLEVEL_CHANGED</tt> to announce it has moved to the specified
	 * start level.
	 *
	 * <p>If the specified start level is lower than the active start level, the
	 * Framework will continue to decrease the start level
	 * until the Framework has reached the specified start level
	 * stopping bundles at each
	 * start level as described in the <tt>Bundle.stop</tt> method except that their
	 * persistently recorded state indicates that they must be restarted in the
	 * future.
	 *
	 * At each intermediate start level value on the
	 * way to and including the specified start level, the framework must:
	 * <ol>
	 * <li>Stop bundles at the intermediate start level in
	 * descending order by <tt>Bundle.getBundleId</tt>.
	 * <li>Change the active start level to the intermediate start level value.
	 * </ol>
	 * When this process completes after the specified start level is reached,
	 * the Framework will broadcast a Framework event of
	 * type <tt>FrameworkEvent.STARTLEVEL_CHANGED</tt> to announce it has moved to the specified
	 * start level.
	 *
	 * <p>If the specified start level is equal to the active start level, then
	 * no bundles are started or stopped, however, the Framework must broadcast
	 * a Framework event of type <tt>FrameworkEvent.STARTLEVEL_CHANGED</tt> to
	 * announce it has finished moving to the specified start level. This
	 * event may arrive before the this method return.
	 *
	 * @param newSL The requested start level for the Framework.
	 * @throws IllegalArgumentException If the specified start level is less than or
	 * equal to zero.
	 * @throws SecurityException If the caller does not have the
	 * <tt>AdminPermission</tt> and the Java runtime environment supports
	 * permissions.
	 */
	public void setStartLevel(int newSL, org.osgi.framework.Bundle callerBundle, FrameworkListener... listeners) {
		if (newSL <= 0) {
			throw new IllegalArgumentException(NLS.bind(Msg.STARTLEVEL_EXCEPTION_INVALID_REQUESTED_STARTLEVEL, "" + newSL)); //$NON-NLS-1$ 
		}
		framework.checkAdminPermission(framework.systemBundle, AdminPermission.STARTLEVEL);

		if (Debug.DEBUG_STARTLEVEL) {
			Debug.println("StartLevelImpl: setStartLevel: " + newSL + "; callerBundle = " + callerBundle.getBundleId()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		issueEvent(new StartLevelEvent(StartLevelEvent.CHANGE_FW_SL, newSL, (AbstractBundle) callerBundle, listeners));

	}

	public void setStartLevel(int newSL) {
		setStartLevel(newSL, framework.systemBundle);
	}

	/**
	 *  Internal method to shut down the framework synchronously by setting the startlevel to zero
	 *  and calling the StartLevelListener worker calls directly
	 *
	 *  This method does not return until all bundles are stopped and the framework is shut down.
	 */
	protected void shutdown() {
		doSetStartLevel(0);
	}

	/**
	 *  Internal worker method to set the startlevel
	 *
	 * @param newSL start level value                  
	 * @param callerBundle - the bundle initiating the change in start level
	 */
	void doSetStartLevel(int newSL, FrameworkListener... listeners) {
		synchronized (lock) {
			ClassLoader previousTCCL = Thread.currentThread().getContextClassLoader();
			ClassLoader contextFinder = framework.getContextFinder();
			if (contextFinder == previousTCCL)
				contextFinder = null;
			else
				Thread.currentThread().setContextClassLoader(contextFinder);
			try {
				int tempSL = activeSL;
				if (newSL > tempSL) {
					boolean launching = tempSL == 0;
					for (int i = tempSL; i < newSL; i++) {
						if (Debug.DEBUG_STARTLEVEL) {
							Debug.println("sync - incrementing Startlevel from " + tempSL); //$NON-NLS-1$
						}
						tempSL++;
						// Note that we must get a new list of installed bundles each time;
						// this is because additional bundles could have been installed from the previous start-level
						incFWSL(i + 1, getInstalledBundles(framework.bundles, false));
					}
					if (launching) {
						framework.systemBundle.state = Bundle.ACTIVE;
						framework.publishBundleEvent(BundleEvent.STARTED, framework.systemBundle);
						framework.publishFrameworkEvent(FrameworkEvent.STARTED, framework.systemBundle, null);
					}
				} else {
					AbstractBundle[] sortedBundles = getInstalledBundles(framework.bundles, true);
					for (int i = tempSL; i > newSL; i--) {
						if (Debug.DEBUG_STARTLEVEL) {
							Debug.println("sync - decrementing Startlevel from " + tempSL); //$NON-NLS-1$
						}
						tempSL--;
						decFWSL(i - 1, sortedBundles);
					}
					if (newSL == 0) {
						// unload all bundles
						unloadAllBundles(framework.bundles);
						stopSystemBundle();
					}
				}
				framework.publishFrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, framework.systemBundle, null, listeners);
				if (Debug.DEBUG_STARTLEVEL) {
					Debug.println("StartLevelImpl: doSetStartLevel: STARTLEVEL_CHANGED event published"); //$NON-NLS-1$
				}
			} catch (Error e) {
				framework.publishFrameworkEvent(FrameworkEvent.ERROR, framework.systemBundle, e, listeners);
				throw e;
			} catch (RuntimeException e) {
				framework.publishFrameworkEvent(FrameworkEvent.ERROR, framework.systemBundle, e, listeners);
				throw e;
			} finally {
				if (contextFinder != null)
					Thread.currentThread().setContextClassLoader(previousTCCL);
			}
		}
	}

	/** 
	 * This method is used within the package to save the actual active startlevel value for the framework.
	 * Externally the setStartLevel method must be used.
	 * 
	 * @param newSL - the new startlevel to save
	 */
	protected void saveActiveStartLevel(int newSL) {
		synchronized (lock) {
			activeSL = newSL;
		}
	}

	/**
	 * Return the persistent state of the specified bundle.
	 *
	 * <p>This method returns the persistent state of a bundle.
	 * The persistent state of a bundle indicates whether a bundle
	 * is persistently marked to be started when it's start level is
	 * reached.
	 *
	 * @return <tt>true</tt> if the bundle is persistently marked to be started,
	 * <tt>false</tt> if the bundle is not persistently marked to be started.
	 * @exception java.lang.IllegalArgumentException If the specified bundle has been uninstalled.
	 */
	public boolean isBundlePersistentlyStarted(org.osgi.framework.Bundle bundle) {
		return ((AbstractBundle) bundle).isPersistentlyStarted();
	}

	public boolean isBundleActivationPolicyUsed(Bundle bundle) {
		return ((AbstractBundle) bundle).isActivationPolicyUsed();
	}

	/**
	 * Return the assigned start level value for the specified Bundle.
	 *
	 * @param bundle The target bundle.
	 * @return The start level value of the specified Bundle.
	 * @exception java.lang.IllegalArgumentException If the specified bundle has been uninstalled.
	 */
	public int getBundleStartLevel(org.osgi.framework.Bundle bundle) {
		return ((AbstractBundle) bundle).getStartLevel();
	}

	/**
	 * Assign a start level value to the specified Bundle.
	 *
	 * <p>The specified bundle will be assigned the specified start level. The
	 * start level value assigned to the bundle will be persistently recorded
	 * by the Framework.
	 *
	 * If the new start level for the bundle is lower than or equal to the active start level of
	 * the Framework, the Framework will start the specified bundle as described
	 * in the <tt>Bundle.start</tt> method if the bundle is persistently marked
	 * to be started. The actual starting of this bundle must occur asynchronously.
	 *
	 * If the new start level for the bundle is higher than the active start level of
	 * the Framework, the Framework will stop the specified bundle as described
	 * in the <tt>Bundle.stop</tt> method except that the persistently recorded
	 * state for the bundle indicates that the bundle must be restarted in the
	 * future. The actual stopping of this bundle must occur asynchronously.
	 *
	 * @param bundle The target bundle.
	 * @param newSL The new start level for the specified Bundle.
	 * @throws IllegalArgumentException
	 * If the specified bundle has been uninstalled or
	 * if the specified start level is less than or equal to zero, or the  specified bundle is
	 * the system bundle.
	 * @throws SecurityException if the caller does not have the
	 * <tt>AdminPermission</tt> and the Java runtime environment supports
	 * permissions.
	 */
	public void setBundleStartLevel(org.osgi.framework.Bundle bundle, int newSL) {

		String exceptionText = null;
		if (bundle.getBundleId() == 0) { // system bundle has id=0
			exceptionText = Msg.STARTLEVEL_CANT_CHANGE_SYSTEMBUNDLE_STARTLEVEL;
		} else if (bundle.getState() == Bundle.UNINSTALLED) {
			exceptionText = NLS.bind(Msg.BUNDLE_UNINSTALLED_EXCEPTION, ((AbstractBundle) bundle).getBundleData().getLocation());
		} else if (newSL <= 0) {
			exceptionText = NLS.bind(Msg.STARTLEVEL_EXCEPTION_INVALID_REQUESTED_STARTLEVEL, "" + newSL); //$NON-NLS-1$ 
		}
		if (exceptionText != null)
			throw new IllegalArgumentException(exceptionText);
		// first check the permission of the caller
		framework.checkAdminPermission(bundle, AdminPermission.EXECUTE);
		try {
			// if the bundle's startlevel is not already at the requested startlevel
			if (newSL != ((org.eclipse.osgi.framework.internal.core.AbstractBundle) bundle).getInternalStartLevel()) {
				final AbstractBundle b = (AbstractBundle) bundle;
				b.getBundleData().setStartLevel(newSL);
				try {
					AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
						public Object run() throws Exception {
							b.getBundleData().save();
							return null;
						}
					});
				} catch (PrivilegedActionException e) {
					if (e.getException() instanceof IOException) {
						throw (IOException) e.getException();
					}
					throw (RuntimeException) e.getException();
				}
				// handle starting or stopping the bundle asynchronously
				issueEvent(new StartLevelEvent(StartLevelEvent.CHANGE_BUNDLE_SL, newSL, (AbstractBundle) bundle));
			}
		} catch (IOException e) {
			framework.publishFrameworkEvent(FrameworkEvent.ERROR, bundle, e);
		}

	}

	/**
	 *  This method sends the StartLevelEvent to the EventManager for dispatching
	 * 
	 * @param sle The event to be queued to the Event Manager
	 */
	private void issueEvent(StartLevelEvent sle) {

		/* queue to hold set of listeners */
		ListenerQueue<Object, Object, StartLevelEvent> queue = new ListenerQueue<Object, Object, StartLevelEvent>(eventManager);

		/* add set of StartLevelListeners to queue */
		queue.queueListeners(startLevelListeners.entrySet(), this);

		/* dispatch event to set of listeners */
		queue.dispatchEventAsynchronous(sle.getType(), sle);
	}

	/**
	 * This method is the call back that is called once for each listener.
	 * This method must cast the EventListener object to the appropriate listener
	 * class for the event type and call the appropriate listener method.
	 *
	 * @param listener This listener must be cast to the appropriate listener
	 * class for the events created by this source and the appropriate listener method
	 * must then be called.
	 * @param listenerObject This is the optional object that was passed to
	 * EventListeners.addListener when the listener was added to the EventListeners.
	 * @param eventAction This value was passed to the ListenerQueue object via one of its
	 * dispatchEvent* method calls. It can provide information (such
	 * as which listener method to call) so that this method
	 * can complete the delivery of the event to the listener.
	 * @param event This object was passed to the ListenerQueue object via one of its
	 * dispatchEvent* method calls. This object was created by the event source and
	 * is passed to this method. It should contain all the necessary information (such
	 * as what event object to pass) so that this method
	 * can complete the delivery of the event to the listener.
	 */
	public void dispatchEvent(Object listener, Object listenerObject, int eventAction, StartLevelEvent event) {
		try {
			switch (eventAction) {
				case StartLevelEvent.CHANGE_BUNDLE_SL :
					setBundleSL(event);
					break;
				case StartLevelEvent.CHANGE_FW_SL :
					doSetStartLevel(event.getNewSL(), event.getListeners());
					break;
			}
		} catch (Throwable t) {
			// allow the adaptor to handle this unexpected error
			framework.adaptor.handleRuntimeError(t);
		}
	}

	/** 
	 *  Increment the active startlevel by one
	 */
	protected void incFWSL(int incToSL, AbstractBundle[] launchBundles) {
		if (Debug.DEBUG_STARTLEVEL) {
			Debug.println("SLL: incFWSL: saving activeSL of " + incToSL); //$NON-NLS-1$
		}
		// save the startlevel
		saveActiveStartLevel(incToSL);
		// resume all bundles at the startlevel
		resumeBundles(launchBundles, incToSL);
	}

	/**
	 * Build an array of all installed bundles to be launch.
	 * The returned array is sorted by increasing startlevel/id order.
	 * @param bundles - the bundles installed in the framework
	 * @return A sorted array of bundles 
	 */
	AbstractBundle[] getInstalledBundles(BundleRepository bundles, boolean sortByDependency) {

		/* make copy of bundles vector in case it is modified during launch */
		AbstractBundle[] installedBundles;

		synchronized (bundles) {
			List<AbstractBundle> allBundles = bundles.getBundles();
			installedBundles = new AbstractBundle[allBundles.size()];
			allBundles.toArray(installedBundles);

			/* sort bundle array in ascending startlevel / bundle id order
			 * so that bundles are started in ascending order.
			 */
			Util.sort(installedBundles, 0, installedBundles.length);
			if (sortByDependency)
				sortByDependency(installedBundles);
		}
		return installedBundles;
	}

	void sortByDependency(AbstractBundle[] bundles) {
		synchronized (framework.bundles) {
			if (bundles.length <= 1)
				return;
			int currentSL = bundles[0].getInternalStartLevel();
			int currentSLindex = 0;
			boolean lazy = false;
			for (int i = 0; i < bundles.length; i++) {
				if (currentSL != bundles[i].getInternalStartLevel()) {
					if (lazy)
						sortByDependencies(bundles, currentSLindex, i);
					currentSL = bundles[i].getInternalStartLevel();
					currentSLindex = i;
					lazy = false;
				}
				lazy |= (bundles[i].getBundleData().getStatus() & Constants.BUNDLE_LAZY_START) != 0;
			}
			// sort the last set of bundles
			if (lazy)
				sortByDependencies(bundles, currentSLindex, bundles.length);
		}
	}

	private void sortByDependencies(AbstractBundle[] bundles, int start, int end) {
		if (end - start <= 1)
			return;
		List<BundleDescription> descList = new ArrayList<BundleDescription>(end - start);
		List<AbstractBundle> missingDescs = new ArrayList<AbstractBundle>(0);
		for (int i = start; i < end; i++) {
			BundleDescription desc = bundles[i].getBundleDescription();
			if (desc != null)
				descList.add(desc);
			else
				missingDescs.add(bundles[i]);
		}
		if (descList.size() <= 1)
			return;
		BundleDescription[] descriptions = descList.toArray(new BundleDescription[descList.size()]);
		framework.adaptor.getPlatformAdmin().getStateHelper().sortBundles(descriptions);
		for (int i = start; i < descriptions.length + start; i++)
			bundles[i] = framework.bundles.getBundle(descriptions[i - start].getBundleId());
		if (missingDescs.size() > 0) {
			Iterator<AbstractBundle> missing = missingDescs.iterator();
			for (int i = start + descriptions.length; i < end && missing.hasNext(); i++)
				bundles[i] = missing.next();
		}
	}

	/**
	 *  Resume all bundles in the launch list at the specified start-level
	 * @param launch a list of Bundle Objects to launch
	 * @param currentSL the current start-level that the bundles must meet to be resumed
	 */
	private void resumeBundles(AbstractBundle[] launch, int currentSL) {
		// Resume all bundles that were previously started and whose startlevel is <= the active startlevel
		// first resume the lazy activated bundles
		resumeBundles(launch, true, currentSL);
		// now resume all non lazy bundles
		resumeBundles(launch, false, currentSL);
	}

	private void resumeBundles(AbstractBundle[] launch, boolean lazyOnly, int currentSL) {
		for (int i = 0; i < launch.length && !framework.isForcedRestart(); i++) {
			int bsl = launch[i].getInternalStartLevel();
			if (bsl < currentSL) {
				// skip bundles who should have already been started
				continue;
			} else if (bsl == currentSL) {
				if (Debug.DEBUG_STARTLEVEL) {
					Debug.println("SLL: Active sl = " + currentSL + "; Bundle " + launch[i].getBundleId() + " sl = " + bsl); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				boolean isLazyStart = launch[i].isLazyStart();
				if (lazyOnly ? isLazyStart : !isLazyStart)
					framework.resumeBundle(launch[i]);
			} else {
				// can stop resuming bundles since any remaining bundles have a greater startlevel than the framework active startlevel
				break;
			}
		}
	}

	/** 
	 *  Decrement the active startlevel by one
	 * @param decToSL -  the startlevel value to set the framework to
	 */
	protected void decFWSL(int decToSL, AbstractBundle[] shutdown) {
		if (Debug.DEBUG_STARTLEVEL) {
			Debug.println("SLL: decFWSL: saving activeSL of " + decToSL); //$NON-NLS-1$
		}

		saveActiveStartLevel(decToSL);

		// just decrementing the active startlevel - framework is not shutting down
		// Do not check framework.isForcedRestart here because we want to stop the active bundles regardless.
		for (int i = shutdown.length - 1; i >= 0; i--) {
			int bsl = shutdown[i].getInternalStartLevel();
			if (bsl > decToSL + 1)
				// skip bundles who should have already been stopped
				continue;
			else if (bsl <= decToSL)
				// stopped all bundles we are going to for this start level
				break;
			else if (shutdown[i].isActive()) {
				// if bundle is active or starting, then stop the bundle
				if (Debug.DEBUG_STARTLEVEL)
					Debug.println("SLL: stopping bundle " + shutdown[i].getBundleId()); //$NON-NLS-1$
				framework.suspendBundle(shutdown[i], false);
			}
		}
	}

	/**
	 * Stops the system bundle
	 */
	private void stopSystemBundle() {
		try {
			framework.systemBundle.context.stop();
		} catch (BundleException sbe) {
			if (Debug.DEBUG_STARTLEVEL) {
				Debug.println("SLL: Bundle suspend exception: " + sbe.getMessage()); //$NON-NLS-1$
				Debug.printStackTrace(sbe.getNestedException() == null ? sbe : sbe.getNestedException());
			}

			framework.publishFrameworkEvent(FrameworkEvent.ERROR, framework.systemBundle, sbe);
		}

		framework.systemBundle.state = Bundle.RESOLVED;
		framework.publishBundleEvent(BundleEvent.STOPPED, framework.systemBundle);
	}

	/**
	 *  Unloads all bundles in the vector passed in.
	 * @param bundles list of Bundle objects to be unloaded
	 */
	private void unloadAllBundles(BundleRepository bundles) {
		synchronized (bundles) {
			/* unload all installed bundles */
			List<AbstractBundle> allBundles = bundles.getBundles();
			int size = allBundles.size();

			for (int i = 0; i < size; i++) {
				AbstractBundle bundle = allBundles.get(i);

				if (Debug.DEBUG_STARTLEVEL) {
					Debug.println("SLL: Trying to unload bundle " + bundle); //$NON-NLS-1$
				}
				bundle.refresh();
				try {
					// make sure we close all the bundle data objects
					bundle.getBundleData().close();
				} catch (IOException e) {
					// ignore, we are shutting down anyway
				}
			}
		}
	}

	/** 
	 *  Set the bundle's startlevel to the new value
	 *  This may cause the bundle to start or stop based on the active framework startlevel
	 * @param startLevelEvent - the event requesting change in bundle startlevel
	 */
	protected void setBundleSL(StartLevelEvent startLevelEvent) {
		synchronized (lock) {
			int currentSL = getStartLevel();
			int newSL = startLevelEvent.getNewSL();
			AbstractBundle bundle = startLevelEvent.getBundle();

			if (Debug.DEBUG_STARTLEVEL) {
				Debug.print("SLL: bundle active=" + bundle.isActive()); //$NON-NLS-1$
				Debug.print("; newSL = " + newSL); //$NON-NLS-1$
				Debug.println("; activeSL = " + currentSL); //$NON-NLS-1$
			}

			if (bundle.isActive() && (newSL > currentSL)) {
				if (Debug.DEBUG_STARTLEVEL) {
					Debug.println("SLL: stopping bundle " + bundle.getBundleId()); //$NON-NLS-1$
				}
				framework.suspendBundle(bundle, false);
			} else {
				if (!bundle.isActive() && (newSL <= currentSL)) {
					if (Debug.DEBUG_STARTLEVEL) {
						Debug.println("SLL: starting bundle " + bundle.getBundleId()); //$NON-NLS-1$
					}
					framework.resumeBundle(bundle);
				}
			}
			if (Debug.DEBUG_STARTLEVEL) {
				Debug.println("SLL: Bundle Startlevel set to " + newSL); //$NON-NLS-1$
			}
		}
	}
}
