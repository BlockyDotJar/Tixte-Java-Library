/**
 * Copyright 2022 Dominic (aka. BlockyDotJar)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.blocky.library.tixte.internal.utils.logging;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.Util;
import org.slf4j.spi.LocationAwareLogger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import static dev.blocky.library.tixte.internal.utils.logging.SimpleLogger.SimpleLoggerConfiguration.*;

/**
 * A custom {@link SimpleLogger}. (from slf4j-simple).
 *
 * @author BlockyDotJar
 * @version v1.1.0
 * @since v1.0.0-alpha.3
 */
public class SimpleLogger extends MarkerIgnoringBase
{

    private static final long serialVersionUID = -632788891211436180L;

    private static final long START_TIME = System.currentTimeMillis();

    private static final int LOG_LEVEL_TRACE = LocationAwareLogger.TRACE_INT;
    private static final int LOG_LEVEL_DEBUG = LocationAwareLogger.DEBUG_INT;
    private static final int LOG_LEVEL_INFO = LocationAwareLogger.INFO_INT;
    private static final int LOG_LEVEL_WARN = LocationAwareLogger.WARN_INT;
    private static final int LOG_LEVEL_ERROR = LocationAwareLogger.ERROR_INT;

    private static final String TID_PREFIX = "tid=";

    /**
     * The {@code OFF} level can only be used in configuration files to disable logging.
     * <br>It has no printing method associated with it in o.s.Logger interface.
     */
    private static final int LOG_LEVEL_OFF = LOG_LEVEL_ERROR + 10;

    private static boolean INITIALIZED = false;

    /**
     * The current log level.
     */
    private static int currentLogLevel = LOG_LEVEL_INFO;

    /**
     * The short name of this simple log instance.
     */
    private transient String shortLogName;

    /**
     * All system properties used by {@code SimpleLogger} start with this prefix.
     */
    private static final String SYSTEM_PREFIX = "org.slf4j.simpleLogger.";

    private static final String LOG_KEY_PREFIX = SYSTEM_PREFIX + "log.";

    private static final String CACHE_OUTPUT_STREAM_STRING_KEY = SimpleLogger.SYSTEM_PREFIX + "cacheOutputStream";

    private static final String WARN_LEVEL_STRING_KEY = SimpleLogger.SYSTEM_PREFIX + "warnLevelString";

    private static final String LEVEL_IN_BRACKETS_KEY = SimpleLogger.SYSTEM_PREFIX + "levelInBrackets";

    private static final String LOG_FILE_KEY = SimpleLogger.SYSTEM_PREFIX + "logFile";

    private static final String SHOW_SHORT_LOG_NAME_KEY = SimpleLogger.SYSTEM_PREFIX + "showShortLogName";

    private static final String SHOW_LOG_NAME_KEY = SimpleLogger.SYSTEM_PREFIX + "showLogName";

    private static final String SHOW_THREAD_NAME_KEY = SimpleLogger.SYSTEM_PREFIX + "showThreadName";

    private static final String SHOW_THREAD_ID_KEY = SimpleLogger.SYSTEM_PREFIX + "showThreadId";

    private static final String DATE_TIME_FORMAT_KEY = SimpleLogger.SYSTEM_PREFIX + "dateTimeFormat";

    private static final String SHOW_DATE_TIME_KEY = SimpleLogger.SYSTEM_PREFIX + "showDateTime";

    private static final String DEFAULT_LOG_LEVEL_KEY = SimpleLogger.SYSTEM_PREFIX + "defaultLogLevel";

    /**
     * Package access allows only {@link SimpleLoggerConfiguration} to instantiate {@link SimpleLogger} instances.
     *
     * @param name The name of the logger.
     */
    SimpleLogger(@NotNull String name)
    {
        if (!INITIALIZED)
        {
            init();
        }

        this.name = name;

        String levelString = recursivelyComputeLevelString();

        if (levelString != null)
        {
            currentLogLevel = stringToLevel(levelString);
        }
        else
        {
            currentLogLevel = DEFAULT_LOG_LEVEL;
        }
    }

