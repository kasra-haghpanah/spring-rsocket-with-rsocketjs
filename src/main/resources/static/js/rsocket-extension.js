//"use strict";
(function f() {

    if (window.rsocketExt === undefined) {

        window.rsocketExt = {

            clone: function () {
                var object = {};
                for (var key in this) {
                    object[key] = this[key];
                }
                return object;
            },

            getTypeObject: function (object) {

                var stringConstructor = "test".constructor;
                var arrayConstructor = [].constructor;
                var objectConstructor = ({}).constructor;

                if (object === null) {
                    return "null";
                }
                if (object === undefined) {
                    return "undefined";
                }
                if (object.constructor === stringConstructor) {
                    return "String";
                }
                if (object.constructor === arrayConstructor) {
                    return "Array";
                }
                if (object.constructor === objectConstructor) {
                    return "Object";
                }
                {
                    return "don't know";
                }
            },
            sliceFile: function (buffer, format, chunkSize) {
                //var format = file.contentType.indexOf("text") > -1 ? "utf-8" : "binary";
                buffer = Buffer.from(buffer, format);
                var byteLength = buffer.length;

                var chunkNumbers = parseInt(byteLength / chunkSize);
                if (byteLength % chunkSize > 0) {
                    chunkNumbers++;
                }

                var clientRequests = [];

                for (var i = 0; i < chunkNumbers; i++) {

                    if (i < chunkNumbers - 1) {
                        clientRequests.push(buffer.slice(i * chunkSize, (i + 1) * chunkSize));
                    } else {
                        clientRequests.push(buffer.slice(i * chunkSize, byteLength));
                    }
                }
                return clientRequests;
            },

            replaceDotSlash: function (data) {
                if (data == null) {
                    data = "";
                }
                data = data + "";
                return data.replace(/\./g, "-dot-").replace(/\//g, "-slash-");
            },

            jsonToBinary: function (json) {
                if (json == null) return null;
                return Buffer.from(JSON.stringify(json), 'utf-8'); //Buffer.from(JSON.stringify(json));
            },

            binaryToJson: function (buffer) {
                var content = this.binaryToString(buffer);
                if (content != null) {
                    return JSON.parse(content);
                }
                return null;
            },

            binaryToString: function (buffer) {
                if (buffer != null && buffer.constructor === Buffer) {
                    return buffer.toString('utf8');
                }
                return null;
            },

            stringToBinary: function (text) {
                if (text == null) {
                    return null;
                }
                return Buffer.from(text, "utf-8"); //Buffer.from(text, "utf-8");
            },

            metadata: function (path) {
                path = Buffer.from(path, 'utf-8'); //HTML5.Buffer.from(path, 'utf-8');
                var asciiCodes = [65534, 0, 0, path.length + 1, path.length];
                for (var i = 0; i < path.length; i++) {
                    //asciiCodes.push(path.charCodeAt(i));
                    asciiCodes.push(path[i]);
                }
                return Buffer.from(asciiCodes);
            },

            metadataBearer: function (/* String */ token) {
                var metadata = 'message/x.rsocket.authentication.bearer.v0';
                //var metadata = "message/x.rsocket.authentication.v0";
                //metadata: [metadata.length - 1] + metadata + [0, 1, (token.length - 256)] + token
                var binary = [metadata.length - 1];

                for (var i = 0; i < metadata.length; i++) {
                    binary.push(metadata.charCodeAt(i));
                }
                binary.push(0);
                binary.push(1);
                var token = token;
                binary.push(token.length - 256);
                for (var i = 0; i < token.length; i++) {
                    binary.push(token.charCodeAt(i));
                }
                return Buffer.from(binary);
            },

            getTokenBuffer: function (token) {
                if (token == null) {
                    return null;
                }
                return Buffer.from(token);
            },

            rsocketDownload: function (data) {
                /*
                                {
                                    connection: rsocket ,
                                    metadata: 'download.' + $scope.downloadId,
                                    payload: item.fileId,
                                    mimeString: 'video/mp4',
                                    onProgress: function (reicieveData) {},
                                    onComplete: function (base64, binary) {},
                                    onError: function (error) {}
                                }
                */


                var connection = (data.connection === undefined || data.connection == null || !data.connection._machine._connectionAvailability) ? null : data.connection;
                var result = {value: null};
                if (connection == null) {
                    result.error = "your connection is closed!";
                    return result;
                }
                var metaData = (data.metadata === undefined || data.metadata == null) ? null : data.metadata;
                var payload = (data.payload === undefined || data.payload == null) ? null : data.payload;

                var objectType = this.getTypeObject(payload);

                if (payload != null && objectType != "String") {
                    try {
                        payload = JSON.stringify(payload);
                    } catch (e) {

                    }
                    if (Array.isArray(payload)) {
                        var arrayToString = "["
                        for (var i = 0; i < payload.length; i++) {
                            arrayToString += payload[i];
                            if (i < payload.length - 1) {
                                arrayToString += ",";
                            }
                            arrayToString += "]";
                        }
                    }
                }
                payload = this.stringToBinary(payload);

                var mimeString = (data.mimeString === undefined || data.mimeString == null) ? null : data.mimeString;
                var size = (data.size === undefined || data.size == null) ? 0 : data.size;
                var onProgress = (data.onProgress === undefined || data.onProgress == null) ? null : data.onProgress;
                var onComplete = (data.onComplete === undefined || data.onComplete == null) ? null : data.onComplete;
                var onError = (data.onError === undefined || data.onError == null) ? null : data.onError;


                var recieveData = 0;
                var arrayBuffer = []; //new Uint8Array(size);
                var _this = this;
                if (connection != null) {

                    connection.requestStream({
                        data: payload,
                        metadata: _this.metadata(metaData)
                    }).subscribe({
                        onComplete: function () {
                            result.value = Buffer.concat(arrayBuffer);//arrayBuffer;
                            if (onComplete != null) {
                                onComplete(result.value);
                            }
                        },
                        onError: function (error) {
                            result.error = error;
                            if (onError != null) {
                                onError(error);
                            }
                        },
                        onNext: function (payload) {
                            //arrayBuffer.set(new Uint8Array(payload.data), recieveData);
                            arrayBuffer.push(payload.data);
                            recieveData += payload.data.length;
                            if (onProgress != null) {
                                onProgress(recieveData);
                            }
                        },
                        onSubscribe: function (subscription) {
                            subscription.request(2147483647);
                        }
                    });
                }
                return result;
            },
            rsocketUpload: function (data) {

                var _this = this;
                return new Promise(function (resolve, reject) {


                    const {Flowable} = require('rsocket-flowable');

                    /*
                    {
                        connection: rsocket ,
                        metadata: 'download.' + $scope.downloadId,
                        file:{
                            name: "1.mp4",
                            size: 112,
                            contentType: "video/mp4",
                            duration: 354,
                            content: [11,121,...]
                        },
                        chunkSize:4*1024
                        onComplete: function () {},
                        onError: function (result) {},
                        onNext: function (result) {},
                        request: function (chunkNumber , chunkLength) {},
                        cancel: function () {}
                     */


                    var connection = (data.connection === undefined || data.connection == null || !data.connection._machine._connectionAvailability) ? null : data.connection;

                    var result = {value: null};
                    if (connection == null) {
                        result.error = "your connection is closed!";
                        return result;
                    }
                    var metadata = (data.metadata === undefined || data.metadata == null) ? null : data.metadata;
                    metadata = _this.metadata(metadata);

                    var file = (data.file === undefined || data.file == null) ? null : data.file;

                    var chunkSize = isNaN(data.chunkSize) ? 0 : parseInt(data.chunkSize);

                    if (file == null) {
                        return null;
                    }


                    var onComplete = (data.onComplete === undefined || data.onComplete == null) ? null : data.onComplete;
                    var onError = (data.onError === undefined || data.onError == null) ? null : data.onError;
                    var onNext = (data.onNext === undefined || data.onNext == null) ? null : data.onNext;
                    var request = (data.request === undefined || data.request == null) ? null : data.request;
                    var cancel = (data.cancel === undefined || data.cancel == null) ? function () {
                        return false;
                    } : data.cancel;


                    if (file.duration === undefined) {
                        file.duration = "0";
                    }

                    //var metadata = $rootScope.metadata('upload.kasra.haghpanah.' + $rootScope.replaceDotSlash($scope.file.name) + '.' + $rootScope.replaceDotSlash($scope.file.contentType) + '.' + $scope.file.size + '.' + $scope.file.duration);
                    var size = file.size;
                    var buffer = file.content;//.substring($scope.file.content.indexOf(',') + 1);
                    //content = HTML5.base64ToBinary(content);
                    // var byteLength = Buffer.byteLength(content, 'binary');//'base64'
                    // var buffer = Buffer.from(buffer, 'binary');//'base64'

                    var isUTF_8 = (file.content.byteLength === undefined) ? true : false;
                    if (isUTF_8) {
                        if (file.contentType.indexOf("text") < 0) {
                            file.contentType = "text/plain";
                        }
                    }

                    var format = file.contentType.indexOf("text") > -1 ? "utf-8" : "binary";
                    var clientRequests = _this.sliceFile(buffer, format, chunkSize);
                    var j = 0;

                    var mimeString = "";
                    var chunkLength = clientRequests.length;

                    clientRequests = clientRequests.map(function (node) {
                        return {
                            data: node,
                            metadata: metadata
                            //metadata: String.fromCharCode('requestChannel.kasra.haghpanah'.length) + 'requestChannel.kasra.haghpanah'
                        }

                    });

                    //var stream = Flowable.just(clientRequests);


                    var chunkIndex = 0;
                    // https://stremler.io/2020-05-31-rsocket-messaging-with-spring-boot-and-rsocket-js/


                    var flow = new Flowable(subscriber => {
                        //$scope.subscriber = subscriber;
                        subscriber.onSubscribe({
                            cancel: function () {
                                //data.connection._machine._receivers.delete(data.connection._machine._nextStreamId);
                                // cancelled = cancel();
                                resolve("upload is cancel!");
                            },
                            request: async function (n) {

                                var cancelled = cancel();

/*                                while (clientRequests.length > 0) {
                                    await subscriber.onNext(clientRequests.shift());
                                    if (request != null) {
                                        request(chunkIndex, chunkLength);
                                    }
                                    chunkIndex++;
                                }
                                subscriber.onComplete();*/

                                if (!cancelled && n > 0 && clientRequests.length > 0) {
                                    await subscriber.onNext(clientRequests.shift());
                                    if (request != null) {
                                        request(chunkIndex, chunkLength);
                                    }
                                    // $scope.uploadPercent = parseInt(chunkIndex * 100 / lengthChunk);
                                    // $scope.update();
                                    chunkIndex++;
                                    n--;
                                }
                                if (!cancelled && clientRequests.length == 0) {
                                    subscriber.onComplete();
                                }
                            }
                        });

                        /*                        subscriber.onNext({
                                                    data: clientRequests.shift().data,
                                                    metadata: metadata
                                                });*/


                    });


                    rsocket.requestChannel(flow)
                        .subscribe({
                            onComplete: function () {

                                if (onComplete != null) {
                                    onComplete();
                                }
                                resolve("upload is done!");
                                // $scope.uploadPercent = 100;
                                // $scope.update();
                            },
                            onError: function (error) {
                                if (onError != null) {
                                    onError(error);
                                }
                                reject(error);
                                // $scope.error += ("\n" + error);
                                // $scope.update();
                                // $scope.error(error);
                            },
                            onNext: function (payload) {
                                if (onNext != null) {
                                    onNext(payload);
                                }
                                // var data = $rootScope.binaryToJson(payload.data);
                                // data.color = "black";
                                // data.percent = 0;
                                // console.log(data);
                                // $scope.messages.push(data);
                                // $scope.update();
                            },
                            onSubscribe: function (subscriber) {
                                //subscriber.request(0x7fffffff);
                                subscriber.request(clientRequests.length);
                            }
                        });

                });
            }


        }

    }
    ;


})
();