/*
 * Copyright 2005 The Apache Software Foundation
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.DefaultContext;
import org.apache.avalon.framework.logger.Logger;
import org.apache.cocoon.Cocoon;
import org.apache.cocoon.Constants;
import org.apache.cocoon.Processor;
import org.apache.cocoon.components.ContextHelper;
import org.apache.cocoon.components.source.SourceUtil;
import org.apache.cocoon.components.source.impl.DelayedRefreshSourceWrapper;
import org.apache.cocoon.core.container.spring.BeanFactoryUtil;
import org.apache.cocoon.core.container.spring.AvalonEnvironment;
import org.apache.cocoon.core.container.spring.ConfigReader;
import org.apache.cocoon.core.container.spring.ConfigurationInfo;
import org.apache.cocoon.core.container.util.ComponentContext;
import org.apache.cocoon.core.container.util.ConfigurationBuilder;
import org.apache.cocoon.core.container.util.SimpleSourceResolver;
import org.apache.cocoon.environment.Context;
import org.apache.cocoon.util.ClassUtils;
import org.apache.cocoon.util.StringUtils;
import org.apache.cocoon.util.location.Location;
import org.apache.cocoon.util.location.LocationImpl;
import org.apache.cocoon.util.location.LocationUtils;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceResolver;
import org.apache.excalibur.source.TraversableSource;
import org.apache.excalibur.source.impl.URLSource;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.xml.sax.InputSource;

/**
 * This is an utility class to create a new Cocoon instance.
 * 
 * @version $Id$
 * @since 2.2
 */
public class CoreUtil {

    /** Parameter map for the context protocol */
    protected static final Map CONTEXT_PARAMETERS = Collections.singletonMap("force-traversable", Boolean.TRUE);

    /** The callback to the real environment. */
    protected final BootstrapEnvironment env;

    /** "legacy" support: create an avalon context. */
    protected final DefaultContext appContext = new ComponentContext();

    /** The settings. */
    protected MutableSettings settings;

    /** The root logger. */
    protected Logger log;

    /** The Root processor instance */
    protected Processor processor;

    protected ClassLoader classloader;

    /** The core object. */
    protected Core core;

    /** The environment context. */
    protected final Context environmentContext;

    /** The container. */
    protected ConfigurableBeanFactory container;

    /** The configuration file */
    protected Source configurationFile;

    // Register the location finder for Avalon configuration objects and exceptions
    // and keep a strong reference to it.
    private static final LocationUtils.LocationFinder confLocFinder = new LocationUtils.LocationFinder() {
        public Location getLocation(Object obj, String description) {
            if (obj instanceof Configuration) {
                Configuration config = (Configuration)obj;
                String locString = config.getLocation();
                Location result = LocationUtils.parse(locString);
                if (LocationUtils.isKnown(result)) {
                    // Add description
                    StringBuffer desc = new StringBuffer().append('<');
                    // Unfortunately Configuration.getPrefix() is not public
                    try {
                        if (config.getNamespace().startsWith("http://apache.org/cocoon/sitemap/")) {
                            desc.append("map:");
                        }
                    } catch (ConfigurationException e) {
                        // no namespace: ignore
                    }
                    desc.append(config.getName()).append('>');
                    return new LocationImpl(desc.toString(), result);
                } else {
                    return result;
                }
            }
            
            if (obj instanceof Exception) {
                // Many exceptions in Cocoon have a message like "blah blah at file://foo/bar.xml:12:1"
                String msg = ((Exception)obj).getMessage();
                if (msg == null) return null;
                
                int pos = msg.lastIndexOf(" at ");
                if (pos != -1) {
                    return LocationUtils.parse(msg.substring(pos + 4));
                } else {
                    // Will try other finders
                    return null;
                }
            }
            
            // Try next finders.
            return null;
        }
    };
    
    static {
        LocationUtils.addFinder(confLocFinder);
    }
    
    /**
     * Setup a new instance.
     * @param context     The environment context.
     * @throws Exception
     */
    public CoreUtil(Context              context)
    throws Exception {
        this(context, null);
    }

