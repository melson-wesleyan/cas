package org.apereo.cas.mgmt.services.web.factory;

import com.google.common.base.Throwables;
import org.apereo.cas.grouper.services.GrouperRegisteredServiceAccessStrategy;
import org.apereo.cas.mgmt.services.web.beans.RegisteredServiceEditBean;
import org.apereo.cas.mgmt.services.web.beans.RegisteredServiceSupportAccessEditBean;
import org.apereo.cas.services.DefaultRegisteredServiceAccessStrategy;
import org.apereo.cas.services.RegisteredServiceAccessStrategy;
import org.apereo.cas.services.RemoteEndpointServiceAccessStrategy;
import org.apereo.cas.services.TimeBasedRegisteredServiceAccessStrategy;
import org.apereo.cas.mgmt.services.web.beans.RegisteredServiceViewBean;

import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Default mapper for converting {@link RegisteredServiceAccessStrategy} to/from {@link RegisteredServiceEditBean.ServiceData}.
 *
 * @author Daniel Frett
 * @since 4.2
 */
public class DefaultAccessStrategyMapper implements AccessStrategyMapper {

    @Override
    public void mapAccessStrategy(final RegisteredServiceAccessStrategy accessStrategy, final RegisteredServiceEditBean.ServiceData bean) {
        final RegisteredServiceSupportAccessEditBean accessBean = bean.getSupportAccess();
        accessBean.setCasEnabled(accessStrategy.isServiceAccessAllowed());
        accessBean.setSsoEnabled(accessStrategy.isServiceAccessAllowedForSso());

        if (accessStrategy.getUnauthorizedRedirectUrl() != null) {
            accessBean.setUnauthzUrl(accessStrategy.getUnauthorizedRedirectUrl().toString());
        }
        
        if (accessStrategy instanceof DefaultRegisteredServiceAccessStrategy) {
            final DefaultRegisteredServiceAccessStrategy def = (DefaultRegisteredServiceAccessStrategy) accessStrategy;
            accessBean.setRequireAll(def.isRequireAllAttributes());
            accessBean.setRequiredAttr(def.getRequiredAttributes());
            accessBean.setRejectedAttr(def.getRejectedAttributes());
            accessBean.setCaseSensitive(def.isCaseInsensitive());
            accessBean.setType(RegisteredServiceSupportAccessEditBean.Types.DEFAULT);
        }

        if (accessStrategy instanceof TimeBasedRegisteredServiceAccessStrategy) {
            final TimeBasedRegisteredServiceAccessStrategy def = (TimeBasedRegisteredServiceAccessStrategy)
                    accessStrategy;
            accessBean.setStartingTime(def.getStartingDateTime());
            accessBean.setEndingTime(def.getEndingDateTime());
            accessBean.setType(RegisteredServiceSupportAccessEditBean.Types.TIME);
        }

        if (accessStrategy instanceof GrouperRegisteredServiceAccessStrategy) {
            final GrouperRegisteredServiceAccessStrategy def = (GrouperRegisteredServiceAccessStrategy) accessStrategy;
            accessBean.setGroupField(def.getGroupField().toString());
            accessBean.setType(RegisteredServiceSupportAccessEditBean.Types.GROUPER);
        }

        if (accessStrategy instanceof RemoteEndpointServiceAccessStrategy) {
            final RemoteEndpointServiceAccessStrategy def = (RemoteEndpointServiceAccessStrategy) accessStrategy;
            accessBean.setCodes(def.getAcceptableResponseCodes());
            accessBean.setUrl(def.getEndpointUrl());
            accessBean.setType(RegisteredServiceSupportAccessEditBean.Types.REMOTE);
        }
    }

    @Override
    public void mapAccessStrategy(final RegisteredServiceAccessStrategy accessStrategy,
                                  final RegisteredServiceViewBean bean) {
        bean.setSasCASEnabled(accessStrategy.isServiceAccessAllowed());
    }

    @Override
    public RegisteredServiceAccessStrategy toAccessStrategy(final RegisteredServiceEditBean.ServiceData bean) {
        final RegisteredServiceSupportAccessEditBean supportAccess = bean.getSupportAccess();

        final DefaultRegisteredServiceAccessStrategy accessStrategy = new DefaultRegisteredServiceAccessStrategy();

        accessStrategy.setEnabled(supportAccess.isCasEnabled());
        accessStrategy.setSsoEnabled(supportAccess.isSsoEnabled());
        accessStrategy.setRequireAllAttributes(supportAccess.isRequireAll());
        accessStrategy.setCaseInsensitive(supportAccess.isCaseSensitive());

        final Map<String, Set<String>> requiredAttrs = supportAccess.getRequiredAttr();
        final Set<Map.Entry<String, Set<String>>> entries = requiredAttrs.entrySet();
        final Iterator<Map.Entry<String, Set<String>>> it = entries.iterator();
        while (it.hasNext()) {
            final Map.Entry<String, Set<String>> entry = it.next();
            if (entry.getValue().isEmpty()) {
                it.remove();
            }
        }
        accessStrategy.setRequiredAttributes(requiredAttrs);
        
        final Map<String, Set<String>> rejectedAttrs = supportAccess.getRejectedAttr();
        final Set<Map.Entry<String, Set<String>>> rejectedEntries = rejectedAttrs.entrySet();
        final Iterator<Map.Entry<String, Set<String>>> it2 = rejectedEntries.iterator();
        while (it2.hasNext()) {
            final Map.Entry<String, Set<String>> entry = it2.next();
            if (entry.getValue().isEmpty()) {
                it2.remove();
            }
        }
        accessStrategy.setRejectedAttributes(rejectedAttrs);
        
        if (supportAccess.getUnauthzUrl() != null && !supportAccess.getUnauthzUrl().trim().isEmpty()) {
            try {
                accessStrategy.setUnauthorizedRedirectUrl(new URI(supportAccess.getUnauthzUrl()));
            } catch (final Exception e) {
                throw Throwables.propagate(e);
            }
        }
        
        if (supportAccess.getType() == RegisteredServiceSupportAccessEditBean.Types.TIME) {
            ((TimeBasedRegisteredServiceAccessStrategy) accessStrategy).setEndingDateTime(supportAccess.getEndingTime());
            ((TimeBasedRegisteredServiceAccessStrategy) accessStrategy).setStartingDateTime(supportAccess.getStartingTime());
        }

        if (supportAccess.getType() == RegisteredServiceSupportAccessEditBean.Types.GROUPER) {
            ((GrouperRegisteredServiceAccessStrategy) accessStrategy)
                    .setGroupField(GrouperRegisteredServiceAccessStrategy.GrouperGroupField.valueOf(supportAccess.getGroupField()));
        }

        if (supportAccess.getType() == RegisteredServiceSupportAccessEditBean.Types.REMOTE) {
            ((RemoteEndpointServiceAccessStrategy) accessStrategy).setAcceptableResponseCodes(supportAccess.getCodes());
            ((RemoteEndpointServiceAccessStrategy) accessStrategy).setEndpointUrl(supportAccess.getUrl());
        }
        
        return accessStrategy;
    }
}
