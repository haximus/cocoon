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
package org.apache.cocoon.components.language.programming.python;

import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceManager;

import org.apache.cocoon.components.language.programming.Program;
import org.apache.cocoon.components.language.generator.CompiledComponent;
import org.apache.cocoon.core.container.AbstractComponentHandler;
import org.apache.cocoon.core.container.ComponentEnvironment;
import org.apache.cocoon.core.container.ComponentHandler;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

/**
 * This class represents program in the Python language.
 *
 * @author <a href="mailto:vgritsenko@apache.org">Vadim Gritsenko</a>
 * @version CVS $Id$
 */
public class PythonProgram extends AbstractLogEnabled implements Program {

    protected File file;
    protected Class clazz;
    protected DefaultConfiguration config;

    public PythonProgram(File file, Class clazz, Collection dependecies) {
        this.file = file;
        this.clazz = clazz;

        config = new DefaultConfiguration("", "GeneratorSelector");
        DefaultConfiguration child = new DefaultConfiguration("file", "");
        child.setValue(file.toString());
        config.addChild(child);

        for (Iterator i = dependecies.iterator(); i.hasNext(); ) {
            child = new DefaultConfiguration("dependency", "");
            child.setValue(i.next().toString());
            config.addChild(child);
        }
    }

    public String getName() {
        return file.toString();
    }

    public ComponentHandler getHandler(ServiceManager manager,
                                       Context context)
    throws Exception {
        final ComponentEnvironment env = new ComponentEnvironment();
        env.serviceManager = manager;
        env.context = context;
        env.logger = this.getLogger();

        return AbstractComponentHandler.getComponentHandler(
                null, clazz, config, env, null);
    }

    public CompiledComponent newInstance() throws Exception {
        CompiledComponent instance = (CompiledComponent) clazz.newInstance();
        if (instance instanceof Configurable)
            ((Configurable) instance).configure(config);
        return instance;
    }
}