    /**
     * Setup a new instance.
     * @param context     The environment context.
     * @param environment The optional hook back to the environment.
     * @throws Exception
     */
    public CoreUtil(Context              context,
                    BootstrapEnvironment environment)
    throws Exception {
        this.environmentContext = context;
        this.env = environment;
        this.init();
    }

    protected void init()
    throws Exception {
        // first let's set up the appContext with some values to make
        // the simple source resolver work

        // add root url
        try {
            appContext.put(ContextHelper.CONTEXT_ROOT_URL,
                           new URL(this.getContextUrl()));
        } catch (MalformedURLException ignore) {
            // we simply ignore this
        }

        // add environment context
        this.appContext.put(Constants.CONTEXT_ENVIRONMENT_CONTEXT,
                            this.environmentContext);

        // now add environment specific information
        if ( this.env != null ) {
            this.env.configure(appContext);
        }

        // create settings
        this.settings = this.createSettings();

        // first init the work-directory for the logger.
        // this is required if we are running inside a war file!
        final String workDirParam = this.settings.getWorkDirectory();
        File workDir;
        if (workDirParam != null) {
            // No context path : consider work-directory as absolute
            workDir = new File(workDirParam);
        } else {
            workDir = new File("cocoon-files");
        }
        workDir.mkdirs();
        this.appContext.put(Constants.CONTEXT_WORK_DIR, workDir);
        this.settings.setWorkDirectory(workDir.getAbsolutePath());

        // Init logger
        this.log = BeanFactoryUtil.createRootLogger(this.environmentContext,
                                                    this.settings);

        // Output some debug info
        if (this.log.isDebugEnabled()) {
            this.log.debug("Context URL: " + this.getContextUrl());
            if (workDirParam != null) {
                this.log.debug("Using work-directory " + workDir);
            } else {
                this.log.debug("Using default work-directory " + workDir);
            }
        }

        final String uploadDirParam = this.settings.getUploadDirectory();
        File uploadDir;
        if (uploadDirParam != null) {
            uploadDir = new File(uploadDirParam);
            if (this.log.isDebugEnabled()) {
                this.log.debug("Using upload-directory " + uploadDir);
            }
        } else {
            uploadDir = new File(workDir, "upload-dir" + File.separator);
            if (this.log.isDebugEnabled()) {
                this.log.debug("Using default upload-directory " + uploadDir);
            }
        }
        uploadDir.mkdirs();
        appContext.put(Constants.CONTEXT_UPLOAD_DIR, uploadDir);
        this.settings.setUploadDirectory(uploadDir.getAbsolutePath());

        String cacheDirParam = this.settings.getCacheDirectory();
        File cacheDir;
        if (cacheDirParam != null) {
            cacheDir = new File(cacheDirParam);
            if (this.log.isDebugEnabled()) {
                this.log.debug("Using cache-directory " + cacheDir);
            }
        } else {
            cacheDir = new File(workDir, "cache-dir" + File.separator);
            File parent = cacheDir.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            if (this.log.isDebugEnabled()) {
                this.log.debug("cache-directory was not set - defaulting to " + cacheDir);
            }
        }
        cacheDir.mkdirs();
        appContext.put(Constants.CONTEXT_CACHE_DIR, cacheDir);
        this.settings.setCacheDirectory(cacheDir.getAbsolutePath());

        // update configuration
        final URL u = this.getConfigFile(this.settings.getConfiguration());
        this.settings.setConfiguration(u.toExternalForm());
        this.appContext.put(Constants.CONTEXT_CONFIG_URL, u);

        // set encoding
        this.appContext.put(Constants.CONTEXT_DEFAULT_ENCODING, settings.getFormEncoding());

        // set class loader
        this.createClassloader();
        this.appContext.put(Constants.CONTEXT_CLASS_LOADER, this.classloader);

        // dump system properties
        this.dumpSystemProperties();

        // create the Core object
        this.core = this.createCore();

        // settings can't be changed anymore
        settings.makeReadOnly();

        // test the setup of the spring based container
        this.container = this.setupSpringContainer();
    }

