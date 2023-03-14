angular.module("syndicatedloan").controller("LeadArrangerController", LeadArrangerController);
angular.module("syndicatedloan").controller("LeadArrangerKeydataController", LeadArrangerKeydataController);
angular.module("syndicatedloan").controller("LeadArrangerDocumentController", LeadArrangerDocumentController);
angular.module("syndicatedloan").controller("LeadArrangerPartiesController", LeadArrangerPartiesController);
angular.module("syndicatedloan").controller("LeadArrangerSubscriptionController", LeadArrangerSubscriptionController);
angular.module("syndicatedloan").controller("LeadArrangerLoanDetailsController", LeadArrangerLoanDetailsController);
angular.module("syndicatedloan").controller("LeadArrangerInterestPaymentController", LeadArrangerInterestPaymentController);

angular.module("syndicatedloan").directive('fileInput', function ($parse) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            var model = $parse(attrs.fileInput),
                modelSetter = model.assign; //define a setter for fileInput

            //Bind change event on the element
            element.bind('change', function () {
                //Call apply on scope, it checks for value changes and reflect them on UI
                scope.$apply(function () {
                    //set the model value
                    modelSetter(scope, element[0].files[0]);
                });
            });
        }
    };
});


function LeadArrangerController($scope, $window, $state, $rootScope) {

    var la = this;
    var curState = $state.$current.name;

    if (curState === 'leadArranger-home' || curState === 'leadArranger-keydata') {
        la.currentNavItem = 'leadArrangerKeydata';
        $scope.loading = true;
        $state.go('leadArranger-keydata');
    }
    else if (curState === 'leadArranger-document') {
        la.currentNavItem = 'leadArrangerDocument';
        $scope.loading = true;
        $state.go('leadArranger-document');
    }
    else if (curState === 'leadArranger-parties') {
        la.currentNavItem = 'leadArrangerParties';
        $scope.loading = true;
        $state.go('leadArranger-parties');
    }
    else if (curState === 'leadArranger-subscription') {
        la.currentNavItem = 'leadArrangerSubscription';
        $scope.loading = true;
        $state.go('leadArranger-subscription');
    }
    else if (curState === 'leadArranger-loanDetails') {
        la.currentNavItem = 'leadArrangerLoanDetails';
        $scope.loading = true;
        $state.go('leadArranger-loanDetails');
    }
    else if (curState === 'leadArranger-interestPayment') {
        la.currentNavItem = 'leadArrangerInterestPayment';
        $scope.loading = true;
        $state.go('leadArranger-interestPayment');
    }
    else {
        isLogin = true;
        $window.location.href = '/leadArranger-home';
    }
}

