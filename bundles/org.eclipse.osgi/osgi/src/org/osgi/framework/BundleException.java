/*
 * $Header: /home/technology/org.eclipse.equinox/plugins/org.eclipse.osgi/osgi/src/org/osgi/framework/BundleException.java,v 1.1 2003/11/10 17:49:34 jeff Exp $
 *
 * Copyright (c) The Open Services Gateway Initiative (2000-2001).
 * All Rights Reserved.
 *
 * Implementation of certain elements of the Open Services Gateway Initiative
 * (OSGI) Specification may be subject to third party intellectual property
 * rights, including without limitation, patent rights (such a third party may
 * or may not be a member of OSGi). OSGi is not responsible and shall not be
 * held responsible in any manner for identifying or failing to identify any or
 * all such third party intellectual property rights.
 *
 * This document and the information contained herein are provided on an "AS
 * IS" basis and OSGI DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO ANY WARRANTY THAT THE USE OF THE INFORMATION HEREIN WILL
 * NOT INFRINGE ANY RIGHTS AND ANY IMPLIED WARRANTIES OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT WILL OSGI BE LIABLE FOR ANY
 * LOSS OF PROFITS, LOSS OF BUSINESS, LOSS OF USE OF DATA, INTERRUPTION OF
 * BUSINESS, OR FOR DIRECT, INDIRECT, SPECIAL OR EXEMPLARY, INCIDENTIAL,
 * PUNITIVE OR CONSEQUENTIAL DAMAGES OF ANY KIND IN CONNECTION WITH THIS
 * DOCUMENT OR THE INFORMATION CONTAINED HEREIN, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH LOSS OR DAMAGE.
 *
 * All Company, brand and product names may be trademarks that are the sole
 * property of their respective owners. All rights reserved.
 */

package org.osgi.framework;

/**
 * A Framework exception used to indicate that a bundle lifecycle problem occurred.
 *
 * <p><tt>BundleException</tt> object is created by the Framework to denote an exception condition
 * in the lifecycle of a bundle.
 * <tt>BundleException</tt>s should not be created by bundle developers.
 *
 * @version $Revision: 1.1 $
 * @author Open Services Gateway Initiative
 */

public class BundleException extends Exception
{
    /**
     * Nested exception.
     */
    private transient Throwable throwable;

    /**
     * Creates a <tt>BundleException</tt> that wraps another exception.
     *
     * @param msg The associated message.
     * @param throwable The nested exception.
     */
    public BundleException(String msg, Throwable throwable)
    {
        super(msg);
        this.throwable = throwable;
    }

    /**
     * Creates a <tt>BundleException</tt> object with the specified message.
     *
     * @param msg The message.
     */
    public BundleException(String msg)
    {
        super(msg);
        this.throwable = null;
    }

    /**
     * Returns any nested exceptions included in this exception.
     *
     * @return The nested exception; <tt>null</tt> if there is
     * no nested exception.
     */
    public Throwable getNestedException()
    {
        return(throwable);
    }
}


