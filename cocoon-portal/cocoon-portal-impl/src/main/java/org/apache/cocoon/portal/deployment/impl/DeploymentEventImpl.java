/*
 * Copyright 2005 The Apache Software Foundation.
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
package org.apache.cocoon.portal.deployment.impl;

import org.apache.cocoon.portal.deployment.DeploymentEvent;
import org.apache.cocoon.portal.deployment.DeploymentObject;

/**
 * Default implementation of the deployment event.
 *
 * @version $Id$
 */
public class DeploymentEventImpl implements DeploymentEvent {

    /** The corresponding deployment object. */
    protected DeploymentObject deploymentObject;

    /** The deployment status. */
    protected int status = STATUS_EVAL;

    /**
     * 
     */
    public DeploymentEventImpl(DeploymentObject depObject) {
        this.deploymentObject = depObject;
    }
    
    /**
     * @see org.apache.cocoon.portal.deployment.DeploymentEvent#getDeploymentObject()
     */
    public DeploymentObject getDeploymentObject() {        
        return this.deploymentObject;
    }

    /**
     * @see org.apache.cocoon.portal.deployment.DeploymentStatus#getStatus()
     */
    public int getStatus() {
        return this.status;
    }

    /**
     * @see org.apache.cocoon.portal.deployment.DeploymentEvent#setStatus(int)
     */
    public void setStatus(int i) {
        this.status = i;
    }
}
