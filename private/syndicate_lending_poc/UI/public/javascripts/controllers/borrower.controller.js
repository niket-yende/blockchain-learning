angular.module("syndicatedloan").controller("BorrowerController", BorrowerController);
angular.module("syndicatedloan").controller("BorrowerDocumentController", BorrowerDocumentController);
angular.module("syndicatedloan").controller("BorrowerLoanDetailsController", BorrowerLoanDetailsController);
angular.module("syndicatedloan").controller("BorrowerInterestPaymentController", BorrowerInterestPaymentController);
angular.module("syndicatedloan").controller("BorrowerSubscriptionController", BorrowerSubscriptionController);

function BorrowerController($scope, $window, $state){
    var bc=this;
    var curState = $state.$current.name;
    
    if(curState === 'borrower-home' || curState === 'borrower-document'){
      bc.currentNavItem = 'borrowerDocument';
      $scope.loading = true;
      $state.go('borrower-document');
    }
    else if(curState === 'borrower-subscription'){
      bc.currentNavItem = 'borrowerSubscription';
      $scope.loading = true;
      $state.go('borrower-subscription');
    }
    else if(curState === 'borrower-loanDetails'){
      bc.currentNavItem = 'borrowerLoanDetails';
      $scope.loading = true;
      $state.go('borrower-loanDetails');
    }
    else if(curState === 'borrower-interestPayment'){
      bc.currentNavItem = 'borrowerInterestPayment';
      $scope.loading = true;
      $state.go('borrower-interestPayment');
    }
    else{
      isLogin = true;
      $window.location.href = '/borrower-home';
    }
}

