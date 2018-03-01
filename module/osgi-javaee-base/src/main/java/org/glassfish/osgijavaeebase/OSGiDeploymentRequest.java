/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.osgijavaeebase;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.WritableArchive;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.server.ServerEnvironmentImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a stateful service. This is responsible for deployment
 * of artifacts in JavaEE runtime.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public abstract class OSGiDeploymentRequest
{
    private static final Logger logger =
            Logger.getLogger(OSGiUndeploymentRequest.class.getPackage().getName());

    private ActionReport reporter;
    private Bundle b;
    private boolean dirDeployment;
    private Deployment deployer;
    private ArchiveFactory archiveFactory;
    private ServerEnvironmentImpl env;
    private ReadableArchive archive;
    private OSGiDeploymentContext dc;
    private OSGiApplicationInfo result;

    public OSGiDeploymentRequest(Deployment deployer,
                                   ArchiveFactory archiveFactory,
                                   ServerEnvironmentImpl env,
                                   ActionReport reporter,
                                   Bundle b)
    {
        this.deployer = deployer;
        this.archiveFactory = archiveFactory;
        this.env = env;
        this.reporter = reporter;
        this.b = b;
    }

    protected void preDeploy() throws DeploymentException {}

    protected void postDeploy() {}

    /**
     * Deploys a web application bundle in GlassFish Web container.
     * It properly rolls back if something goes wrong.
     * @return OSGIApplicationInfo
     */
    public OSGiApplicationInfo execute()
    {
        try {
            preDeploy();
        } catch (DeploymentException e) {
            reporter.failure(logger,
                    "Failed while deploying bundle " + b, e);
            return result; // return without calling postDeploy()
        }
        // This is where the fun is...
        try
        {
            prepare();
            result = deploy(); // Now actual deployment begins
        }
        catch (Exception e)
        {
            reporter.failure(logger,
                    "Failed while deploying bundle " + b, e);
        } finally {
            postDeploy(); // call even if something failed so that the actions in predeploy() can be rolled back.
        }
        return result;
    }

    private void prepare() throws Exception
    {
        // The steps are described below:
        // 1. Create an Archive from the bundle
        //    - If the bundle has been installed with reference: scheme,
        //    get hold hold of the underlying file and read from it, else
        //    use the bundle directly to create the archive.
        // 2. Prepare a context for deployment. This includes setting up
        // various deployment options, setting up of an ArchiveHandler,
        // expansion of the archive, etc. The archive needs to be expanded before we create
        // deployment context, because in order to create WABClassLoader, we need to know
        // expansion directory location, so that we can configure the repositories correctly.
        // More over, we need to create the deployment options before expanding the archive, because
        // we set application name = archive.getName(). If we explode first and then create OpsParams, then
        // we will end up using the same name as used by "asadmin deploy --type=osgi" and eventually hit by
        // issue #10536.
        // 3. Finally deploy and store the result in our inmemory map.

        archive = makeArchive();

        // Set up a deployment context
        OpsParams opsParams = getDeployParams();

        // expand if necessary, else set directory deployment to true
        expandIfNeeded();

        dc = getDeploymentContextImpl(
                reporter,
                logger,
                archive,
                opsParams,
                env,
                b);
    }

    /**
     * Factory method. Subclasses override this to create specialised Archive instance.
     * @return
     */
    protected ReadableArchive makeArchive() {
        return new OSGiBundleArchive(b);
    }

    protected abstract OSGiDeploymentContext getDeploymentContextImpl(ActionReport reporter, Logger logger, ReadableArchive archive, OpsParams opsParams, ServerEnvironmentImpl env, Bundle b) throws Exception;

    private OSGiApplicationInfo deploy()
    {
        // Need to declare outside to do proper cleanup of target dir
        // when deployment fails. We can't rely on exceptions as
        // deployer.deploy() eats most of the exceptions.
        ApplicationInfo appInfo = null;
        try
        {
            appInfo = deployer.deploy(dc);
            if (appInfo != null)
            {
                // Pass in the final classloader so that it can be used to set appropriate context
                // while using underlying EE components in pure OSGi context like registering EJB as services.
                // This won't be needed if we figure out a way of navigating to the final classloader from
                // an EE component like EJB.
                return new OSGiApplicationInfo(appInfo, dirDeployment, b, dc.getFinalClassLoader());
            }
            else
            {
                logger.logp(Level.FINE, "OSGiDeploymentRequest",
                        "deploy", "failed to deploy {0} for following reason: {1} ", new Object[]{b, reporter.getMessage()});
                throw new RuntimeException("Failed to deploy bundle [ " + b + " ], root cause: " + reporter.getMessage(),
                        reporter.getFailureCause());
            }
        }
        finally
        {
            if (!dirDeployment && appInfo == null)
            {
                try
                {
                    File dir = dc.getSourceDir();
                    assert (dir.isDirectory());
                    if (FileUtils.whack(dir)) {
                        logger.logp(Level.INFO, "OSGiDeploymentRequest", "deploy",
                                "Deleted {0}", new Object[]{dir});
                    } else {
                        logger.logp(Level.WARNING, "OSGiDeploymentRequest", "deploy", "Unable to delete {0} ", new Object[]{dir});
                    }
                }
                catch (Exception e2)
                {
                    logger.logp(Level.WARNING, "OSGiDeploymentRequest", "deploy",
                            "Exception while cleaning up target directory.", e2);
                    // don't throw this anymore
                }
            }
        }
    }

    private void expandIfNeeded() throws IOException
    {
        // Try to obtain a handle to the underlying archive.
        // First see if it is backed by a file or a directory, else treat
        // it as a generic bundle.
        File file = makeFile(archive);

        // expand if necessary, else set directory deployment to true
        dirDeployment = file != null && file.isDirectory();
        if (dirDeployment)
        {
            logger.logp(Level.FINE, "OSGiDeploymentRequest", "expandIfNeeded",
                    "Archive is already expanded at = {0}", new Object[]{file});
            archive = archiveFactory.openArchive(file);
            return;
        }

        // ok we need to explode the archive somwhere and
        // remember to delete it on shutdown
        File tmpFile = getExplodedDir();
        tmpFile.deleteOnExit();

        // We don't reuse old directory at this point of time as we are not completely sure if it is safe to reuse.
        // It is possible that we had a semicomplete directory from previous execution.
        // e.g., if glassfish can be killed while osgi-container is exploding the archive.
        // or glassfish can be killed while it was in the middle of deleting the previously exploded directory.
        // So, better to not reuse existing directory until we ensure that explosion or removal are atomic operations.
        if (tmpFile.exists()) {
            logger.logp(Level.INFO, "OSGiDeploymentRequest", "expandIfNeeded",
                    "Going to delete existing exploded content found at {0}", new Object[]{tmpFile});
            if (!FileUtils.whack(tmpFile)) {
                throw new IOException("Unable to delete directory wth name " + tmpFile);
            }
        }
        if (!tmpFile.mkdirs())
        {
            throw new IOException("Not able to expand " + archive.getName() + " in " + tmpFile);
        }
        WritableArchive targetArchive = archiveFactory.createArchive(tmpFile);
        new OSGiArchiveHandler().expand(archive, targetArchive, dc);
        logger.logp(Level.INFO, "OSGiDeploymentRequest", "expandIfNeeded",
                "Expanded at {0}", new Object[]{targetArchive.getURI()});
        archive = archiveFactory.openArchive(tmpFile);
    }
    
    /**
     * We don't keep the file in tmpdir, because in some deployment environment, the tmpdir is periodically cleaned
     * up by external programs to reclaim memory. So, we keep them under application dir in bundle private storage area.
     * More over, we need to make sure that the directory is named uniquely to avoid accidental reuse.
     * So, we include Bundle-Id and Bundle-LastModifiedTimestamp in the dir name.
     * 
     * @return the directory where the OSGi application will be exploded during deployment
     */
    private File getExplodedDir() {
    	BundleContext ctx = getBundleContext(this.getClass());
    	File bundleBaseStorage = ctx.getDataFile("");
    	// We keep everything under application directory.
    	// We include Bundle-Id and Bundle-Timestamp to ensure that we don't accidentally use stale contents.
    	// Assume the following where stale contents can be there in a file if we don't use timestamp in name:
    	// a wab deployed. Now system is stopped forcefully. During next restart osgi-webcontainer
    	// is not started, but wab is updated. Next time, osgi-web-container comes up, it should not end up using
    	// previously exploded directory.
    	final String name = "applications" + File.separator + "bundle" + b.getBundleId() + "-" + b.getLastModified();
    	return new File(bundleBaseStorage, name);
    }

    /**
     * @param a The archive
     * @return a File object that corresponds to this archive.
     * return null if it can't determine the underlying file object.
     */
    public static File makeFile(ReadableArchive a)
    {
        try
        {
            return new File(a.getURI());
        }
        catch (Exception e)
        {
            // Ignore, if we can't convert
        }
        return null;
    }

    protected DeployCommandParameters getDeployParams() throws Exception
    {
        assert(archive != null);
        DeployCommandParameters parameters = new DeployCommandParameters();
        parameters.name = archive.getName();
        parameters.enabled = Boolean.TRUE;
        parameters.origin = DeployCommandParameters.Origin.deploy;
        parameters.force = false;
        parameters.target = getInstanceName();
        return parameters;
    }

    public Bundle getBundle() {
        return b;
    }

    public ReadableArchive getArchive() {
        return archive;
    }

    public OSGiApplicationInfo getResult() {
        return result;
    }

    private String getInstanceName() {
        ServerEnvironment se = Globals.get(ServerEnvironment.class);
        String target = se.getInstanceName();
        return target;
    }
    
    /** Obtaining BundleContext which belongs to osgi-javaee-base
     * @param clazz some class belongs to osgi-javaee-base
     * @return BundleContext which belongs to osgi-javaee-base
     */
    private BundleContext getBundleContext(Class<?> clazz) {
    	return BundleReference.class.cast(clazz.getClassLoader())
                .getBundle().getBundleContext();

    }
}
