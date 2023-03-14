angular.module("syndicatedloan").controller("LoginController", LoginController);

function LoginController($scope, $state, $mdToast, $cookies, $mdDialog, AuthenticationService) {
	$cookies.remove('token');
	$scope.login = function () {
		$scope.loading = true;
		$mdDialog.show({
			templateUrl: '/views/partials/spinner-dialog.template.html',
			parent: angular.element(document.body),
			clickOutsideToClose: false,
		});

		AuthenticationService.Login($scope.uname, $scope.pwd, function (response) {
			$mdDialog.hide();
			if (response && response.data && response.data.success) {
				console.log(response.data.Role + " login Successful.");
				//AuthenticationService.SetCredentials($scope.uname, $scope.pwd, response.data.Role, response.message);
				if (response.data.Role == "LEADARRANGER") $state.transitionTo('leadArranger-home', { reload: true });
				else if (response.data.Role == "BORROWER") $state.go('borrower-home');
				else if (response.data.Role == "LENDER") $state.go('lender-home');
				else $state.go('party-login');
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