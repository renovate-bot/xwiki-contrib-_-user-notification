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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

@Component
@Singleton
public class DefaultUserNotificationConfiguration implements UserNotificationConfiguration
{
    private static final LocalDocumentReference CONFURATION_REFERENCE = new LocalDocumentReference("XWiki",
        "UserNotificationAdmin");

    private static final LocalDocumentReference CONFURATION_CLASS = new LocalDocumentReference("XWiki",
        "UserNotificationConfigurationClass");

    private static final String DEFAULT_USERCREATION_CONTENT = "XWiki.UserCreationMail";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private Logger logger;

    private BaseObject getConfiguration()
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument document;
        try {
            document = xcontext.getWiki().getDocument(CONFURATION_REFERENCE, xcontext);
        } catch (XWikiException e) {
            this.logger.error("Failed to get configuration object", e);

            return null;
        }

        return document.getXObject(CONFURATION_CLASS);
    }

    private BaseProperty getBaseProperty(String name)
    {
        BaseObject configurationObject = getConfiguration();

        if (configurationObject != null) {
            return (BaseProperty) configurationObject.safeget(name);
        }

        return null;
    }

    private boolean getBoolean(String name, boolean def)
    {
        boolean value = def;

        BaseProperty property = getBaseProperty(name);

        if (property != null) {
            value = (Integer) property.getValue() == 1;
        }

        return value;
    }

    private String getString(String name, String def)
    {
        String value = def;

        BaseProperty property = getBaseProperty(name);

        if (property != null) {
            value = (String) property.getValue();
        }

        return value;
    }

    @Override
    public boolean isUserCreationEnabled()
    {
        return getBoolean("usercreation.enabled", false);
    }

    @Override
    public DocumentReference getUserCreationTemplate()
    {
        return referenceResolver.resolve(getString("usercreation.template", DEFAULT_USERCREATION_CONTENT));
    }
}
