/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.core;

import java.io.IOException;

import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.eventmgr.EventListeners;
import org.eclipse.osgi.framework.eventmgr.EventManager;
import org.eclipse.osgi.framework.eventmgr.EventQueue;
import org.eclipse.osgi.framework.eventmgr.EventSource;
import org.osgi.framework.FrameworkEvent;

/**
 * StartLevel service implementation for the OSGi specification.
 *
 * Framework service which allows management of framework and bundle startlevels.
 *
 * If present, there will only be a single instance of this service
 * registered in the framework.
 *
 */
public class StartLevelImpl implements EventSource {

    protected static Framework framework;
    protected static EventManager eventManager;
    protected static EventListeners startLevelListeners;
    protected static StartLevelListener startLevelListener;

	/** The framework beginning startlevel.  Default is 1 */
	protected int frameworkBeginningStartLevel = 1;

	/** The initial bundle start level for newly installed bundles */
	protected int initialBundleStartLevel = 1;
	// default value is 1 for compatibility mode

    /** The currently active framework start level */
    private static int activeSL=0;

    /** The requested framework start level */
    private static int requestedSL=0;

    /** An object used to lock the active startlevel while it is being referenced */
    private static final Object lock = new Object();

    /** This constructor is called by the Framework */
    protected StartLevelImpl (Framework framework) {
        StartLevelImpl.framework = framework;
    }

    protected void initialize() {
        initialBundleStartLevel = framework.adaptor.getInitialBundleStartLevel();

		// Set Framework Beginning Start Level Property
		String value = framework.getProperty(Constants.KEY_FRAMEWORKBEGINNINGSTARTLEVEL);
		if (value == null) {
			value = Constants.DEFAULT_STARTLEVEL;
		} else {
			try {
				if (Integer.parseInt(value) <= 0) {
					System.err.println(Msg.formatter.getString("PROPERTIES_INVALID_FW_STARTLEVEL", Constants.DEFAULT_STARTLEVEL));
					value = Constants.DEFAULT_STARTLEVEL;
				}
			} catch (NumberFormatException nfe) {
				System.err.println(Msg.formatter.getString("PROPERTIES_INVALID_FW_STARTLEVEL", Constants.DEFAULT_STARTLEVEL));
				value = Constants.DEFAULT_STARTLEVEL;
			}
		}
		framework.setProperty(Constants.KEY_FRAMEWORKBEGINNINGSTARTLEVEL, value);
		frameworkBeginningStartLevel = Integer.parseInt(value);
       
        
        // create an event manager and a start level listener
        eventManager = new EventManager("Start Level Event Dispatcher");

        startLevelListener = new StartLevelListener();
        startLevelListeners = new EventListeners();
        startLevelListeners.addListener(startLevelListener, startLevelListener);            
    }

