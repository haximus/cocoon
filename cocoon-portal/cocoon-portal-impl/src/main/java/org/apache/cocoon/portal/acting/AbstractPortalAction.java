/*
 * Copyright 1999-2005 The Apache Software Foundation.
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
package org.apache.cocoon.portal.acting;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.cocoon.acting.ServiceableAction;
import org.apache.cocoon.portal.PortalService;

/**
 * This abstract action provides access to the {@link PortalService}.
 *
 * @version $Id$
 */
public abstract class AbstractPortalAction
    extends ServiceableAction
    implements ThreadSafe, Disposable {

    protected PortalService portalService;

    /**
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    public void dispose() {
        if ( this.manager != null ) {
            this.manager.release(this.portalService);
            this.portalService = null;
            this.manager = null;
        }
    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager manager) throws ServiceException {
        super.service(manager);
        this.portalService = (PortalService) this.manager.lookup(PortalService.ROLE);
    }
}
