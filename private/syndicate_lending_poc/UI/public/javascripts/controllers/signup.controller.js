angular.module("syndicatedloan").controller("SignupController", SignupController);

function SignupController($scope, $state, $mdToast, $cookies, $mdDialog, AuthenticationService) {
    $scope.register = function() {
        $scope.loading = true;
        $mdDialog.show({
            templateUrl: '/views/partials/spinner-dialog.template.html',
            parent: angular.element(document.body),
            clickOutsideToClose: false,
        });

        AuthenticationService.register($scope.username, $scope.password, $scope.role, function(response) {
            $mdDialog.hide();
            if (response && response.data && response.data.success) {
                console.log(response.data.Role + " register Successful.");
                $state.go('party-signup');
            } else {
                console.log(response);
                console.log("Authentication Failed");
                let type = "error-toast";
                let errorMessage = (response.data && response.data.message) ? response.data.message : "Internal Server Error";
                $mdToast.show(
                    $mdToast.simple()
                        .textContent(errorMessage)
                        .position('bottom right')
                        .hideDelay(3000)
                        .theme(type)
                );
            }
        });
    };
}
