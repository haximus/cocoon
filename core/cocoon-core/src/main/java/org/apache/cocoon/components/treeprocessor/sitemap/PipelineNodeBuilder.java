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
package org.apache.cocoon.components.treeprocessor.sitemap;

import java.util.ArrayList;
import java.util.List;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.cocoon.components.pipeline.ProcessingPipeline;
import org.apache.cocoon.components.treeprocessor.AbstractParentProcessingNodeBuilder;
import org.apache.cocoon.components.treeprocessor.ProcessingNode;
import org.apache.cocoon.components.treeprocessor.ProcessingNodeBuilder;
import org.apache.cocoon.components.treeprocessor.TreeBuilder;
import org.apache.cocoon.configuration.Settings;

/**
 * Builds a &lt;map:pipeline&gt;
 *
 * @version $Id$
 */
public class PipelineNodeBuilder extends AbstractParentProcessingNodeBuilder {

    private static final String ATTRIBUTE_INTERNAL_ONLY = "internal-only";

    private static final String PROPERTY_SITEMAP_INTERNALONLY = "org.apache.cocoon.sitemap.internalonly.disable";

    /** Will the ignore-internal flag of a pipeline be ignored? */
    protected boolean ignoreInternalOnly;

    /**
     * @see org.apache.cocoon.components.treeprocessor.AbstractProcessingNodeBuilder#setBuilder(org.apache.cocoon.components.treeprocessor.TreeBuilder)
     */
    public void setBuilder(TreeBuilder treeBuilder) {
        super.setBuilder(treeBuilder);
        // check ssettings for ignoring of internal only pipeline flags
        this.ignoreInternalOnly = false;
        final Settings settings = treeBuilder.getContainer().getSettings();
        final String value = settings.getProperty(PipelineNodeBuilder.PROPERTY_SITEMAP_INTERNALONLY);
        if ( value != null ) {
            this.ignoreInternalOnly = Boolean.valueOf(value).booleanValue();
        }
    }

    /** This builder can have parameters -- return <code>true</code> */
    protected boolean hasParameters() {
        return true;
    }

    /**
     * @see org.apache.cocoon.components.treeprocessor.ProcessingNodeBuilder#buildNode(org.apache.avalon.framework.configuration.Configuration)
     */
    public ProcessingNode buildNode(Configuration config)
    throws Exception {
        String type = this.treeBuilder.getTypeForStatement(config, ProcessingPipeline.ROLE);
        PipelineNode node = new PipelineNode(type);

        this.treeBuilder.setupNode(node, config);
        // if internal only is ignore, all pipelines are directly accessible
        if ( this.ignoreInternalOnly ) {
            node.setInternalOnly(false);
        } else {
            node.setInternalOnly(config.getAttributeAsBoolean(PipelineNodeBuilder.ATTRIBUTE_INTERNAL_ONLY, false));
        }

        // Main (with no "type" attribute) error handler: new in Cocoon 2.1, must have a generator
        ProcessingNode mainHandler = null;

        // 404 & 500 error handlers as in Cocoon 2.0.x, have an implicit generator
        ProcessingNode error404Handler = null;
        ProcessingNode error500Handler = null;

        Configuration[] childConfigs = config.getChildren();
        List children = new ArrayList();
        for (int i = 0; i < childConfigs.length; i++) {
            Configuration childConfig = childConfigs[i];
            if (isChild(childConfig)) {

                ProcessingNodeBuilder builder = this.treeBuilder.createNodeBuilder(childConfig);
                if (builder instanceof HandleErrorsNodeBuilder) {
                    // Error handler : check type
                    HandleErrorsNode handler = (HandleErrorsNode)builder.buildNode(childConfig);
                    int status = handler.getStatusCode();

                    switch(status) {
                        case -1: // main handler (needs generator)
                            if (mainHandler != null) {
                                throw new ConfigurationException("Duplicate <handle-errors> at " + handler.getLocation());
                            } else if (error500Handler != null || error404Handler != null) {
                                throw new ConfigurationException("Cannot mix <handle-errors> with and without 'type' attribute at " +
                                                                 handler.getLocation());
                            } else {
                                mainHandler = handler;
                            }
                            break;

                        case 404:
                            if (error404Handler != null) {
                                throw new ConfigurationException("Duplicate <handle-errors type='404' at " + handler.getLocation());
                            } else if(mainHandler != null) {
                                throw new ConfigurationException("Cannot mix <handle-errors> with and without 'type' attribute at " +
                                                                 handler.getLocation());
                            } else {
                                error404Handler = handler;
                            }
                            break;

                        case 500:
                            if (error500Handler != null) {
                                throw new ConfigurationException("Duplicate <handle-errors type='500' at " + handler.getLocation());
                            } else if (mainHandler != null) {
                                throw new ConfigurationException("Cannot mix <handle-errors> with and without 'type' attribute at " +
                                                                 handler.getLocation());
                            } else {
                                error500Handler = handler;
                            }
                            break;

                        default:
                            throw new ConfigurationException("Unknown handle-errors type (" + type + ") at " + handler.getLocation());
                    }
                } else {
                    // Regular builder
                    children.add(builder.buildNode(childConfig));
                }
            }
        }

        node.setChildren(toNodeArray(children));
        node.set404Handler(error404Handler);
        // Set either main or error500 handler as only one can exist
        node.set500Handler(error500Handler == null ? mainHandler : error500Handler);

        return node;
    }
}