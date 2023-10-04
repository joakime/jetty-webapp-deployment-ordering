package org.eclipse.jetty.deploy.late;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class FileID
{
    /**
     * Retrieve the basename of a path. This is the name of the
     * last segment of the path, with any dot suffix (e.g. ".war") removed
     *
     * @param path The string path
     * @return The last segment of the path without any dot suffix
     */
    public static String getBasename(Path path)
    {
        Path filename = path.getFileName();
        if (filename == null)
            return "";
        String basename = filename.toString();
        int dot = basename.lastIndexOf('.');
        if (dot >= 0)
            basename = basename.substring(0, dot);
        return basename;
    }

    /**
     * <p>
     * Retrieve the extension of a file path (not a directory).
     * </p>
     *
     * <p>
     * This is the name of the last segment of the file path with a substring
     * for the extension (if any), excluding the dot, lower-cased.
     * </p>
     *
     * <pre>{@code
     * "foo.tar.gz" "gz"
     * "foo.bar"    "bar"
     * "foo."       ""
     * "foo"        null
     * ".bar"       null
     * null         null
     * }</pre>
     *
     * @param path The string path
     * @return The last segment extension, or null if not a file, or null if there is no extension present
     */
    public static String getExtension(Path path)
    {
        if (path == null)
            return null; // no path

        if (!Files.isRegularFile(path))
            return null; // not a file

        return getExtension(path.getFileName().toString());
    }

    /**
     * Retrieve the extension of a file path (not a directory).
     * This is the extension of filename of the last segment of the file path with a substring
     * for the extension (if any), including the dot, lower-cased.
     *
     * @param filename The string path
     * @return The last segment extension excluding the leading dot;
     *         or null if not a file;
     *         or null if there is no extension present
     */
    public static String getExtension(String filename)
    {
        if (filename == null)
            return null; // no filename
        if (filename.endsWith("/") || filename.endsWith("\\"))
            return null; // not a filename
        int lastSlash = filename.lastIndexOf(File.separator);
        if (lastSlash >= 0)
            filename = filename.substring(lastSlash + 1);
        int lastDot = filename.lastIndexOf('.');
        if (lastDot <= 0)
            return null; // no extension, or only filename that is only an extension (".foo")
        return filename.substring(lastDot + 1).toLowerCase(Locale.ENGLISH);
    }

    /**
     * Test if Path matches any of the indicated extensions.
     *
     * @param path the Path to test
     * @param extensions the list of extensions (all lowercase, without preceding {@code .} dot)
     * @return true if Path is a file, and has an extension, and it matches any of the indicated extensions
     */
    public static boolean isExtension(Path path, String... extensions)
    {
        return matchesExtension(getExtension(path), extensions);
    }

    private static boolean matchesExtension(String ext, String... extensions)
    {
        if (ext == null)
            return false;
        for (String extension : extensions)
        {
            if (ext.equals(extension))
                return true;
        }
        return false;
    }

}
