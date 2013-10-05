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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.mailsender.MailSenderPlugin;

@Component
@Singleton
public class DefaultUserNotificationNotifier implements UserNotificationNotifier
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Override
    public void send(DocumentReference template, String mail, Map<String, Object> parameters) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        // Get mailsenderplugin
        MailSenderPlugin emailService = (MailSenderPlugin) xcontext.getWiki().getPlugin(MailSenderPlugin.ID, xcontext);
        if (emailService == null) {
            return;
        }

        // Get wiki administrator email (default : mailer@xwiki.localdomain.com)
        String sender = xcontext.getWiki().getXWikiPreference("admin_email", "mailer@xwiki.localdomain.com", xcontext);

        String language = xcontext.getWiki().getLanguagePreference(xcontext);

        // Send message from template
        emailService.sendMailFromTemplate(this.serializer.serialize(template), sender, mail, null, null, language,
            parameters, xcontext);
    }
}
