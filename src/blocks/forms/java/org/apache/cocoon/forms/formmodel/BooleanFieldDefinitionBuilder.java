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
package org.apache.cocoon.forms.formmodel;

import java.util.Iterator;

import org.apache.cocoon.forms.Constants;
import org.apache.cocoon.forms.event.ValueChangedListener;
import org.apache.cocoon.forms.util.DomHelper;
import org.w3c.dom.Element;

/**
 * Builds {@link BooleanFieldDefinition}s.
 * 
 * @version $Id$
 */
public final class BooleanFieldDefinitionBuilder extends AbstractWidgetDefinitionBuilder {
    public WidgetDefinition buildWidgetDefinition(Element widgetElement) throws Exception {
        BooleanFieldDefinition definition = new BooleanFieldDefinition();
        
        super.setupDefinition(widgetElement, definition);
        setDisplayData(widgetElement, definition);
        Iterator iter = buildEventListeners(widgetElement, "on-value-changed", ValueChangedListener.class).iterator();
        while (iter.hasNext()) {
            definition.addValueChangedListener((ValueChangedListener)iter.next());
        }

        // Initial value
        Element initialValueElement = DomHelper.getChildElement(widgetElement, Constants.DEFINITION_NS, "initial-value", false);
        if (initialValueElement != null) {
            Boolean initialValue = Boolean.valueOf(DomHelper.getElementText(initialValueElement));
        }        

        definition.makeImmutable();
        return definition;
    }
}
