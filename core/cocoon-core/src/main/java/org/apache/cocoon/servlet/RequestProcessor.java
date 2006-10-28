/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.servlet;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.logger.Logger;
import org.apache.cocoon.ConnectionResetException;
import org.apache.cocoon.Constants;
import org.apache.cocoon.ProcessingUtil;
import org.apache.cocoon.Processor;
import org.apache.cocoon.RequestListener;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.components.notification.DefaultNotifyingBuilder;
import org.apache.cocoon.components.notification.Notifier;
import org.apache.cocoon.components.notification.Notifying;
import org.apache.cocoon.configuration.Settings;
import org.apache.cocoon.environment.Context;
import org.apache.cocoon.environment.Environment;
import org.apache.cocoon.environment.http.HttpContext;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.environment.internal.EnvironmentHelper;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * This is the entry point for Cocoon execution as an HTTP Servlet.
 *
 * @version $Id$
 */
public class RequestProcessor {

    // Processing time message
    protected static final String PROCESSED_BY = "Processed by Apache Cocoon in ";

    // Used by "show-time"
    static final float SECOND = 1000;
    static final float MINUTE = 60 * SECOND;
    static final float HOUR   = 60 * MINUTE;

    /** The servlet context. */
    protected final ServletContext servletContext;

    protected final String containerEncoding;

    /** The logger. */
    protected final Logger log;

    protected final Context environmentContext;

    /** Root Cocoon Bean Factory. */
    protected final BeanFactory cocoonBeanFactory;

    /** The root settings. */
    protected final Settings settings;

    /** The processor. */
    protected Processor processor;

    /**
     * An optional component that is called before and after processing all
     * requests.
     */
    protected RequestListener requestListener;

    public RequestProcessor(ServletContext servletContext) {
        this.servletContext = servletContext;
        this.cocoonBeanFactory = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        this.settings = (Settings) this.cocoonBeanFactory.getBean(Settings.ROLE);
        final String encoding = this.settings.getContainerEncoding();
        if ( encoding == null ) {
            this.containerEncoding = "ISO-8859-1";
        } else {
            this.containerEncoding = encoding;
        }
        this.log = (Logger) this.cocoonBeanFactory.getBean(ProcessingUtil.LOGGER_ROLE);
        this.processor = (Processor)this.cocoonBeanFactory.getBean(Processor.ROLE);
        this.environmentContext = new HttpContext(this.servletContext);
        // get the optional request listener
        if (this.cocoonBeanFactory.containsBean(RequestListener.ROLE)) {
            this.requestListener = (RequestListener) this.cocoonBeanFactory.getBean(RequestListener.ROLE);
        }
    }

    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

    /**
     * Process the specified <code>HttpServletRequest</code> producing output
     * on the specified <code>HttpServletResponse</code>.
     */
    public void service(HttpServletRequest request, HttpServletResponse res)
    throws ServletException, IOException {        
        // used for timing the processing
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // add the cocoon header timestamp
        if (this.settings.isShowVersion()) {
            res.addHeader("X-Cocoon-Version", Constants.VERSION);
        }

        // We got it... Process the request
        final String uri = RequestUtil.getCompleteUri(request, res);
        if ( uri == null ) {
            // a redirect occured, so we are finished
            return;
        }
        String contentType = null;

        Environment env;
        try{
            // Pass uri into environment without URLDecoding, as it is already decoded.
            env = getEnvironment(uri, request, res);
        } catch (Exception e) {
            if (getLogger().isErrorEnabled()) {
                getLogger().error("Problem with Cocoon servlet", e);
            }

            manageException(request, res, null, uri,
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Problem in creating the Environment", null, null, e);
            return;
        }

        try {
            if (this.process(env)) {
                contentType = env.getContentType();
            } else {
                // We reach this when there is nothing in the processing change that matches
                // the request. For example, no matcher matches.
                getLogger().fatalError("The Cocoon engine failed to process the request.");
                manageException(request, res, env, uri,
                                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                "Request Processing Failed",
                                "Cocoon engine failed in process the request",
                                "The processing engine failed to process the request. This could be due to lack of matching or bugs in the pipeline engine.",
                                null);
                return;
            }
        } catch (ResourceNotFoundException e) {
            if (getLogger().isDebugEnabled()) {
                getLogger().warn(e.getMessage(), e);
            } else if (getLogger().isWarnEnabled()) {
                getLogger().warn(e.getMessage());
            }

            manageException(request, res, env, uri,
                            HttpServletResponse.SC_NOT_FOUND,
                            "Resource Not Found",
                            "Resource Not Found",
                            "The requested resource \"" + request.getRequestURI() + "\" could not be found",
                            e);
            return;

        } catch (ConnectionResetException e) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug(e.toString(), e);
            } else if (getLogger().isWarnEnabled()) {
                getLogger().warn(e.toString());
            }

        } catch (IOException e) {
            // Tomcat5 wraps SocketException into ClientAbortException which extends IOException.
            if (getLogger().isDebugEnabled()) {
                getLogger().debug(e.toString(), e);
            } else if (getLogger().isWarnEnabled()) {
                getLogger().warn(e.toString());
            }

        } catch (Exception e) {
            if (getLogger().isErrorEnabled()) {
                getLogger().error("Internal Cocoon Problem", e);
            }

            manageException(request, res, env, uri,
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Internal Server Error", null, null, e);
            return;
        }

        stopWatch.stop();
        String timeString = null;
        if (getLogger().isInfoEnabled()) {
            timeString = processTime(stopWatch.getTime());
            getLogger().info("'" + uri + "' " + timeString);
        }

        if (contentType != null && contentType.equals("text/html")) {
            String showTime = request.getParameter(Constants.SHOWTIME_PARAM);
            boolean show = this.settings.isShowTime();
            if (showTime != null) {
                show = !showTime.equalsIgnoreCase("no");
            }
            if (show) {
                if ( timeString == null ) {
                    timeString = processTime(stopWatch.getTime());
                }
                boolean hide = this.settings.isHideShowTime();
                if (showTime != null) {
                    hide = showTime.equalsIgnoreCase("hide");
                }
                ServletOutputStream out = res.getOutputStream();
                out.print((hide) ? "<!-- " : "<p>");
                out.print(timeString);
                out.println((hide) ? " -->" : "</p>");
            }
        }
        
        /*
         * Servlet Specification 2.2, 6.5 Closure of Response Object:
         *
         *   A number of events can indicate that the servlet has provided all of the
         *   content to satisfy the request and that the response object can be
         *   considered to be closed. The events are:
         *     o The termination of the service method of the servlet.
         *     o When the amount of content specified in the setContentLength method
         *       of the response has been written to the response.
         *     o The sendError method is called.
         *     o The sendRedirect method is called.
         *   When a response is closed, all content in the response buffer, if any remains,
         *   must be immediately flushed to the client.
         *
         * Due to the above, out.flush() and out.close() are not necessary, and sometimes
         * (if sendError or sendRedirect were used) request may be already closed.
         */
    }