    protected void cleanup() {
        eventManager = null;
        startLevelListeners.removeAllListeners();
        startLevelListeners = null;
        startLevelListener = null;
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
		 * Return the initial start level used when the framework is started.
		 *
		 * @return The framework start level.
		 */
	public int getFrameworkStartLevel() {
		return frameworkBeginningStartLevel;
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
        framework.checkAdminPermission();
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
         * @param startlevel The requested start level for the Framework.
         * @throws IllegalArgumentException If the specified start level is less than or
         * equal to zero.
         * @throws SecurityException If the caller does not have the
         * <tt>AdminPermission</tt> and the Java runtime environment supports
         * permissions.
         */
    public void setStartLevel(int newSL, org.osgi.framework.Bundle callerBundle) {
        if (newSL <= 0) {
            throw new IllegalArgumentException(Msg.formatter.getString("STARTLEVEL_EXCEPTION_INVALID_REQUESTED_STARTLEVEL", ""+newSL));
        }
        framework.checkAdminPermission();

		if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL){
			Debug.println("StartLevelImpl: setStartLevel: "+newSL+"; callerBundle = "+callerBundle.getBundleId());				
		}
		doSetStartLevel(newSL, false, (Bundle)callerBundle);
        
    }
    
    protected void setStartLevel(int newSL) {
        setStartLevel(newSL, framework.systemBundle);
    }

    /**
     *  Internal method to allow the framework to be launched synchronously by calling the
     *  StartLevelListener worker calls directly
     *
     *  This method does not return until all bundles that should be started are started
     */
    protected void launch(int startlevel) {

        doSetStartLevel(startlevel, true, framework.systemBundle);
    }

    /**
     *  Internal method to shut down the framework synchronously by setting the startlevel to zero
     *  and calling the StartLevelListener worker calls directly
     *
     *  This method does not return until all bundles are stopped and the framework is shut down.
     */
    protected void shutdown() {

        doSetStartLevel(0, true, framework.systemBundle);
    }

    /**
     *  Internal worker method to set the startlevel
     *
     * @param new start level value                  
     * @param boolean - true if start level change should be done synchronously, false for asynchronously
     */
    private void doSetStartLevel (int newSL, boolean sync, Bundle callerBundle) {
        
        int tempSL = activeSL;
        
        if (sync) {
            if (newSL > tempSL) {
                for (int i=tempSL; i < newSL; i++) {
                	if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL) {
						Debug.println("sync - incrementing Startlevel from "+tempSL);
                	}
                    
                    tempSL++;
                    
                    startLevelListener.incFWSL(new StartLevelEvent(StartLevelEvent.INC_FW_SL, i+1, newSL, callerBundle, framework));
                }
            } else {
                for (int i=tempSL; i > newSL; i--) {
					if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL) {
                        Debug.println("sync - decrementing Startlevel from "+tempSL);
					}
                    tempSL--;                    

                    startLevelListener.decFWSL(new StartLevelEvent(StartLevelEvent.DEC_FW_SL, i-1, newSL, callerBundle, framework));
                }
            }
        } else {
            if (newSL > tempSL) {
                for (int i=tempSL; i < newSL; i++) {
                	if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL) {
                        Debug.println("async - incrementing Startlevel from "+tempSL);
                	}
                    tempSL++;

                    // do the startlevel change asynchronously
                    issueEvent(new StartLevelEvent(StartLevelEvent.INC_FW_SL, i+1, newSL, callerBundle, framework));
                }
            } else {
                for (int i=tempSL; i > newSL; i--) {
					if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL) {
                        Debug.println("async - decrementing Startlevel from "+tempSL);
					}
                    tempSL--;                    
                    
                    // do the startlevel change asynchronously
                    issueEvent(new StartLevelEvent(StartLevelEvent.DEC_FW_SL, i-1, newSL, callerBundle, framework));
                }
            }
        }
        framework.publishFrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, callerBundle, null);
		if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL) {
            Debug.println("StartLevelImpl: doSetStartLevel: STARTLEVEL_CHANGED event published");
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

        if (bundle.getState() == Bundle.UNINSTALLED) {
            throw new IllegalArgumentException (Msg.formatter.getString("BUNDLE_UNINSTALLED_EXCEPTION"));
        }
        Bundle b = (Bundle) bundle;
        int status = b.bundledata.getStatus();
        return((status & org.eclipse.osgi.framework.internal.core.Constants.BUNDLE_STARTED) == Constants.BUNDLE_STARTED);
    }
    /**
         * Return the assigned start level value for the specified Bundle.
         *
         * @param bundle The target bundle.
         * @return The start level value of the specified Bundle.
         * @exception java.lang.IllegalArgumentException If the specified bundle has been uninstalled.
         */
    public int getBundleStartLevel(org.osgi.framework.Bundle bundle) {

        if (bundle.getState() == Bundle.UNINSTALLED) {
            throw new IllegalArgumentException (Msg.formatter.getString("BUNDLE_UNINSTALLED_EXCEPTION"));
        }
        return((Bundle)bundle).startLevel;
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
         * @param startlevel The new start level for the specified Bundle.
         * @throws IllegalArgumentException
         * If the specified bundle has been uninstalled or
         * if the specified start level is less than or equal to zero, or the  specified bundle is
         * the system bundle.
         * @throws SecurityException if the caller does not have the
         * <tt>AdminPermission</tt> and the Java runtime environment supports
         * permissions.
         */
    public void setBundleStartLevel(org.osgi.framework.Bundle bundle, int newSL) {

        String exceptionText="";
        if (bundle.getBundleId()==0) {  // system bundle has id=0
            exceptionText = Msg.formatter.getString("STARTLEVEL_CANT_CHANGE_SYSTEMBUNDLE_STARTLEVEL");
        } else if (bundle.getState() == Bundle.UNINSTALLED) {
            exceptionText = Msg.formatter.getString("BUNDLE_UNINSTALLED_EXCEPTION");
        } else if (newSL <= 0) {
            exceptionText = Msg.formatter.getString("STARTLEVEL_EXCEPTION_INVALID_REQUESTED_STARTLEVEL", ""+newSL);
        }
        if (exceptionText.length()>0) {
            throw new IllegalArgumentException(exceptionText);
        }

        try {
            // if the bundle's startlevel is not already at the requested startlevel
            if (newSL != ((org.eclipse.osgi.framework.internal.core.Bundle)bundle).startLevel) {
				Bundle b = (Bundle) bundle;
				b.bundledata.setStartLevel(newSL);
                b.bundledata.save();
                ((org.eclipse.osgi.framework.internal.core.Bundle)bundle).startLevel = newSL;

                framework.checkAdminPermission();

                // handle starting or stopping the bundle asynchronously
                issueEvent(new StartLevelEvent(StartLevelEvent.CHANGE_BUNDLE_SL, newSL, newSL, (Bundle)bundle, framework));
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
        EventQueue queue = new EventQueue(eventManager);

        /* add set of UserAdminListeners to queue */
        queue.queueListeners(startLevelListeners, this);

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
     * ListenerList.addListener when the listener was added to the ListenerList.
     * @param eventAction This value was passed to the EventQueue object via one of its
     * dispatchEvent* method calls. It can provide information (such
     * as which listener method to call) so that this method
     * can complete the delivery of the event to the listener.
     * @param eventObject This object was passed to the EventQueue object via one of its
     * dispatchEvent* method calls. This object was created by the event source and
     * is passed to this method. It should contain all the necessary information (such
     * as what event object to pass) so that this method
     * can complete the delivery of the event to the listener.
     */
    public void dispatchEvent(Object listener, Object listenerObject, int eventAction, Object eventObject) {
        StartLevelListener sll = (StartLevelListener)listener;
        switch (eventAction) {
        case StartLevelEvent.INC_FW_SL:
            sll.incFWSL((StartLevelEvent)eventObject);
            break;
        case StartLevelEvent.DEC_FW_SL:
            sll.decFWSL((StartLevelEvent)eventObject);
            break;
        case StartLevelEvent.CHANGE_BUNDLE_SL:
            sll.setBundleSL((StartLevelEvent)eventObject);
            break;
        }
    }




}

