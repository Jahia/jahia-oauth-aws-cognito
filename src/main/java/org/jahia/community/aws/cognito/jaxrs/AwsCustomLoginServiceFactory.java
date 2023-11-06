package org.jahia.community.aws.cognito.jaxrs;

import org.jahia.community.aws.cognito.api.AwsCustomLoginService;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.sites.JahiaSitesService;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(service = AwsCustomLoginServiceFactory.class)
public class AwsCustomLoginServiceFactory {
    private static final Logger logger = LoggerFactory.getLogger(AwsCustomLoginServiceFactory.class);

    private final Map<String, AwsCustomLoginService> awsCustomLoginServices;
    private JahiaSitesService jahiaSitesService;

    public AwsCustomLoginServiceFactory() {
        awsCustomLoginServices = new ConcurrentHashMap<>();
    }

    @Reference
    private void setJahiaSitesService(JahiaSitesService jahiaSitesService) {
        this.jahiaSitesService = jahiaSitesService;
    }

    @Reference(service = AwsCustomLoginService.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeAwsCustomLoginService")
    private void addAwsCustomLoginService(AwsCustomLoginService awsCustomLoginService) {
        final Bundle bundle = FrameworkUtil.getBundle(awsCustomLoginService.getClass());
        awsCustomLoginServices.put(bundle.getSymbolicName(), awsCustomLoginService);
        logger.info(String.format("Registered an AWS custom login service %s provided by %s", awsCustomLoginService, BundleUtils.getDisplayName(bundle)));
    }

    public void removeAwsCustomLoginService(AwsCustomLoginService awsCustomLoginService) {
        final Bundle bundle = FrameworkUtil.getBundle(awsCustomLoginService.getClass());
        awsCustomLoginServices.remove(bundle.getSymbolicName());
        logger.info(String.format("Unregistered an AWS custom login service %s provided by %s", awsCustomLoginService, BundleUtils.getDisplayName(bundle)));
    }

    public AwsCustomLoginService getAwsCustomLoginService(String siteKey, JCRSessionWrapper jcrSessionWrapper) {
        try {
            return jahiaSitesService.getSiteByKey(siteKey, jcrSessionWrapper)
                    .getInstalledModulesWithAllDependencies()
                    .stream().filter(awsCustomLoginServices::containsKey).findFirst()
                    .map(awsCustomLoginServices::get)
                    .orElse(null);
        } catch (RepositoryException e) {
            logger.error("", e);
            return null;
        }
    }
}
