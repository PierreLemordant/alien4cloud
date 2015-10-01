define(function(require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  require('scripts/applications/services/application_services');
  require('scripts/applications/services/application_environment_services');
  require('scripts/applications/services/application_version_services');

  require('scripts/applications/controllers/application_deployment_locations');
  require('scripts/applications/controllers/application_deployment_match');
  require('scripts/applications/controllers/application_deployment_input');
  require('scripts/applications/services/deployment_topology_processor.js');

  require('scripts/deployment/directives/display_outputs');
  require('scripts/common/filters/inputs');

  function refreshDeploymentContext(deploymentContext, application, deploymentTopologyServices, deploymentTopologyProcessor) {
    return deploymentTopologyServices.get({
      appId: application.id,
      envId: deploymentContext.selectedEnvironment.id
    }).$promise.then(function(response) {
        deploymentTopologyProcessor.process(response.data);
        deploymentContext.deploymentTopologyDTO = response.data;
        return deploymentContext;
      });
  }

  states.state('applications.detail.deployment', {
    url: '/deployment',
    resolve: {
      deploymentContext: ['application', 'appEnvironments', 'deploymentTopologyServices', 'deploymentTopologyProcessor',
        function(application, appEnvironments, deploymentTopologyServices, deploymentTopologyProcessor) {
          var deploymentContextResult = {
            selectedEnvironment: appEnvironments.environments[0]
          };
          return refreshDeploymentContext(deploymentContextResult, application.data, deploymentTopologyServices, deploymentTopologyProcessor);
        }
      ]
    },
    templateUrl: 'views/applications/application_deployment.html',
    controller: 'ApplicationDeploymentCtrl',
    menu: {
      id: 'am.applications.detail.deployment',
      state: 'applications.detail.deployment',
      key: 'NAVAPPLICATIONS.MENU_DEPLOYMENT',
      icon: 'fa fa-cloud-upload',
      roles: ['APPLICATION_MANAGER', 'APPLICATION_DEPLOYER'], // is deployer
      priority: 300
    }
  });

  modules.get('a4c-applications').controller('ApplicationDeploymentCtrl',
    ['$scope', 'authService', 'application', '$state', 'appEnvironments', 'menu', 'deploymentContext', 'deploymentTopologyServices', 'deploymentTopologyProcessor', 'applicationServices',
      function($scope, authService, applicationResult, $state, appEnvironments, menu, deploymentContext, deploymentTopologyServices, deploymentTopologyProcessor, applicationServices) {
        $scope.deploymentContext = deploymentContext;
        var pageStateId = $state.current.name;
        $scope.menu = menu;

        // Initialization
        $scope.application = applicationResult.data;
        $scope.envs = appEnvironments.deployEnvironments;

        $scope.$watch('deploymentContext.selectedEnvironment', function() {
          refreshDeploymentContext($scope.deploymentContext, $scope.application, deploymentTopologyServices, deploymentTopologyProcessor);
        });

        // Retrieval and validation of the topology associated with the deployment.
        var checkTopology = function() {

          $scope.isTopologyValid($scope.topologyId, $scope.deploymentContext.selectedEnvironment.id).$promise.then(function(validTopologyResult) {
            $scope.validTopologyDTO = validTopologyResult.data;
            prepareTasksAndWarnings($scope.validTopologyDTO);
          });

          var processTopologyInfoResult = $scope.processTopologyInformations($scope.topologyId);

          // when the selected environment is deployed => refresh output properties
          processTopologyInfoResult.$promise.then(function() {
            if ($scope.deploymentContext.selectedEnvironment.status === 'DEPLOYED') {
              $scope.refreshInstancesStatuses($scope.application.id, $scope.deploymentContext.selectedEnvironment.id, pageStateId);
            }
          });

        };

        //register the checking topo function for others states to use it
        $scope.checkTopology = checkTopology;

        $scope.goToLocationTab = function() {
          $scope.setTopologyId($scope.application.id, $scope.deploymentContext.selectedEnvironment.id, checkTopology).$promise.then(function() {
            $state.go('applications.detail.deployment.locations');
          });
        };

        $scope.goToLocationTab(); // immediately go to location tab

        // Just group tasks / warnings by category for the display
        function prepareTasksAndWarnings(validTopologyDTO) {
          // currently prepare warnings
          if (_.defined(validTopologyDTO.warningList)) {
            var preparedWarningList = {};
            validTopologyDTO.warningList.forEach(function(task) {
              if (!preparedWarningList.hasOwnProperty(task.code)) {
                preparedWarningList[task.code] = [];
              }
              preparedWarningList[task.code].push(task);
            });
            // replace the default warning list
            validTopologyDTO.warningList = preparedWarningList;
          }
        }

        // Change the selected environment (set only if required).
        var changeEnvironment = function(newEnvironment) {
          if (_.defined(newEnvironment) && newEnvironment.id !== $scope.deploymentContext.selectedEnvironment.id) {
            setEnvironmentAndForwardToLocations(newEnvironment);
          }
        };

        $scope.showTodoList = function() {
          return !$scope.validTopologyDTO.valid && $scope.isManager;
        };

        $scope.showWarningList = function() {
          return angular.isObject($scope.validTopologyDTO.warningList) && Object.keys($scope.validTopologyDTO.warningList).length > 0;
        };

        // Map functions that should be available from scope.
        $scope.changeEnvironment = changeEnvironment;

        // Application rights
        $scope.isManager = authService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');
        $scope.isDevops = authService.hasResourceRole($scope.application, 'APPLICATION_DEVOPS');
        // Application environment rights
        $scope.isDeployer = authService.hasResourceRole($scope.deploymentContext.selectedEnvironment, 'DEPLOYMENT_MANAGER');
        $scope.isUser = authService.hasResourceRole($scope.deploymentContext.selectedEnvironment, 'APPLICATION_USER');

        $scope.finalOutputAttributesValue = $scope.outputAttributesValue;
        $scope.validTopologyDTO = false;

        $scope.isAllowedDeployment = function() {
          return $scope.isDeployer || $scope.isManager;
        };

        // if the status or the environment changes we must update the event registration.
        $scope.$watch(function(scope) {
          if (_.defined($scope.deploymentContext.selectedEnvironment)) {
            return $scope.deploymentContext.selectedEnvironment.id + '__' + $scope.deploymentContext.selectedEnvironment.status;
          }
          return 'UNDEPLOYED';
        }, function(newValue) {
          var undeployedValue = $scope.deploymentContext.selectedEnvironment.id + '__UNDEPLOYED';
          // no registration for this environement -> register if not undeployed!
          if (newValue === undeployedValue) {
            // if status the application is not undeployed we should register for events.
            $scope.stopEvent();
          } else {
            $scope.stopEvent();
            $scope.setTopologyId($scope.application.id, $scope.deploymentContext.selectedEnvironment.id, checkTopology).$promise.then(function(result) {
              $scope.processTopologyInformations(result.data).$promise.then(function() {
                $scope.refreshInstancesStatuses($scope.application.id, $scope.deploymentContext.selectedEnvironment.id, pageStateId);
              });
            });
          }
        });

        // when scope change, stop current event listener
        $scope.$on('$destroy', function() {
          $scope.stopEvent();
        });


        // Deployment handler
        $scope.deploy = function() {
          // Application details with deployment properties
          var deployApplicationRequest = {
            applicationId: $scope.application.id,
            applicationEnvironmentId: $scope.deploymentContext.selectedEnvironment.id
          };
          $scope.isDeploying = true;
          applicationServices.deployApplication.deploy([], angular.toJson(deployApplicationRequest), function() {
            $scope.deploymentContext.selectedEnvironment.status = 'DEPLOYMENT_IN_PROGRESS';
            $scope.isDeploying = false;
          }, function() {
            $scope.isDeploying = false;
          });
        };

        $scope.undeploy = function() {
          $scope.isUnDeploying = true;
          applicationServices.deployment.undeploy({
            applicationId: $scope.application.id,
            applicationEnvironmentId: $scope.deploymentContext.selectedEnvironment.id
          }, function() {
            $scope.deploymentContext.selectedEnvironment.status = 'UNDEPLOYMENT_IN_PROGRESS';
            $scope.isUnDeploying = false;
            $scope.stopEvent();
          }, function() {
            $scope.isUnDeploying = false;
          });
        };

      }
    ]); //controller
}); //Define