    /**
     * Return the core object.
     */
    public Core getCore() {
        return this.core;
    }

    public Logger getRootLogger() {
        return this.log;
    }

    /**
     * Create a new core instance.
     * This method can be overwritten in sub classes.
     * @return A new core object.
     */
    protected Core createCore() {
        final Core c = new Core(this.settings, this.appContext);
        return c;
    }

    /**
     * Return the settings object.
     */
    public Settings getSettings() {
        return this.settings;
    }

    /**
     * Get the settings for Cocoon.
     * This method reads several property files and merges the result. If there
     * is more than one definition for a property, the last one wins.
     * The property files are read in the following order:
     * 1) context://WEB-INF/properties/*.properties
     *    Default values for the core and each block - the order in which the files are read is not guaranteed.
     * 2) context://WEB-INF/properties/[RUNNING_MODE]/*.properties
     *    Default values for the running mode - the order in which the files are read is not guaranteed.
     * 3) Property providers (ToBeDocumented)
     * 4) The environment (CLI, Servlet etc.) adds own properties (e.g. from web.xml)
     * 5) Additional property file specified by the "org.apache.cocoon.settings" system property or
     *    if the property is not found, the file ".cocoon/settings.properties" is tried to be read from
     *    the user directory.
     * 6) System properties
     *
     * @return A new Settings object
     */
    protected MutableSettings createSettings() {
        // get the running mode
        final String mode = System.getProperty(Settings.PROPERTY_RUNNING_MODE, Settings.DEFAULT_RUNNING_MODE);
        this.environmentContext.log("Running in mode: " + mode);

        // create an empty settings objects
        final MutableSettings s = new MutableSettings();

        // we need our own resolver
        final SourceResolver resolver = this.createSourceResolver(new LoggerWrapper(this.environmentContext));

        // now read all properties from the properties directory
        this.readProperties("context://WEB-INF/properties", s, resolver);
        // read all properties from the mode dependent directory
        this.readProperties("context://WEB-INF/properties/" + mode, s, resolver);

        // Next look for custom property providers
        Iterator i = s.getPropertyProviders().iterator();
        while ( i.hasNext() ) {
            final String className = (String)i.next();
            try {
                PropertyProvider provider = (PropertyProvider)ClassUtils.newInstance(className);
                s.fill(provider.getProperties());
            } catch (Exception ignore) {
                this.environmentContext.log("Unable to get property provider for class " + className, ignore);
                this.environmentContext.log("Continuing initialization.");            
            }
        }
        // fill from the environment configuration, like web.xml etc.
        if ( this.env != null ) {
            env.configure(s);
        }

        // read additional properties file
        String additionalPropertyFile = s.getProperty(Settings.PROPERTY_USER_SETTINGS, 
                                                      System.getProperty(Settings.PROPERTY_USER_SETTINGS));
        // if there is no property defining the addition file, we try it in the home directory
        if ( additionalPropertyFile == null ) {
            additionalPropertyFile = System.getProperty("user.home") + File.separator + ".cocoon/settings.properties";
            final File testFile = new File(additionalPropertyFile);
            if ( !testFile.exists() ) {
                additionalPropertyFile = null;
            }
        }
        if ( additionalPropertyFile != null ) {
            this.environmentContext.log("Reading user settings from '" + additionalPropertyFile + "'");
            final Properties p = new Properties();
            try {
                FileInputStream fis = new FileInputStream(additionalPropertyFile);
                p.load(fis);
                fis.close();
            } catch (IOException ignore) {
                this.environmentContext.log("Unable to read '" + additionalPropertyFile + "'.", ignore);
                this.environmentContext.log("Continuing initialization.");
            }
        }
        // now overwrite with system properties
        s.fill(System.getProperties());

        return s;
    }

