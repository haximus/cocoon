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
package org.apache.cocoon.components.treeprocessor.sitemap;

import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.components.treeprocessor.InvokeContext;
import org.apache.cocoon.components.treeprocessor.ParameterizableProcessingNode;
import org.apache.cocoon.components.treeprocessor.SimpleSelectorProcessingNode;
import org.apache.cocoon.components.treeprocessor.variables.VariableResolver;
import org.apache.cocoon.environment.Environment;
import org.apache.cocoon.matching.Matcher;
import org.apache.cocoon.sitemap.PatternException;

/**
 *
 * @author <a href="mailto:sylvain@apache.org">Sylvain Wallez</a>
 * @version CVS $Id: MatchNode.java,v 1.5 2004/07/15 12:49:50 sylvain Exp $
 */

public class MatchNode extends SimpleSelectorProcessingNode
        implements ParameterizableProcessingNode {

    /** The 'pattern' attribute */
    private VariableResolver pattern;

    /** The 'name' for the variable anchor */
    private String name;

    private Map parameters;

    public MatchNode(String type, VariableResolver pattern, String name) throws PatternException {
        super(Matcher.ROLE + "Selector", type);
        this.pattern = pattern;
        this.name = name;
    }

    public void setParameters(Map parameterMap) {
        this.parameters = parameterMap;
    }

    public final boolean invoke(Environment env, InvokeContext context)
      throws Exception {
	
        // Perform any common invoke functionality 
        super.invoke(env, context);

        Map objectModel = env.getObjectModel();

        String resolvedPattern = pattern.resolve(context, objectModel);
        Parameters resolvedParams = VariableResolver.buildParameters(this.parameters, context, objectModel);

        Map result = null;

        if (this.hasThreadSafeComponent()) {
            // Avoid select() and try/catch block (faster !)
            result = this.executor.invokeMatcher(this, 
                                                 objectModel, 
                                                 (Matcher)this.getThreadSafeComponent(), 
                                                 resolvedPattern, 
                                                 resolvedParams);
        } else {
            // Get matcher from selector
            Matcher matcher = (Matcher)this.selector.select(this.componentName);
            try {
                result = this.executor.invokeMatcher(this, 
                        objectModel, 
                        matcher, 
                        resolvedPattern, 
                        resolvedParams);
            } finally {
                this.selector.release(matcher);
            }
        }

        if (result != null) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Matcher '" + this.componentName + "' matched pattern '" + this.pattern +
                    "' at " + this.getLocation());
            }

            // Invoke children with the matcher results
            return this.invokeNodes(children, env, context, name, result);
        } else {
            // Matcher failed
            return false;
        }
    }
}
