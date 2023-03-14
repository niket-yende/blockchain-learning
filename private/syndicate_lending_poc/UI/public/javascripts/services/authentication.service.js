(function () {
    'use strict';

    angular
        .module('syndicatedloan')
        .factory('AuthenticationService', AuthenticationService);

    function AuthenticationService($http, $cookies, $window, $rootScope, RESTService) {
        var LOCAL_TOKEN_KEY = 'token';
        var USER_NAME = 'username';
        var USER_ROLE = 'userrole';
        var isAuthenticated = false;
        var authToken;
       
        function loadUserCredentials() {
          //var token = $window.localStorage.getItem(LOCAL_TOKEN_KEY);
          var token = $cookies.get('token');
          if (token) {
            useCredentials(token);
          }
        }
       
        function storeUserCredentials(username, role, token) {
          //$window.localStorage.setItem(LOCAL_TOKEN_KEY, token);
          $rootScope.globals = {
            currentUser: {
                username: username,
                userRole:role
            }
          };
          $cookies.put(LOCAL_TOKEN_KEY,token);
          $window.localStorage.setItem(USER_NAME, username);
          $window.localStorage.setItem(USER_ROLE, role);
          useCredentials(token);
        }
       
        function useCredentials(token) {
          isAuthenticated = true;
          authToken = token;
      
          // Set the token as header for your requests!
          $http.defaults.headers.common.Authorization = authToken;
          console.log($http.defaults.headers.common.Authorization);
        }
       
        function destroyUserCredentials() {
          authToken = undefined;
          isAuthenticated = false;
          $http.defaults.headers.common.Authorization = undefined;
          $rootScope.globals = {};
          $cookies.remove('token');
          //$window.localStorage.removeItem(LOCAL_TOKEN_KEY);
          $window.localStorage.removeItem(USER_NAME);
          $window.localStorage.removeItem(USER_ROLE);
        }
             
        var login = function(username, password, callback) {
            RESTService.post('/authenticate', { "username": username, "password": password}, function (response) {
                console.log(response);
                if(response && response.status === 200){
                    if(response.data && response.data.success){
                        console.log("Authentication: Success");
                        storeUserCredentials(username,response.data.role,response.data.token);
                    } else console.log("Authentication: Failed");
                } else console.log("Internal Server Error!");
                callback(response);
            })
        };

        var register = function(username, password, role, callback) {
          RESTService.post('/register', { "username": username, "password": password, "role":role}, function (response) {
              console.log(response);
              if(response && response.status === 200){
                  if(response.data && response.data.success){
                      console.log("Registration: Success");      
                  } else console.log("Registration: Failed");
              } else console.log("Internal Server Error!");
              callback(response);
          })
        }
       
        var logout = function() {
          destroyUserCredentials();
        };
       
        loadUserCredentials();
       
        return {
          Login: login,
          register: register,
          logout: logout,
          isAuthenticated: function() {return isAuthenticated;},
        };
      }
})();