    /**
     * Dump System Properties.
     */
    protected void dumpSystemProperties() {
        if (this.log.isDebugEnabled()) {
            try {
                Enumeration e = System.getProperties().propertyNames();
                this.log.debug("===== System Properties Start =====");
                for (; e.hasMoreElements();) {
                    String key = (String) e.nextElement();
                    this.log.debug(key + "=" + System.getProperty(key));
                }
                this.log.debug("===== System Properties End =====");
            } catch (SecurityException se) {
                // Ignore Exceptions.
            }
        }
    }

    /**
     * Read all property files from the given directory and apply them to the settings.
     */
    protected void readProperties(String          directoryName,
                                  MutableSettings s,
                                  SourceResolver  resolver) {
        Source directory = null;
        try {
            directory = resolver.resolveURI(directoryName, null, CONTEXT_PARAMETERS);
            if (directory.exists() && directory instanceof TraversableSource) {
                final Iterator c = ((TraversableSource) directory).getChildren().iterator();
                while (c.hasNext()) {
                    final Source src = (Source) c.next();
                    if ( src.getURI().endsWith(".properties") ) {
                        final InputStream propsIS = src.getInputStream();
                        this.environmentContext.log("Reading settings from '" + src.getURI() + "'.");
                        final Properties p = new Properties();
                        p.load(propsIS);
                        propsIS.close();
                        s.fill(p);
                    }
                }
            }
        } catch (IOException ignore) {
            this.environmentContext.log("Unable to read from directory 'WEB-INF/properties'.", ignore);
            this.environmentContext.log("Continuing initialization.");            
        } finally {
            resolver.release(directory);
        }
    }

    /**
     * Create a simple source resolver.
     */
    protected SourceResolver createSourceResolver(Logger logger) {
        // Create our own resolver
        final SimpleSourceResolver resolver = new SimpleSourceResolver();
        resolver.enableLogging(logger);
        try {
            resolver.contextualize(this.appContext);
        } catch (ContextException ce) {
            throw new CoreInitializationException(
                    "Cannot setup source resolver.", ce);
        }
        return resolver;        
    }

    /**
     * Create the classloader that inlcudes all the [block]/BLOCK-INF/classes directories. 
     * @throws Exception
     */
    protected void createClassloader() throws Exception {
        // get the wiring
        final SourceResolver resolver = this.createSourceResolver(this.log);    
        Source wiringSource = null;
        final Configuration wiring;
        try {
            wiringSource = resolver.resolveURI(Constants.WIRING);
            DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
            wiring = builder.build( wiringSource.getInputStream() );            
        } catch(org.apache.excalibur.source.SourceNotFoundException snfe) {
            throw new WiringNotFoundException("wiring.xml not found in the root directory of your Cocoon application.");
        } finally {
            resolver.release(wiringSource);
        }
        
        // get all wired blocks and add their classed directory to the classloader
        List urlList = new ArrayList();        
        Configuration[] blocks = wiring.getChildren("block");
        for(int i = 0; i < blocks.length; i++) {
            String location = blocks[i].getAttribute("location");
            if(this.log.isDebugEnabled()) {
                this.log.debug("Found block " + blocks[i].getAttribute("id") + " at " + location);
            }
            Source classesDir = null;
            try {
               classesDir = resolver.resolveURI(location + "/" + Constants.BLOCK_META_DIR + "/classes");
               if(classesDir.exists()) {
                   String classesDirURI = classesDir.getURI();
                   urlList.add(new URL(classesDirURI));
                   if(this.log.isDebugEnabled()) {
                       this.log.debug("added " + classesDir.getURI());
                   }
               }               
            } finally {
                resolver.release(classesDir);
            }
        }
    
        // setup the classloader using the current classloader as parent
        ClassLoader parentClassloader = Thread.currentThread().getContextClassLoader();
        URL[] urls = (URL[]) urlList.toArray(new URL[urlList.size()]);        
        URLClassLoader classloader = new URLClassLoader(urls, parentClassloader);
        Thread.currentThread().setContextClassLoader(classloader);
        this.classloader = Thread.currentThread().getContextClassLoader();
    }

    /**
     * Creates the Cocoon object and handles exception handling.
     */
    public synchronized Cocoon createCocoon()
    throws Exception {        
        this.createProcessor();
        return (Cocoon)this.processor;
    }

