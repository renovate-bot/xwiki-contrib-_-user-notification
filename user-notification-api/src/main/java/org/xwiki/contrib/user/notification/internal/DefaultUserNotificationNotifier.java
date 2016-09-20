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
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.mail.XWikiAuthenticator;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;

@Component
@Singleton
public class DefaultUserNotificationNotifier implements UserNotificationNotifier
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("template")
    private MimeMessageFactory<MimeMessage> messageFactory;

    @Inject
    private MailSender mailSender;

    @Inject
    @Named("database")
    // TODO: remove Provider when is fixed http://jira.xwiki.org/browse/XWIKI-13720
    private Provider<MailListener> databaseMailListenerProvider;

    @Inject
    private MailSenderConfiguration configuration;

    @Inject
    private Logger logger;

    @Override
    public void send(DocumentReference template, String mail, Map<String, Object> inputParameters)
        throws MessagingException
    {
        // Create session
        Session session;
        if (this.configuration.usesAuthentication()) {
            session =
                Session.getInstance(this.configuration.getAllProperties(), new XWikiAuthenticator(this.configuration));
        } else {
            session = Session.getInstance(this.configuration.getAllProperties());
        }

        // Create the message
        Map<String, Object> parameters = new HashMap<>();
        // Enable attachments
        parameters.put("includeTemplateAttachments", true);
        // Set variable accessible in the template
        parameters.put("velocityVariables", inputParameters);
        // Set language
        XWikiContext xcontext = this.xcontextProvider.get();
        parameters.put("language", xcontext.getWiki().getLanguagePreference(xcontext));
        MimeMessage message = this.messageFactory.createMessage(template, parameters);

        message.addRecipient(RecipientType.TO, new InternetAddress(mail));

        // Send mail asynchronously
        this.mailSender.sendAsynchronously(Arrays.asList(message), session, this.databaseMailListenerProvider.get());

        this.logger.debug("Mail sent to {}", mail);
    }
}