    @Nullable
    @CheckReturnValue
    private String recursivelyComputeLevelString()
    {
        String tempName = name;
        String levelString = null;

        int indexOfLastDot = tempName.length();

        while (levelString == null && indexOfLastDot > -1)
        {
            tempName = tempName.substring(0, indexOfLastDot);
            levelString = getStringProperty(LOG_KEY_PREFIX + tempName, null);
            indexOfLastDot = tempName.lastIndexOf(".");
        }
        return levelString;
    }

    /**
     * This is our internal implementation for logging regular (non-parameterized) log messages.
     *
     * @param level One of the LOG_LEVEL_XXX constants defining the log level.
     * @param message The message itself.
     * @param t The exception whose stack trace should be logged.
     */
    private void log(int level, @NotNull String message, @Nullable Throwable t)
    {
        if (!isLevelEnabled(level))
        {
            return;
        }

        StringBuilder buf = new StringBuilder(32);

        // Append date-time if so configured.
        if (SHOW_DATE_TIME)
        {
            if (DATE_FORMATTER != null)
            {
                buf.append(getFormattedDate());
                buf.append(' ');
            }
            else
            {
                buf.append(System.currentTimeMillis() - START_TIME);
                buf.append(' ');
            }
        }

        // Append current thread name if so configured.
        if (SHOW_THREAD_NAME)
        {
            buf.append('[');
            buf.append(Thread.currentThread().getName());
            buf.append("] ");
        }

        if (SHOW_THREAD_ID)
        {
            buf.append(TID_PREFIX);
            buf.append(Thread.currentThread().getId());
            buf.append(' ');
        }

        if (LEVEL_IN_BRACKETS)
        {
            buf.append('[');
        }

        // Append a readable representation of the log level.
        String levelStr = renderLevel(level);
        buf.append(levelStr);

        if (LEVEL_IN_BRACKETS)
        {
            buf.append(']');
        }

        buf.append(' ');

        // Append the name of the log instance if so configured
        if (SHOW_SHORT_LOG_NAME)
        {
            if (shortLogName == null)
            {
                shortLogName = computeShortName();
            }

            buf.append(shortLogName).append(" - ");
        }
        else if (SHOW_LOG_NAME)
        {
            buf.append(name).append(" - ");
        }

        // Append the message.
        buf.append(message);

        write(buf, t);
    }

    @Nullable
    @CheckReturnValue
    private String renderLevel(int level)
    {
        switch (level)
        {
            case LOG_LEVEL_TRACE:
                return "TRACE";
            case LOG_LEVEL_DEBUG:
                return "DEBUG";
            case LOG_LEVEL_INFO:
                return "INFO";
            case LOG_LEVEL_WARN:
                return WARN_LEVEL_STRING;
            case LOG_LEVEL_ERROR:
                return "ERROR";
        }
        throw new IllegalStateException("Unrecognized level [" + level + "]");
    }

    /**
     * To avoid intermingling of log messages and associated stack traces, this is a synchronized method.
     *
     * @param buf The message to write.
     * @param t   The exception whose stack trace should be logged.
     */
    private synchronized void write(@NotNull StringBuilder buf, @Nullable Throwable t)
    {
        PrintStream targetStream = OUTPUT_CHOICE.getTargetPrintStream();

        targetStream.println(buf);
        writeThrowable(t, targetStream);
        targetStream.flush();
    }

    private void writeThrowable(@Nullable Throwable t, @NotNull PrintStream targetStream)
    {
        if (t != null)
        {
            t.printStackTrace(targetStream);
        }
    }

    @NotNull
    private synchronized String getFormattedDate()
    {
        Date now = new Date();
        String dateText;

        dateText = DATE_FORMATTER.format(now);

        return dateText;
    }

