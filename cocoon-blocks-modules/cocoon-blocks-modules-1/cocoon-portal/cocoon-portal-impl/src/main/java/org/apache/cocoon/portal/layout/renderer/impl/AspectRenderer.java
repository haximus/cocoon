/*
 * Copyright 1999-2002,2004-2005 The Apache Software Foundation.
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
package org.apache.cocoon.portal.layout.renderer.impl;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceSelector;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.cocoon.components.ContextHelper;
import org.apache.cocoon.portal.PortalService;
import org.apache.cocoon.portal.layout.Layout;
import org.apache.cocoon.portal.layout.renderer.Renderer;
import org.apache.cocoon.portal.layout.renderer.aspect.RendererAspect;
import org.apache.cocoon.portal.layout.renderer.aspect.impl.DefaultRendererContext;
import org.apache.cocoon.portal.layout.renderer.aspect.impl.RendererAspectChain;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Container for chain of aspect renderers. All aspect renderers are applied in order
 * of appearance.
 *
 * <h2>Configuration</h2>
 * <table><tbody>
 * <tr><th>aspects</th><td>List of aspect renderers to apply. See 
 *      {@link org.apache.cocoon.portal.layout.renderer.aspect.impl.RendererAspectChain}</td>
 *      <td></td><td>Configuration</td><td><code>EmptyConfiguration</code></td></tr>
 * </tbody></table>
 *
 * @version $Id$
 */
public class AspectRenderer
    extends AbstractLogEnabled
    implements Renderer, Serviceable, Configurable, Disposable, ThreadSafe, Contextualizable {

    protected ServiceManager manager;

    protected RendererAspectChain chain;

    protected ServiceSelector aspectSelector;

    protected Context context;
    
    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager aManager) throws ServiceException {
        this.manager = aManager;
        this.aspectSelector = (ServiceSelector) this.manager.lookup( RendererAspect.ROLE+"Selector");
    }

    /**
     * Stream out raw layout 
     */
    public void toSAX(Layout layout, PortalService service, ContentHandler handler) throws SAXException {
        DefaultRendererContext renderContext = new DefaultRendererContext(this.chain);
        renderContext.setObjectModel(ContextHelper.getObjectModel(this.context));
        renderContext.invokeNext(layout, service, handler);
    }

	/**
	 * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
	 */
	public void configure(Configuration conf) throws ConfigurationException {
        this.chain = new RendererAspectChain();
        this.chain.configure(this.aspectSelector, conf.getChild("aspects"));
	}

	/**
	 * @see org.apache.avalon.framework.activity.Disposable#dispose()
	 */
	public void dispose() {
		if (this.manager != null) {
            if ( this.chain != null) {
                this.chain.dispose( this.aspectSelector );
            }
            this.manager.release( this.aspectSelector );
            this.aspectSelector = null;
            this.manager = null;
		}
	}

    /**
     * @see org.apache.avalon.framework.context.Contextualizable#contextualize(org.apache.avalon.framework.context.Context)
     */
    public void contextualize(Context aContext) throws ContextException {
        this.context = aContext;
    }
}