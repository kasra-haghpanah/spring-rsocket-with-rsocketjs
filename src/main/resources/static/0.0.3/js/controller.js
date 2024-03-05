/**
 * Created by kasra.haghpanah on 03/04/2016.
 * https://code.angularjs.org/1.8.0
 */
//"use strict";
(function () {

    if (typeof window.demoApp === 'undefined') {
        window.demoApp = angular.module("demoApp", ['ngResource', 'ngRoute', 'pascalprecht.translate', 'ngLocale']);
        //window.demoApp = angular.module("demoApp", ['ngResource', 'ngRoute', 'pascalprecht.translate', 'ngLocale']);

    }

})();

String.prototype.toUnicode = function () {
    var result = "";
    for (var i = 0; i < this.length; i++) {
        result += "\\u" + ("000" + this[i].charCodeAt(0).toString(16)).substr(-4);
    }
    return result;
};

String.prototype.escapeHtml = function () {

    var text = document.createTextNode(this);
    var div = document.createElement('div');
    div.appendChild(text);
    var result = div.innerHTML.toString();
    delete div;
    return result;
}


demoApp.run(['$rootScope', '$translate', '$http', '$location', async function ($rootScope, $translate, $http, $location) {

    $rootScope.viewVersion = document.getElementsByTagName("view-version").item(0).textContent;
    $rootScope.isUploading = false;
    $rootScope.level = -1;
    $rootScope.title = "";
    $rootScope.text = "";
    $rootScope.file = null;
    $rootScope.modal = false;
    $rootScope.uploadPercent = 0;
    $rootScope.window = window;
    $rootScope.isShowDropDown = false;
    $rootScope.error = null;
    window.cancels = [];

    $rootScope.languages = [
        {key: "en", name: "english"},
        {key: "fa", name: "فارسی"}
    ]

    $rootScope.languageKey = $rootScope.languages[0].key;

    $rootScope.treeType = "";
    $rootScope.treeTypes = ["", "filesystem"];

    $rootScope.showLine = "true";
    $rootScope.showLineOptions = ["true", "false"];

    $rootScope.color = "#2FC140";

    $rootScope.options = ["18", "24", "30", "36", "42", "48", "54"];
    $rootScope.treeSize = $rootScope.options[2];
    $rootScope.isSettings = false;

    $rootScope.setMetadata = function (metadata) {
        if (metadata !== undefined && metadata != null) {
            for (var key in metadata) {
                $rootScope[key] = metadata[key];
                if (key == 'languageKey') {
                    window.changeLanguage($rootScope[key]);
                }
            }
        }
    }

    $rootScope.openModel = function (item) {
        $rootScope.isSettings = false;
        if (arguments.length > 0 && item == 'Settings') {
            $rootScope.isSettings = true;
            $rootScope.modal = !$rootScope.modal;
            return;
        }

        if (arguments.length > 0 && item == 'close') {
            $rootScope.isSettings = true;
            $rootScope.modal = false;
            $rootScope.file = null;
            return;
        }

        $rootScope.maxLength = 20;
        $rootScope.hasFile = true;
        if ($rootScope.uploadPercent == 100) {
            $rootScope.uploadPercent = 0;
        }


        if (arguments.length > 0) {
            $rootScope.id = item.id;
            $rootScope.title = item.title;
            $rootScope.text = item.text;
            $rootScope.hasFile = false;
            $rootScope.maxLength = 10000;
        } else {
            $rootScope.title = "";
            $rootScope.text = "";
        }

        if ($rootScope.uploadPercent != 0 && $rootScope.uploadPercent != 100) {
            return
        }

        $rootScope.modal = !$rootScope.modal;
        if (!$rootScope.modal) {
            $rootScope.file = null;
        }
    }

    $rootScope.showDropDown = function () {
        $rootScope.isShowDropDown = !$rootScope.isShowDropDown;
    }
    $rootScope.location = $location;

    $rootScope.isEmpty = function (value, length) {
        if (value === undefined) {
            return true;
        }
        if (value == null) {
            return true;
        }
        if (value == '') {
            return true;
        }
        if (arguments.length > 1) {
            return value.toString().length != length;
        }

        return false;
    }

    $rootScope.openConnection = function () {

        return new Promise(function (resolve, reject) {
            //if ($rootScope.token === undefined || $rootScope.token == null || $rootScope.token == "") {
            $http.put("/cookie")
                .then(async function (response) {
                        $rootScope.token = response.data.token;
                        $rootScope.username = response.data.username;
                        $rootScope.level = response.data.level;
                        $rootScope.domain = response.data.domain;
                        $rootScope.setMetadata(response.data.metadata);
                        //if (window.cancels.length == 0) {
                        window.rsocket = await $rootScope.openRsocket($rootScope.token);
                        if ($location.$$path == "/ng/signin") {
                            $location.path("/ng/tree");
                        }
                        resolve(window.rsocket);
                        // }

                    },
                    function (error) {
                        $location.path();
                        reject(error);
                    }
                );
            //}

        });


    }

    $rootScope.dir = "ltr";
    window.changeLanguage = function (key) {
        if (key.value !== undefined) {
            key = key.value;
        }
        $translate.use(key);
        $rootScope.dir = "ltr";
        $rootScope.languageKey = key;
        if (key == "fa") {
            $rootScope.dir = "rtl";
        }
    };

    window.changeLanguage($rootScope.languageKey);
    $rootScope.messages = [];

    $rootScope.openRsocket = function (token) {

        return new Promise(function (resolve, reject) {

            if (window.cancels.length == 0) {

                const {
                    RSocketClient,
                    BufferEncoders
                } = require('rsocket-core');
                const RSocketWebSocketClient = require('rsocket-websocket-client').default;
                const {Flowable} = require('rsocket-flowable');
                window.Flowable = Flowable;

                var webSocketClient = new RSocketWebSocketClient({
                        url: `ws://${$location.host()}:${$location.port()}/rsocket`,
                        onNext: function (payload) {
                            if (payload.data != null) {
                                var metadata = rsocketExt.binaryToString(payload.metadata);
                                var color = "black";
                                var isChange = false;
                                if (payload.metadata != null) {
                                    if (metadata.indexOf("delete") > -1) {
                                        var data = rsocketExt.binaryToString(payload.data);
                                        if (data != "0") {
                                            for (var i = 0; i < $rootScope.messages.length; i++) {
                                                if ($rootScope.messages[i].id == data) {
                                                    $rootScope.messages.splice(i, 1);
                                                    isChange = true;
                                                }
                                            }
                                        }
                                    } else if (metadata.indexOf("pushMessage") > -1) {
                                        var data = rsocketExt.binaryToJson(payload.data);
                                        data.percent = 0;
                                        $rootScope.messages.push(data);
                                        isChange = true;
                                    } else if (metadata.indexOf("updateMessage") > -1) {
                                        var data = rsocketExt.binaryToJson(payload.data);
                                        for (var i = 0; i < $rootScope.messages.length; i++) {
                                            if ($rootScope.messages[i].id == data.id) {
                                                $rootScope.messages[i].title = data.title;
                                                $rootScope.messages[i].text = data.text;
                                                isChange = true;
                                            }
                                        }
                                    }

                                    if (isChange) {
                                        $rootScope.$apply();
                                    }
                                }
                            }

                        }
                    },
                    BufferEncoders
                );

                var client = new RSocketClient({
                    setup: {
                        // ms btw sending keepalive to server
                        keepAlive: 60000,/* 60s in ms */
                        // ms timeout if no keepalive response
                        lifetime: 360000,/*360s in ms */
                        // format of data
                        // dataMimeType: 'application/json',
                        dataMimeType: 'application/octet-stream',
                        // format of metadata
                        //metadataMimeType: 'message/x.rsocket.routing.v0'
                        metadataMimeType: 'message/x.rsocket.composite-metadata.v0',
                        payload: {
                            data: rsocketExt.stringToBinary("token"),
                            metadata: rsocketExt.metadataBearer(token)
                        }

                    },
                    transport: webSocketClient
                });


                try {

                    client.connect().subscribe({
                        onComplete: function (rsocket2) {
                            if (window.cancels.length > 1) {
                                clearInterval(window.reconnectFunc);
                                var cancel = window.cancels.shift();
                                while (window.cancels.length > 0) {
                                    window.cancels.shift()();
                                }
                                window.cancels.push(cancel);
                                return;
                            }
                            window.rsocket = rsocket2;
                            if (window.reconnectFunc != undefined) {
                                $rootScope.allMessages();
                            }
                            resolve(rsocket2);

                            window.rsocket.connectionStatus()
                                .subscribe(function (status) {
                                    // console.log(status);

                                    if (
                                        // for firefox
                                        status.kind == "CLOSED"
                                        ||
                                        // for chrome & edge
                                        (status.kind == "ERROR" && status.error !== undefined && status.error != null && status.error.message.indexOf("Socket closed unexpectedly"))
                                    ) {

                                        // if (window.reconnectFunc === undefined) {

                                        window.reconnectFunc = setInterval(async function () {
                                            try {
                                                while (window.cancels.length > 0) {
                                                    window.cancels.shift()();
                                                }
                                                window.rsocket = null;
                                                $rootScope.messages = [];

                                            } catch (e) {
                                                //console.log(e);
                                            }
                                            try {
                                                await $rootScope.openConnection($rootScope.token);
                                                clearInterval(window.reconnectFunc);
                                            } catch (e) {
                                                //console.log(e);
                                            }
                                            if (window.cancels.length > 0) {
                                                clearInterval(window.reconnectFunc);
                                            }
                                        }, 1000);

                                        // }

                                    }
                                    // alert(status.kind);
                                    // $rootScope.close();
                                    // $rootScope.openRsocket($rootScope.token);
                                });
                            // $rootScope.allMessages();
                        },
                        onError: function (error) {
                            //console.error(error);
                            reject(error);


                            //$rootScope.error(error);
                        },
                        onSubscribe: function (cancel) {
                            if (window.cancels.length > 0) {
                                clearInterval(window.reconnectFunc);
                                console.log("336", window.rsocket);
                                cancel();
                            } else {
                                window.cancels.push(cancel);
                            }
                            /* call cancel() to abort */
                            // cancel();
                        }

                    });

                } catch (e) {
                    reject(e);
                }


            }

        });
    }

    $rootScope.allMessages = async function () {
        if (window.rsocket == undefined || window.rsocket == null) {
            await $rootScope.openConnection();
        }

        if ($rootScope.messages === undefined || $rootScope.messages == null) {
            $rootScope.messages = [];
        }


        return new Promise(function (resolve, reject) {

            rsocket.requestStream({
                data: null,
                metadata: rsocketExt.metadata('all.messages')
            }).subscribe({
                onComplete: function () {
                    resolve($rootScope.messages);
                    // console.log('complete');
                    //console.log($rootScope.messages.length)
                },
                onError: function (error) {
                    $rootScope.error += ("\n" + error);
                    reject(error);
                    $rootScope.$apply();
                    // $scope.error(error);
                },
                onNext: function (payload) {
                    var data = rsocketExt.binaryToJson(payload.data);
                    if (data.file != undefined) {
                        var contentType = data.file.contentType.toLowerCase();
                        if (contentType.indexOf("video") > -1 || contentType.indexOf("audio") > -1) {
                            data.file.mediaSource = null; //new MediaSource();
                        }
                    }

                    data.color = "black";
                    data.percent = 0;
                    $rootScope.messages.push(data);
                    $rootScope.$apply();
                    // $scope.addMessage("rsocket-message", "green", payload.data);
                },
                onSubscribe: function (subscription) {
                    subscription.request(2147483647);
                }
            });
        });
    }

    $rootScope.showUploadFile = function (file) {

        var data = HTML5.fileReader({
            file: file,
            element: document.getElementById("media"),
            class: 'col-12',
            maxSizeByMB: 3000,
            height: 400,
            isArrayBuffer: false,
            videoDuration: function (video) {
                $rootScope.file.duration = video.duration.toString().replace(/\./g, '-dot-');
                $rootScope.file.size = video.size;
            }
        });

        data.percent = 0;
        return data;

    }

    $rootScope.changeFile = function (file) {

        if (typeof FileReader == "function") {

            if (typeof FileReader == "function") {
                var mime = HTML5.mimeContentType(file);
                var str = mime.mbSize + " " + mime.contentType + " " + mime.name;

                document.getElementById("metadata").innerHTML = str;
                document.getElementById("media").innerHTML = null;

                $rootScope.file = $rootScope.showUploadFile(file);
                $rootScope.$apply();

                // console.log(data);

            }
        }
    }

    $rootScope.upload = async function () {
        $rootScope.uploadPercent = 0;
        var fileDom = document.getElementById("formFileLg");
        $rootScope.file = null;
        //$rootScope.$apply();
        var file = await HTML5.readFileAsync(fileDom, "arraybuffer"); //await ffmpegUtil.getFile(fileDom, "arraybuffer");
        $rootScope.file = file;
        //$rootScope.$apply();

        var message = null;

        var metadata = `upload.${rsocketExt.replaceDotSlash($rootScope.title)}.${rsocketExt.replaceDotSlash($rootScope.text)}.${rsocketExt.replaceDotSlash(file.name)}.${rsocketExt.replaceDotSlash(file.contentType)}.${file.size}.${rsocketExt.replaceDotSlash(file.duration)}`;


        var contentType = file.contentType.toLowerCase();

        var data = {
            connection: window.rsocket,
            metadata: metadata,
            file: file,
            /*{
            name: "1.mp4",
                size: 112,
                contentType: "video/mp4",
                duration: 354,
                content: [11,121,...]
            }*/
            chunkSize: 32 * 1024,
            onComplete: function () {
                $rootScope.isUploading = false;
                $rootScope.uploadPercent = 100;
                $rootScope.openModel();
                $rootScope.$apply();
            },
            onError: function (error) {
                $rootScope.isUploading = false;
                $rootScope.error += ("\n" + error);
                $rootScope.$apply();
            },
            onNext: function (payload) {
                $rootScope.isUploading = false;
                var data = rsocketExt.binaryToJson(payload.data);
                data.color = "black";
                data.percent = 0;
                //console.log(data);
                $rootScope.messages.push(data);
                $rootScope.$apply();
            },
            request: function (chunkNumber, chunkLength) {
                $rootScope.isUploading = true;
                $rootScope.uploadPercent = parseInt(chunkNumber * 100 / chunkLength);
                $rootScope.$apply();
            },
            cancel: function () {
                return false; //($scope.status != "Cancel");
            }
        };


        await rsocketExt.clone().rsocketUpload(data);


    }

    $rootScope.updateMessage = function () {

        if (window.rsocket != null) {

            var data = {
                "id": $rootScope.id,
                "title": $rootScope.title,
                "text": $rootScope.text
            };

            rsocket.requestResponse({
                data: rsocketExt.jsonToBinary(data),
                metadata: rsocketExt.metadata('update.message')
            }).subscribe({
                onComplete: function (payload) {
                    var data = rsocketExt.binaryToJson(payload.data);
                    for (var i = 0; i < $rootScope.messages.length; i++) {
                        if ($rootScope.messages[i].id == data.id) {
                            $rootScope.messages[i].title = data.title;
                            $rootScope.messages[i].text = data.text;
                        }
                    }
                    $rootScope.openModel();
                    $rootScope.$apply();
                },
                onError: function (error) {
                    $rootScope.error = error;
                    $rootScope.$apply();
                },
                onSubscribe: function (subscription) {
                    // ...
                }
            });
        }

    }

    $rootScope.getUsers = async function () {

        if (window.rsocket == undefined || window.rsocket == null) {
            await $rootScope.openConnection();
        }

        if ($rootScope.users != undefined && $rootScope.users != null) {
            return;
        }

        $rootScope.users = [];

        return new Promise(function (resolve, reject) {
            // if (window.rsocket != null) {
            rsocket.requestStream({
                data: null,
                metadata: rsocketExt.metadata('getUsers')
            }).subscribe({
                onComplete: function () {
                    //$rootScope.users = $scope.initial($rootScope.users);
                    resolve($rootScope.users);
                    $rootScope.$apply();
                },
                onError: function (error) {
                    $rootScope.error += ("\n" + error);
                    $rootScope.$apply();
                    reject(error);
                    // $rootScope.error(error);
                },
                onNext: function (payload) {
                    var data = rsocketExt.binaryToJson(payload.data);
                    $rootScope.users.push(data);
                },
                onSubscribe: function (subscription) {
                    subscription.request(2147483647);
                }
            });
            //  }

        });

    }


    $rootScope.editUserMetadata = function () {
        if (window.rsocket != null) {

            var data = {
                "metadata": {
                    "languageKey": $rootScope.languageKey,
                    "treeType": $rootScope.treeType,
                    "color": $rootScope.color,
                    "showLine": $rootScope.showLine,
                    "treeSize": $rootScope.treeSize
                }
            };

            rsocket.requestResponse({
                data: rsocketExt.jsonToBinary(data),
                metadata: rsocketExt.metadata('edit.user.metadata')
            }).subscribe({
                onComplete: function (payload) {
                    var data = rsocketExt.binaryToJson(payload.data);
                    $rootScope.languageKey = data.metadata.languageKey;
                    $rootScope.treeType = data.metadata.treeType;
                    $rootScope.color = data.metadata.color;
                    $rootScope.showLine = data.metadata.showLine;
                    $rootScope.treeSize = data.metadata.treeSize;

                    $rootScope.modal = false;
                    $rootScope.$apply();
                },
                onError: function (error) {
                    $rootScope.error = error;
                    $rootScope.$apply();
                },
                onSubscribe: function (subscription) {
                    // ...
                }
            });
        }

    }

    /*    if ($rootScope.token === undefined || $rootScope.token == null) {
            if (window.rsocket === undefined || window.rsocket == null) {
                await $rootScope.openConnection();
                await $rootScope.getUsers();
            }
        }*/


}])

    .controller("SigninController", ['$rootScope', '$scope', '$http', '$location', '$timeout', '$q', function ($rootScope, $scope, $http, $location, $timeout, $q) {

        //$rootScope.error = null;

        $scope.signin = function () {

            $http({
                method: 'PUT',
                url: '/signin',
                data: $scope.user,
                transformRequest: function (data, headersGetter) {
                    var param = [];
                    for (var field in data) {
                        if (typeof data[field] == 'string') {
                            data[field] = data[field].trim();
                        }
                        param.push(encodeURIComponent(field) + "=" + encodeURIComponent(data[field]));
                    }
                    return param.join("&");
                },
                headers: {'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'}
            }).then(
                async function (response) {
                    // $scope.result = response.data;
                    if (response.data.token != "ErrorSignInMessage") {
                        $rootScope.error = null;
                        $rootScope.token = response.data.token;
                        $rootScope.username = response.data.username;
                        $rootScope.level = response.data.level;
                        $rootScope.domain = response.data.domain;
                        $rootScope.setMetadata(response.data.metadata);
                        if (response.data.me)

                            var rsocket = await $rootScope.openRsocket(response.data.token);
                        window.location.href = "/ng/tree";
                    }

                },
                function (error) {
                    $rootScope.error = error.data.token;
                }
            );

        }


    }])

    .controller("SignupController", ['$rootScope', '$scope', '$http', '$location', '$routeParams', '$timeout', '$q', function ($rootScope, $scope, $http, $location, $routeParams, $timeout, $q) {

        if (window.reconnectFunc !== undefined) {
            clearInterval(window.reconnectFunc);
        }

        $scope.routeParams = $routeParams;
        $scope.phone = $routeParams.phone;
        $scope.code = $routeParams.code;
        $scope.changePassword = function () {

            if ($rootScope.isEmpty($scope.phone) || $rootScope.isEmpty($scope.code)) {
                $location.path();
            }

            $http.put(`/change/${$scope.phone}/${$scope.code}/${$scope.user.password}`)
                .then(async function (response) {
                        $rootScope.token = response.data.token;
                        if ($rootScope.token == "ErrorSignInMessage") {
                            $location.path();
                        }
                        $rootScope.username = response.data.username;
                        $rootScope.level = response.data.level;
                        $location.path("/tree");
                    },
                    function (error) {
                        $location.path();
                    }
                );
        }

        $scope.signup = function () {

            $http({
                method: 'PUT',
                url: '/signup',
                data: $scope.user,
                transformRequest: function (data, headersGetter) {
                    var param = [];
                    for (var field in data) {
                        if (typeof data[field] == 'string') {
                            data[field] = data[field].trim();
                        }
                        param.push(encodeURIComponent(field) + "=" + encodeURIComponent(data[field]));
                    }
                    return param.join("&");
                },
                headers: {'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'}
            }).then(
                function (response) {
                    $scope.result = response.data;
                    $rootScope.token = response.data.token;
                    $rootScope.username = response.data.username;
                    $rootScope.level = response.data.level;
                    $rootScope.setMetadata(response.data.metadata);
                    $location.path("/tree");
                },
                function (error) {
                    $scope.error = error.data.message;
                }
            );

        }


    }])

    .controller("TreeController", ['$rootScope', '$scope', '$http', '$location', '$timeout', '$q', function ($rootScope, $scope, $http, $location, $timeout, $q) {

        $scope.add = true;
        $scope.user = {};
        $scope.isSuperGroup = false;

        $scope.setSuperGroup = function () {
            $scope.isSuperGroup = !$scope.isSuperGroup;
        }

        $scope.forgotPassword = function () {

            if (window.rsocket != null) {
                rsocket.requestResponse({
                    data: rsocketExt.stringToBinary($scope.user.username + ""),
                    metadata: rsocketExt.metadata('forgot')
                }).subscribe({
                    onComplete: function (payload) {

                        var data = rsocketExt.binaryToJson(payload.data);
                        $scope.parent.code = data.code;
                        $rootScope.link = `${$rootScope.domain}/active/${$scope.parent.username}/${$scope.parent.code}`;
                        $rootScope.userFullName = $scope.parent.firstName + " " + $scope.parent.lastName;
                        $scope.parent.active = false;
                        $scope.$apply();
                    },
                    onError: function (error) {
                        $scope.error = error;
                        $scope.$apply();
                    },
                    onSubscribe: function (subscription) {
                        // ...
                    }
                });
            }

        }

        $scope.addUser = function () {
            $scope.user = {}
            $scope.add = true;
        }

        $scope.editUser = function () {
            $scope.add = false;
            $scope.user.firstname = $scope.parent.firstName;
            $scope.user.lastname = $scope.parent.lastName;
            $scope.user.username = parseInt($scope.parent.username);
            $scope.user.childsCount = parseInt($scope.parent.childsCount);
            $scope.user.activeChildsCount = parseInt($scope.parent.activeChildsCount);
            //$scope.user.childsCount = $scope.parent.childsCount;
            //$scope.user
        }

        $scope.copyLink = function () {

            var copyText = document.getElementById("link");
            copyText.select();
            document.execCommand("copy");
            $rootScope.isLinkSave = true;
            //$scope.$apply();

        }

        $scope.config = {
            color: function (node) {
                if (arguments.length > 0 && node.active == false) {
                    return "gray";
                }
                return $scope.color;
            },
            dir: function () {
                return $rootScope.dir;
            },
            height: "100%",
            width: "100%",
            size: function () {
                return parseInt($rootScope.treeSize);
            },
            class: "",
            defaultShowNodeByLevel: function () {
                return $rootScope.level;
            },
            debug: false,
            showLine: function () {
                return $rootScope.showLine;
            },
            loading: false,
            ctrlScope: $scope,
            yourarguments: arguments,
            type: function () {
                return $rootScope.treeType;
            },
            model: {
                id: 'id',
                parentId: 'parentId',
                name: 'fullName',
                leafNode: 'leafNode'
            },
            init: function (ctrlScope, $scope) {
                /*                if (window.rsocket == undefined || window.rsocket == null) {
                                    await $rootScope.openConnection();
                                }*/

                if ($rootScope.users !== undefined && $rootScope.users != null) {
                    $scope.initial($rootScope.users, false);
                    return;
                }

                $rootScope.getUsers()
                    .then(function (result) {
                            $rootScope.treeViewScope = $scope;
                            if ($rootScope.users === undefined) {
                                $rootScope.users = [];
                            } else {
                                $rootScope.users = $scope.initial($rootScope.users);
                                $scope.$apply();
                                return;
                            }
                        },
                        function (error) {

                        });


                /*                $http({
                                    method: 'GET',
                                    url: '/users',
                                    headers: {'mobile': ''}
                                }).then(
                                    function (response) {
                                        $scope.initial(response.data);
                                    }, function (erroe) {

                                    });*/
                // $rootScope.users = await $rootScope.getUsers();


                /*                if (window.rsocket != null) {
                                    rsocket.requestStream({
                                        data: null,
                                        metadata: rsocketExt.metadata('getUsers')
                                    }).subscribe({
                                        onComplete: function () {
                                            $rootScope.users = $scope.initial($rootScope.users);
                                            $scope.$apply();
                                        },
                                        onError: function (error) {
                                            $scope.error += ("\n" + error);
                                            $scope.$apply();
                                            $scope.error(error);
                                        },
                                        onNext: function (payload) {
                                            var data = rsocketExt.binaryToJson(payload.data);
                                            $rootScope.users.push(data);
                                        },
                                        onSubscribe: function (subscription) {
                                            subscription.request(2147483647);
                                        }
                                    });
                                }*/

            },
            clickName: function (ctrlScope, item, $scope, $arguments, yourarguments) {

                $rootScope.isLinkSave = false;
                $scope.isSuperGroup = false;
                $rootScope.error = null;
                $rootScope.link = null;
                ctrlScope.parent = item;

                if (item.active == false) {
                    $rootScope.userFullName = item.firstName + " " + item.lastName;
                    $rootScope.link = `${$rootScope.domain}/active/${item.username}/${item.code}`;
                    ctrlScope.add = false;
                }
                if ($rootScope.level != 0) {
                    if (item.username != $rootScope.username) {
                        ctrlScope.add = false;
                    }
                }

                if (ctrlScope.add) {
                    ctrlScope.addUser();
                } else {
                    ctrlScope.editUser();
                }

            },
            afterSelect: function (ctrlScope, item, $scope, $arguments, yourarguments) {

            },
            beforeSelect: function (ctrlScope, item, $scope, $arguments, yourarguments) {
                //console.log(item);
            },
            dragDrop: function (ctrlScope, dragNode, $scope, $event, $this, dropNode) {

                //console.log(dragNode);
                //console.log(dropNode);

            },
            beforeCollapse: function (ctrlScope, item, $scope, $arguments, yourarguments) {
                //  console.log(item.collapsed);
                // if ($rootScope.level != 0) {
                //     return;
                // }
                if (item.collapsed == true) {

                    /*                    $http({
                                            method: 'GET',
                                            url: `/get/childs/parentId/${item.id}`,
                                        }).then(
                                            function (response) {
                                                $scope.removeChildNodesByParentNode(item, false, false);
                                                $scope.addChildNodes(item, response.data);
                                            },
                                            function (error) {
                                                $scope.error = error.data.message;
                                            }
                                        );*/

                    var users = [];
                    if (window.rsocket != null) {
                        rsocket.requestStream({
                            data: null,
                            metadata: rsocketExt.metadata(`getChildsByParentId.${item.id}`)
                        }).subscribe({
                            onComplete: function () {
                                $scope.removeChildNodesByParentNode(item, false, false);
                                $scope.addChildNodes(item, users);
                                $scope.$apply();
                            },
                            onError: function (error) {
                                $scope.error += ("\n" + error);
                                $scope.$apply();
                                //$scope.error(error);
                            },
                            onNext: function (payload) {
                                var data = rsocketExt.binaryToJson(payload.data);
                                users.push(data);
                            },
                            onSubscribe: function (subscription) {
                                subscription.request(2147483647);
                            }
                        });
                    }


                } else {
                    $scope.removeChildNodesByParentNode(item, false, false);
                }


            },
            afterCollapse: function (ctrlScope, item, $scope, $arguments, yourarguments) {

            }
        };


        $scope.delete = function () {

            /*            $http({
                            method: 'DELETE',
                            url: `/delete/users/id/${$scope.parent.id}`,
                        }).then(
                            function (response) {

                                if (response.data.status == true) {
                                    var childsCount = $scope.parent.childsCount;
                                    var parentNode = $scope.treeViewScope.removeChildNodesByParentNode($scope.parent, true, true);
                                    if (parentNode != null && childsCount !== undefined && childsCount != null) {
                                        while (true) {
                                            parentNode.childsCount = parentNode.childsCount - childsCount - 1;
                                            parentNode = parentNode.parent;
                                            if (parentNode === undefined || parentNode == null) {
                                                break;
                                            }
                                        }
                                    }

                                } else {
                                    alert("it was not deleted!");
                                }
                            },
                            function (error) {
                                $scope.error = error.data.message;
                            }
                        );*/


            if (window.rsocket != null) {


                rsocket.requestResponse({
                    data: null,
                    metadata: rsocketExt.metadata(`delete.getUsersBy.${$scope.parent.id}`)
                }).subscribe({
                    onComplete: function (payload) {
                        var data = rsocketExt.binaryToJson(payload.data);
                        if (data.status == true) {
                            var childsCount = $scope.parent.childsCount;
                            var activeChildsCount = $scope.parent.activeChildsCount;
                            var isActive = $scope.parent.active;
                            var parentNode = $rootScope.treeViewScope.removeChildNodesByParentNode($scope.parent, true, true);
                            if (parentNode != null && childsCount !== undefined && childsCount != null) {
                                while (true) {
                                    parentNode.childsCount = parentNode.childsCount - childsCount - 1;
                                    if (isActive) {
                                        parentNode.activeChildsCount = parentNode.activeChildsCount - activeChildsCount - 1;
                                    }
                                    parentNode = parentNode.parent;
                                    if (parentNode === undefined || parentNode == null) {
                                        break;
                                    }
                                }
                            }

                        } else {
                            alert("it was not deleted!");
                        }
                        $scope.$apply();
                    },
                    onError: function (error) {
                        $scope.error = error;
                        $scope.$apply();
                    },
                    onSubscribe: function (subscription) {
                        // ...
                    }
                });
            }


        }

        $scope.role = function (increase) {

            if (window.rsocket != null) {

                rsocket.requestResponse({
                    data: rsocketExt.stringToBinary(increase + ''),
                    metadata: rsocketExt.metadata(`role.${$scope.user.username}`)
                }).subscribe({
                    onComplete: function (payload) {

                        var data = rsocketExt.binaryToString(payload.data);
                        $rootScope.error = data == "true" ? "Success" : "Faild";

                        $scope.$apply();
                    },
                    onError: function (error) {
                        $scope.error = error;
                        $scope.$apply();
                    },
                    onSubscribe: function (subscription) {
                        // ...
                    }
                });
            }

        }

        $scope.send = function () {
            $rootScope.error = null;
            $rootScope.link = null;
            if ($scope.add) {
                /*                $http({
                                    method: 'POST',
                                    url: '/add/user',
                                    data: {
                                        "parentId": $scope.parent.id,
                                        "firstName": $scope.user.firstname,
                                        "lastName": $scope.user.lastname,
                                        "username": $scope.user.username + "",
                                        "password": $scope.user.password
                                    },
                                    //headers: {'mobile': ''}
                                }).then(
                                    function (response) {
                                        response.data.childsCount = 0;
                                        var newNode = $scope.treeViewScope.addChildNodes($scope.parent, [response.data]);
                                        newNode = newNode.parent;
                                        while (true) {
                                            newNode.childsCount++;
                                            newNode = newNode.parent;
                                            if (newNode === undefined || newNode == null) {
                                                break;
                                            }
                                        }
                                        if ($scope.parent != null && $scope.parent.collapsed !== undefined && $scope.parent.collapsed) {
                                            $scope.treeViewScope.collapse($scope.parent);
                                        }
                                    },
                                    function (error) {
                                        $scope.error = error.data.message;
                                    }
                                );*/

                if (window.rsocket != null) {

                    var data = {
                        "parentId": $scope.parent.id,
                        "firstName": $scope.user.firstname,
                        "lastName": $scope.user.lastname,
                        "username": $scope.user.username + "",
                        "password": $scope.user.password
                    };

                    if ($scope.isSuperGroup) {
                        data['parentId'] = null;
                    }

                    rsocket.requestResponse({
                        data: rsocketExt.jsonToBinary(data),
                        metadata: rsocketExt.metadata('add.user')
                    }).subscribe({
                        onComplete: function (payload) {
                            $rootScope.error = null;
                            $rootScope.link = null;
                            var data = rsocketExt.binaryToJson(payload.data);
                            if (data.body != undefined && data.body == "duplicateKey") {
                                $rootScope.error = "isExistsUser";
                                $rootScope.$apply();
                                return;
                            }
                            //data = rsocketExt.binaryToJson(payload.data);
                            data.childsCount = 0;
                            var newNode = null;
                            if (data['parentId'] != null) {
                                newNode = $rootScope.treeViewScope.addChildNodes($scope.parent, [data]);
                                newNode = newNode.parent;
                                while (true) {
                                    newNode.childsCount++;
                                    newNode = newNode.parent;
                                    if (newNode === undefined || newNode == null) {
                                        break;
                                    }
                                }
                                if ($scope.parent != null && $scope.parent.collapsed !== undefined && $scope.parent.collapsed) {
                                    $rootScope.treeViewScope.collapse($scope.parent);
                                }

                            } else {
                                newNode = $rootScope.treeViewScope.addChildNodeAsRoot(data);
                                $scope.isSuperGroup = false;
                            }

                            $scope.$apply();
                        },
                        onError: function (error) {
                            $scope.error = error;
                            $scope.$apply();
                        },
                        onSubscribe: function (subscription) {
                            // ...
                        }
                    });
                }


            } else {

                /*                $http({
                                    method: 'POST',
                                    url: '/edit/user',
                                    data: {
                                        "id": $scope.parent.id,
                                        "parentId": $scope.parent.parentId,
                                        "firstName": $scope.user.firstname,
                                        "lastName": $scope.user.lastname,
                                        "username": $scope.user.username + "",
                                        "password": $scope.user.password
                                    },
                                    //headers: {'mobile': ''}
                                }).then(
                                    function (response) {
                                        $scope.treeViewScope.editNode($scope.parent, response.data);
                                    },
                                    function (error) {
                                        $scope.error = error.data.message;
                                    }
                                );*/

                if (window.rsocket != null) {

                    var data = {
                        "id": $scope.parent.id,
                        "parentId": $scope.parent.parentId,
                        "firstName": $scope.user.firstname,
                        "lastName": $scope.user.lastname,
                        "username": $scope.user.username + "",
                        "password": $scope.user.password
                    };

                    rsocket.requestResponse({
                        data: rsocketExt.jsonToBinary(data),
                        metadata: rsocketExt.metadata('edit.user')
                    }).subscribe({
                        onComplete: function (payload) {
                            var data = rsocketExt.binaryToJson(payload.data);
                            $rootScope.error = null;
                            $rootScope.link = null;
                            data.childsCount = $scope.parent.childsCount;
                            data.leafNode = $scope.parent.leafNode;
                            $rootScope.treeViewScope.editNode($scope.parent, data);
                            $scope.$apply();
                        },
                        onError: function (error) {
                            $scope.error = error;
                            $scope.$apply();
                        },
                        onSubscribe: function (subscription) {
                            // ...
                        }
                    });
                }


            }
        }

    }])

    .controller("ContentController", ['$rootScope', '$scope', '$http', '$location', '$timeout', '$q', function ($rootScope, $scope, $http, $location, $timeout, $q) {

        $scope.showFile = async function (item) {

            if (item.content == undefined || item.content == null || item.content == "") {
                return;
            }

            if (item.isDownloaded != undefined || item.isDownloaded == true) {
                return;
            }

            var content = item.content
            var contentType = item.contentType;
            var videoOrAudioElement = null;
            var rsocketMessage = document.getElementById(item.id);

            var type = contentType.toLowerCase().indexOf("video") > -1 ? "video" : contentType;
            if (type != "video") {
                type = contentType.toLowerCase().indexOf("audio") > -1 ? "audio" : contentType;
            }

            /*            if (type == "video" || type == "audio") {
                            var videoTags = rsocketMessage.getElementsByTagName(type);
                            if (videoTags.length > 0) {
                                videoOrAudioElement = videoTags[0];
                            }
                        }*/


            if (item.fileId != null) {
                if (item.content == undefined || item.content == null) {
                    item.content = "";
                }
                var element = await HTML5.createFileElement({
                    filename: item.filename,
                    contentType: contentType,
                    content: content,
                    class: 'col-7',
                    height: "400px",
                    videoOrAudioElement: null,//videoOrAudioElement,
                    isDecodeHTMLCode: true
                });


                if (item.contentType.toLowerCase().indexOf("text") > -1) {
                    element.style.border = "1px solid gray";
                    element.style.overflow = "scroll";
                }
                if (element != null) {
                    if (videoOrAudioElement == null) {
                        //  rsocketMessage.appendChild(element);
                    }
                    item.isDownloaded = true;
                }

                item.content = element;

            }
            // rsocketMessage.scrollTop = rsocketMessage.scrollHeight - rsocketMessage.clientHeight;
        }

        $scope.showPdf = function (item) {
            var type = item.contentType.toLowerCase();
            //var element = e.target || e.srcElement || e.currentTarget || e.toElement || e.path[0];
            if (item.isDownloaded === true && type.indexOf('pdf') > -1) {
                if (document.getElementById(item.id) == null) {
                    setTimeout(function () {
                        if (type.indexOf('pdf') > -1 && document.getElementById(item.id) != null) {
                            document.getElementById(item.id).setAttribute("src", item.content);
                        }
                    }, 3000);
                }

            }
        }

        $scope.pauseVideo = function (item) {
            var videos = document.getElementsByTagName("VIDEO");
            if (videos != null && videos.length !== undefined) {
                for (var i = 0; i < videos.length; i++) {
                    videos[i].pause();
                }
            }

            var audios = document.getElementsByTagName("AUDIO");
            if (audios != null && audios.length !== undefined) {
                for (var i = 0; i < audios.length; i++) {
                    audios[i].pause();
                }
            }

            var mediaVision = document.getElementById(item.id);
            if (mediaVision != null && item.play != undefined) {
                mediaVision.play();
            }
        }

        $scope.download = function (item) {

            var type = item.contentType.toLowerCase();


            if (item.isDownloading == true || item.isDownloaded == true) {
                if (type.indexOf('pdf') > -1 && document.getElementById(item.id) != null) {
                    // document.getElementById(item.id).setAttribute("src", item.content);
                    $scope.showPdf(item);
                }
                return;
            }
            //return await new Promise( function (resolve, reject) {


            /*                if (item.percent != undefined && item.percent == 100 && item.fileId != null && item.content != null && item.content != "") {
                                HTML5.downloadFile(item.name, item.contentType, item.content);
                                return;
                            }*/

            var data = {
                connection: window.rsocket,
                metadata: "download",
                payload: item.fileId,
                mimeString: item.contentType,
                size: item.size,
                onProgress: function (recieveData) {
                    item.isDownloading = true;
                    var percent = parseInt(recieveData * 100 / item.size);
                    item.percent = percent;
                    $scope.$apply();
                },
                onComplete: function (arrayBuffer) {
                    item.isDownloading = false;
                    item.isDownloaded = true;
                    var text = "";

                    if (type.indexOf('text') > -1) {
                        item.content = Buffer.from(arrayBuffer, 'utf-8');

                    } else {
                        item.content = HTML5.toBlob(arrayBuffer, item.contentType); //HTML5.arrayBufferToBase64(arrayBuffer);
                        item.content = URL.createObjectURL(item.content);
                    }


                    item.percent = 100;


                    if (type.indexOf('pdf') > -1) {
                        if (document.getElementById(item.id) != null) {
                            document.getElementById(item.id).setAttribute("src", item.content);
                        }
                    }


                    var newItem = HTML5.clone(item);

                    if (text != "") {
                        newItem.content = text;
                    }

                    if (text != "" && newItem.contentType == "") {
                        newItem.contentType = "text/plain";
                    }
                    // $scope.showFile(newItem);
                    // $scope.$apply();
                    // document.getElementById(item.id).setAttribute("src", item.content);
                    // HTML5.downloadFile(item.filename, item.contentType, item.content);
                    $scope.$apply();
                    //  resolve("true")
                },
                onError: function (error) {
                    item.isDownloading = false;
                    $scope.error = error;
                    $scope.$apply();
                    // reject("error");
                }
            }

            rsocketExt.clone().rsocketDownload(data);


        }

        $scope.delete = function (item) {
            // delete by item.id
            if (window.rsocket != null) {

                rsocket.requestResponse({
                    data: rsocketExt.jsonToBinary({id: item.id, fileId: item.fileId}),
                    metadata: rsocketExt.metadata('delete')
                }).subscribe({
                    onComplete: function (payload) {
                        var id = rsocketExt.binaryToString(payload.data);
                        for (var i = 0; i < $rootScope.messages.length; i++) {
                            if ($rootScope.messages[i].id == id) {
                                $rootScope.messages.splice(i, 1);
                            }
                        }
                        $scope.$apply();
                        //console.log("delete: " + id);
                    },
                    onError: function (error) {
                        $scope.error += ("\n" + error);
                        $scope.$apply();
                        $scope.error(error);
                    },
                    onSubscribe: function (subscription) {
                        // ...
                    }
                });
            }


        }

        if ($rootScope.loadMessage === undefined) {
            $rootScope.loadMessage = true;
            // $scope.allMessages();
        }

        if ($rootScope.messages === undefined || $rootScope.messages == null || $rootScope.messages.length == 0) {
            $rootScope.allMessages()
                .then(function (result) {
                        $rootScope.messages = result;
                        $scope.$apply();
                    },
                    function (error) {

                    });
        }

    }
    ]);

