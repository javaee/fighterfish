/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.fighterfish.test.app18;

import org.glassfish.osgicdi.OSGiService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

/**
 * Session Bean implementation class ServiceListenerEJB
 */
@Singleton
@Startup
@DependsOn("EjbLifecycleObserverEJB")
public class ServiceListenerEJB {

    @Inject BundleContext bundleCtx;
    
    @Inject 
    MyServiceListener listener;
    
    @PostConstruct
    public void installListener() {
        String filter = "(" + Constants.OBJECTCLASS + "=" + Foo.class.getName() + ")";
        try {
            bundleCtx.addServiceListener(listener, filter);
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void uninstallListener() {
        bundleCtx.removeServiceListener(listener);
        System.out.println("ServiceListenerEJB.uninstallListener() " + "Removed service listener " + listener);
    }

    public static class MyServiceListener implements ServiceListener 
    {
        @Inject @OSGiService(dynamic=true)
        private EjbLifecycleObserver observer;
        public synchronized void serviceChanged(ServiceEvent event) {
            System.out.println(getClass().getName() + ".serviceChanged() " + event);
            switch (event.getType()) {
                case ServiceEvent.REGISTERED:
                    observer.registered(Foo.class.getName());
                    break;
            }
        }
    }

}
