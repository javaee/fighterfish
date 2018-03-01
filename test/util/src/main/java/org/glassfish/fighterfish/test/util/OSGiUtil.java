/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */


package org.glassfish.fighterfish.test.util;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class OSGiUtil {
    // TODO(Sahoo): Move the functionality to TestContext so that the service references can be closed upon end of test
    public static <T> T getService(BundleContext ctx, Class<T> type) {
        ServiceTracker st = new ServiceTracker(ctx, type.getName(), null);
        st.open();
        try {
            return type.cast(st.getService());
        } finally {
//            st.close();
        }
    }

    public static <T> T getService(BundleContext ctx, Class<T> type, long timeout) throws InterruptedException {
        ServiceTracker st = new ServiceTracker(ctx, type.getName(), null);
        st.open();
        try {
            return type.cast(st.waitForService(timeout));
        } finally {
//            st.close();
        }
    }

    /**
     * Wait for a specified amount of time for a service of a given type to be made available by a given bundle.
     *
     * @param ctx BundleContext that should be used to track the service
     * @param b Bundle registering the service
     * @param service FQN of the service type
     * @param timeout no of milliseconds to wait for the service to be available before returning null
     * @return a reference to the service being tracked
     * @throws InterruptedException
     */
    public static Object waitForService(BundleContext ctx, final Bundle b, String service, long timeout) throws InterruptedException {
        ServiceTracker st = new ServiceTracker(ctx, service, null){
            @Override
            public Object addingService(ServiceReference reference) {
                if (reference.getBundle() == b) {
                    return reference;
                } else {
                    return null;
                }
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                // no need to unget, as we don't get the service in addingService
            }
        };
        st.open(false);
        Object s;
        try {
            s = st.waitForService(timeout);
        } finally {
//            st.close();
        }
        return s;
    }
}
