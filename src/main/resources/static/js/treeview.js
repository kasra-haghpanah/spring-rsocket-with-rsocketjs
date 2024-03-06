//"use strict";
angular.module('ngTreeview', [])
    .directive("ngTreeview", function () {
        return {
            restrict: "E",
            scope: {
                config: "="
            },
            //require: '?ngModel',
            template: '<div class="{{config.class}}" style="width: {{config.width}};height: {{config.height}};" dir="{{config.dir()}}">\n\n    <div ng-repeat="row in rows" ng-if="row.show==true" ng-style="{\'height\': config.size()+\'px\'}"\n         class="row ">\n\n        <!--<div ng-repeat="i in getArr(row[config.model.levelId]-1) track by $index" ng-style="{\'width\': config.size() + \'px\', \'height\': config.size() + \'px\', \'white-space\': \'nowrap\', \'position\': \'relative\'}">-->\n        <div ng-repeat="i in getArr(row[config.model.levelId]-1) track by $index" ng-style="{\'width\': config.size() + \'px\', \'height\': config.size() + \'px\', \'white-space\': \'nowrap\', \'position\': \'relative\'}">    \n        <div ng-style="{\'width\': config.size()+\'px\',\'height\': config.size()+\'px\'}"\n                 ng-if="row.parent != null && row.parent[config.model.levelId] == $index && row.lastNode ==false">\n\n                <div ng-if="config.showLine() == \'true\'">\n                    <div ng-if="config.dir()==\'rtl\'" ng-style="{\'width\': config.size()+\'px\',\'height\': config.size()+\'px\'}">\n                        <div ng-style="{\'width\': (config.size()/2-1)+\'px\',\'height\': (config.size()/2-1)+\'px\',\'border-bottom\': \'1px dotted \'+config.color()}"\n                             class="float-left"></div>\n                        <div ng-style="{\'width\': (config.size()/2-1)+\'px\',\'height\': config.size() +\'px\',\'border-left\': \'1px dotted \'+config.color()}"\n                             class="float-left"></div>\n                    </div>\n\n\n                    <div ng-if="config.dir()!=\'rtl\'" ng-style="{\'width\': config.size()+\'px\',\'height\': config.size()+\'px\'}">\n                        <div ng-style="{\'width\': (config.size()/2-.5)+\'px\',\'height\': (config.size()-1)+\'px\',\'border-right\': \'1px dotted \'+config.color()}"\n                             class="float-left"></div>\n                        <div ng-style="{\'width\': (config.size()/2-.5)+\'px\',\'height\': (config.size()/2-1)+\'px\',\'border-bottom\': \'1px dotted \'+config.color()}"\n                             class="float-right"></div>\n                    </div>\n\n                </div>\n            </div>\n            <div\n                    ng-if="row.parent != null && row.parent[config.model.levelId] == $index && row.lastNode == true" ng-style="{\'width\': config.size()+\'px\',\'height\': config.size()+\'px\'}">\n\n                <div ng-if="config.showLine() == \'true\'">\n                    <div ng-if="config.dir()==\'rtl\'"\n                         ng-style="{\'width\': (config.size()/2)+\'px\',\'height\': (config.size()/2)+\'px\',\'border-right\': \'1px dotted \'+config.color(),\'border-bottom\': \'1px dotted \'+config.color()}"\n                         class="float-left">&nbsp;\n                    </div>\n                    <div ng-if="config.dir()==\'rtl\'"\n                         ng-style="{\'width\': (config.size()/2)+\'px\',\'height\': (config.size())+\'px\'}"\n                         class="float-left">&nbsp;\n                    </div>\n\n\n                    <div ng-if="config.dir()!=\'rtl\'"\n                         ng-style="{\'width\': (config.size()/2-1.5)+\'px\',\'height\': (config.size()/2-1)+\'px\'}"\n                         class="float-left">&nbsp;\n                    </div>\n                    <div ng-if="config.dir()!=\'rtl\'"\n                         ng-style="{\'width\': (config.size()/2-1.5)+\'px\',\'height\': (config.size()/2-1)+\'px\',\'border-left\': \'1px dotted \'+config.color(),\'border-bottom\': \'1px dotted \'+config.color()}"\n                         class="float-left">&nbsp;\n                    </div>\n\n\n                </div>\n\n\n            </div>\n            <div ng-style="{\'width\': config.size()+\'px\',\'height\': config.size()+\'px\'}"\n                 ng-if="row.parent!=null && (row.parent[config.model.levelId] > $index) && row.indexes[$index] != null">\n                <div ng-if="config.showLine() == \'true\'">\n                    <div ng-style="{\'width\': (config.size()/2)+\'px\',\'height\': (config.size()-1)+\'px\',\'border-right\': \'1px dotted \'+config.color()}"\n                         class="float-left">&nbsp;\n                    </div>\n                </div>\n\n            </div>\n        </div>\n        <div ng-style="{\'width\': config.size() + \'px\', \'height\': config.size() + \'px\', \'white-space\': \'nowrap\', \'position\': \'relative\'}"\n        >\n            <div ng-if="row[config.model.leafNode]==false" ng-click="collapse(row)"\n                 ng-class="(row.collapsed == false)?\'glyphicon glyphicon-triangle-bottom\':(config.dir() == \'rtl\')?\'glyphicon glyphicon-triangle-left\':\'glyphicon glyphicon-triangle-right\'"\n                 ng-style="{\'font-size\' : (config.size()-10) + \'px\',\'color\':config.color(),\'cursor\':\'pointer\'}"\n            >\n            </div>\n            <div ng-if="row[config.model.leafNode]==true"\n                 ng-style="{\'width\': config.size() + \'px\', \'height\': config.size() + \'px\'}"\n\n            >&nbsp;\n            </div>\n        </div>\n        <div ng-if="config.type()!=\'filesystem\'"\n             ng-style="{\'width\': config.size() + \'px\', \'height\': config.size() + \'px\', \'white-space\': \'nowrap\', \'position\': \'relative\'}"\n             ng-click="click(row)">\n\n\n            <div ng-if="row.selected==true" class="glyphicon glyphicon-check"\n                 ng-style="{\'font-size\' : (config.size()-4) + \'px\',\'color\':config.color(row),\'cursor\':\'pointer\'}"></div>\n            <div ng-if="row.nonSelected" class="glyphicon glyphicon-unchecked"\n                 ng-style="{\'font-size\' : (config.size()-4) + \'px\',\'color\':config.color(row),\'cursor\':\'pointer\'}"></div>\n\n            <div ng-if="row.partSelected" class="glyphicon glyphicon-share"\n                 ng-style="{\'font-size\' : (config.size()-4) + \'px\',\'color\':config.color(row),\'cursor\':\'pointer\'}"></div>\n\n        </div>\n\n        <div ng-if="config.type()==\'filesystem\'" ng-mousedown="mousedown(row,$event)"\n             ng-style="{\'width\': config.size() + \'px\', \'height\': config.size() + \'px\', \'white-space\': \'nowrap\', \'position\': \'relative\'}"\n        >\n\n\n            <div ng-if="!row[config.model.leafNode]" class="glyphicon glyphicon-folder-open"\n                 ng-style="{\'font-size\' : (config.size()-4) + \'px\',\'color\':config.color(row),\'cursor\':\'pointer\'}"></div>\n            <div ng-if="row[config.model.leafNode]" class="glyphicon glyphicon-file"\n                 ng-style="{\'font-size\' : (config.size()-4) + \'px\',\'color\':config.color(row),\'cursor\':\'pointer\'}"></div>\n\n\n        </div>\n\n        <div ng-click="clickName(row)" ng-style="{\'height\': config.size()+\'px\',\'line-height\': config.size()+\'px\',\'font-size\': \'13px\',\'white-space\': \'nowrap\',\'cursor\':\'pointer\'}"\n        >\n            &nbsp;{{row[config.model.name]}}&nbsp;\n        </div>\n    </div>\n\n\n</div>\n        \n',
            controller: function ($scope, $compile, $element, $attrs) {

                if ($scope.config.model.leafNode === undefined) {
                    $scope.config.model.leafNode = "leafNode";
                }
                if ($scope.config.model.levelId === undefined) {
                    $scope.config.model.levelId = "levelId";
                }


                $scope.removeChildNodesByParentNode = function (parentNode, isRemoveParentNode, setLeafNode) {
                    if ($scope.rows == null || $scope.rows.length < 1) {
                        return;
                    }
                    var parent = parentNode;
                    if (isRemoveParentNode) {
                        if (parent['parent'] != null && parent['parent'] != undefined) {
                            parent = parent['parent'];
                        } else {
                            parent = null;
                        }
                    }

                    if (!isRemoveParentNode) {
                        if (parentNode.list == null || parentNode.list.length == 0) {
                            parentNode[$scope.config.model.leafNode] = true;
                            return;
                        }
                        if (parentNode.partSelected == true) {
                            parentNode.partSelected = false;
                            parentNode.nonSelected = true;
                            parentNode.selected = false;
                        }
                    }

                    var lengthAllNodes = $scope.rows.length;
                    var index = $scope.rows.indexOf(parentNode);
                    if (index < 0) {
                        return;
                    }
                    //parentNode.collapsed = true;
                    var indexLastChildNode = index;
                    var start = index;
                    if (!isRemoveParentNode) {
                        start = index + 1;
                    }
                    for (var i = start; i < lengthAllNodes; i++) {
                        if (parentNode.levelId === undefined) {
                            parentNode.levelId = 0;
                        }
                        if ($scope.rows[i].levelId <= parentNode.levelId && $scope.rows[i] != parentNode) {
                            break;
                        }
                        if ($scope.rows[i].parent != undefined && $scope.rows[i].parent != null) {
                            $scope.rows[i].parent.list.splice($scope.rows[i].parent.list.indexOf($scope.rows[i]), 1);
                            var list = $scope.rows[i].parent.list;
                            if (list.length > 0) {
                                list[list.length - 1].lastNode = true;
                                $scope.rows[i].indexes = $scope.copyArray($scope.rows[i].parent.indexes).push(null);
                            }
                        }
                        $scope.rows[i].parent = null;
                        $scope.rows[i].parentId = -1;
                        indexLastChildNode++;
                    }
                    //if (indexLastChildNode > index) {
                    parentNode.list = [];
                    if (!isRemoveParentNode) {
                        $scope.rows.splice(index + 1, indexLastChildNode - index);
                    } else {
                        $scope.rows.splice(index, indexLastChildNode - index);
                    }
                    //}
                    if (setLeafNode && parent != null && (parent.list === undefined || parent.list == null || parent.list.length == 0)) {
                        parent[$scope.config.model.leafNode] = true;
                    }
                    return parent;
                }

                $scope.editNode = function (currentNode, newNode) {
                    for (var key in newNode) {
                        currentNode[key] = newNode[key];
                    }
                }

                $scope.addChildNodeAsRoot = function (node) {
                    treeNode = $scope.createTreeNode(node);
                    treeNode.parent = null;
                    treeNode.collapsed = true;
                    treeNode.lastNode = false;
                    treeNode.indexes = [null];
                    treeNode.levelId = 0;
                    treeNode.status = "root";
                    treeNode.show = true;
                    $scope.rows.push(treeNode);
                    return treeNode;
                }

                $scope.addChildNodes = function (parentNode, childNodes) {
                    if (parentNode == null || parentNode.length == 0) {
                        return;
                    }
                    if (childNodes == null || childNodes.length == 0) {
                        parentNode[$scope.config.model.leafNode] = true;
                        return;
                    }
                    //parentNode.list = [];
                    var prevLastChild = null;
                    if (parentNode.list.length > 0) {
                        parentNode.list[parentNode.list.length - 1].lastNode = false;
                        var indexes = $scope.copyArray(parentNode.indexes);
                        indexes[indexes.length] = parentNode.levelId;
                        prevLastChild = parentNode.list[parentNode.list.length - 1];
                        prevLastChild.indexes = indexes;
                    }
                    var nodeName = $scope.config.model.name;
                    var parentNodeChildsLength = parentNode.list.length;

                    //$scope.removeChildNodes(parentNode);
                    var index = $scope.rows.indexOf(parentNode);
                    if (parentNode.list.length > 0) {
                        for (var i = index + 1; i < $scope.rows.length; i++) {
                            if ($scope.rows[i].levelId <= parentNode.levelId) {
                                break;
                            }
                            index++;
                        }
                    }

                    var treeNode = null;
                    for (var i = 0; i < childNodes.length; i++) {
                        treeNode = $scope.createTreeNode(childNodes[i]);
                        treeNode.parent = parentNode;
                        treeNode.collapsed = true;
                        treeNode.lastNode = false;
                        treeNode.indexes = $scope.copyArray(treeNode.parent.indexes);
                        if (i == childNodes.length - 1) {
                            treeNode.lastNode = true;
                            treeNode.indexes.push(null);
                        } else {
                            treeNode.indexes.push(treeNode.parent.levelId);
                        }
                        treeNode.parent.list.push(treeNode);
                        treeNode.parent.selectedCount = treeNode.parent.list.length;
                        treeNode.parent[$scope.config.model.leafNode] = false;
                        treeNode.levelId = treeNode.parent.levelId + 1;
                        treeNode.status = "child";
                        treeNode.show = true;
                        $scope.rows.splice(index + 1 + i, 0, treeNode);
                    }

                    $scope.treeDown(parentNode, parentNode.selected);
                    $scope.treeUp(parentNode);
                    return treeNode;


                }

                var $arguments = arguments;

                $scope.getArr = function (num) {
                    return new Array(num + 1);
                }

                $scope.mousedown = function (dragNode, $event) {

                    var target = $event.target || $event.srcElement || $event.currentTarget;
                    var $this = angular.element(target);
                    var eventMove = null;
                    var eventMouseup = null;
                    var newTarget = null;


                    $element[0].onmousemove = function (event) {
                        //console.log("up: "+eventMouseup);
                        if (eventMouseup != null) {
                            this.onmousemove = null;
                            eventMove = null;
                            return;
                        }
                        eventMove = event.type;//console.log(eventMove);
                        if (window.getSelection) {
                            window.getSelection().removeAllRanges();
                        } else {
                            document.selection.createRange().select();
                        }
                        newTarget = event.target || event.srcElement || event.currentTarget;


                    };

                    $element[0].onmouseup = function (event) {
                        //console.log("move: "+eventMove);
                        $element[0].onmousemove = null;
                        this.onmouseup = null;
                        if (eventMove == null) {
                            eventMouseup = null;
                            return;
                        }
                        eventMove = null;
                        eventMouseup = event.type;
                        var dropNode = null;
                        while (newTarget) {
                            if (newTarget.getAttribute('ng-repeat')) {
                                var data = newTarget.getAttribute('ng-repeat').trim().split(' ');
                                dropNode = angular.element(newTarget).scope()[data[0]];
                                break;
                            }
                            newTarget = newTarget.parentNode;
                        }

                        //$element[0].onmouseup = null;
                        //$element[0].onmouseout = null;
                        if (typeof $scope.config.dragDrop != 'undefined') {
                            if (dragNode == dropNode) return;

                            $scope.config.dragDrop($scope.config.ctrlScope, dragNode, $scope, $event, $this, dropNode);
                        }

                    }


                }

                $scope.clickName = function (node) {

                    if (typeof $scope.config.clickName != 'undefined') {
                        $scope.config.clickName($scope.config.ctrlScope, node, $scope, $arguments, $scope.config.yourarguments);
                    }

                }

                $scope.click = function (node) {

                    if (typeof $scope.config.beforeSelect != 'undefined') {
                        $scope.config.beforeSelect($scope.config.ctrlScope, node, $scope, $arguments, $scope.config.yourarguments);
                    }

                    $scope.treeDown(node, (!node.selected));
                    $scope.treeUp(node);

                    if ($scope.config.debug == true) {
                        console.log(node);
                    }
                    ////////////////////////////////////////////////////
                    if (typeof $scope.config.afterSelect != 'undefined') {
                        $scope.config.afterSelect($scope.config.ctrlScope, node, $scope, $arguments, $scope.config.yourarguments);
                    }

                }

                $scope.collapse = function (node) {

                    if (typeof $scope.config.beforeCollapse != 'undefined') {
                        $scope.config.beforeCollapse($scope.config.ctrlScope, node, $scope, $arguments, $scope.config.yourarguments);
                    }
                    $scope.collapseBody(node);
                    if (typeof $scope.config.afterCollapse != 'undefined') {
                        $scope.config.afterCollapse($scope.config.ctrlScope, node, $scope, $arguments, $scope.config.yourarguments);
                    }
                }

                $scope.collapseBody = function (node) {
                    var length = node.list.length;
                    node.collapsed = (!node.collapsed);
                    for (var i = 0; i < length; i++) {
                        node.list[i].show = true;
                        $scope.treeDown(node.list[i], null);
                    }
                    if ($scope.config.debug == true) {
                        console.log(node);
                    }
                }

                $scope.treeUp = function (node) {

                    node = node.parent;
                    while (node) {

                        var count = 0;
                        var isNonSelected = true;
                        var thisLength = node.list.length;
                        for (var i = 0; i < thisLength; i++) {
                            if (node.list[i].selected == true) count++;
                            if (node.list[i].nonSelected == false) {
                                isNonSelected = false;
                            }

                        }
                        node.selectedCount = count;

                        if (node.selectedCount == node.list.length) {
                            node.selected = true;
                            node.nonSelected = false;
                            node.partSelected = false;
                        } else if (isNonSelected) {
                            node.selected = false;
                            node.nonSelected = true;
                            node.partSelected = false;
                        } else {
                            node.selected = false;
                            node.nonSelected = false;
                            node.partSelected = true;
                        }

                        node = node.parent;
                    }

                }

                $scope.treeDown = function (node, selectedStatus) {

                    if (node == null) return;
                    if (selectedStatus != null) {
                        if (selectedStatus && node.parent && node.selected != selectedStatus) {
                            node.parent.selectedCount++;
                        } else if (selectedStatus == false && node.parent && node.selected != selectedStatus) {
                            node.parent.selectedCount--;
                        }

                        node.selected = selectedStatus;
                        node.nonSelected = (!selectedStatus);
                        node.partSelected = false;
                    }
                    node.show = true;
                    if (node.parent != null && (node.parent['collapsed'] == true || node.parent['show'] == false)) {
                        node['show'] = false;
                    }

                    var length = node.list.length;

                    for (var i = 0; i < length; i++) {
                        $scope.treeDown(node.list[i], selectedStatus);
                    }

                }


                var memberId = $scope.config.model.id;
                var parentId = $scope.config.model.parentId;
                //var levelId = $scope.config.model.levelId;
                var name = $scope.config.model.name;


                $scope.createTreeNode = function (node) {
                    if (node.status != undefined) {
                        return node;
                    }
                    var treeNode = {
                        // "id": node[memberId],
                        // "parentId": node[parentId],
                        // "name": node[name],
                        "parent": null,
                        "leafNode": true,
                        "list": [],//childs
                        "status": "root",
                        "levelId": -1,
                        "selected": true,
                        "collapsed": true,
                        "nonSelected": false,
                        "selectedCount": 0,//number of childs
                        "partSelected": false,
                        "show": false,
                        "lastNode": null,
                        //"indexes": [null]
                    };

                    treeNode[memberId] = node[memberId];
                    treeNode[parentId] = node[parentId];
                    treeNode[name] = node[name];

                    for (var key in node) {
                        treeNode[key] = node[key];
                    }

                    return treeNode;


                }

                $scope.walkTree = function (node, levelId, showMinLevelId) {

                    showMinLevelId = showMinLevelId;
                    showMinLevelId = isNaN(showMinLevelId) ? 0 : parseInt(showMinLevelId);
                    if (showMinLevelId < 0) {
                        showMinLevelId = 0;
                    }
                    node.levelId = levelId;
                    var numChilds = node.list.length;

                    if (levelId < showMinLevelId) {
                        node.show = true;
                        node["collapsed"] = false;
                    }

                    if (node.levelId == showMinLevelId) {
                        node.show = true;
                        node["collapsed"] = true;
                    }

                    for (var i = 0; i < numChilds; i++) {
                        $scope.walkTree(node.list[i], levelId + 1, showMinLevelId);
                    }

                }


                $scope.convertListToTree = function (list) {

                    var length = list.length;
                    if (length == 0) return null;
                    var hashMap = {};
                    var treeList = [];

                    for (var i = 0; i < length; i++) {
                        var treeNode = $scope.createTreeNode(list[i]);
                        hashMap[treeNode[memberId]] = treeNode;
                    }

                    for (var i in hashMap) {

                        var nodeParentId = hashMap[i][parentId];

                        if (nodeParentId != -1) {
                            hashMap[i].parent = hashMap[nodeParentId];
                            if (hashMap[i].parent === undefined) {
                                hashMap[i].parent = null;
                            }
                            if (hashMap[i].parent != null) {
                                hashMap[i].parent.list.push(hashMap[i]);
                                hashMap[i].parent.selectedCount = hashMap[i].parent.list.length;
                                hashMap[i].parent.leafNode = false;
                                hashMap[i].status = "child";
                            }
                        }
                    }

                    for (var i in hashMap) {
                        if (hashMap[i].parent == null) {
                            $scope.walkTree(hashMap[i], 0, $scope.config.defaultShowNodeByLevel());
                            treeList.push(hashMap[i]);
                        }
                    }

                    return treeList;
                };

                $scope.copyArray = function (array) {
                    if (array == null) {
                        return null;
                    }
                    var copy = [];
                    for (var index in array) {
                        copy[index.toString()] = array[index];
                    }
                    return copy;
                }

                $scope.convertTreeToList = function (treeNode, array, isLastNode) {

                    if (treeNode.constructor.toString().indexOf('Array') != -1) {
                        for (var i = 0; i < treeNode.length; i++) {
                            treeNode[i]['indexes'] = null;
                            treeNode['lastNode'] = false;
                            $scope.convertTreeToList(treeNode[i], array, false);
                        }
                    } else if (treeNode != null) {
                        treeNode['indexes'] = [];
                        treeNode['lastNode'] = isLastNode;
                        if (treeNode.parent != null) {
                            var parentIndexes = treeNode['parent']['indexes'];

                            for (var attr in parentIndexes) {
                                treeNode['indexes'][attr.toString()] = parentIndexes[attr];
                            }

                            var levelId = treeNode['parent'][$scope.config.model.levelId];
                            treeNode['indexes'][levelId.toString()] = levelId;
                            if (isLastNode) {
                                treeNode['indexes'][levelId.toString()] = null;
                            }

                        }

                        array.push(treeNode);
                        var length = treeNode.list.length;
                        for (var i = 0; i < length; i++) {
                            var isLastNodeCall = false;
                            if (i == length - 1) isLastNodeCall = true;
                            $scope.convertTreeToList(treeNode.list[i], array, isLastNodeCall);
                        }


                    }

                }


                $scope.initial = function (allNodeData, doProcess) {

                    if (doProcess !== undefined && doProcess == false) {
                        $scope.rows = allNodeData;
                        return $scope.rows;
                    }
                    if ($scope.treeData === undefined) {
                        $scope.treeData = $scope.convertListToTree(allNodeData);//($scope.config.data
                    }
                    /*                    if ($element[0].previousElementSibling == null) {
                     var newDiv = document.createElement('div');
                     newDiv.setAttribute('style', 'width: 100%;height: 100%;z-index: 1;position: absolute;top: 0px;left: 0px;background-color: rgba(0,0,0,0.6);');
                     newDiv.setAttribute('ng-show', 'config.loading');
                     $compile(newDiv)($scope);
                     $element[0].parentNode.insertBefore(newDiv, $element[0]);

                     $element[0].parentNode.style.position = "relative";


                     }*/

                    var rows = [];//$scope.config.data;//($scope.config.data
                    $scope.rows = [];
                    //$scope.config.type = $scope.config.type.toLowerCase();
                    $scope.convertTreeToList($scope.treeData, rows, false);
                    $scope.rows = rows;
                    return $scope.rows;
                    //console.log($scope.rows);

                }

                $scope.widthCal = function () {
                    return $scope.config.width;
                    var maxDeep = 0;
                    var list = $scope.rows;
                    if (list.length > 0) {

                        for (var i = 0; i < list.length; i++) {
                            if (maxDeep < list[i][$scope.config.model.levelId]) {
                                maxDeep = list[i][$scope.config.model.levelId];
                            }
                        }

                        $scope.config.width = maxDeep * $scope.config.size() + 50;
                    }

                    return $scope.config.width;
                }

                if (typeof $scope.config.init != 'undefined') {
                    $scope.config.init($scope.config.ctrlScope, $scope);
                }


                $scope.setCssClass = function () {
                    $scope.left = {
                        'float': 'left'
                    };
                    $scope.right = {
                        'float': 'right'
                    };


                    /*                    $scope.config.size1 = function () {
                                            var size = parseInt($scope.config.size());
                                            if (size < 18) {
                                                size = 18;
                                                return size;
                                            }
                                            var mode6 = size % 6;
                                            if (mode6 != 0) {
                                                size = size + (6 - mode6);
                                                return size;
                                            }
                                            return size;
                                        }*/


                    $scope.borderPartSelected = $scope.config.size() / 6;
                    var mode10 = $scope.config.size() % 10;
                    $scope.heightArrow = $scope.config.size();
                    if (mode10 != 0) {
                        $scope.heightArrow = $scope.config.size() + 10 - mode10;

                    }
                    $scope.heightArrow = (($scope.heightArrow) / 5) + 1;
                }

                $scope.setCssClass();


            }
        }

    });
