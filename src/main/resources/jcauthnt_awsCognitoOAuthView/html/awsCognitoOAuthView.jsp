<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<template:addResources type="javascript" resources="i18n/jahia-oauth-aws-cognito-i18n_${renderContext.UILocale}.js"
                       var="i18nJSFile"/>
<c:if test="${empty i18nJSFile}">
    <template:addResources type="javascript" resources="i18n/jahia-oauth-aws-cognito-i18n.js"/>
</c:if>
<template:addResources type="javascript" resources="aws-cognito-connector-controller.js"/>

<md-card ng-controller="AwsCognitoController as awsCognito">
    <div layout="row">
        <md-card-title flex>
            <md-card-title-text>
                <span class="md-headline" message-key="jcauthnt_awsCognitoOAuthView"></span>
            </md-card-title-text>
        </md-card-title>
        <div flex layout="row" layout-align="end center">
            <md-button class="md-icon-button" ng-click="awsCognito.toggleCard()">
                <md-tooltip md-direction="top">
                    <span message-key="tooltip.toggleSettings"></span>
                </md-tooltip>
                <md-icon ng-show="!awsCognito.expandedCard">keyboard_arrow_down</md-icon>
                <md-icon ng-show="awsCognito.expandedCard">keyboard_arrow_up</md-icon>
            </md-button>
        </div>
    </div>

    <md-card-content layout="column" ng-show="awsCognito.expandedCard">
        <form name="awsCognitoForm">
            <div layout="row">
                <md-switch ng-model="awsCognito.enabled">
                    <span message-key="label.activate"></span>
                </md-switch>
            </div>

            <div layout="row">
                <md-input-container flex>
                    <label message-key="label.apiKey"></label>
                    <input type="text" ng-model="awsCognito.apiKey" name="apiKey"/>
                    <div ng-messages="awsCognitoForm.apiKey.$error" role="alert">
                        <div ng-message="required" message-key="error.apiKey.required"></div>
                    </div>
                </md-input-container>

                <div flex="5"></div>

                <md-input-container flex>
                    <label message-key="label.apiSecret"></label>
                    <input type="password" ng-model="awsCognito.apiSecret" name="apiSecret"/>
                    <div ng-messages="awsCognitoForm.apiSecret.$error" role="alert">
                        <div ng-message="required" message-key="error.apiSecret.required"></div>
                    </div>
                </md-input-container>
            </div>
            <div layout="row">
                <md-input-container flex>
                    <label message-key="label.endpoint"></label>
                    <input type="text" ng-model="awsCognito.endpoint" name="endpoint"/>
                    <div class="hint" message-key="hint.endpoint"></div>
                    <div ng-messages="awsCognitoForm.endpoint.$error" role="alert">
                        <div ng-message="required" message-key="error.endpoint.required"></div>
                    </div>
                </md-input-container>

                <div flex="5"></div>

                <md-input-container flex>
                    <label message-key="label.scope"></label>
                    <input type="text" ng-model="awsCognito.scope" name="scope"/>
                    <div ng-messages="awsCognitoForm.scope.$error" role="alert">
                        <div ng-message="required" message-key="error.scope.required"></div>
                    </div>
                </md-input-container>
            </div>
            <div layout="row">
                <md-input-container flex>
                    <label message-key="label.callbackUrl"></label>
                    <input type="text" ng-model="awsCognito.callbackUrl" name="callbackUrl"/>
                    <div class="hint" message-key="hint.callbackUrl"></div>
                    <div ng-messages="awsCognitoForm.callbackUrl.$error" role="alert">
                        <div ng-message="required" message-key="error.callbackUrl.required"></div>
                    </div>
                </md-input-container>
            </div>
            <md-switch ng-model="awsCognito.logoutAWS">
                <span message-key="label.logoutAWS"></span>
            </md-switch>
            <div layout="row" ng-show="awsCognito.logoutAWS">
                <md-input-container flex>
                    <label message-key="label.logoutCallbackUrl"></label>
                    <input type="text" ng-model="awsCognito.logoutCallbackUrl" name="logoutCallbackUrl"/>
                    <div class="hint" message-key="hint.logoutCallbackUrl"></div>
                </md-input-container>
            </div>
        </form>

        <md-card-actions layout="row" layout-align="end center">
            <md-button class="md-accent" message-key="label.save" ng-click="awsCognito.saveSettings()"></md-button>
        </md-card-actions>
    </md-card-content>
</md-card>
