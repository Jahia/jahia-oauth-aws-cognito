<%@ page import="org.jahia.community.aws.cognito.provider.AwsCognitoKarafConfigurationFactory" %>
<%@ page import="org.jahia.osgi.BundleUtils" %>
<%@ page import="org.osgi.service.cm.ConfigurationAdmin" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Dictionary" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapperImpl"--%>
<%--@elvariable id="providerKey" type="java.lang.String"--%>
<%--@elvariable id="pageContext" type="javax.servlet.jsp.PageContext"--%>
<%
    AwsCognitoKarafConfigurationFactory awsCognitoKarafConfigurationFactory = BundleUtils.getOsgiService(AwsCognitoKarafConfigurationFactory.class, null);
    ConfigurationAdmin configurationAdmin = BundleUtils.getOsgiService(ConfigurationAdmin.class, null);
    String providerKey = (String) pageContext.findAttribute("providerKey");
    if (providerKey != null) {
        Dictionary<String, Object> properties = configurationAdmin.getConfiguration(awsCognitoKarafConfigurationFactory.getConfigPID(providerKey)).getProperties();
        Enumeration<String> keys = properties.keys();
        Map<String, Object> awsCognitoProperties = new HashMap<>();
        String key;
        while (keys.hasMoreElements()) {
            key = keys.nextElement();
            awsCognitoProperties.put(key, properties.get(key));
        }
        pageContext.setAttribute("awsCognitoProperties", awsCognitoProperties);
    } else {
        pageContext.setAttribute("awsCognitoProperties", Collections.emptyMap());
    }
%>
<utility:setBundle basename="resources.jahia-oauth-aws-cognito" var="bundle"/>
<jcr:jqom statement="SELECT * FROM [jnt:virtualsite] WHERE ISCHILDNODE('/sites') AND localname() <> 'systemsite'"
          var="sites"/>
<datalist id="sites">
    <c:forEach items="${sites.nodes}" var="site">
        <option value="${site.name}"></option>
    </c:forEach>
</datalist>
<template:addResources type="javascript" resources="jquery.min.js,jquery.form.min.js"/>
<c:set var="credentialsLink" value="https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/get-started.html"/>
<c:set var="documentationLink" value="https://developer.awsCognito.com/docs/guides/create-an-api-token/main/"/>
<div class="row">
    <div class="col-md-12">
        <fieldset title="local">
            <div>
                <div class="form-group">
                    <a class="btn btn-link pull-right" href="<c:url value='${credentialsLink}'/>" target="_blank">
                        <i class="material-icons">open_in_new</i>
                        <fmt:message bundle="${bundle}" key="AwsCognitoConfiguration.credentialsLink"/>
                    </a>
                    <a class="btn btn-link pull-right" href="<c:url value='${documentationLink}'/>" target="_blank">
                        <i class="material-icons">open_in_new</i>
                        <fmt:message bundle="${bundle}" key="AwsCognitoConfiguration.documentationLink"/>
                    </a>
                </div>
                <div class="form-group">
                    <div class="col-md-4">
                        <label class="control-label"><fmt:message bundle="${bundle}"
                                                                  key="AwsCognitoUserGroupProvider.name"/></label>
                    </div>
                    <div class="col-md-8">
                        <input class="form-control" type="text" name="configName"
                               value="${awsCognitoProperties['configName']}"
                               <c:if test="${not empty providerKey}">disabled</c:if>/>
                    </div>
                </div>

                <div class="form-group">
                    <div class="col-md-4">
                        <label class="control-label">
                            <fmt:message bundle="${bundle}" key="AwsCognitoConfiguration.site"/>
                        </label>
                    </div>
                    <div class="col-md-8">
                        <input type="text" name="propValue.target.site" class="form-control"
                               value="${awsCognitoProperties['target.site']}" list="sites"/>
                    </div>
                </div>

                <div class="form-group">
                    <div class="col-md-4">
                        <label class="control-label">
                            <fmt:message bundle="${bundle}" key="AwsCognitoConfiguration.accessKeyId"/>
                        </label>
                    </div>
                    <div class="col-md-8">
                        <input type="text" name="propValue.accessKeyId" class="form-control"
                               value="${awsCognitoProperties['accessKeyId']}"/>
                    </div>
                </div>
                <div class="form-group">
                    <div class="col-md-4">
                        <label class="control-label">
                            <fmt:message bundle="${bundle}" key="AwsCognitoConfiguration.secretAccessKey"/>
                        </label>
                    </div>
                    <div class="col-md-8">
                        <input type="password" name="propValue.secretAccessKey" class="form-control"
                               value="${awsCognitoProperties['secretAccessKey']}"/>
                    </div>
                </div>
                <div class="form-group">
                    <div class="col-md-4">
                        <label class="control-label">
                            <fmt:message bundle="${bundle}" key="AwsCognitoConfiguration.userPoolId"/>
                        </label>
                    </div>
                    <div class="col-md-8">
                        <input type="text" name="propValue.userPoolId" class="form-control"
                               value="${awsCognitoProperties['userPoolId']}"/>
                    </div>
                </div>
                <div class="form-group">
                    <div class="col-md-4">
                        <label class="control-label">
                            <fmt:message bundle="${bundle}" key="AwsCognitoConfiguration.apiKey"/>
                        </label>
                    </div>
                    <div class="col-md-8">
                        <input type="text" name="propValue.apiKey" class="form-control"
                               value="${awsCognitoProperties['apiKey']}"/>
                    </div>
                </div>
            </div>
        </fieldset>
    </div>
</div>
