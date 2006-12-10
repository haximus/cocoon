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
package org.apache.cocoon.core.xml.impl;

import org.apache.cocoon.util.AbstractLogEnabled;
import org.xml.sax.EntityResolver;

/**
 * An abstract base class for implementing Jaxp based parsers.
 *
 * @see JaxpDOMParser
 * @see JaxpSAXParser
 *
 * @version $Id$
 * @since 2.2
 */
public abstract class AbstractJaxpParser
    extends AbstractLogEnabled {

    /** the Entity Resolver */
    protected EntityResolver resolver;

    /** do we want to reuse parsers ? */
    protected boolean reuseParsers = true;

    /** Do we want to validate? */
    protected boolean validate = false;

    public void setEntityResolver(EntityResolver r) {
        this.resolver = r;
    }

    public EntityResolver getEntityResolver() {
        return this.resolver;
    }

    /**
     * @see #setReuseParsers(boolean)
     */
    public boolean isReuseParsers() {
        return reuseParsers;
    }

    /**
     * Do we want to reuse parsers or create a new parser for each parse ?
     * (Default is true)
     * <i>Note</i> : even if this parameter is <code>true</code>, parsers are not
     * recycled in case of parsing errors : some parsers (e.g. Xerces) don't like
     * to be reused after failure.
     */
    public void setReuseParsers(boolean reuseParsers) {
        this.reuseParsers = reuseParsers;
    }

    /**
     * @see #setValidate(boolean)
     */
    public boolean isValidate() {
        return validate;
    }

    /**
     * should the parser validate parsed documents ?
     * Default is false.
     */
    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    /**
     * Load a class
     */
    protected Class loadClass( String name ) throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if( loader == null ) {
            loader = getClass().getClassLoader();
        }
        return loader.loadClass( name );
    }
}
