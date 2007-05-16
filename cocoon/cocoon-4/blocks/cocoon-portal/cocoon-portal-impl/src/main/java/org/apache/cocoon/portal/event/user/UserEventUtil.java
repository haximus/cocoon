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
package org.apache.cocoon.portal.event.user;

import org.apache.cocoon.portal.PortalService;
import org.apache.cocoon.portal.event.Event;
import org.apache.cocoon.portal.om.PortalUser;

/**
 * Utility class for user events.
 *
 * @version $Id$
 * @since 2.2
 */
public class UserEventUtil {

    private UserEventUtil() {
        // avoid instantiation
    }

    public static void sendUserDidLoginEvent(PortalService service,
                                             PortalUser    user) {
        final Event event = new UserDidLoginEvent(user);
        service.getEventManager().send(event);
    }

    public static void sendUserWillLogoutEvent(PortalService service,
                                               PortalUser    user) {
        final Event event = new UserWillLogoutEvent(user);
        service.getEventManager().send(event);
    }

    public static void sendUserIsAccessingEvent(PortalService service,
                                                PortalUser    user) {
        final Event event = new UserIsAccessingEvent(user);
        service.getEventManager().send(event);
    }
}
