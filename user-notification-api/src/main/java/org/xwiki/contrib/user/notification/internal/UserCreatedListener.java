/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.user.notification.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.MessagingException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.RegexEntityReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;

@Component
@Named("org.xwiki.contrib.user.notification.internal.UserCreatedListener")
@Singleton
public class UserCreatedListener implements EventListener
{
    /**
     * The reference to match class XWiki.XWikiUsers on main wiki.
     */
    private static final RegexEntityReference USERSCLASS_REFERENCE =
        new RegexEntityReference(Pattern.compile("([^:]*:)?XWiki.XWikiUsers\\[\\d*\\]"), EntityType.OBJECT);

    /**
     * The matched events.
     */
    private static final List<Event> EVENTS = Arrays.<Event>asList(new XObjectAddedEvent(USERSCLASS_REFERENCE));

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private UserNotificationConfiguration configuration;

    @Inject
    private UserNotificationNotifier notifier;

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return UserCreatedListener.class.getName();
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        this.logger.debug("Received event [{}] for document [{}]", event, source);

        XWikiDocument userDoc = (XWikiDocument) source;
        XWikiContext xcontext = (XWikiContext) data;

        if (this.configuration.isUserCreationEnabled()) {
            this.logger.debug("User creation notification is enabled");

            String mail = userDoc.getStringValue("email");

            if (StringUtils.isNotEmpty(mail)) {
                Map<String, Object> parameters = new HashMap<>();

                parameters.put("user_document", userDoc.newDocument(xcontext));
                parameters.put("user_password", getRequestPassword());

                DocumentReference template = this.configuration.getUserCreationTemplate();

                this.logger.debug("Sendind user creation mail to [{}]", mail);

                try {
                    this.notifier.send(template, mail, parameters);
                } catch (MessagingException e) {
                    this.logger.error("Faled to send notification of created user to [{}]", mail, e);
                }
            }
        }
    }

    private String getRequestPassword()
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        String password = xcontext.getRequest().get("XWiki.XWikiUsers_0_password");
        if (password == null) {
            password = xcontext.getRequest().get("register_password");
        }

        return password;
    }
}
