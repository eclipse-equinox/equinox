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

import java.util.EventObject;

/**
 * StartLevel Event for the OSGi framework.
 *
 * Event which signifies that a start level change has been requested for the framework or for a bundle.
 *
 */
class StartLevelEvent extends EventObject {
    
    public final static int INC_FW_SL = 0x00000000;
    public final static int DEC_FW_SL = 0x00000001;
    public final static int CHANGE_BUNDLE_SL = 0x00000002;
    
    /**
     * Event Type
     */
    private transient int type;

    /**
     * StartLevel - value depends on event type: 
     *  INC_FW_SL - value is the current active startlevel to be incremented
     *  DEC_FW_SL - value is the current active startlevel to be decremented
     *  CHANGE_BUNDLE_SL - value is the new bundle startlevel
     * 
     */
    private transient int sl;

    /**
     * The final target startlevel - when this is reached, a FrameworkEvent.STARTLEVEL_CHANGED event must be published
     */
    private transient int finalSL;
    
    /**
     * Bundle related to the event.
     * bugbug If the bundle is the System Bundle, then it is a change to the Framework active startlevel.  
     * Otherwise it is a change to the specified bundle.
     */
    private transient Bundle bundle;
    
    /**
     * Framework object
     */
    private transient Framework framework;
    
    
    
    /**
     * Creates a StartLevel event regarding the specified bundle.
     *
     * @param int type The type of startlevel event (inc or dec)
     * @param int sl The next requested startlevel (the interim startlevel)
     * @param int finalSL the ultimate requested startlevel we are on our way to
     * @param bundle The affected bundle, or system bundle if it is for the framework
     * @param framework The framework object so we can retrieve things like active startlevel
     */
    public StartLevelEvent(int type, int sl, int finalSL, Bundle bundle, Framework framework)
    {
        super(bundle);
        this.type = type;
        this.sl = sl;
        this.finalSL = finalSL;
        this.bundle = bundle;
        this.framework = framework;
    }
    
    public int getType() {
        return this.type;
    }
    
    public int getSL() {
        return this.sl;
    }
    
    public int getFinalSL() {
        return this.finalSL;
    }
    
    public Bundle getBundle() {
        return this.bundle;
    }
    
    public Framework getFramework() {
        return this.framework;
    }
    
}
