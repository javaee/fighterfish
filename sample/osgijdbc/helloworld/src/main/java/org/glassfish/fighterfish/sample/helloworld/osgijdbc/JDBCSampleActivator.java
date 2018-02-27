/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2018 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.fighterfish.sample.helloworld.osgijdbc;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class JDBCSampleActivator implements BundleActivator {

	private static final String DSNAME = "jdbc/__default"; // Should be a configurable property
	private ServiceTracker st;

	@Override
	public void start(BundleContext context) throws Exception {
		debug("Activator started");

		// Create an LDAP filter which matches both the interface type
		// as well as jndi-name property.
		Filter filter = context.createFilter("(&" + "(" + Constants.OBJECTCLASS
				+ "=" + DataSource.class.getName() + ")" + "(jndi-name="
				+ DSNAME + ")" + ")");
		st = new ServiceTracker(context, filter, null) {

			@Override
			public Object addingService(ServiceReference reference) {
				DataSource ds = (DataSource) context.getService(reference);
				try {
					debug(ds.getConnection().toString());
				} catch (SQLException e) {
					e.printStackTrace();
				}

				return super.addingService(reference);
			}

			@Override
			public void removedService(ServiceReference reference,
					Object service) {
				super.removedService(reference, service);
			}

		};
		st.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		st.close();
		debug("Activator stopped");
	}

	private void debug(String msg) {
		System.out.println("JDBCTestBundleActivator: " + msg);
	}

}
