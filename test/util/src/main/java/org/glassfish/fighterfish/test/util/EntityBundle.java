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

import java.util.concurrent.TimeUnit;

/**
 * A class that helps tests in the deployment of entity bundles. An entity bundle is a bundle containing
 * JPA entities which upon successful deployment registers a service of type EntityManagerFactory.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class EntityBundle {
    private Bundle b;
    private BundleContext ctx;
    private String[] services = {"javax.persistence.EntityManagerFactory"};

    /**
     * Create a new EntityBundle
     * @param ctx BundleContext of the test - this is not the bundle context of the entity bundle being deployed.
     * @param b EntityBundle being deployed.
     */
    public EntityBundle(BundleContext ctx, Bundle b) {
        this.b = b;
        this.ctx = ctx;
    }

    /**
     * Deploy this entity bundle. If a service of type EntityManagerFactory does not get registered in
     * the specified time, assume the deployment has failed and throw a TimeoutException.
     * @param timeout
     * @param timeUnit
     * @throws BundleException
     * @throws InterruptedException
     */
    public void deploy(long timeout, TimeUnit timeUnit) throws BundleException, InterruptedException {
        b.start(Bundle.START_TRANSIENT);
        for (String service : services) {
            if (OSGiUtil.waitForService(ctx, b, service, timeUnit.toMillis(timeout)) == null) {
                throw new TimeoutException("Deployment timed out. No service of type " + service + " found.");
            }
        }
    }

    public void undeploy() throws BundleException {
        b.stop();
    }

    public Bundle getBundle() {
        return b;
    }
}
