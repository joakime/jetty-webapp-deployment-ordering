package org.eclipse.jetty.deploy.late;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jetty.deploy.App;
import org.eclipse.jetty.deploy.AppProvider;
import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.deploy.providers.WebAppProvider;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LateDeployProvider extends AbstractLifeCycle implements AppProvider
{
    private static final Logger LOG = LoggerFactory.getLogger(LateDeployProvider.class);
    private DeploymentManager deploymentManager;
    private WebAppProvider webAppProvider;

    public LateDeployProvider(WebAppProvider webAppProvider)
    {
        this.webAppProvider = Objects.requireNonNull(webAppProvider, "A configured WebAppProvider is required");
        this.deploymentManager = Objects.requireNonNull(this.webAppProvider.getDeploymentManager(), "WebAppProvider.getDeploymentManager is null");
    }

    @Override
    public ContextHandler createContextHandler(App app) throws Exception
    {
        return this.webAppProvider.createContextHandler(app);
    }

    public void deploy(Path scanDir)
    {
        try
        {
            List<Path> deployables = findDeployables(scanDir);
            if (LOG.isDebugEnabled())
            {
                for (Path path : deployables)
                {
                    LOG.debug("  Deployables: {}", path);

                    if (FileID.isExtension(path, "xml", "war"))
                    {
                        App app = new App(this.deploymentManager, this, path.toString());
                        this.deploymentManager.addApp(app);
                    }
                    else
                    {
                        LOG.info("Ignoring unrecognized deployable file: {}", path);
                    }
                }
            }
            LOG.info("Late Deploy Complete");
        }
        catch (Exception e)
        {
            LOG.warn("Failed late deploy", e);
        }
    }

    public DeploymentManager getDeploymentManager()
    {
        return deploymentManager;
    }

    @Override
    public void setDeploymentManager(DeploymentManager deploymentManager)
    {
        this.deploymentManager = deploymentManager;
    }

    private List<Path> findDeployables(Path dir) throws IOException
    {
        List<Path> deployables = new ArrayList<>();

        try (Stream<Path> entryStream = Files.list(dir))
        {
            List<Path> relevantFiles = entryStream
                .filter(Files::isRegularFile)
                .filter(e -> FileID.isExtension(e, "xml", "war"))
                .sorted()
                .collect(Collectors.toList());
            if (LOG.isDebugEnabled())
            {
                for (Path path : relevantFiles)
                {
                    LOG.debug("  Relevant File: {}", path);
                }
            }
            while (!relevantFiles.isEmpty())
            {
                Path topEntry = relevantFiles.remove(0);
                if (LOG.isDebugEnabled())
                    LOG.debug("Top Entry: {}", topEntry);
                if (FileID.isExtension(topEntry, "war"))
                {
                    // look for XML override
                    Path xmlOverride = topEntry.getParent().resolve(FileID.getBasename(topEntry) + ".xml");
                    if (LOG.isDebugEnabled())
                        LOG.debug("Checking for XML Override {}", xmlOverride);

                    if (Files.exists(xmlOverride))
                    {
                        if (LOG.isDebugEnabled())
                            LOG.debug("XML Override {} exists for {}", xmlOverride, topEntry);
                        relevantFiles.remove(xmlOverride);
                        deployables.add(xmlOverride);
                    }
                    else
                    {
                        if (LOG.isDebugEnabled())
                            LOG.debug("Using WAR Directly {}", topEntry);
                        deployables.add(topEntry);
                    }
                }
                else if (FileID.isExtension(topEntry, "xml"))
                {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Using XML Directly {}", topEntry);
                    deployables.add(topEntry);
                }
                else
                {
                    LOG.info("Ignoring unrecognized deployable file: {}", topEntry);
                }
            }
        }

        return deployables;
    }
}