    @NotNull
    private String computeShortName()
    {
        return name.substring(name.lastIndexOf(".") + 1);
    }

    /**
     * For formatted messages, first substitute arguments and then log.
     *
     * @param level The log level.
     * @param format The message pattern which will be parsed and formatted.
     * @param arg1 The argument to be substituted in place of the first formatting anchor.
     * @param arg2 The argument to be substituted in place of the second formatting anchor.
     */
    private void formatAndLog(int level, @NotNull String format, @NotNull Object arg1, @Nullable Object arg2)
    {
        if (!isLevelEnabled(level))
        {
            return;
        }

        FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);

        log(level, tp.getMessage(), tp.getThrowable());
    }

    /**
     * For formatted messages, first substitute arguments and then log.
     *
     * @param level The log level.
     * @param format The message pattern which will be parsed and formatted.
     * @param arguments A list of 3 or more arguments.
     */
    private void formatAndLog(int level, @NotNull String format, @NotNull Object... arguments)
    {
        if (!isLevelEnabled(level))
        {
            return;
        }

        FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);

        log(level, tp.getMessage(), tp.getThrowable());
    }

    /**
     * Is the given log level currently enabled?
     *
     * @param logLevel Is this level enabled?
     *
     * @return b>true</b> - If this level is enabled.
     *         <br><b>false</b> - If this level is not enabled.
     */
    private boolean isLevelEnabled(int logLevel)
    {
        // Log level are numerically ordered so can use simple numeric comparison.
        return (logLevel >= currentLogLevel);
    }

    /**
     * Are {@code trace} messages currently enabled?
     *
     * @return <b>true</b> - If trace messages are enabled.
     *         <br><b>false</b> - If trace messages are not enabled.
     */
    @Override
    public boolean isTraceEnabled()
    {
        return isLevelEnabled(LOG_LEVEL_TRACE);
    }

    /**
     * A simple implementation which logs messages of level {@link #LOG_LEVEL_TRACE TRACE} according to
     * the format outlined above.
     *
     * @param message The message to log.
     */
    @Override
    public void trace(@NotNull String message)
    {
        log(LOG_LEVEL_TRACE, message, null);
    }

    /**
     * Perform single parameter substitution before logging the message of level
     * {@link #LOG_LEVEL_TRACE TRACE} according to the format outlined above.
     *
     * @param format The message pattern which will be parsed and formatted.
     * @param arg The argument to be substituted in place of the first formatting anchor.
     */
    @Override
    public void trace(@NotNull String format, @NotNull Object arg)
    {
        formatAndLog(LOG_LEVEL_TRACE, format, arg, null);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * {@link #LOG_LEVEL_TRACE TRACE} according to the format outlined above.
     *
     * @param format The message pattern which will be parsed and formatted.
     * @param arg1 The argument to be substituted in place of the first formatting anchor.
     * @param arg2 The argument to be substituted in place of the second formatting anchor.
     */
    @Override
    public void trace(@NotNull String format, @NotNull Object arg1, @Nullable Object arg2)
    {
        formatAndLog(LOG_LEVEL_TRACE, format, arg1, arg2);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * {@link #LOG_LEVEL_TRACE TRACE} according to the format outlined above.
     *
     * @param format The message pattern which will be parsed and formatted.
     * @param arguments A list of 3 or more arguments.
     */
    @Override
    public void trace(@NotNull String format, @NotNull Object... arguments)
    {
        formatAndLog(LOG_LEVEL_TRACE, format, arguments);
    }

    /**
     * Log a message of level {@link #LOG_LEVEL_TRACE TRACE}, including an exception.
     *
     * @param message The message to log.
     * @param t The exception whose stack trace should be logged.
     */
    @Override
    public void trace(@NotNull String message, @Nullable Throwable t)
    {
        log(LOG_LEVEL_TRACE, message, t);
    }

    /**
     * Are {@code debug} messages currently enabled?
     *
     * @return <b>true</b> - If debug messages are enabled.
     *         <br><b>false</b> - If debug messages are not enabled.
     */
    @Override
    public boolean isDebugEnabled()
    {
        return isLevelEnabled(LOG_LEVEL_DEBUG);
    }

    /**
     * A simple implementation which logs messages of level {@link #LOG_LEVEL_DEBUG DEBUG} according to
     * the format outlined above.
     * 
     * @param message The message to log.
     */
    @Override
    public void debug(@NotNull String message)
    {
        log(LOG_LEVEL_DEBUG, message, null);
    }

    /**
     * Perform single parameter substitution before logging the message of level
     * {@link #LOG_LEVEL_DEBUG DEBUG} according to the format outlined above.
     *
     * @param format The message pattern which will be parsed and formatted.
     * @param arg The argument to be substituted in place of the first formatting anchor.
     */
    @Override
    public void debug(@NotNull String format, @NotNull Object arg)
    {
        formatAndLog(LOG_LEVEL_DEBUG, format, arg, null);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * {@link #LOG_LEVEL_DEBUG DEBUG} according to the format outlined above.
     *
     * @param format The message pattern which will be parsed and formatted.
     * @param arg1 The argument to be substituted in place of the first formatting anchor.
     * @param arg2 The argument to be substituted in place of the second formatting anchor.
     */
    @Override
    public void debug(@NotNull String format, @NotNull Object arg1, @Nullable Object arg2)
    {
        formatAndLog(LOG_LEVEL_DEBUG, format, arg1, arg2);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * {@link #LOG_LEVEL_DEBUG DEBUG} according to the format outlined above.
     *
     * @param format The message pattern which will be parsed and formatted.
     * @param arguments A list of 3 or more arguments.
     */
    @Override
    public void debug(@NotNull String format, @NotNull Object... arguments)
    {
        formatAndLog(LOG_LEVEL_DEBUG, format, arguments);
    }

    /**
     * Log a message of level {@link #LOG_LEVEL_DEBUG DEBUG}, including an exception.
     *
     * @param message The message to log.
     * @param t The exception whose stack trace should be logged.
     */
    @Override
    public void debug(@NotNull String message, @Nullable Throwable t)
    {
        log(LOG_LEVEL_DEBUG, message, t);
    }

    /**
     * Are {@code info} messages currently enabled?
     *
     * @return <b>true</b> - If info messages are enabled.
     *         <br><b>false</b> - If info messages are not enabled.
     */
    @Override
    public boolean isInfoEnabled()
    {
        return isLevelEnabled(LOG_LEVEL_INFO);
    }

    /**
     * A simple implementation which logs messages of level {@link #LOG_LEVEL_INFO INFO} according to
     * the format outlined above.
     * 
     * @param message The message to log.
     */
    @Override
    public void info(@NotNull String message)
    {
        log(LOG_LEVEL_INFO, message, null);
    }

    /**
     * Perform single parameter substitution before logging the message of level
     * {@link #LOG_LEVEL_INFO INFO} according to the format outlined above.
     *
     * @param format The message pattern which will be parsed and formatted.
     * @param arg The argument to be substituted in place of the first formatting anchor.
     */
    @Override
    public void info(@Nullable String format, @NotNull Object arg)
    {
        formatAndLog(LOG_LEVEL_INFO, format, arg, null);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * {@link #LOG_LEVEL_INFO INFO} according to the format outlined above.
     *
     * @param format The message pattern which will be parsed and formatted.
     * @param arg1 The argument to be substituted in place of the first formatting anchor.
     * @param arg2 The argument to be substituted in place of the second formatting anchor.
     */
    @Override
    public void info(@NotNull String format, @NotNull Object arg1, @Nullable Object arg2)
    {
        formatAndLog(LOG_LEVEL_INFO, format, arg1, arg2);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * {@link #LOG_LEVEL_INFO INFO} according to the format outlined above.
     *
     * @param format The message pattern which will be parsed and formatted.
     * @param arguments A list of 3 or more arguments.
     */
    @Override
    public void info(@NotNull String format, @NotNull Object... arguments)
    {
        formatAndLog(LOG_LEVEL_INFO, format, arguments);
    }

    /**
     * Log a message of level {@link #LOG_LEVEL_INFO INFO}, including an exception.
     *
     * @param message The message to log.
     * @param t The exception whose stack trace should be logged.
     */
    @Override
    public void info(@NotNull String message, @Nullable Throwable t)
    {
        log(LOG_LEVEL_INFO, message, t);
    }

    /**
     * Are {@code warn} messages currently enabled?
     *
     * @return <b>true</b> - If warn messages are enabled.
     *         <br><b>false</b> - If warn messages are not enabled.
     */
    @Override
    public boolean isWarnEnabled() 
    {
        return isLevelEnabled(LOG_LEVEL_WARN);
    }

    /**
     * A simple implementation which always logs messages of level {@link #LOG_LEVEL_WARN WARN}
     * according to the format outlined above.
     * 
     * @param message The message to log.
     */
    @Override
    public void warn(@NotNull String message)
    {
        log(LOG_LEVEL_WARN, message, null);
    }

    /**
     * Perform single parameter substitution before logging the message of level
     * {@link #LOG_LEVEL_WARN WARN} according to the format outlined above.
     *
     * @param format The message pattern which will be parsed and formatted.
     * @param arg The argument to be substituted in place of the first formatting anchor.
     */
    @Override
    public void warn(@NotNull String format, @NotNull Object arg) 
    {
        formatAndLog(LOG_LEVEL_WARN, format, arg, null);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * {@link #LOG_LEVEL_WARN WARN} according to the format outlined above.
     *
     * @param format The message pattern which will be parsed and formatted.
     * @param arg1 The argument to be substituted in place of the first formatting anchor.
     * @param arg2 The argument to be substituted in place of the second formatting anchor.
     */
    @Override
    public void warn(@NotNull String format, @NotNull Object arg1, @Nullable Object arg2) 
    {
        formatAndLog(LOG_LEVEL_WARN, format, arg1, arg2);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * {@link #LOG_LEVEL_WARN WARN} according to the format outlined above.
     *
     * @param format The message pattern which will be parsed and formatted.
     * @param arguments A list of 3 or more arguments.
     */
    @Override
    public void warn(@NotNull String format, @NotNull Object... arguments) 
    {
        formatAndLog(LOG_LEVEL_WARN, format, arguments);
    }

    /**
     * Log a message of level {@link #LOG_LEVEL_WARN WARN}, including an exception.
     *
     * @param message The message to log.
     * @param t The exception whose stack trace should be logged.
     */
    @Override
    public void warn(@NotNull String message, @Nullable Throwable t) 
    {
        log(LOG_LEVEL_WARN, message, t);
    }

    /**
     * Are {@code error} messages currently enabled?
     *
     * @return <b>true</b> - If error messages are enabled.
     *         <br><b>false</b> - If error messages are not enabled.
     */
    @Override
    public boolean isErrorEnabled() 
    {
        return isLevelEnabled(LOG_LEVEL_ERROR);
    }

    /**
     * A simple implementation which always logs messages of level {@link #LOG_LEVEL_ERROR ERROR}
     * according to the format outlined above.
     * 
     * @param message The message to log.
     */
    @Override
    public void error(@NotNull String message)
    {
        log(LOG_LEVEL_ERROR, message, null);
    }

    /**
     * Perform single parameter substitution before logging the message of level
     * {@link #LOG_LEVEL_ERROR ERROR} according to the format outlined above.
     *
     * @param format The message pattern which will be parsed and formatted.
     * @param arg The argument to be substituted in place of the first formatting anchor.
     */
    @Override
    public void error(@NotNull String format, @NotNull Object arg)
    {
        formatAndLog(LOG_LEVEL_ERROR, format, arg, null);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * {@link #LOG_LEVEL_ERROR ERROR} according to the format outlined above.
     *
     * @param format The message pattern which will be parsed and formatted.
     * @param arg1 The argument to be substituted in place of the first formatting anchor.
     * @param arg2 The argument to be substituted in place of the second formatting anchor.
     */
    @Override
    public void error(@NotNull String format, @NotNull Object arg1, @Nullable Object arg2)
    {
        formatAndLog(LOG_LEVEL_ERROR, format, arg1, arg2);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * {@link #LOG_LEVEL_ERROR ERROR} according to the format outlined above.
     *
     * @param format The message pattern which will be parsed and formatted.
     * @param arguments A list of 3 or more arguments.
     */
    @Override
    public void error(@NotNull String format, @NotNull Object... arguments)
    {
        formatAndLog(LOG_LEVEL_ERROR, format, arguments);
    }

    /**
     * Log a message of level {@link #LOG_LEVEL_ERROR ERROR}, including an exception.
     *
     * @param message The message to log.
     * @param t The exception whose stack trace should be logged.
     */
    @Override
    public void error(@NotNull String message, @Nullable Throwable t)
    {
        log(LOG_LEVEL_ERROR, message, t);
    }

    /**
     * This class holds configuration values for {@link SimpleLogger}.
     * <br>The values are computed at runtime.
     *
     * @author BlockyDotJar
     * @version v1.0.0
     * @since v1.0.0-beta.2
     */
    static class SimpleLoggerConfiguration
    {
        private static final String CONFIGURATION_FILE = "simpleLogger.properties";

        private static final int DEFAULT_LOG_LEVEL_DEFAULT = SimpleLogger.LOG_LEVEL_INFO;
        static int DEFAULT_LOG_LEVEL = DEFAULT_LOG_LEVEL_DEFAULT;

        private static final boolean SHOW_DATE_TIME_DEFAULT = false;
        static boolean SHOW_DATE_TIME = SHOW_DATE_TIME_DEFAULT;

        private static final String DATE_TIME_FORMAT_STR_DEFAULT = null;
        private static String DATE_TIME_FORMAT_STR = DATE_TIME_FORMAT_STR_DEFAULT;

        static DateFormat DATE_FORMATTER;

        private static final boolean SHOW_THREAD_NAME_DEFAULT = true;
        static boolean SHOW_THREAD_NAME = SHOW_THREAD_NAME_DEFAULT;

        private static final boolean SHOW_THREAD_ID_DEFAULT = false;
        static boolean SHOW_THREAD_ID = SHOW_THREAD_ID_DEFAULT;

        private static final boolean SHOW_LOG_NAME_DEFAULT = true;
        static boolean SHOW_LOG_NAME = SHOW_LOG_NAME_DEFAULT;

        private static final boolean SHOW_SHORT_LOG_NAME_DEFAULT = false;
        static boolean SHOW_SHORT_LOG_NAME = SHOW_SHORT_LOG_NAME_DEFAULT;

        private static final boolean LEVEL_IN_BRACKETS_DEFAULT = false;
        static boolean LEVEL_IN_BRACKETS = LEVEL_IN_BRACKETS_DEFAULT;

        private  static final String LOG_FILE_DEFAULT = "System.err";
        private  static String LOG_FILE = LOG_FILE_DEFAULT;
        static OutputChoice OUTPUT_CHOICE;

        private static final boolean CACHE_OUTPUT_STREAM_DEFAULT = false;
        private static boolean CACHE_OUTPUT_STREAM = CACHE_OUTPUT_STREAM_DEFAULT;

        private static final String WARN_LEVEL_STRING_DEFAULT = "WARN";
        static String WARN_LEVEL_STRING = WARN_LEVEL_STRING_DEFAULT;

        private static final Properties SIMPLE_LOGGER_PROPS = new Properties();

        static void init()
        {
            INITIALIZED = true;
            loadProperties();

            String defaultLogLevelString = getStringProperty(DEFAULT_LOG_LEVEL_KEY, null);

            if (defaultLogLevelString != null)
            {
                DEFAULT_LOG_LEVEL = stringToLevel(defaultLogLevelString);
            }

            SHOW_LOG_NAME = getBooleanProperty(SHOW_LOG_NAME_KEY, SHOW_LOG_NAME_DEFAULT);
            SHOW_SHORT_LOG_NAME = getBooleanProperty(SHOW_SHORT_LOG_NAME_KEY, SHOW_SHORT_LOG_NAME_DEFAULT);
            SHOW_DATE_TIME = getBooleanProperty(SHOW_DATE_TIME_KEY, SHOW_DATE_TIME_DEFAULT);
            SHOW_THREAD_NAME = getBooleanProperty(SHOW_THREAD_NAME_KEY, SHOW_THREAD_NAME_DEFAULT);
            SHOW_THREAD_ID = getBooleanProperty(SHOW_THREAD_ID_KEY, SHOW_THREAD_ID_DEFAULT);

            DATE_TIME_FORMAT_STR = getStringProperty(DATE_TIME_FORMAT_KEY, DATE_TIME_FORMAT_STR_DEFAULT);
            LEVEL_IN_BRACKETS = getBooleanProperty(LEVEL_IN_BRACKETS_KEY, LEVEL_IN_BRACKETS_DEFAULT);
            WARN_LEVEL_STRING = getStringProperty(WARN_LEVEL_STRING_KEY, WARN_LEVEL_STRING_DEFAULT);

            LOG_FILE = getStringProperty(LOG_FILE_KEY, LOG_FILE);

            CACHE_OUTPUT_STREAM = getBooleanProperty(SimpleLogger.CACHE_OUTPUT_STREAM_STRING_KEY, CACHE_OUTPUT_STREAM_DEFAULT);

            OUTPUT_CHOICE = computeOutputChoice(LOG_FILE, CACHE_OUTPUT_STREAM);

            if (DATE_TIME_FORMAT_STR != null)
            {
                try
                {
                    DATE_FORMATTER = new SimpleDateFormat(DATE_TIME_FORMAT_STR);
                }
                catch (IllegalArgumentException e)
                {
                    Util.report("Bad date format in " + CONFIGURATION_FILE + "; will output relative time", e);
                }
            }
        }

        private static void loadProperties()
        {
            // Add props from the resource simpleLogger.properties.
            InputStream in = AccessController.doPrivileged((PrivilegedAction<InputStream>) () ->
            {
                ClassLoader threadCL = Thread.currentThread().getContextClassLoader();

                if (threadCL != null)
                {
                    return threadCL.getResourceAsStream(CONFIGURATION_FILE);
                }
                else
                {
                    return ClassLoader.getSystemResourceAsStream(CONFIGURATION_FILE);
                }
            });

            if (null != in)
            {
                try
                {
                    SIMPLE_LOGGER_PROPS.load(in);
                }
                catch (java.io.IOException e)
                {
                    // Ignored.
                }
                finally
                {
                    try
                    {
                        in.close();
                    }
                    catch (java.io.IOException e)
                    {
                        // Ignored.
                    }
                }
            }
        }

        @Nullable
        @CheckReturnValue
        static String getStringProperty(@NotNull String name, @Nullable String defaultValue)
        {
            String prop = getStringProperty(name);
            return (prop == null) ? defaultValue : prop;
        }

        static boolean getBooleanProperty(@NotNull String name, boolean defaultValue)
        {
            String prop = getStringProperty(name);
            return (prop == null) ? defaultValue : "true".equalsIgnoreCase(prop);
        }

        @Nullable
        @CheckReturnValue
        static String getStringProperty(@NotNull String name)
        {
            String prop = null;

            try
            {
                prop = System.getProperty(name);
            }
            catch (SecurityException e)
            {
                // Ignore.
            }
            return (prop == null) ? SIMPLE_LOGGER_PROPS.getProperty(name) : prop;
        }

        static int stringToLevel(@NotNull String levelStr)
        {
            if ("trace".equalsIgnoreCase(levelStr))
            {
                return LOG_LEVEL_TRACE;
            }
            else if ("debug".equalsIgnoreCase(levelStr))
            {
                return LOG_LEVEL_DEBUG;
            }
            else if ("info".equalsIgnoreCase(levelStr))
            {
                return LOG_LEVEL_INFO;
            }
            else if ("warn".equalsIgnoreCase(levelStr))
            {
                return LOG_LEVEL_WARN;
            }
            else if ("error".equalsIgnoreCase(levelStr))
            {
                return LOG_LEVEL_ERROR;
            }
            else if ("off".equalsIgnoreCase(levelStr))
            {
                return LOG_LEVEL_OFF;
            }

            // Assume INFO by default.
            return LOG_LEVEL_INFO;
        }

        @NotNull
        static OutputChoice computeOutputChoice(@NotNull String logFile, boolean cacheOutputStream)
        {
            if ("System.err".equalsIgnoreCase(logFile))
            {
                if (cacheOutputStream)
                {
                    return new OutputChoice(OutputChoiceType.CACHED_SYS_ERR);
                }
                else
                {
                    return new OutputChoice(OutputChoiceType.SYS_ERR);
                }
            }
            else if ("System.out".equalsIgnoreCase(logFile))
            {
                if (cacheOutputStream)
                {
                    return new OutputChoice(OutputChoiceType.CACHED_SYS_OUT);
                }
                else
                {
                    return new OutputChoice(OutputChoiceType.SYS_OUT);
                }
            }
            else
            {
                try
                {
                    FileOutputStream fos = new FileOutputStream(logFile);
                    PrintStream printStream = new PrintStream(fos);
                    return new OutputChoice(printStream);
                }
                catch (FileNotFoundException e)
                {
                    Util.report("Could not open [" + logFile + "]. Defaulting to System.err", e);
                    return new OutputChoice(OutputChoiceType.SYS_ERR);
                }
            }
        }
    }

    /**
     * This class encapsulates the user's choice of output target.
     *
     * @author BlockyDotJar
     * @version v1.0.0
     * @since v1.0.0-beta.2
     */
    private static class OutputChoice
    {
        private final OutputChoiceType OUTPUT_CHOICE_TYPE;
        private final PrintStream TARGET_STREAM;

        private OutputChoice(@NotNull OutputChoiceType outputChoiceType)
        {
            if (outputChoiceType == OutputChoiceType.FILE)
            {
                throw new IllegalArgumentException();
            }

            this.OUTPUT_CHOICE_TYPE = outputChoiceType;

            if (outputChoiceType == OutputChoiceType.CACHED_SYS_OUT)
            {
                this.TARGET_STREAM = System.out;
            }
            else if (outputChoiceType == OutputChoiceType.CACHED_SYS_ERR)
            {
                this.TARGET_STREAM = System.err;
            }
            else
            {
                this.TARGET_STREAM = null;
            }
        }

        private OutputChoice(@NotNull PrintStream printStream)
        {
            this.OUTPUT_CHOICE_TYPE = OutputChoiceType.FILE;
            this.TARGET_STREAM = printStream;
        }

        @NotNull
        private PrintStream getTargetPrintStream()
        {
            switch (OUTPUT_CHOICE_TYPE)
            {
                case SYS_OUT:
                    return System.out;
                case SYS_ERR:
                    return System.err;
                case CACHED_SYS_ERR:
                case CACHED_SYS_OUT:
                case FILE:
                    return TARGET_STREAM;
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    private enum OutputChoiceType
    {
        SYS_OUT, CACHED_SYS_OUT, SYS_ERR, CACHED_SYS_ERR, FILE
    }
}