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

import java.util.EventListener;
import java.util.List;
import java.util.Vector;

import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;

/**
 * A <tt>StartLevel</tt> Event listener.
 *
 * <p><tt>StartLevelListener</tt> listens for StartLevelEvents and handles them by initiating 
 * changes in bundle or framework startlevels.
 * @see StartLevelEvent
 */
class StartLevelListener implements EventListener {
    /** bundle started first as the logservice bundle */
    protected Bundle logservice;

    /** lock to synchronize framework startlevel changes */
    private Object lock = new Object();

    private Framework framework;

    protected StartLevelListener() {
        super();
    }


    /** 
     *  Increment the active startlevel by one
     */
    protected void incFWSL(StartLevelEvent startLevelEvent) {
        synchronized (lock) {
            framework = startLevelEvent.getFramework();
            int activeSL = startLevelEvent.getSL();

			if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL) {
				Debug.println("SLL: incFWSL: saving activeSL of " + activeSL);
			}
            
            framework.startLevelImpl.saveActiveStartLevel(activeSL);
            
            Bundle [] launch;
            BundleRepository bundles = framework.bundles;

            launch = getInstalledBundles(bundles);

            if (activeSL==1) {  // framework was not active

                /* Load all installed bundles */
                loadInstalledBundles(launch);
                
                /* attempt to resolve all bundles */
				framework.packageAdmin.setResolvedBundles();

                /* Resume all bundles */
                resumeBundles(launch, true);


                /* publish the framework started event */
                if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL)
                {
                    Debug.println("SLL: Framework started");
                }

                framework.publishFrameworkEvent(FrameworkEvent.STARTED, startLevelEvent.getBundle(), null);

            } else {
                // incrementing an already active framework
                resumeBundles(launch, false);
            }
        }
    }

    /**
     * Build an array of all installed bundles to be launch.
     * The returned array is sorted by increasing startlevel/id order.
     * @param bundles - the bundles installed in the framework
     * @return A sorted array of bundles 
     */
    private Bundle[] getInstalledBundles(BundleRepository bundles) {

        /* make copy of bundles vector in case it is modified during launch */
        Bundle[] installedBundles;

        synchronized (bundles)
        {
        	List allBundles = bundles.getBundles();
            installedBundles = new Bundle[allBundles.size()];
            allBundles.toArray(installedBundles);

            /* sort bundle array in ascending startlevel / bundle id order
             * so that bundles are started in ascending order.
             */
            Util.sort(installedBundles, 0, installedBundles.length);
        }

        return installedBundles;
    }

    private void loadInstalledBundles(Bundle[] installedBundles) {
        logservice = null;

        for (int i = 0; i < installedBundles.length; i++)
        {
            Bundle bundle = installedBundles[i];

            try
            {
                if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL)
                {
                    Debug.println("SLL: Trying to load bundle "+bundle);
                }

                bundle.load();

                // TODO Do we really need to do this here?  This requries the loading of the manifest which
                // otherwise is not needed.  Removing for now.
                /* Locate logservice bundle */
//                if (logservice == null)
//                {
//                    String exportSpec = (String)bundle.getBundleData().getManifest().get(Constants.EXPORT_SERVICE);
//                    // TODO what parsing should go here?
//                    ManifestElement[] exportedServices = ManifestElement.parseBasicCommaSeparation(Constants.EXPORT_SERVICE, exportSpec);
//                    if (exportedServices != null)
//                    {
//                    	for (int j=0; j<exportedServices.length; j++){
//                    		if(exportedServices[j].getValue().equals(Constants.OSGI_LOGSERVICE_NAME)){
//                    			int status = bundle.bundledata.getStatus(); 
//								if ((status & Constants.BUNDLE_STARTED) != 0)
//								{
//									logservice = bundle;
//									break;
//								}
//                    		}
//                    	}
//                    }
//                }
            } catch (BundleException be)
            {
                if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL)
                {
                    Debug.println("SLL: Bundle load exception: "+be.getMessage());
                    Debug.printStackTrace(be.getNestedException());
                }

                framework.publishFrameworkEvent(FrameworkEvent.ERROR, bundle, be);
            }
        }
    }

    private void resumeBundles(Bundle[] launch, boolean launchingFW) {
        BundleException sbe = null;
        if (launchingFW) {
            /* Start the system bundle */

            try
            {
                framework.systemBundle.context.start();
            } catch (BundleException be)
            {
            	// TODO: We may have to do something more drastic here if the SystemBundle did not start.
                sbe = be;
            }


        }
        /* Resume all bundles that were previously started and whose startlevel is <= the active startlevel */
		
		int fwsl = framework.startLevelImpl.getStartLevel();
        for (int i = 0; i < launch.length; i++)
        {        	
        	int bsl = launch[i].startLevel;

            if (bsl < fwsl) {
                // skip bundles who should have already been started
                continue;
            } 
            else if (bsl==fwsl) {
				if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL) {
					Debug.println("SLL: Active sl = "+fwsl +
						"; Bundle "+launch[i].getBundleId()+" sl = "+bsl);
				}
				framework.resumeBundle(launch[i]);
            } else { 
		    // can stop resuming bundles since any remaining bundles have a greater startlevel than the framework active startlevel
               break;            
            }
        }

        if (sbe == null)
        {
            framework.systemBundle.state = Bundle.ACTIVE;
        } else
        {
            if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL)
            {
                Debug.println("SLL: Bundle resume exception: "+sbe.getMessage());
                Debug.printStackTrace(sbe.getNestedException());
            }

            framework.publishFrameworkEvent(FrameworkEvent.ERROR, framework.systemBundle, sbe);
        }
    }

    /** 
     *  Decrement the active startlevel by one
     * The startlevel value passed within the startLevelEvent is the
     * new desired active framework startlevel
     */
    protected void decFWSL(StartLevelEvent startLevelEvent) {
        synchronized (lock) {
            framework = startLevelEvent.getFramework();
            int activeSL = startLevelEvent.getSL();

			if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL) {
				Debug.println("SLL: decFWSL: saving activeSL of " + activeSL);
			}
            
            framework.startLevelImpl.saveActiveStartLevel(activeSL);
            
            BundleRepository bundles = framework.bundles;

            if (activeSL == 0) {  // stopping the framework


                framework.systemBundle.state = Bundle.STOPPING;

                /* stop all running bundles */

                suspendAllBundles(bundles);

                unloadAllBundles(bundles);

            } else {
                // just decrementing the active startlevel - framework is not shutting down
                synchronized (bundles) {
                    // get the list of installed bundles, sorted by startlevel
                    Bundle [] shutdown = this.getInstalledBundles(bundles);
                    for (int i = shutdown.length - 1; i >= 0; i--) {
                        int bsl = shutdown[i].startLevel;
                        if (bsl>activeSL+1) {
                            // don't need to mess with bundles with startlevel > the previous active - they should
                            // already have been stopped
                            continue;
                        } else if (bsl<=activeSL) {
                            // don't need to keep going - we've stopped all we're going to stop
                            break;      
                        } else if (shutdown[i].isActive() ) {
                            // if bundle is active or starting, then stop the bundle
                        	if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL) {
								Debug.println("SLL: stopping bundle " + shutdown[i].getBundleId());
                        	}                        	                        	                          
                            framework.suspendBundle(shutdown[i],false);
                        }
                    }
                }
            }
        }
    }

    private void suspendAllBundles(BundleRepository bundles) {
        synchronized (bundles) {
            boolean changed;
            do
            {
                changed = false;

                Bundle[] shutdown = this.getInstalledBundles(bundles);

                // shutdown all running bundles
                for (int i = shutdown.length - 1; i >= 0; i--)
                {
                    Bundle bundle = shutdown[i];


                        if (framework.suspendBundle(bundle, false))
                        {
                        	if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL) {
								Debug.println("SLL: stopped bundle " + bundle.getBundleId());
                        	}                            
                            changed = true;
                        }
                    
                }
            }
            while (changed);

            try
            {
                framework.systemBundle.context.stop();
            } catch (BundleException sbe)
            {
                if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL)
                {
                    Debug.println("SLL: Bundle suspend exception: "+sbe.getMessage());
                    Debug.printStackTrace(sbe.getNestedException());
                }

                framework.publishFrameworkEvent(FrameworkEvent.ERROR, framework.systemBundle, sbe);
            }

            framework.systemBundle.state = Bundle.STARTING;
        }
    }

    private void unloadAllBundles(BundleRepository bundles) {
        synchronized (bundles)
        {
            /* unload all installed bundles */
        	List allBundles = bundles.getBundles();
            int size = allBundles.size();

            for (int i = 0; i < size; i++)
            {
                Bundle bundle = (Bundle)allBundles.get(i);

                if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL)
                {
                    Debug.println("SLL: Trying to unload bundle "+bundle);
                }

                try {
					bundle.refresh();
				} catch (BundleException e) {
					// do nothing.
				}
            }
        }
    }

    /** 
     *  Set the bundle's startlevel to the new value
     *  This may cause the bundle to start or stop based on the active framework startlevel
     */
    protected void setBundleSL(StartLevelEvent startLevelEvent) {
        synchronized (lock) {
            framework = startLevelEvent.getFramework();
            int activeSL = framework.startLevelImpl.getStartLevel();
            int newSL = startLevelEvent.getSL();
            Bundle bundle = startLevelEvent.getBundle();

            int bundlestate = bundle.getState();
            if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL) {
            	Debug.print("SLL: bundle active="+bundle.isActive());
				Debug.print("; newSL = " + newSL);
				Debug.println("; activeSL = " + activeSL);
            }
            
            if (bundle.isActive() && (newSL>activeSL)) {
            	if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL) {
					Debug.println("SLL: stopping bundle "+bundle.getBundleId());
            	}                
                framework.suspendBundle(bundle, false);
            } else {
                if ( !bundle.isActive() && (newSL<=activeSL) ) {
					if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL) {
                        Debug.println("SLL: starting bundle "+bundle.getBundleId());
					}
                    framework.resumeBundle(bundle);
                }
            }
			if (Debug.DEBUG && Debug.DEBUG_STARTLEVEL) {
                Debug.println("SLL: Bundle Startlevel set to "+newSL);
			}
        }
    }

}
