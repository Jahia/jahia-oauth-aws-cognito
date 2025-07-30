package org.jahia.community.aws.cognito.connector;

import org.jahia.community.aws.cognito.api.AwsCognitoConstants;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.filter.AbstractFilter;
import org.jahia.services.render.filter.RenderChain;
import org.jahia.services.render.filter.RenderFilter;
import org.jahia.services.seo.urlrewrite.UrlRewriteService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

@Component(service = RenderFilter.class)
public class CookieRedirectFilter extends AbstractFilter {
    private String moduleName;

    @Reference
    private UrlRewriteService urlRewriteService;

    public CookieRedirectFilter() {
        setPriority(10);
        setApplyOnConfigurations(Resource.CONFIGURATION_PAGE);
        setApplyOnModes(org.jahia.api.Constants.LIVE_WORKSPACE);
    }

    @Activate
    private void onActivate(BundleContext bundleContext) {
        moduleName = bundleContext.getBundle().getSymbolicName();
    }

    @Override
    public boolean areConditionsMatched(RenderContext renderContext, Resource resource) {
        return super.areConditionsMatched(renderContext, resource) && renderContext.getSite().getAllInstalledModules().contains(moduleName);
    }

    @Override
    public String execute(String previousOut, RenderContext renderContext, Resource resource, RenderChain chain) throws Exception {
        Cookie cookie;
        String redirect = renderContext.getRequest().getRequestURI().replace("\n\r", "");
        try {
            cookie = new Cookie(AwsCognitoConstants.SESSION_OAUTH_AWS_COGNITO_RETURN_URL, urlRewriteService.rewriteOutbound(redirect, renderContext.getRequest(), renderContext.getResponse()));
        } catch (InvocationTargetException | IOException e) {
            cookie = new Cookie(AwsCognitoConstants.SESSION_OAUTH_AWS_COGNITO_RETURN_URL, redirect);
        }
        cookie.setPath("/");
        renderContext.getResponse().addCookie(cookie);
        return super.execute(previousOut, renderContext, resource, chain);
    }
}
