(function () {
    'use strict';

    angular
        .module('syndicatedloan')
        .factory('AuthInterceptor', AuthInterceptor);

    function AuthInterceptor($rootScope, $q, $state, $window, $cookies, AUTH_EVENTS) {
        return {
            'request': function (config) {
                //console.log(config);
                config.headers = config.headers || {};
                if ($window.localStorage.getItem('token')) {
                    //config.headers.Authorization = 'Bearer ' + $window.localStorage.getItem('token');
                    config.headers.Authorization = $window.localStorage.getItem('token');
                }
                return config;
            },
            'responseError': function(response) {
                if(response.status === 401 || response.status === 403) {
                    if($window.localStorage.getItem('username')) $window.localStorage.removeItem('username');
                    if($window.localStorage.getItem('userrole'))  $window.localStorage.removeItem('userrole');
                    if($cookies.get('token')) $cookies.remove('token');

                    let curstate = $state.current.name;
                    let param = "/party-login";
                    $window.location.href = '/token-authentication-failed?url='+param;
                }
                return $q.reject(response);
            }
        };
    }
        
})();