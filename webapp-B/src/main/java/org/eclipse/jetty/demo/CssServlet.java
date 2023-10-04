package org.eclipse.jetty.demo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CssServlet extends HttpServlet
{
    private byte[] rawCss;

    @Override
    public void init() throws ServletException
    {
        System.err.println("## Init (in WebApp B)");
        String cssURLRoot = getInitParameter("cssURLRoot");
        if (cssURLRoot == null)
            throw new IllegalStateException("Unable to find 'cssURLRoot' init-param");
        System.err.println("## cssURLRoot is " + cssURLRoot);
        URI uriCss = URI.create(cssURLRoot).resolve("main.css");
        System.err.println("## uriCss is " + uriCss);

        // Fetch CSS details from WebApp A
        try
        {
            HttpURLConnection http = (HttpURLConnection)uriCss.toURL().openConnection();
            http.setConnectTimeout(2000);
            if (http.getResponseCode() != 200)
                throw new ServletException("Error: Response code [" + http.getResponseCode() + "] on GET of " + uriCss);
            try (InputStream stream = http.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream())
            {
                byte[] buf = new byte[8096];
                int len;
                while ((len = stream.read(buf)) != -1)
                {
                    out.write(buf, 0, len);
                }
                rawCss = out.toByteArray();
            }
            System.err.printf("### Got rawCss of size %d%n", rawCss.length);
        }
        catch (IOException e)
        {
            throw new ServletException("Unable to get css: " + uriCss, e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        resp.setContentType("text/css");
        resp.setCharacterEncoding("utf-8");
        OutputStream out = resp.getOutputStream();
        out.write(rawCss);
    }
}
