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

package org.glassfish.fighterfish.sample.testing.surefire;

import org.glassfish.fighterfish.test.util.FighterFishJUnitRunner;
import org.glassfish.fighterfish.test.util.TestContext;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;


/**
 * This sample test demonstrates use of a custom JUnit test runner called {@link FighterFishJUnitRunner}
 * to execute a JUnit test using maven surefire plugin. The test name is suffixed with Test so as to be
 * automatically included by maven surefire plugin.
 *
 * The custom runner has the ability to provision GlassFish, which includes downloading of the GlassFish bundles,
 * installing the smae and bootstrapping GlassFish inside or outside the current JVM. All these steps are
 * pretty configurable via various configuration options specified as system properties. See the pom.xml
 * to see various configuration options. If you chose to control those options from code, then you can provide
 * in a method in test class annotated with @Configuration.
 *
 * @author sanjeeb.sahoo@oracle.com
 */
@RunWith(FighterFishJUnitRunner.class)
public class FighterFishJUnitRunnerTest {
    /**
     * This is how one can inject BundleContext. In fact, one can even inject provisioned services.
     */
    @Inject
    private BundleContext ctx;

	@Test
	public void test() throws Exception {
		System.out.println("FighterFishJUnitRunnerTest.test()");
		TestContext tc = TestContext.create(getClass());
		try {
            Assert.assertSame(ctx, tc.getBundleContext());
            System.out.println("tc.getBundleContext() = " + tc.getBundleContext());
			System.out.println(tc.getGlassFish());
		} finally {
			tc.destroy();
		}
	}

    @Test
    public void test2() throws Exception {
        System.out.println("FighterFishJUnitRunnerTest.test2()");
        TestContext tc = TestContext.create(getClass());
        try {
            System.out.println("tc.getBundleContext() = " + tc.getBundleContext());
            System.out.println(tc.getGlassFish());
        } finally {
            tc.destroy();
        }
    }
}