    /**
     * Gets the current cocoon object.
     * Reload cocoon if configuration changed or we are reloading.
     * Ensure that the correct classloader is set.
     */
    public Cocoon getCocoon(final String pathInfo, final String reloadParam)
    throws Exception {
        this.getProcessor(pathInfo, reloadParam);
        return (Cocoon)this.processor;
    }

    /**
     * Creates the root processor object and handles exception handling.
     */
    public synchronized Processor createProcessor()
    throws Exception {

        this.updateEnvironment();
        this.forceLoad();

        try {
            if (this.log.isInfoEnabled()) {
                this.log.info("Reloading from: " + this.settings.getConfiguration());
            }
            Processor p = (Processor)this.container.getBean("org.apache.cocoon.Cocoon");

            this.settings.setCreationTime(System.currentTimeMillis());
            this.processor = p;
        } catch (Exception e) {
            this.log.error("Exception reloading root processor.", e);
            throw e;
        }
        return this.processor;
    }

    /**
     * Gets the current root processor object.
     * Reload the root processor if configuration changed or we are reloading.
     * Ensure that the correct classloader is set.
     */
    public Processor getProcessor(final String pathInfo, final String reloadParam)
    throws Exception {
        // set the blocks classloader for this thread
        Thread.currentThread().setContextClassLoader(this.classloader);        
        
        if (this.settings.isReloadingEnabled("config")) {
            boolean reload = false;

            if (this.processor != null) {
                if (this.settings.getCreationTime() < this.configurationFile.getLastModified()) {
                    if (this.log.isInfoEnabled()) {
                        this.log.info("Configuration changed reload attempt");
                    }
                    reload = true;
                } else if (pathInfo == null && reloadParam != null) {
                    if (this.log.isInfoEnabled()) {
                        this.log.info("Forced reload attempt");
                    }
                    reload = true;
                }
            } else if (pathInfo == null && reloadParam != null) {
                if (this.log.isInfoEnabled()) {
                    this.log.info("Invalid configurations reload");
                }
                reload = true;
            }

            if (reload) {
                if (this.container != null) {
                    this.container = null;
                }
                this.init();
                this.createProcessor();
            }
        }
        return this.processor;
    }

    protected ConfigurableBeanFactory setupSpringContainer() throws Exception {
        if (this.log.isInfoEnabled()) {
            this.log.info("Reading root configuration: " + this.settings.getConfiguration());
        }

        URLSource urlSource = new URLSource();
        urlSource.init(new URL(this.settings.getConfiguration()), null);
        this.configurationFile = new DelayedRefreshSourceWrapper(urlSource,
                this.settings.getReloadDelay("config"));
        final InputSource is = SourceUtil.getInputSource(this.configurationFile);

        final ConfigurationBuilder builder = new ConfigurationBuilder(settings);
        final Configuration rootConfig = builder.build(is);

        if (!"cocoon".equals(rootConfig.getName())) {
            throw new ConfigurationException("Invalid configuration file\n" + rootConfig.toString());
        }
        if (this.log.isDebugEnabled()) {
            this.log.debug("Configuration version: " + rootConfig.getAttribute("version"));
        }
        if (!Constants.CONF_VERSION.equals(rootConfig.getAttribute("version"))) {
            throw new ConfigurationException("Invalid configuration schema version. Must be '" + Constants.CONF_VERSION + "'.");
        }

        if (this.log.isInfoEnabled()) {
            this.log.info("Setting up root Spring container.");
        }
        AvalonEnvironment env = new AvalonEnvironment();
        env.context = this.appContext;
        env.core = this.core;
        env.logger = this.log;
        env.servletContext = this.environmentContext;
        env.settings = this.core.getSettings();
        ConfigurableBeanFactory rootContext = BeanFactoryUtil.createRootApplicationContext(env);
        ConfigurationInfo result = ConfigReader.readConfiguration(settings.getConfiguration(), env);
        ConfigurableBeanFactory mainContext = BeanFactoryUtil.createApplicationContext(env, result, rootContext, true);

        return mainContext;
    }
    /**
     * @see org.apache.cocoon.core.BootstrapEnvironment#getConfigFile(java.lang.String)
     */
    protected URL getConfigFile(final String configFileName)
    throws Exception {
        final String usedFileName;

        if (configFileName == null) {
            if (this.log.isWarnEnabled()) {
                this.log.warn("No configuration for Cocoon configuration file specified, attempting to use '/WEB-INF/cocoon.xconf'");
            }
            usedFileName = "/WEB-INF/cocoon.xconf";
        } else {
            usedFileName = configFileName;
        }

        if (this.log.isDebugEnabled()) {
            this.log.debug("Using configuration file: " + usedFileName);
        }

        URL result;
        try {
            // test if this is a qualified url
            if (usedFileName.indexOf(':') == -1) {
                result = this.environmentContext.getResource(usedFileName);
            } else {
                result = new URL(usedFileName);
            }
        } catch (Exception mue) {
            String msg = "Setting for 'configurations' is invalid : " + usedFileName;
            this.log.error(msg, mue);
            throw new CoreInitializationException(msg, mue);
        }

        if (result == null) {
            File resultFile = new File(usedFileName);
            if (resultFile.isFile()) {
                try {
                    result = resultFile.getCanonicalFile().toURL();
                } catch (Exception e) {
                    String msg = "Init parameter 'configurations' is invalid : " + usedFileName;
                    this.log.error(msg, e);
                    throw new CoreInitializationException(msg, e);
                }
            }
        }

        if (result == null) {
            String msg = "Init parameter 'configuration' doesn't name an existing resource : " + usedFileName;
            this.log.error(msg);
            throw new CoreInitializationException(msg);
        }
        return result;
    }

