angular.module("syndicatedloan").controller("LenderController", LenderController);
angular.module("syndicatedloan").controller("LenderSubscriptionController", LenderSubscriptionController);
angular.module("syndicatedloan").controller("LenderLoanDetailsController", LenderLoanDetailsController);

function LenderController($scope, $window, $state) {
  var lc = this;
  var curState = $state.$current.name;

  if (curState === 'lender-home' || curState === 'lender-subscription') {
    lc.currentNavItem = 'lenderSubscription';
    $scope.loading = true;
    $state.go('lender-subscription');
  }
  else if (curState === 'lender-loanDetails') {
    lc.currentNavItem = 'lenderLoanDetails';
    $scope.loading = true;
    $state.go('lender-loanDetails');
  }
  else {
    isLogin = true;
    $window.location.href = '/lender-home';
  }
}

function LenderSubscriptionController($scope, $window, RESTService, $mdDialog, $mdToast, $q) {
  var lsc = this;
  lsc.currency = "USD (million)";
  lsc.currency1 = "USD (million)"
  $scope.lenderName = $window.localStorage.getItem('username');
  lsc.dataAvailable = false;
  RESTService.get('modules/get', { "url": 'subscription/getLoanIds' }, function (response) {
    if (response.data.data != "") {
      lsc.dataAvailable = true
      lsc.LoanRefernce = JSON.parse(response.data.data);
    }
  })

  lsc.getSubscription = function (loanRef1) {
    if (loanRef1 != null) {
      RESTService.get('modules/get', { "url": 'subscription/getSubscriptionsById/' + loanRef1 }, function (response) {
        angular.forEach((JSON.parse(response.data.data)), function (data, key) {
          lsc.loanRef2 = loanRef1,
          lsc.subscriptionRef = data.subscriptionId;
          lsc.subscription = data.subscriptionName;
          lsc.startDate = data.startDate;
          lsc.endDate = data.endDate;
          lsc.loanAmount = data.loanAmount;
          lsc.tenor = data.tenure;
          lsc.termSheet = data.termSheet;
          lsc.leadArrangerName = "INDUS_SECURITIES";
        });
      })
    }
  }

  lsc.subscribe = function () {
    $mdDialog.show({
      templateUrl: '/views/partials/spinner-dialog.template.html',
      parent: angular.element(document.body),
      clickOutsideToClose: false,
    });
    var subscribeData = {
      "url": 'subscription/approveLenderSubscriptionById',
      "params": {
        "loanRef": lsc.loanRef2,
        "subscriptionAmount": lsc.subscribedAmount
      }
    }
    RESTService.put('modules/put', subscribeData, { "Content-Type": "application/json" }, function (response) {
      $mdDialog.hide();
      let message = "";
      let theme = "";
      if (response && response.status === 200) {
        if (response.data.status == 1) {
          message = (response.data.message) ? response.data.message : "Subscription Approved Successfully";
          theme = "success-toast";
          $scope.getAllApprovedSubscription();

        } else {
          message = (response.data.message) ? response.data.message : "Some Error occurred!";
          theme = "error-toast";
        }
      } else {
        message = (response.data && response.data.message) ? response.data.message : "Internal server error";
        theme = "error-toast";
      }
      $mdToast.show(
        $mdToast.simple()
          .textContent(message)
          .position('bottom right')
          .hideDelay(3000)
          .theme(theme)
      );
    })
  }

  $scope.getAllApprovedSubscription = function () {
    lsc.loadTableQuery = {
      limit: 5,
      page: 1
    };
    RESTService.get('modules/get', { "url": 'subscription/getAllApprovedSubscriptions' }, function (response) {
      $scope.loading = false;
      if (response.status !== undefined && response.status === 200) {
        lsc.loadTableQuery.page = 1;
        lsc.dataStatus = -1;
        var deferred = $q.defer();
        if (response.status !== undefined && response.status === 200) {
          if (response.data.status == 1) {
            lsc.dataStatus = 1;
            lsc.loadHistory = JSON.parse(response.data.data);
            lsc.loadLimitOptions = [5, 10, 15, {
              label: 'All',
              value: function () {
                return lsc.loadHistory.length;
              }
            }];
          } else {
            lsc.dataStatus = response.data.status;
            lsc.loadErrorMsg = response.data.message;
          }
          deferred.resolve();

        } else {
          deferred.reject();
          lsc.loadErrorMsg = (response.data && response.data.message) ? response.data.message : "Internal server error!";
        }
        lsc.loadPromise = deferred.promise;
      };
    })
  }
  $scope.getAllApprovedSubscription();

  lsc.downloadDoc = function (loanRef) {
    RESTService.get('modules/get', { "url": "termSheet/download/" + loanRef }, function (response) {
      lsc.download("TermSheet_" + loanRef, response.data.data);
    })
  }
  lsc.download = function (filename, text) {
    var element = document.createElement('a');
    element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
    element.setAttribute('download', filename);
    element.style.display = 'none';
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  }
  lsc.collapse = function (panel, form) {
    panel.collapse();
  }

}

function LenderLoanDetailsController(RESTService) {
  var ldc = this;
  ldc.dataAvailable = false;

  RESTService.get('modules/get', { "url": 'loan/getLoanIds' }, function (response) {
    console.log("loanids", response)
    if (response.data.data != "")
      ldc.LoanRefernce1 = JSON.parse(response.data.data);
  })

  ldc.getBalance = function (loanRef) {
    if (loanRef != null) {
      RESTService.get('modules/get', { "url": 'loan/checkAccountBalance/' + loanRef }, function (response) {
        console.log("getbalance", response);
        ldc.dataAvailable = true;
        var data1 = JSON.parse(response.data.data)
        ldc.accountId = data1.accountId;
        ldc.balance ="USD "+data1.balance+" million";
      });
    }
  }
}
