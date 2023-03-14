angular.module("syndicatedloan").controller("IndexController", IndexController);
angular.module("syndicatedloan").controller("WelcomeNoteController", WelcomeNoteController);

function IndexController($scope, $state, $window, $transitions, $location, $rootScope, AuthenticationService) {

	$state.go('party-login');

	$transitions.onBefore({}, function (transition) {
		// check if the state should be protected
		if (!transition.to().name) {
			// redirect to the 'login' state
			console.log("here");
			return transition.router.stateService.target('party-login');
		}
	});

	$transitions.onSuccess({}, function (transition) {
		console.log(
			"Successful Transition from " + transition.from().name +
			" to " + transition.to().name
		);
		console.log($state.current);
	});

	$scope.logout = function (url) {
		AuthenticationService.logout();
		$state.go(url);
	};

}

function WelcomeNoteController($scope, $state, $location, $window, $rootScope) {
	$scope.setName = "Welcome " + $window.localStorage.getItem('username') + "!";
}
