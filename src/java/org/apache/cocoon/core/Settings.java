/*
 * Copyright 2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.core;

import java.util.List;

/**
 * This object holds the global configuration of Cocoon.
 *
 * @version $Id$
 * @since 2.2
 */
public interface Settings extends BaseSettings, DynamicSettings {

    /**
     * Get the value of a property.
     * @param key The name of the property.
     * @return The value of the property or null.
     */
    String getProperty(String key);

    /**
     * Get the value of a property.
     * @param key The name of the property.
     * @param defaultValue The value returned if the property is not available.
     * @return The value of the property or if the property cannot
     *         be found the default value.
     */
    String getProperty(String key, String defaultValue);

    /**
     * Return all available properties starting with the prefix.
     * @param keyPrefix The prefix each property name must have.
     * @return A list of property names (including the prefix) or
     *         an empty list.
     */
    List getProperties(String keyPrefix);
}
