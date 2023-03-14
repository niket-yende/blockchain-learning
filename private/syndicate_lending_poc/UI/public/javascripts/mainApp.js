var mainApp = angular.module('syndicatedloan', ['ui.router','ngStorage','toaster','ngMaterial','ngAnimate','ngMessages','ngCookies', 'material.components.expansionPanels', 'md.data.table', 'ui.bootstrap']);
mainApp.config(config);

mainApp.constant('AUTH_EVENTS', {
	notAuthenticated: 'auth-not-authenticated'
  });
   

function config($stateProvider, $locationProvider, $httpProvider, $mdIconProvider, $mdThemingProvider) {
	$locationProvider.html5Mode(true);
	console.log("mainApp");
	$httpProvider.interceptors.push('AuthInterceptor');
		
	$mdThemingProvider.theme('default')
			.primaryPalette("blue")
			.accentPalette('pink')
			.warnPalette('red');
			
	$mdThemingProvider.theme("success-toast");
	$mdThemingProvider.theme("error-toast");

	/* Setting up route states */


	var partyLoginState = {
		name: 'party-login',
		url: '/party-login',
		controller: 'LoginController',
		templateUrl: '/views/partials/login-screen.html',
		data: {
			
		},
		resolve: function($q, $timeout){
			var deferred = $q.defer();
			$timeout(function() {
				deferred.resolve('Hello!');
			}, 10);
			return deferred.promise;
		}
	}

	var partySignUpState = {
		name: 'party-signup',
		url: '/party-signup',
		controller: 'SignupController',
		templateUrl: '/views/partials/signUp-screen.html',
		data: {
			
		},
		resolve: function($q, $timeout){
			var deferred = $q.defer();
			$timeout(function() {
				deferred.resolve('Hello!');
			}, 10);
			return deferred.promise;
		}
	}

	//Lead Arranger

	var leadArrangerHomeState = {
		name: 'leadArranger-home',
		url: '/leadArranger-home',
		templateUrl: '/views/partials/leadArranger-home-screen.html',
		controller: 'LeadArrangerController',
		controllerAs: 'la',
		data: {
			
		},
		resolve: function($q, $timeout){
			var deferred = $q.defer();
			$timeout(function() {
				deferred.resolve('Hello!');
			}, 10);
			return deferred.promise;
		}
	}

	var leadArrangerKeydataState = {
		name: 'leadArranger-keydata',
		url: '/leadArranger-keydata',
		parent: leadArrangerHomeState,
		templateUrl: '/views/partials/leadArranger-keydata-screen.html',
		controller: 'LeadArrangerKeydataController',
		controllerAs: 'lakd',
		data: {
			
		},
		resolve: function($q, $timeout){
			var deferred = $q.defer();
			$timeout(function() {
				deferred.resolve('Hello!');
			}, 10);
			return deferred.promise;
		}
	}

	var leadArrangerDocumentState = {
		name: 'leadArranger-document',
		url: '/leadArranger-document',
		parent: leadArrangerHomeState,
		templateUrl: '/views/partials/leadArranger-document-screen.html',
		controller: 'LeadArrangerDocumentController',
		controllerAs: 'lad',
		data: {
			
		},
		resolve: function($q, $timeout){
			var deferred = $q.defer();
			$timeout(function() {
				deferred.resolve('Hello!');
			}, 10);
			return deferred.promise;
		}
	}

	var leadArrangerPartiesState = {
		name: 'leadArranger-parties',
		url: '/leadArranger-parties',
		parent: leadArrangerHomeState,
		templateUrl: '/views/partials/leadArranger-parties-screen.html',
		controller: 'LeadArrangerPartiesController',
		controllerAs: 'lap',
		data: {
			
		},
		resolve: function($q, $timeout){
			var deferred = $q.defer();
			$timeout(function() {
				deferred.resolve('Hello!');
			}, 10);
			return deferred.promise;
		}
	}

	var leadArrangeSubscriptionState = {
		name: 'leadArranger-subscription',
		url: '/leadArranger-subscription',
		parent: leadArrangerHomeState,
		templateUrl: '/views/partials/leadArranger-subscription-screen.html',
		controller: 'LeadArrangerSubscriptionController',
		controllerAs: 'lsc',
		data: {
			
		},
		resolve: function($q, $timeout){
			var deferred = $q.defer();
			$timeout(function() {
				deferred.resolve('Hello!');
			}, 10);
			return deferred.promise;
		}
	}

	var leadArrangeLoanDetailsState = {
		name: 'leadArranger-loanDetails',
		url: '/leadArranger-loanDetails',
		parent: leadArrangerHomeState,
		templateUrl: '/views/partials/leadArranger-loanDetails-screen.html',
		controller: 'LeadArrangerLoanDetailsController',
		controllerAs: 'lald',
		data: {
			
		},
		resolve: function($q, $timeout){
			var deferred = $q.defer();
			$timeout(function() {
				deferred.resolve('Hello!');
			}, 10);
			return deferred.promise;
		}
	}

	var leadArrangeInterestPaymentState = {
		name: 'leadArranger-interestPayment',
		url: '/leadArranger-interestPayment',
		parent: leadArrangerHomeState,
		templateUrl: '/views/partials/leadArranger-interestPayment-screen.html',
		controller: 'LeadArrangerInterestPaymentController',
		controllerAs: 'laip',
		data: {
			
		},
		resolve: function($q, $timeout){
			var deferred = $q.defer();
			$timeout(function() {
				deferred.resolve('Hello!');
			}, 10);
			return deferred.promise;
		}
	}

	//Borrower

	var borrowerHomeState = {
		name: 'borrower-home',
		url: '/borrower-home',
		templateUrl: '/views/partials/borrower-home-screen.html',
		controller: 'BorrowerController',
		controllerAs: 'bc',
		data: {
			
		},
		resolve: function($q, $timeout){
			var deferred = $q.defer();
			$timeout(function() {
				deferred.resolve('Hello!');
			}, 10);
			return deferred.promise;
		}
	}

	var borrowerDocumentState = {
		name: 'borrower-document',
		url: '/borrower-document',
		parent: borrowerHomeState,
		templateUrl: '/views/partials/borrower-document-screen.html',
		controller: 'BorrowerDocumentController',
		controllerAs: 'bdc',
		data: {
			
		},
		resolve: function($q, $timeout){
			var deferred = $q.defer();
			$timeout(function() {
				deferred.resolve('Hello!');
			}, 10);
			return deferred.promise;
		}
	}

	var borrowerLoanDetailsState = {
		name: 'borrower-loanDetails',
		url: '/borrower-loanDetails',
		parent: borrowerHomeState,
		templateUrl: '/views/partials/borrower-loanDetails-screen.html',
		controller: 'BorrowerLoanDetailsController',
		controllerAs: 'bld',
		data: {
			
		},
		resolve: function($q, $timeout){
			var deferred = $q.defer();
			$timeout(function() {
				deferred.resolve('Hello!');
			}, 10);
			return deferred.promise;
		}
	}

	var borrowerInterestPaymentState = {
		name: 'borrower-interestPayment',
		url: '/borrower-interestPayment',
		parent: borrowerHomeState,
		templateUrl: '/views/partials/borrower-interestPayment-screen.html',
		controller: 'BorrowerInterestPaymentController',
		controllerAs: 'bip',
		data: {
			
		},
		resolve: function($q, $timeout){
			var deferred = $q.defer();
			$timeout(function() {
				deferred.resolve('Hello!');
			}, 10);
			return deferred.promise;
		}
	}

	var borrowerSubscriptionState = {
		name: 'borrower-subscription',
		url: '/borrower-subscription',
		parent: borrowerHomeState,
		templateUrl: '/views/partials/borrower-subscription-screen.html',
		controller: 'BorrowerSubscriptionController',
		controllerAs: 'bsc',
		data: {
			
		},
		resolve: function($q, $timeout){
			var deferred = $q.defer();
			$timeout(function() {
				deferred.resolve('Hello!');
			}, 10);
			return deferred.promise;
		}
	}


	//Lender

	var lenderHomeState = {
		name: 'lender-home',
		url: '/lender-home',
		templateUrl: '/views/partials/lender-home-screen.html',
		controller: 'LenderController',
		controllerAs: 'lc',
		data: {
			
		},
		resolve: function($q, $timeout){
			var deferred = $q.defer();
			$timeout(function() {
				deferred.resolve('Hello!');
			}, 10);
			return deferred.promise;
		}
	}

	var lenderSubscriptionState = {
		name: 'lender-subscription',
		url: '/lender-subscription',
		parent: lenderHomeState,
		templateUrl: '/views/partials/lender-subscription-screen.html',
		controller: 'LenderSubscriptionController',
		controllerAs: 'lsc',
		data: {
			
		},
		resolve: function($q, $timeout){
			var deferred = $q.defer();
			$timeout(function() {
				deferred.resolve('Hello!');
			}, 10);
			return deferred.promise;
		}
	}

	var lenderLoanDetailsState = {
		name: 'lender-loanDetails',
		url: '/lender-loanDetails',
		parent: lenderHomeState,
		templateUrl: '/views/partials/lender-loanDetails-screen.html',
		controller: 'LenderLoanDetailsController',
		controllerAs: 'ldc',
		data: {
			
		},
		resolve: function($q, $timeout){
			var deferred = $q.defer();
			$timeout(function() {
				deferred.resolve('Hello!');
			}, 10);
			return deferred.promise;
		}
	}


	

	

	$stateProvider.state(partyLoginState);
	$stateProvider.state(partySignUpState);
	$stateProvider.state(leadArrangerHomeState)
			.state(leadArrangerKeydataState)
			.state(leadArrangerDocumentState)
			.state(leadArrangerPartiesState)
			.state(leadArrangeSubscriptionState)
			.state(leadArrangeLoanDetailsState)
			.state(leadArrangeInterestPaymentState);

	$stateProvider.state(borrowerHomeState)
			.state(borrowerDocumentState)
			.state(borrowerLoanDetailsState)
			.state(borrowerInterestPaymentState)
			.state(borrowerSubscriptionState);

	$stateProvider.state(lenderHomeState)
			.state(lenderSubscriptionState)
			.state(lenderLoanDetailsState);
	

}

