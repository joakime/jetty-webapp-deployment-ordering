package org.eclipse.jetty.deploy.late;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jetty.deploy.AppProvider;
import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.deploy.providers.WebAppProvider;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LateDeployListener implements LifeCycle.Listener
{
    private static final Logger LOG = LoggerFactory.getLogger(LateDeployListener.class);

    private Path webappsLatePath;

    public Path getWebappsLatePath()
    {
        return webappsLatePath;
    }

    public void setWebappsLatePath(Path webappsLatePath)
    {
        this.webappsLatePath = webappsLatePath;
    }

    @Override
    public void lifeCycleStarted(LifeCycle event)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("lifeCycleStarted() : {}", event);

        if (event instanceof Server)
        {
            // Now we trigger the late deployment steps.
            LOG.info("Late Deploy Triggered");

            Server server = (Server)event;
            LateDeployProvider lateDeployer = newLateDeployProvider(server);
            if (lateDeployer != null)
                lateDeployer.deploy(this.webappsLatePath);
        }
    }

    public void setWebappsLatePathString(String webappsLatePath)
    {
        setWebappsLatePath(Paths.get(webappsLatePath));
    }

    private LateDeployProvider newLateDeployProvider(Server server)
    {
        DeploymentManager deploymentManager = server.getBean(DeploymentManager.class);
        if (deploymentManager == null)
        {
            LOG.warn("Unable to find required DeploymentManager. Late deploy disabled.");
            return null;
        }

        // Look for existing WebAppProvider (to use its configuration)
        for (AppProvider appProvider : deploymentManager.getAppProviders())
        {
            if (appProvider instanceof WebAppProvider)
            {
                WebAppProvider webAppProvider = (WebAppProvider)appProvider;
                if (LOG.isDebugEnabled())
                    LOG.debug("Using already configured WebAppProvider: {}", webAppProvider);
                // Note we do not add this to the DeploymentManager (as it's too late to do that)
                // The DeploymentManager.addAppProvider() cannot be used once the DeploymentManager is in running state
                return new LateDeployProvider(webAppProvider);
            }
        }

        // Use new WebAppProvider
        WebAppProvider webAppProvider = new WebAppProvider();
        if (LOG.isDebugEnabled())
            LOG.debug("Create a new WebAppProvider: {}", webAppProvider);
        webAppProvider.setDeploymentManager(deploymentManager);
        // Note we do not add this to the DeploymentManager (as it's too late to do that)
        // The DeploymentManager.addAppProvider() cannot be used once the DeploymentManager is in running state
        return new LateDeployProvider(webAppProvider);
    }
}