function LeadArrangerKeydataController(RESTService, $mdDialog, $mdToast, $scope, $q, $state) {
    var lakd = this;
    lakd.loanType = "Senior Loan - Unsecured"

    $scope.getAllKeyData = function () {
        lakd.loadTableQuery = {
            limit: 5,
            page: 1
        };
        RESTService.get('modules/get', { "url": 'getAllInitiationData' }, function (response) {
            $scope.loading = false;
            if (response.status !== undefined && response.status === 200) {
                lakd.loadTableQuery.page = 1;
                lakd.dataStatus = -1;
                var deferred = $q.defer();
                if (response.status !== undefined && response.status === 200) {
                    if (response.data.status == 1) {
                        lakd.dataStatus = 1;
                        lakd.loadHistory = JSON.parse(response.data.data);
                        lakd.loadLimitOptions = [5, 10, 15, {
                            label: 'All',
                            value: function () {
                                return lakd.loadHistory.length;
                            }
                        }];
                    } else {
                        lakd.dataStatus = response.data.status;
                        lakd.loadErrorMsg = response.data.message;
                    }
                    deferred.resolve();

                } else {
                    deferred.reject();
                    lakd.loadErrorMsg = (response.data && response.data.message) ? response.data.message : "Internal server error!";
                }
                lakd.loadPromise = deferred.promise;
            };
        })
    }
    $scope.getAllKeyData();

    lakd.createKeyData = function () {
        $mdDialog.show({
            templateUrl: '/views/partials/spinner-dialog.template.html',
            parent: angular.element(document.body),
            clickOutsideToClose: false,
        });
        lakd.keydata = {
            "url": 'createInitiationState',
            "params": {
                "issuerName": lakd.issuerName,
                "entityType": lakd.entityType,
                "syndicationType": lakd.syndicationType,
                "loanType": lakd.loanType,
                "tenure": Number(lakd.tenor)
            }
        }

        RESTService.post('/modules/post', lakd.keydata, function (response) {
            $mdDialog.hide();
            let message = "";
            let theme = "";
            if (response && response.status === 200) {
                if (response.data.status == 1) {
                    message = (response.data.message) ? response.data.message : "Initiation state succesfully created";
                    theme = "success-toast";
                    $scope.getAllKeyData();

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

    lakd.collapse = function (panel, form) {
        panel.collapse();
    }
}

function LeadArrangerDocumentController(RESTService, $mdDialog, $mdToast, $scope, $q) {
    var lad = this;
    lad.documentType = "Term Sheet"

    lad.collapse = function (panel, form) {
        panel.collapse();
    }
    lad.filesChanged = function (elm) {
        lad.files = elm.files
        lad.$apply();
    }

    $scope.getAllTermSheet = function () {
        lad.loadTableQuery = {
            limit: 5,
            page: 1
        };
        RESTService.get('modules/get', { "url": 'termSheet/getAll' }, function (response) {
            $scope.loading = false;
            if (response.status !== undefined && response.status === 200) {
                lad.loadTableQuery.page = 1;
                lad.dataStatus = -1;
                var deferred = $q.defer();
                if (response.status !== undefined && response.status === 200) {
                    if (response.data.status == 1) {
                        lad.dataStatus = 1;
                        lad.loadHistory = JSON.parse(response.data.data);
                        lad.loadLimitOptions = [5, 10, 15, {
                            label: 'All',
                            value: function () {
                                return lad.loadHistory.length;
                            }
                        }];
                    } else {
                        lad.dataStatus = response.data.status;
                        lad.loadErrorMsg = response.data.message;
                    }
                    deferred.resolve();

                } else {
                    deferred.reject();
                    lad.loadErrorMsg = (response.data && response.data.message) ? response.data.message : "Internal server error!";
                }
                lad.loadPromise = deferred.promise;
            };
        })
    }
    $scope.getAllTermSheet();

    RESTService.get('modules/get', { "url": 'getAllInitiationData' }, function (response) {
        if (response.data.data != "")
            lad.LoanRefernce = JSON.parse(response.data.data);
    })

    lad.uploadFile = function () {
        $mdDialog.show({
            templateUrl: '/views/partials/spinner-dialog.template.html',
            parent: angular.element(document.body),
            clickOutsideToClose: false,
        });
        var fd = new FormData();
        fd.append("loanRef", lad.loanRef);
        fd.append("file", lad.termSheet);
        fd.append("url", 'termSheet/upload');

        RESTService.fileupload('modules/fileupload', fd, function (response) {
            $mdDialog.hide();
            let message = "";
            let theme = "";
            if (response && response.status === 200) {
                if (response.data.status == 1) {
                    message = (response.data.message) ? response.data.message : "Term Sheet File Uploaded Successfully";
                    theme = "success-toast";
                    $scope.getAllTermSheet();

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

    };

    lad.downloadDoc = function (data1) {
        RESTService.get('modules/get', { "url": "termSheet/download/" + data1.loanRef }, function (response) {
            lad.download("TermSheet_" + data1.loanRef, response.data.data);
        })
    }
    lad.download = function (filename, text) {
        var element = document.createElement('a');
        element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
        element.setAttribute('download', filename);

        element.style.display = 'none';
        document.body.appendChild(element);

        element.click();

        document.body.removeChild(element);
    }


}
function LeadArrangerPartiesController(RESTService, $mdDialog, $mdToast, $q, $scope, $state) {
    var lap = this;
    lap.leadArrangerName = "";
    lap.leadArrangerAccount = "";
    lap.rateOfInterest = "";
    lap.leadArrangerBank = "";

    RESTService.get('modules/get', { "url": 'termSheet/getAllApprovedTermSheets/borrower' }, function (response) {
        if (response.data.data != "")
            lap.LoanRefernce = JSON.parse(response.data.data);
    })

    lap.addParties = function () {
        $mdDialog.show({
            templateUrl: '/views/partials/spinner-dialog.template.html',
            parent: angular.element(document.body),
            clickOutsideToClose: false,
        });
        var entityName1;
        if (lap.entityName == "FLIPKART") entityName1 = "Borrower1";
        else if (lap.entityName == "ILFS") entityName1 = "LenderA";
        else if (lap.entityName == "SOFTBANK") entityName1 = "LenderB";
        console.log("entity", entityName1);

        lap.partyData = {
            "url": 'subscription/addMemberDetails',
            "params": {
                "loanRef": lap.loanRef,
                "entityName": entityName1,
                "entityType": lap.entityType,
                "accountName": lap.accountName,
                "acountType": lap.accountType,
                "paymentAccount": lap.paymentAccount,
                "paymentBank": lap.paymentBank,
                "leadArrangerName": lap.leadArrangerName,
                "leadArrangerAccountNumber": lap.leadArrangerAccount,
                "rateOfInterest": Number(lap.rateOfInterest),
                "leadArrangerBank": lap.leadArrangerBank
            }
        }

        RESTService.post('/modules/post', lap.partyData, function (response) {
            $mdDialog.hide();
            let message = "";
            let theme = "";
            if (response && response.status === 200) {
                if (response.data.status == 1) {
                    message = (response.data.message) ? response.data.message : "Static Data Updated Successfully";
                    theme = "success-toast";
                    delete lap.entityName;
                    delete lap.accountName;
                    delete lap.paymentAccount;
                    delete lap.paymentBank;
                    delete lap.leadArrangerName;
                    delete lap.leadArrangerAccount;
                    delete lap.rateOfInterest;
                    delete lap.leadArrangerBank;
                    $scope.getAllStaticData();

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
            $state.go('leadArranger-parties');
        })

    }

    $scope.getAllStaticData = function () {
        lap.loadTableQuery = {
            limit: 5,
            page: 1
        };
        RESTService.get('modules/get', { "url": 'subscription/getAllStaticData' }, function (response) {
            $scope.loading = false;
            if (response.status !== undefined && response.status === 200) {
                lap.loadTableQuery.page = 1;
                lap.dataStatus = -1;
                var deferred = $q.defer();
                if (response.status !== undefined && response.status === 200) {
                    if (response.data.status == 1) {
                        lap.dataStatus = 1;
                        lap.loadHistory = JSON.parse(response.data.data);
                        lap.loadLimitOptions = [5, 10, 15, {
                            label: 'All',
                            value: function () {
                                return lap.loadHistory.length;
                            }
                        }];
                    } else {
                        lap.dataStatus = response.data.status;
                        lap.loadErrorMsg = response.data.message;
                    }
                    deferred.resolve();

                } else {
                    deferred.reject();
                    lap.loadErrorMsg = (response.data && response.data.message) ? response.data.message : "Internal server error!";
                }
                lap.loadPromise = deferred.promise;
            };
        })
    }
    $scope.getAllStaticData();

    lap.collapse = function (panel, form) {
        panel.collapse();
    }
}

function LeadArrangerSubscriptionController(RESTService, $mdDialog, $mdToast, $scope, $q, $window) {
    var lsc = this;
    lsc.sub = true;
    lsc.dataAvailable = false;
    lsc.currency = "USD (million)";
    lsc.currency1 = "USD (million)";
    lsc.leadArrangerName = $window.localStorage.getItem('username');

    RESTService.get('modules/get', { "url": 'termSheet/getAllApprovedTermSheets/borrower' }, function (response) {
        if (response.data.data != "")
            lsc.LoanRefernce = JSON.parse(response.data.data);
    })

    lsc.getDoc = function (data1) {
        if (data1 != null) {            
            var data = JSON.parse(data1);
            RESTService.get('modules/get', { "url": 'subscription/getStateMetadata/'+data.loanRef}, function (response) {
            if (response.data.data != "")
                var staticData=JSON.parse(response.data.data);
                lsc.tenor = staticData[0].tenure;
            })
            lsc.loanRef1 = data.loanRef;
            lsc.termSheet = data.fileHash;
        }
    }

    lsc.createSubscription = function () {

        $mdDialog.show({
            templateUrl: '/views/partials/spinner-dialog.template.html',
            parent: angular.element(document.body),
            clickOutsideToClose: false,
        });
        lsc.subscriptionData = {
            "url": "subscription/addSubscriptionLender",
            "params": {
                "loanRef": lsc.loanRef1,
                "subscriptionName": lsc.subscription,
                "loanAmount": Number(lsc.loanAmount),
                "tenure": Number(lsc.tenor),
                "termSheet": lsc.termSheet,
                "lender": "lender",
                "leadArranger": "O=LeadArranger,L=New York,C=US",
                "subscriptionAmount": 0.0,
                "lenderA": "O=LenderA,L=New York,C=US",
                "lenderB": "O=LenderB,L=New York,C=US"
            }
        }

        RESTService.post('/modules/post', lsc.subscriptionData, function (response) {
            $mdDialog.hide();
            let message = "";
            let theme = "";
            if (response && response.status === 200) {
                if (response.data.status == 1) {
                    message = (response.data.message) ? response.data.message : "The Subscription Ledgers for two lenders is successfully created";
                    theme = "success-toast";
                    $scope.getAllSubscription();

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

    $scope.getAllSubscription = function () {
        lsc.loadTableQuery = {
            limit: 5,
            page: 1
        };
        RESTService.get('modules/get', { "url": 'subscription/getAllSubscriptions' }, function (response) {
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

    $scope.getAllSubscription();

    RESTService.get('modules/get', { "url": 'subscription/getAllApprovedSubscriptions' }, function (response) {
        var LoanRefernce = [];
        angular.forEach((JSON.parse(response.data.data)), function (data, key) {
            if (!LoanRefernce.includes(data.loanRef)) {
                LoanRefernce.push(data.loanRef);
            }
        });
        lsc.LoanRefernce1 = LoanRefernce;
    })

    lsc.getSubscription = function (loanRef1) {
        if (loanRef1 != null) {
            lsc.dataAvailable = true;
            RESTService.get('modules/get', { "url": 'subscription/getSubscriptionsById/' + loanRef1 }, function (response) {
                var bideDatas = [];
                var isFirst = true;
                angular.forEach((JSON.parse(response.data.data)), function (data, key) {
                    if (isFirst) {
                        lsc.loanRef2 = loanRef1,
                            lsc.subscriptionRef = data.subscriptionId;
                        lsc.subscription = data.subscriptionName;
                        lsc.startDate = data.startDate;
                        lsc.endDate = data.endDate;
                        lsc.loanAmount = data.loanAmount;
                        lsc.tenor = data.tenure;
                        isFirst = false;
                    }
                    var bideData = {};
                    bideData.lender = data.lender;
                    bideData.subscriptionAmount = data.subscriptionAmount;
                    bideData.subscriptionStatus = data.subscriptionStatus
                    bideDatas.push(bideData);
                });
                lsc.loadHistory1 = bideDatas;
                if (!lsc.loadHistory1.length > 0)
                    lsc.dataStatus1 = 1;
            })
        }
    }

    lsc.confirmBid = function () {

        $mdDialog.show({
            templateUrl: '/views/partials/spinner-dialog.template.html',
            parent: angular.element(document.body),
            clickOutsideToClose: false,
        });
        lsc.bidData = {
            "url": "bid/confirmBid",
            "params": {
                "loanRef": lsc.loanRef2,
            }
        }

        RESTService.post('/modules/post', lsc.bidData, function (response) {
            $mdDialog.hide();
            let message = "";
            let theme = "";
            if (response && response.status === 200) {
                if (response.data.status == 1) {
                    message = (response.data.message) ? response.data.message : "Bid state is created for consent by the borrower";
                    theme = "success-toast";


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

    lsc.collapse = function (panel, form) {
        panel.collapse();
    }
}

function LeadArrangerLoanDetailsController(RESTService) {
    var lald = this;
    lald.dataAvailable = false;
    RESTService.get('modules/get', { "url": 'loan/getLoanIds' }, function (response) {
        if (response.data.data != "")
            lald.LoanRefernce1 = JSON.parse(response.data.data);
    })
    lald.getBalance = function (loanRef) {
        if (loanRef != null) {
            RESTService.get('modules/get', { "url": 'loan/checkAccountBalance/' + loanRef }, function (response) {
                lald.dataAvailable = true;
                var data1 = JSON.parse(response.data.data)
                lald.accountId = data1.accountId;
                lald.balance ="USD "+data1.balance+" million";
            });
        }
    }
}

function LeadArrangerInterestPaymentController(RESTService, $mdDialog, $mdToast) {
    var laip = this;
    RESTService.get('modules/get', { "url": 'loan/getLoanIds' }, function (response) {
        console.log("intrset paymt loanids", response.data)
        if (response.data.data != "")
            laip.LoanRefernce1 = JSON.parse(response.data.data);
    })

    laip.createPayment = function () {
        $mdDialog.show({
            templateUrl: '/views/partials/spinner-dialog.template.html',
            parent: angular.element(document.body),
            clickOutsideToClose: false,
        });

        laip.paymentData = {
            "url": "interestPayment/create",
            "params": {
                "loanRef": laip.loanRef
            }
        }

        RESTService.post('/modules/post', laip.paymentData, function (response) {
            $mdDialog.hide();
            let message = "";
            let theme = "";
            if (response && response.status === 200) {
                if (response.data.status == 1) {
                    message = (response.data.message) ? response.data.message : "Interest Payment Initiated Successfully";
                    theme = "success-toast";


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

}
