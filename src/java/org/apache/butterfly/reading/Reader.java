/*
 * Copyright 1999-2004 The Apache Software Foundation.
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
package org.apache.butterfly.reading;

import org.apache.butterfly.sitemap.SitemapOutputComponent;



/**
 * A reader can be used to generate binary output for a request.
 *
 * @version CVS $Id: Reader.java,v 1.1 2004/07/23 08:47:20 ugo Exp $
 */
public interface Reader extends SitemapOutputComponent {

    /**
     * Generate the response.
     */
    void generate();

    /**
     * @return the time the read source was last modified or 0 if it is not
     *         possible to detect
     */
    long getLastModified();
}
