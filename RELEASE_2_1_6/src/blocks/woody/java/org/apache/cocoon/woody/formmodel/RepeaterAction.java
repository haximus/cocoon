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
package org.apache.cocoon.woody.formmodel;

/**
 * An action that acts on a repeater.
 * 
 * @see RepeaterActionDefinitionBuilder
 * @author <a href="http://www.apache.org/~sylvain/">Sylvain Wallez</a>
 * @version CVS $Id: RepeaterAction.java,v 1.4 2004/03/09 13:53:55 reinhard Exp $
 */
public class RepeaterAction extends Action {
    
    private Repeater repeater;
    

    public RepeaterAction(ActionDefinition definition) {
        super(definition);
    }
    
    /**
     * Get the repeater on which this action acts.
     */
    public Repeater getRepeater() {
        if (this.repeater == null) {
            String name = ((RepeaterActionDefinition)this.definition).getRepeaterName();
            Widget widget;
            if (name != null) {
                // Get the corresponding sibling
                widget = getParent().getWidget(name);
            } else {
                // Get the grand-parent (parent is the repeater row).
                widget = getParent().getParent();
            }
         
            if (widget == null || !(widget instanceof Repeater)) {
                throw new RuntimeException(name != null ?
                    "Cannot find sibling repeater named '" + name + "'." :
                    "Parent widget is not a repeater");
            }
            
            this.repeater = (Repeater)widget;
        }
        
        return this.repeater;
    }
}
