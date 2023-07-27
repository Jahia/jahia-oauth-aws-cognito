(function () {
    'use strict';
    angular.module('JahiaOAuthApp').controller('AwsCognitoController', AwsCognitoController);
    AwsCognitoController.$inject = ['$location', 'settingsService', 'helperService', 'i18nService'];

    function AwsCognitoController($location, settingsService, helperService, i18nService) {
        // must mach value in the plugin in pom.xml
        i18nService.addKey(jcauthawscognitoi18n);

        const CONNECTOR_SERVICE_NAME = 'AwsCognitoApi20';

        const vm = this;
        vm.expandedCard = false;
        vm.callbackUrl = '';

        vm.saveSettings = () => {
            // Value can't be empty
            if (!vm.secretKey || !vm.loginUrl) {
                helperService.errorToast(i18nService.message('label.missingMandatoryProperties'));
                return false;
            }

            // the node name here must be the same as the one in your spring file
            settingsService.setConnectorData({
                connectorServiceName: CONNECTOR_SERVICE_NAME,
                properties: {
                    enabled: vm.enabled,
                    apiKey: 'AWS_COGNITO_API_KEY',
                    apiSecret: 'AWS_COGNITO_API_KEY',
                    endpoint: vm.endpoint,
                    region: vm.region,
                    secretKey: vm.secretKey,
                    loginUrl: vm.loginUrl
                }
            }).success(() => {
                vm.connectorHasSettings = true;
                helperService.successToast(i18nService.message('label.saveSuccess'));
            }).error(data => {
                helperService.errorToast(`${i18nService.message('jcauthnt_awsCognitoOAuthView')}: ${data.error}`);
            });
        };
        vm.toggleCard = () => {
            vm.expandedCard = !vm.expandedCard;
        };

        settingsService.getConnectorData('AwsCognitoApi20', ['enabled', 'apiKey', 'apiSecret', 'secretKey', 'loginUrl']).success(data => {
            if (data && !angular.equals(data, {})) {
                vm.connectorHasSettings = true;
                vm.expandedCard = true;
                vm.enabled = data.enabled;
                vm.apiKey = data.apiKey;
                vm.apiSecret = data.apiSecret;
                vm.endpoint = data.endpoint;
                vm.region = data.region;
                vm.secretKey = data.secretKey;
                vm.loginUrl = data.loginUrl;
            } else {
                vm.connectorHasSettings = false;
                vm.enabled = false;
            }
        }).error(data => {
            helperService.errorToast(`${i18nService.message('jcauthnt_awsCognitoOAuthView')}: ${data.error}`);
        });
    }
})();
