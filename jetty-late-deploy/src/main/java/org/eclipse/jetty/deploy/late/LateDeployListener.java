package org.eclipse.jetty.deploy.late;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LateDeployListener implements LifeCycle.Listener
{
    private static final Logger LOG = LoggerFactory.getLogger(LateDeployListener.class);

    private ContextHandlerCollection contexts;
    private Path webappsLatePath;

    public ContextHandlerCollection getContexts()
    {
        return contexts;
    }

    public void setContexts(ContextHandlerCollection contexts)
    {
        this.contexts = contexts;
    }

    public Path getWebappsLatePath()
    {
        return webappsLatePath;
    }

    public void setWebappsLatePath(Path webappsLatePath)
    {
        this.webappsLatePath = webappsLatePath;
    }

    @Override
    public void lifeCycleFailure(LifeCycle event, Throwable cause)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("lifeCycleFailure() : {}", event, cause);
    }

    @Override
    public void lifeCycleStarted(LifeCycle event)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("lifeCycleStarted() : {}", event);

        if (event instanceof Server)
        {
            // Now we trigger the late deployment steps.
            triggerDeploy();
        }
    }

    @Override
    public void lifeCycleStarting(LifeCycle event)
    {
        LifeCycle.Listener.super.lifeCycleStarting(event);
    }

    @Override
    public void lifeCycleStopped(LifeCycle event)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("lifeCycleStopped() : {}", event);
    }

    @Override
    public void lifeCycleStopping(LifeCycle event)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("lifeCycleStopping() : {}", event);
    }

    public void setWebappsLatePathString(String webappsLatePath)
    {
        setWebappsLatePath(Paths.get(webappsLatePath));
    }

    private void addContext(ContextHandler contextHandler) throws Exception
    {
        if (LOG.isDebugEnabled())
            LOG.debug("Starting Context {}", contextHandler);
        contextHandler.setServer(contexts.getServer());
        contextHandler.start();
        if (LOG.isDebugEnabled())
            LOG.debug("Adding Context to Handler Tree {}", contextHandler);
        contexts.addHandler(contextHandler);
    }

    private void deployWar(Path path) throws Exception
    {
        if (LOG.isDebugEnabled())
            LOG.debug("Deploying WAR {}", path);
        WebAppContext webapp = new WebAppContext();
        Resource warResource = Resource.newResource(path);
        webapp.setWarResource(warResource);
        webapp.setContextPath(FileID.getBasename(path));

        addContext(webapp);
    }

    private void deployXml(Path path) throws Exception
    {
        if (LOG.isDebugEnabled())
            LOG.debug("Deploying XML {}", path);
        Resource resource = Resource.newResource(path);

        XmlConfiguration xmlc = new XmlConfiguration(resource);
        xmlc.setJettyStandardIdsAndProperties(contexts.getServer(), resource);

        ContextHandler contextHandler = (ContextHandler)xmlc.configure();

        addContext(contextHandler);
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
                else if(FileID.isExtension(topEntry, "xml"))
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

    private void triggerDeploy()
    {
        LOG.info("Late Deploy Triggered");

        try
        {
            List<Path> deployables = findDeployables(webappsLatePath);
            if (LOG.isDebugEnabled())
            {
                for (Path path : deployables)
                {
                    LOG.debug("  Deployables: {}", path);

                    if (FileID.isExtension(path, "xml"))
                    {
                        deployXml(path);
                    }
                    else if (FileID.isExtension(path, "war"))
                    {
                        deployWar(path);
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
}