    protected String getContextUrl() {
        String servletContextURL;
        String servletContextPath = this.environmentContext.getRealPath("/");
        String path = servletContextPath;

        if (path == null) {
            // Try to figure out the path of the root from that of WEB-INF/web.xml
            try {
                path = this.environmentContext.getResource("/WEB-INF/web.xml").toString();
            } catch (MalformedURLException me) {
                throw new CoreInitializationException("Unable to get resource 'WEB-INF/web.xml'.", me);
            }
            path = path.substring(0, path.length() - "WEB-INF/web.xml".length());
        }
        try {
            if (path.indexOf(':') > 1) {
                servletContextURL = path;
            } else {
                servletContextURL = new File(path).toURL().toExternalForm();
            }
        } catch (MalformedURLException me) {
            // VG: Novell has absolute file names starting with the
            // volume name which is easily more then one letter.
            // Examples: sys:/apache/cocoon or sys:\apache\cocoon
            try {
                servletContextURL = new File(path).toURL().toExternalForm();
            } catch (MalformedURLException ignored) {
                throw new CoreInitializationException("Unable to determine servlet context URL.", me);
            }
        }
        return servletContextURL;
    }

    /**
     * Handle the <code>load-class</code> parameter. This overcomes
     * limits in many classpath issues. One of the more notorious
     * ones is a bug in WebSphere that does not load the URL handler
     * for the <code>classloader://</code> protocol. In order to
     * overcome that bug, set <code>load-class</code> parameter to
     * the <code>com.ibm.servlet.classloader.Handler</code> value.
     *
     * <p>If you need to load more than one class, then separate each
     * entry with whitespace, a comma, or a semi-colon. Cocoon will
     * strip any whitespace from the entry.</p>
     */
    protected void forceLoad() {
        final Iterator i = this.settings.getLoadClasses().iterator();
        while (i.hasNext()) {
            final String fqcn = (String)i.next();
            try {
                if (this.log.isDebugEnabled()) {
                    this.log.debug("Loading: " + fqcn);
                }
                ClassUtils.loadClass(fqcn).newInstance();
            } catch (Exception e) {
                if (this.log.isWarnEnabled()) {
                    this.log.warn("Could not load class: " + fqcn, e);
                }
                // Do not throw an exception, because it is not a fatal error.
            }
        }
    }