    protected void manageException(HttpServletRequest req, HttpServletResponse res, Environment env,
                                   String uri, int errorStatus,
                                   String title, String message, String description,
                                   Exception e)
    throws IOException {
        if (this.settings.isManageExceptions()) {
            if (env != null) {
                env.tryResetResponse();
            } else {
                res.reset();
            }

            String type = Notifying.FATAL_NOTIFICATION;
            HashMap extraDescriptions = null;

            if (errorStatus == HttpServletResponse.SC_NOT_FOUND) {
                type = "resource-not-found";
                // Do not show the exception stacktrace for such common errors.
                e = null;
            } else {
                extraDescriptions = new HashMap(2);
                extraDescriptions.put(Notifying.EXTRA_REQUESTURI, req.getRequestURI());
                if (uri != null) {
                     extraDescriptions.put("Request URI", uri);
                }

                // Do not show exception stack trace when log level is WARN or above. Show only message.
                if (!getLogger().isInfoEnabled()) {
                    Throwable t = DefaultNotifyingBuilder.getRootCause(e);
                    if (t != null) extraDescriptions.put(Notifying.EXTRA_CAUSE, t.getMessage());
                    e = null;
                }
            }

            Notifying n = new DefaultNotifyingBuilder().build(this,
                                                              e,
                                                              type,
                                                              title,
                                                              "Cocoon Servlet",
                                                              message,
                                                              description,
                                                              extraDescriptions);

            res.setContentType("text/html");
            res.setStatus(errorStatus);
            Notifier.notify(n, res.getOutputStream(), "text/html");
        } else {
            res.sendError(errorStatus, title);
            res.flushBuffer();
        }
    }

    /**
     * Create the environment for the request
     */
    protected Environment getEnvironment(String uri,
                                         HttpServletRequest req,
                                         HttpServletResponse res)
    throws Exception {
        HttpEnvironment env;

        String formEncoding = req.getParameter("cocoon-form-encoding");
        if (formEncoding == null) {
            formEncoding = this.settings.getFormEncoding();
        }
        env = new HttpEnvironment(uri,
                                  req,
                                  res,
                                  this.servletContext,
                                  this.environmentContext,
                                  this.containerEncoding,
                                  formEncoding);
        env.enableLogging(getLogger());
        return env;
    }

    private String processTime(long time) {
        StringBuffer out = new StringBuffer(PROCESSED_BY);
        if (time <= SECOND) {
            out.append(time);
            out.append(" milliseconds.");
        } else if (time <= MINUTE) {
            out.append(time / SECOND);
            out.append(" seconds.");
        } else if (time <= HOUR) {
            out.append(time / MINUTE);
            out.append(" minutes.");
        } else {
            out.append(time / HOUR);
            out.append(" hours.");
        }
        return out.toString();
    }

    protected Logger getLogger() {
        return this.log;
    }

    /**
     * @see org.apache.cocoon.Processor#process(org.apache.cocoon.environment.Environment)
     */
    protected boolean process(Environment environment) throws Exception {
        environment.startingProcessing();
        final int environmentDepth = EnvironmentHelper.markEnvironment();
        EnvironmentHelper.enterProcessor(this.processor, environment);
        try {
            boolean result;

            if (this.requestListener != null) {
                try {
                    requestListener.onRequestStart(environment);
                } catch (Exception e) {
                    this.log.error("Error encountered monitoring request start: "
                            + e.getMessage());
                }
            }
            result = this.processor.process(environment);

            if (this.requestListener != null) {
                try {
                    requestListener.onRequestEnd(environment);
                } catch (Exception e) {
                    this.log.error("Error encountered monitoring request start: "
                            + e.getMessage());
                }
            }

            // commit response on success
            environment.commitResponse();

            return result;
        } catch (Exception any) {
            if (this.requestListener != null) {
                try {
                    requestListener.onRequestException(environment, any);
                } catch (Exception e) {
                    this.log.error("Error encountered monitoring request start: "
                            + e.getMessage());
                }
            }
            // reset response on error
            environment.tryResetResponse();
            throw any;
        } finally {
            EnvironmentHelper.leaveProcessor();
            environment.finishingProcessing();
            ProcessingUtil.cleanup();

            EnvironmentHelper.checkEnvironment(environmentDepth, this.getLogger());
        }
    }
}