function BorrowerDocumentController(RESTService,$mdDialog,$mdToast,$q,$window,$scope){
  var bdc=this;
  bdc.doc=true;
  bdc.documentType="Term Sheet"

  RESTService.get('modules/get',{"url":'termSheet/getAll'},function(response){
    if(response.data.data !=null)
        bdc.LoanRefernce =JSON.parse(response.data.data);
  })

  bdc.getTermSheet=function(data1){
    if(data1 !=null){
        var data=JSON.parse(data1)
        bdc.status=data.status;
        bdc.loanRef=data.loanRef;
        bdc.fileHash=data.fileHash;
    }
  }

  bdc.downloadDoc=function(){
    RESTService.get('modules/get',{"url":"termSheet/download/"+bdc.loanRef},function(response){
        bdc.download("TermSheet_"+bdc.loanRef,response.data.data);
      })
  }
  bdc.downloadDoc1=function(data){
    RESTService.get('modules/get',{"url":"termSheet/download/"+data.loanRef},function(response){
        bdc.download("TermSheet_"+data.loanRef,response.data.data);
      })
  }
  bdc.download=function(filename, text) {
    var element = document.createElement('a');
    element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
    element.setAttribute('download', filename);
  
    element.style.display = 'none';
    document.body.appendChild(element);
  
    element.click();
  
    document.body.removeChild(element);
  }

  bdc.getAllApprovedDoc=function(){
    bdc.loadTableQuery = {
      limit: 5,
      page: 1
    };
    RESTService.get('modules/get',{"url":'termSheet/getAllApprovedTermSheets/borrower'},function(response){
      $scope.loading = false;
      if(response.status !== undefined && response.status === 200){
        bdc.loadTableQuery.page = 1;
        bdc.dataStatus = -1;
          var deferred = $q.defer();
          if(response.status !== undefined && response.status === 200){
              if(response.data.status == 1){
                bdc.dataStatus = 1;
                bdc.loadHistory =JSON.parse(response.data.data);
                  bdc.loadLimitOptions = [5, 10, 15, {
                      label: 'All',
                      value: function () {
                          return bdc.loadHistory.length;
                      }
                  }];
              } else {
                bdc.dataStatus = response.data.status;
                bdc.loadErrorMsg = response.data.message;
              }
              deferred.resolve();

          } else {
              deferred.reject();
              bdc.loadErrorMsg = (response.data && response.data.message) ? response.data.message : "Internal server error!";
          }
          bdc.loadPromise = deferred.promise;
      };
    })
  }

  bdc.getAllApprovedDoc();

  bdc.approveDoc=function(){
      $mdDialog.show({
        templateUrl: '/views/partials/spinner-dialog.template.html',
        parent: angular.element(document.body),
        clickOutsideToClose:false,
    });
    var approveData={
      "url":'termSheet/approveByBorrower',
      "params":{
        "loanRef":bdc.loanRef
      }
    }
    RESTService.put('modules/put',approveData,{"Content-Type": "application/json"},function(response){
      $mdDialog.hide();
            let message = "";
            let theme = "";
            if(response && response.status === 200){
                if(response.data.status == 1){
                    message = (response.data.message) ? response.data.message : "Term Sheet Approved Successfully";
                    theme = "success-toast";
                    bdc.getAllApprovedDoc();
               
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

  

  bdc.collapse = function(panel, form){
		panel.collapse();
	}
}

function BorrowerSubscriptionController(RESTService,$scope,$mdDialog,$mdToast,$q){
  var bsc=this;
  bsc.currency="USD (million)";
  bsc.dataAvailable=false;

  RESTService.get('modules/get',{"url":'bid/getAllBidState'},function(response){
    var LoanRefernce = [];
    angular.forEach((JSON.parse(response.data.data)), function(data, key) {
        if(!LoanRefernce.includes(data.loanRef)){
            LoanRefernce.push(data.loanRef);
        }
    });
    bsc.LoanRefernce1=LoanRefernce;
  })
    
  bsc.getSubscription=function(loanRef1){
    if(loanRef1!= null){
        bsc.dataAvailable=true;
        RESTService.get('modules/get',{"url":'bid/getBidStateById/'+loanRef1},function(response){
        var data=JSON.parse(response.data.data);
            bsc.loanRef2=loanRef1,
            bsc.subscriptionRef=data.subscriptionId;
            bsc.subscription=data.subscriptionName;
            bsc.startDate=data.startDate;
            bsc.endDate=data.endDate;
            bsc.loanAmount=data.loanAmount;
            bsc.tenor=data.tenure;
            bsc.lenderAName=data.lenderAName;
            bsc.lenderASubsAmount=data.lenderASubsAmount;
            bsc.lenderBName=data.lenderBName;
            bsc.lenderBSubsAmount=data.lenderBSubsAmount;
        })
  }
}

  bsc.confirmBidBorrower=function(){

    $mdDialog.show({
        templateUrl: '/views/partials/spinner-dialog.template.html',
        parent: angular.element(document.body),
        clickOutsideToClose:false,
    });
    bsc.bidData={
        "url":"bid/confirmBorrowerConsent",
        "params":{
            "loanRef":bsc.loanRef2
        }
      }
  
      RESTService.post('/modules/post',bsc.bidData,function(response){
              $mdDialog.hide();
              let message = "";
              let theme = "";
              if(response && response.status === 200){
                  if(response.data.status == 1){
                      message = (response.data.message) ? response.data.message : "Consent provided by the borrower";
                      theme = "success-toast";
                      bsc.getAllBidState();
                 
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

  bsc.getAllBidState=function(){
    bsc.loadTableQuery = {
      limit: 5,
      page: 1
    };
    RESTService.get('modules/get',{"url":'bid/getAllBidState'},function(response){
      $scope.loading = false;
      if(response.status !== undefined && response.status === 200){
        bsc.loadTableQuery.page = 1;
        bsc.dataStatus = -1;
          var deferred = $q.defer();
          if(response.status !== undefined && response.status === 200){
              if(response.data.status == 1){
                bsc.dataStatus = 1;
                bsc.loadHistory =JSON.parse(response.data.data);
                  bsc.loadLimitOptions = [5, 10, 15, {
                      label: 'All',
                      value: function () {
                          return bsc.loadHistory.length;
                      }
                  }];
              } else {
                bsc.dataStatus = response.data.status;
                bsc.loadErrorMsg = response.data.message;
              }
              deferred.resolve();

          } else {
              deferred.reject();
              bsc.loadErrorMsg = (response.data && response.data.message) ? response.data.message : "Internal server error!";
          }
          bsc.loadPromise = deferred.promise;
      };
    })
  }
  bsc.getAllBidState();

  bsc.collapse = function(panel, form){
		panel.collapse();
    }
  
}

function BorrowerLoanDetailsController($scope,$mdDialog,$mdToast,RESTService,$q,$state){
  var bld=this;
  $scope.loanType="Senior Loan-Unsecured";
  $scope.interestRateType="Fixed";
  bld.dataAvailable=false;
  bld.dataAvailable1=false;
  var today=new Date();
  var date=today.getDate();

  RESTService.get('modules/get',{"url":'bid/getAllBidState'},function(response){
    var LoanRefernce = [];
    angular.forEach((JSON.parse(response.data.data)), function(data, key) {
        if(!LoanRefernce.includes(data.loanRef)){
            LoanRefernce.push(data.loanRef);
        }
    });
    bld.LoanRefernce1=LoanRefernce;
  })

  bld.getLoanDetails=function(loanRef){
    if(loanRef != null){
        bld.dataAvailable=true;
        RESTService.get('modules/get',{"url":'loan/getLoanIssuanceDetails/'+loanRef},function(response){
            var data=JSON.parse(response.data.data);
            bld.loanRff=loanRef;
            bld.loanName=data.loan_detail.loanName;
            bld.loanType=data.loan_detail.loanType;
            bld.principal= data.loan_detail.principal;
            bld.tenor= data.loan_detail.tenure;
            bld.interestRateType = data.loan_detail.interestRateType;
            bld.interestRate = data.loan_detail.interestRate;
            bld.collateral= data.loan_detail.collateral;
            bld.arrangerAccount = data.loan_detail.arranger_account;
            bld.paymentDate = date;
        });
  }
}

  bld.createLoanLedger=function(){

    $mdDialog.show({
        templateUrl: '/views/partials/spinner-dialog.template.html',
        parent: angular.element(document.body),
        clickOutsideToClose:false,
    });
    
    bld.loanData={
        "url":"loan/createLoanLedger",
        "params":{
            "loanRef":bld.loanRff,
            "loanName":bld.loanName,
            "loanType":bld.loanType,
            "principal":bld.principal,
            "tenure":bld.tenor,
            "interestRateType":bld.interestRateType,
            "interestRate":bld.interestRate,
            "frequency":bld.frequency,
            "paymentDate":bld.paymentDate,
            "collateral":bld.collateral,
            "arranger_account":bld.arrangerAccount

        }
      }
  
      RESTService.post('/modules/post',bld.loanData,function(response){
              $mdDialog.hide();
              let message = "";
              let theme = "";
              if(response && response.status === 200){
                  if(response.data.status == 1){
                      message = (response.data.message) ? response.data.message : "Loan ledger & borrower account is successfully created";
                      theme = "success-toast";
                      bld.getAllLoanLedger();                
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
              $state.go('borrower-loanDetails');
      })
      
  }

  bld.getAllLoanLedger=function(){
        bld.loadTableQuery = {
      limit: 5,
      page: 1
    };
    RESTService.get('modules/get',{"url":'loan/getAllLoanLedgers'},function(response){
      $scope.loading = false;
      if(response.status !== undefined && response.status === 200){
        bld.loadTableQuery.page = 1;
        bld.dataStatus = -1;
          var deferred = $q.defer();
          if(response.status !== undefined && response.status === 200){
              if(response.data.status == 1){
                bld.dataStatus = 1;
                bld.loadHistory =JSON.parse(response.data.data);
                bld.loadLimitOptions = [5, 10, 15, {
                      label: 'All',
                      value: function () {
                          return bld.loadHistory.length;
                      }
                  }];
              } else {
                bld.dataStatus = response.data.status;
                bld.loadErrorMsg = response.data.message;
              }
              deferred.resolve();

          } else {
              deferred.reject();
              bld.loadErrorMsg = (response.data && response.data.message) ? response.data.message : "Internal server error!";
          }
          bld.loadPromise = deferred.promise;
      };
    })
    RESTService.get('modules/get',{"url":'loan/getLoanIds'},function(response){
        console.log("blc loanids",response.data);
        if(response.data.data != "")
            bld.LoanRefernce2=JSON.parse(response.data.data);
    })
  }
  bld.getAllLoanLedger();
  
  bld.getBalance=function(loanRef){
    if(loanRef != null){
        RESTService.get('modules/get',{"url":'loan/checkAccountBalance/'+loanRef},function(response){
            bld.dataAvailable1=true;
            var data1=JSON.parse(response.data.data)
            bld.accountId=data1.accountId;
            bld.balance="USD "+data1.balance+" million";
        });
    }
}

  bld.collapse = function(panel, form){
		panel.collapse();
	}
}

function BorrowerInterestPaymentController($mdDialog,$mdToast,RESTService,$q,$state,$scope){
  var bip=this;
  bip.dataAvailable=false;

  RESTService.get('modules/get',{"url":'loan/getLoanIds'},function(response){
    if(response.data.data != "")
        bip.LoanRefernce=JSON.parse(response.data.data);
  })

  bip.getPaymentDetails=function(loanRef){
      if(loanRef!=null){
        bip.dataAvailable=true;
        RESTService.get('modules/get',{"url":'interestPayment/getInterestPaymentsById/'+loanRef},function(response){
            var data=JSON.parse(response.data.data);
            bip.loanRef1=data.loanRef;
            bip.paymentDate=data.paymentDate;
            bip.paymentAccount=data.paymentAccount;
            bip.payingBank=data.borrowerBank;
            bip.leadArrangerAccount=data.leadArrangerAccount;
            bip.leadArrangerBank=data.leadArrangerBank;
            bip.interestObligation="USD "+data.interestObligation+" million";

            })
    }  
    }

    bip.approvePaymet=function(){
        $mdDialog.show({
            templateUrl: '/views/partials/spinner-dialog.template.html',
            parent: angular.element(document.body),
            clickOutsideToClose:false,
        });
        bip.payment={
            "url":"interestPayment/pay",
            "params":{
                "loanRef":bip.loanRef1
            }
          }      
          RESTService.post('/modules/post',bip.payment,function(response){
                  $mdDialog.hide();
                  let message = "";
                  let theme = "";
                  if(response && response.status === 200){
                      if(response.data.status == 1){
                          message = (response.data.message) ? response.data.message : "Interest Payment Done Successfully";
                          theme = "success-toast";
                          $mdDialog.show({
                            templateUrl: '/views/partials/spinner-dialog.template.html',
                            parent: angular.element(document.body),
                            clickOutsideToClose:false,
                            });

                          setTimeout(function(){
                            bip.getAllPayments();
                            $mdDialog.hide();   
                            },10000)
                                                  
                     
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

    bip.getAllPayments=function(){
        bip.loadTableQuery = {
          limit: 5,
          page: 1
        };
        RESTService.get('modules/get',{"url":'interestPayment/getPaidInterestPayments'},function(response){
          $scope.loading = false;
          if(response.status !== undefined && response.status === 200){
            bip.loadTableQuery.page = 1;
            bip.dataStatus = -1;
              var deferred = $q.defer();
              if(response.status !== undefined && response.status === 200){
                  if(response.data.status == 1){
                    bip.dataStatus = 1;
                    bip.loadHistory =JSON.parse(response.data.data);
                    console.log("all data",bip.loadHistory);
                    bip.loadLimitOptions = [5, 10, 15, {
                          label: 'All',
                          value: function () {
                              return bip.loadHistory.length;
                          }
                      }];
                  } else {
                    bip.dataStatus = response.data.status;
                    bip.loadErrorMsg = response.data.message;
                  }
                  deferred.resolve();
    
              } else {
                  deferred.reject();
                  bip.loadErrorMsg = (response.data && response.data.message) ? response.data.message : "Internal server error!";
              }
              bip.loadPromise = deferred.promise;
          };
        })
    }
    
    bip.getAllPayments();

    bip.collapse = function(panel, form){
		panel.collapse();
	}
}