    /**
     * Method to update the environment before Cocoon instances are created.
     *
     * This is also useful if you wish to customize any of the 'protected'
     * variables from this class before a Cocoon instance is built in a derivative
     * of this class (eg. Cocoon Context).
     */
    protected void updateEnvironment() throws Exception {
//        // concatenate the class path and the extra class path
//        String classPath = this.env.getClassPath(this.settings);
//        StringBuffer buffer = new StringBuffer();
//        if ( classPath != null && classPath.length() > 0 ) {
//            buffer.append(classPath);
//        }
//        classPath = this.getExtraClassPath();
//        if ( classPath != null && classPath.length() > 0 ) {
//            if ( buffer.length() > 0 ) {
//                buffer.append(File.pathSeparatorChar);
//            }
//            buffer.append(classPath);
//        }
        // FIXME - for now we just set an empty string as this information is looked up
        //         by other components
        this.appContext.put(Constants.CONTEXT_CLASSPATH, "");
    }

    /**
     * Dispose the root processor when environment is destroyed
     */
    public void destroy() {
        if ( this.container != null ) {
            this.container.destroySingletons();
            this.container = null;
        }
    }

    public ConfigurableBeanFactory getContainer() {
        return this.container;
    }

    /**
     * Retreives the "extra-classpath" attribute, that needs to be
     * added to the class path.
     */
    protected String getExtraClassPath() {
        if (this.settings.getExtraClasspaths().size() > 0) {
            StringBuffer sb = new StringBuffer();
            final Iterator iter = this.settings.getExtraClasspaths().iterator();
            int i = 0;
            while (iter.hasNext()) {
                String s = (String)iter.next();
                if (i++ > 0) {
                    sb.append(File.pathSeparatorChar);
                }
                if ((s.charAt(0) == File.separatorChar) ||
                        (s.charAt(1) == ':')) {
                    if (this.log.isDebugEnabled()) {
                        this.log.debug("extraClassPath is absolute: " + s);
                    }
                    sb.append(s);

                } else {
                    if (s.indexOf("${") != -1) {
                        String path = StringUtils.replaceToken(s);
                        sb.append(path);
                        if (this.log.isDebugEnabled()) {
                            this.log.debug("extraClassPath is not absolute replacing using token: [" + s + "] : " + path);
                        }
                    } else {
                        String path = this.settings.getWorkDirectory() + s;
                        if (this.log.isDebugEnabled()) {
                            this.log.debug("extraClassPath is not absolute pre-pending work-directory: " + path);
                        }
                        sb.append(path);
                    }
                }
            }
            return sb.toString();
        }
        return "";
    }

    protected static final class LoggerWrapper implements Logger {
        private final Context env;

        public LoggerWrapper(Context env) {
            this.env = env;
        }

        protected void text(String arg0, Throwable arg1) {
            if ( arg1 != null ) {
                this.env.log(arg0, arg1);
            } else {
                this.env.log(arg0);
            }
        }

        public void debug(String arg0, Throwable arg1) {
            // we ignore debug
        }

        public void debug(String arg0) {
            // we ignore debug
        }

        public void error(String arg0, Throwable arg1) {
            this.text(arg0, arg1);
        }

        public void error(String arg0) {
            this.text(arg0, null);
        }

        public void fatalError(String arg0, Throwable arg1) {
            this.text(arg0, arg1);
        }

        public void fatalError(String arg0) {
            this.text(arg0, null);
        }

        public Logger getChildLogger(String arg0) {
            return this;
        }

        public void info(String arg0, Throwable arg1) {
            // we ignore info
        }

        public void info(String arg0) {
            // we ignore info
        }

        public boolean isDebugEnabled() {
            return false;
        }

        public boolean isErrorEnabled() {
            return true;
        }

        public boolean isFatalErrorEnabled() {
            return true;
        }

        public boolean isInfoEnabled() {
            return false;
        }

        public boolean isWarnEnabled() {
            return false;
        }

        public void warn(String arg0, Throwable arg1) {
            // we ignore warn
        }

        public void warn(String arg0) {
            // we ignore warn
        }
    }
}