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
 
/**
 * A sample sitemap.
 *
 * @version CVS $Id$
 */
class Sitemap extends Pipeline {
  
    boolean setup(String requestPath) {
        if (requestPath == "") {
            generate "file", "welcome.xml", []
            transform "trax", "welcome.xslt", [] 
            serialize "xml", [ 'encoding': 'UTF-8' ]
        } else if (m = (requestPath =~ "(.*)\.html")) {
            generate "file", m.group(1) + ".xml", []
            transform "trax", "welcome.xslt", []
            serialize "xml", [ 'encoding': 'UTF-8' ] 
        } else if (m = (requestPath =~ "images/(.*)\.gif")) {
            read "resources/images/" + m.group(1) + ".gif", "image/gif", []
        } else if (m = (requestPath =~ "styles/(.*)\.css")) {
            read "resources/styles/" + m.group(1) + ".css", "text/css", []
        } else {
            return false;
        }
        return true;
    }
}