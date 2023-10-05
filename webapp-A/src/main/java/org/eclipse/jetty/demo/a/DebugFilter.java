package org.eclipse.jetty.demo.a;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter to show requests flowing through WebAppA
 */
public class DebugFilter
    implements Filter
{
    private static final Logger LOG = LoggerFactory.getLogger(DebugFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        LOG.debug("[WebAppA] {} to {}", httpRequest.getDispatcherType(), httpRequest.getRequestURL());

        chain.doFilter(httpRequest, response);
    }
}
