//"use strict";
(function () {

    if (typeof window.demoApp === 'undefined') {
        window.demoApp = angular.module("demoApp", ['ngResource', 'ngRoute', 'pascalprecht.translate', 'ngLocale']);

    }

})();


demoApp.directive("grid", function () {

    return {
        restrict: "E",
        scope: {
            data: "="
        },
        template: '<div class="bd-example-row">\n    <div class="bd-example">\n        <div class="container">\n            <div class="row">\n                <div class="col">Column</div>\n                <div class="col">Column</div>\n                <div class="col">Column</div>\n                <div class="col">Column</div>\n            </div>\n\n            <div class="row">\n                <div class="col">Column</div>\n                <div class="col">Column</div>\n                <div class="col">Column</div>\n                <div class="col">Column</div>\n            </div>\n            \n        </div>\n    </div>\n</div>',

        controller: function ($rootScope, $scope, $compile, $element, $attrs, $http, $routeParams, $location, $q) {

        }

    }


});

