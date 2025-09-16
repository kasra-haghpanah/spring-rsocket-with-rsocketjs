/**
 * Created by kasra.haghpanah on 01/09/2016.
 */
//"use strict";
(function () {

    if (typeof window.newTab === "undefined") {

        window.newTab = function (event) {
            var target = event.target || event.srcElement || event.currentTarget;
            event.preventDefault ? event.preventDefault() : (event.returnValue = false);

            if (disabled == 'true') {
                target.style.color = 'black';
                target.style.textDecoration = 'none';
                //target.style.cursor="default";
            }

            var win = window.open(target.href, '_blank');
            win.focus();
        }

    }

    if (typeof window.dec2hex === "undefined") {
        window.dec2hex = function (textString) {
            return (textString + 0).toString(16).toUpperCase();
        }
    }

    if (typeof String.unescapeJavaScript === "undefined") {

        String.prototype.unescapeJavaScript = function () {
            var r = /\\u([\d\w]{4})/gi;
            var str = this;
            str = str.replace(r, function (match, grp) {
                return String.fromCharCode(parseInt(grp, 16));
            });
            return str;
        }
    }

    if (typeof window.hex2char === "undefined") {
        window.hex2char = function (hex) {
            // converts a single hex number to a character
            // note that no checking is performed to ensure that this is just a hex number, eg. no spaces etc
            // hex: string, the hex codepoint to be converted
            var result = '';
            var n = parseInt(hex, 16);
            if (n <= 0xFFFF) {
                result += String.fromCharCode(n);
            } else if (n <= 0x10FFFF) {
                n -= 0x10000
                result += String.fromCharCode(0xD800 | (n >> 10)) + String.fromCharCode(0xDC00 | (n & 0x3FF));
            } else {
                result += 'hex2Char error: Code point out of range: ' + window.dec2hex(n);
            }
            return result;
        }
    }

    if (typeof String.convertUnicode2Char === "undefined") {

        String.prototype.convertUnicode2Char = function () {
            // converts a string containing U+... escapes to a string of characters
            // str: string, the input

            var str = this;
            // first convert the 6 digit escapes to characters
            //   /[Uu]\+10([A-Fa-f0-9]{4})/g
            str = str.replace(/\\[U|u][0-9a-fA-F]{4}/g,
                function (matchstr, parens) {
                    //return hex2char('10' + parens);
                    matchstr = matchstr.replace("\\u", "");
                    matchstr = matchstr.replace("\\U", "");
                    return hex2char(matchstr);
                }
            );
            // next convert up to 5 digit escapes to characters
            str = str.replace(/[\\U|\\u]\+([A-Fa-f0-9]{1,5})/g,
                function (matchstr, parens) {
                    //return  hex2char(parens);
                    matchstr = matchstr.replace("\\u", "");
                    matchstr = matchstr.replace("\\U", "");
                    return hex2char(matchstr);
                }
            );
            return str;
        }
    }

    if (typeof String.toUnicode === "undefined") {
        String.prototype.toUnicode = function () {
            var result = "";
            for (var i = 0; i < this.length; i++) {
                result += "\\u" + ("000" + this[i].charCodeAt(0).toString(16)).substr(-4);
            }
            return result;
        };
    }

    if (typeof window.HTML5 === "undefined") {

        window.HTML5 = {
            xhr: null,
            stripHtml: function (html) {
                //var doc = new DOMParser().parseFromString(text, "text/html");
                //return doc.textContent || doc.innerText || "";
                if (html == null) return null;
                return html.replace(/</gm, '&lt;').replace(/>/gm, '&gt;');
            },
            mimeContentType: function (fileElement) {

                if (!fileElement.files) return false;//if browser does not support
                var file = fileElement.files[0];


                var size = file.size;
                if (size >= 1024 * 1024) size = (Math.round(size / (1024 * 1024))).toString() + ' MB';
                else if (size >= 1024 && size < 1024 * 1024) size = (Math.round(size / 1024)).toString() + ' KB';
                else size = size + " bytes";

                var type = file.type;
                if (type == "" || type == null) {
                    type = "application-slash-octet-stream";
                    try {
                        //this.blobToString(file);
                        type = "text/plain";
                    } catch (e) {

                    }

                }

                return {
                    size: file.size,
                    mbSize: size,
                    contentType: type,
                    name: file.name
                };

            },

            uploadSupport: function (form) {
                var inputs = document.getElementsByTagName("input");
                if (typeof FormData == "function") {
                    for (var i = 0; i < inputs.length; i++)
                        if (inputs.item(i).type == "submit") inputs.item(i).type = "button";
                }

            },

            upload: function (xhrAndFormData, formElement, funcProgress, funcComplete, funcFailed, funcCancel, isUnicode) {

                if (typeof FormData != "function") return false;//if browser does not support

                var fd = new FormData();


                var inputs = formElement.getElementsByTagName("input");
                for (var i = 0; i < inputs.length; i++) {

                    if (inputs.item(i).name != "") {

                        if (inputs.item(i).type == "file") {
                            if (typeof inputs.item(i).files[0] != "undefined") {
                                var nameParameter = inputs.item(i).name;
                                if (isUnicode) {
                                    nameParameter = nameParameter.toUnicode();
                                    if (inputs.item(i).files[0] != null) {
                                        nameParameter = inputs.item(i).files[0].name.toUnicode();
                                        //obj.duration = video.duration;
                                        if (inputs.item(i).files[0].duration != undefined)
                                            alert(inputs.item(i).files[0].duration);
                                    }
                                }
                                fd.append(nameParameter, inputs.item(i).files[0]);
                            }

                        } else if (inputs.item(i).type.toLowerCase() != "checkbox") {

                            if (inputs.item(i).value == null) {
                                inputs.item(i).value = "";
                            }

                            if (inputs.item(i).value != "") {
                                fd.append(
                                    (isUnicode) ? inputs.item(i).getAttribute("name").toUnicode() : inputs.item(i).getAttribute("name"),
                                    inputs.item(i).value
                                );
                            }

                        } else if (inputs.item(i).checked) {
                            fd.append(
                                (isUnicode) ? inputs.item(i).getAttribute("name").toUnicode() : inputs.item(i).getAttribute("name"),
                                inputs.item(i).value
                            );
                        }

                    }
                }

                var selects = formElement.getElementsByTagName("select");
                for (var i = 0; i < selects.length; i++) {
                    var name = selects[i].getAttribute("name");
                    if (isUnicode) {
                        name = name.toUnicode();
                    }
                    fd.append(name, selects[i].value);
                }


                if (typeof XMLHttpRequest == 'function') this.xhr = new XMLHttpRequest();
                else this.xhr = new ActiveXObject("Microsoft.XMLHTTP");

                this.xhr.upload.onprogress = function (evt) {

                    if (evt.lengthComputable) {
                        var percentComplete = Math.round(evt.loaded * 100 / evt.total);
                        funcProgress(percentComplete);
                    } else funcProgress("error");

                }


                this.xhr.onload = function (evt) {
                    funcComplete(evt.target.responseText);
                }

                this.xhr.onerror = function () {
                    funcFailed();//alert("There was an error attempting to upload the file.");
                }
                this.xhr.onabort = function (evt) {
                    funcCancel();//alert("The upload has been canceled by the user or the browser dropped the connection.");
                }

                this.xhr.open(formElement.getAttribute("method").toUpperCase(), formElement.getAttribute("action"));
                if (xhrAndFormData != null) {
                    xhrAndFormData(this.xhr, fd);
                }
                this.xhr.send(fd);


            },
            downloadFile: function (filename, contentType, data, aTagElement) {

                // https://stackoverflow.com/questions/19327749/javascript-blob-filename-without-link

                if (data == null || data == "") {
                    return;
                }

                var isBase64 = data.constructor === String;
                var isBlob = data.constructor === Blob;
                if (!isBlob && data.constructor === Uint8Array) {
                    data = data.buffer;
                }

                if (!isBlob) {
                    data = HTML5.toBlob(data, contentType);
                    isBlob = true;
                }


                if (window.navigator.msSaveBlob != undefined) {
                    window.navigator.msSaveBlob(data, filename);
                    return;
                }

                var a;
                if (aTagElement != undefined || aTagElement != null) {
                    a = aTagElement;
                } else {
                    a = document.createElement("a");
                }

                var url = window.URL.createObjectURL(data);
                a.href = url;
                a.download = filename;
                a.click();
                window.URL.revokeObjectURL(url);
            },
            createFileElement: function (data) {

                /*                data = {
                                    filename: filename,
                                    contentType: contentType,
                                    content: content,
                                    width: width,
                                    height: height,
                                    class: "col-7",
                                    videoOrAudioElement: video,
                                    isDecodeHTMLCode: true
                                }*/
                // var isBase64 = (data.isBase64 === undefined || data.isBase64 == null) ? false : true;

                return new Promise(function (resolve, reject) {

                    var isDecodeHTMLCode = (data.isDecodeHTMLCode === undefined || data.isDecodeHTMLCode == null) ? false : true;
                    var isBase64 = data.content.constructor === String;
                    if (data.content.constructor === Uint8Array) {
                        data.content = data.content.buffer;
                    }
                    var isArrayBuffer = data.content.constructor === ArrayBuffer;
                    if (isArrayBuffer || data.content.constructor === File) {
                        data.content = this.toBlob(data.content, data.contentType);
                        isArrayBuffer = false;
                    }
                    var isBlob = data.content.constructor === Blob;

                    var isDataUri = isBase64 && data.content.indexOf(";Base64,")
                    if (isArrayBuffer || isBlob) {
                        isBase64 = false;
                    }


                    var filename = data.filename;
                    var contentType = data.contentType;
                    var content = data.content;

                    if (contentType.indexOf("text") > -1) {
                        if (isArrayBuffer) {
                            content = this.arrayBufferToBase64(content);
                        } else if (isBase64) {
                            content = this.base64ToUnicode(content);
                        }

                    }

                    var width = data.width === undefined ? null : data.width;
                    var height = data.height === undefined ? null : data.height;
                    var className = data.class === undefined ? null : data.class;
                    var video = data.videoOrAudioElement === undefined ? null : data.videoOrAudioElement;


                    //var td = element.target || element.srcElement || element.currentTarget;
                    if (contentType == null || content == null) {
                        reject("");
                    }
                    var contentTypeLower = contentType.toLowerCase();


                    if (contentTypeLower.indexOf("text") == -1) {
                        if (isDataUri == -1 && isBase64) {
                            content = "data:" + contentType + ";base64," + content;
                        }
                        if (content != "") {
                            if (isArrayBuffer || isBase64) {
                                content = window.URL.createObjectURL(this.toBlob(content, contentType));
                            } else if (isBlob) {
                                content = window.URL.createObjectURL(content);
                            }

                        }

                    }

                    if (contentTypeLower.indexOf("pdf") > -1) {
                        var embed = document.createElement("iframe");
                        embed.setAttribute("src", content);//window.URL.createObjectURL(item.content)
                        embed.setAttribute("type", "application/pdf");
                        embed.setAttribute("alt", "pdf");
                        if (width != null) {
                            embed.setAttribute("width", width);
                        }
                        if (height != null) {
                            embed.setAttribute("height", height);
                        }

                        if (className != null) {
                            embed.className = className;
                        }
                        //embed.setAttribute("pluginspage", "http://www.adobe.com/products/acrobat/readstep2.html");
                        resolve(embed);

                    } else if (contentTypeLower.indexOf("text") > -1) {

                        var div = document.createElement("div");
                        if (isBlob) {
                            content.text()
                                .then(function (text) {
                                    if (contentType.toLowerCase() == "text/html" && isDecodeHTMLCode) {
                                        text = HTML5.stripHtml(text);
                                    }
                                    div.innerHTML = text;
                                })
                        } else {
                            if (isDecodeHTMLCode) {
                                content = HTML5.stripHtml(content);
                            }
                            div.innerHTML = content;
                        }
                        //https://developer.mozilla.org/en-US/docs/Web/API/WindowBase64/Base64_encoding_and_decoding
                        //atob(item.content);  //ajax.b64DecodeUnicode(item.content); //atob(item.content);     //btoa encode to base64     //atob decode base64 to string

                        if (className != null) {
                            div.className = className;
                        }

                        if (width != null || height != null) {
                            var letWidth = width != null ? "width: " + width + "px" : "";
                            var letHeight = height != null ? "height: " + height : "";
                            if (letWidth != "") {
                                letWidth += ";";
                            }
                            div.setAttribute("style", "resize: both;overflow-x: scroll;overflow-y: scroll;" + letWidth + letHeight);
                        }
                        resolve(div);
                    } else if (contentTypeLower.indexOf("video") > -1 || contentTypeLower.indexOf("audio") > -1) {
                        var type = "video";
                        if (contentTypeLower.indexOf("audio") > -1) {
                            type = "audio";
                        }

                        video = video == null ? document.createElement(type) : video;
                        video.src = null;
                        video.src = content; //window.URL.createObjectURL(item.content);
                        //video.play();
                        video.preload = "metadata";
                        video.controls = "controls";
                        video.type = contentType;

                        if (className != null) {
                            video.className = className;
                        }

                        if (width != null || height != null) {
                            var letWidth = width != null ? "width: " + width : "";
                            var letHeight = height != null ? "height: " + height : "";
                            if (type = "audio") {
                                letHeight = "";
                            }
                            if (letWidth != "") {
                                letWidth += ";";
                            }
                            video.setAttribute("style", letWidth + letHeight);
                        }
                        //video.size = "150";
                        //video.height = "150";
                        resolve(video);
                    } else if (contentTypeLower.indexOf("image/") > -1) {
                        var img = document.createElement("img");
                        img.src = content;//window.URL.createObjectURL(item.content);
                        //img.size = "150";
                        //img.height = "150";
                        img.alt = filename;
                        if (className != null) {
                            img.className = className;
                        }

                        if (width != null || height != null) {
                            var letWidth = width != null ? "width: " + width + "px" : "";
                            var letHeight = height != null ? "height: " + height : "";
                            if (letWidth != "") {
                                letWidth += ";";
                            }
                            img.setAttribute("style", letWidth + letHeight);
                        }

                        resolve(img);
                    }

                    reject("");
                });

            },
            arrayBufferToString: function (arrayBuffer) {
                return new TextDecoder("utf-8").decode(arrayBuffer);//text to binary
            },
            readFileAsync: function (fileDom, type) {
                var file = fileDom.files[0];
                if (file === undefined) {
                    return null;
                }
                if (type == null || type == "") {
                    type = "text";
                }
                type = type.toLowerCase();
                var result = {};
                result.name = file.name;
                result.size = file.size;
                result.contentType = file.type;
                result.duration = "0";
                var _this = this;
                var isVideo = false;

                var typeLowerCase = file.type.toLowerCase();
                if (typeLowerCase.indexOf("video") > -1 || typeLowerCase.indexOf("audio") > -1) {
                    isVideo = true;
                }

                return new Promise((resolve, reject) => {
                    var reader = new FileReader();

                    reader.onload = function (event) {
                        result.content = event.target.result;

                        if (isVideo) {
                            var video = document.createElement("VIDEO");
                            video.preload = "metadata";
                            video.controls = "controls";
                            video.size = file.size;
                            video.type = file.type;
                            video.src = window.URL.createObjectURL(_this.toBlob(result.content, file.type));

                            video.oncanplay = function () {
                                result.duration = video.duration;
                                resolve(result);
                                video.oncanplay = null;
                                //delete video;
                            }

                            window.URL.revokeObjectURL(video);
                        }

                        if (!isVideo) {
                            resolve(result);
                        }

                    };

                    reader.onerror = function (event) {
                        reject(event.target.error.code);
                    }

                    switch (type) {
                        case "arraybuffer":
                            reader.readAsArrayBuffer(file);
                            break;
                        case "binarystring":
                            reader.readAsBinaryString(file);
                            break;
                        case "dataurl":
                            reader.readAsDataURL(file);
                            break;
                        default:
                            reader.readAsArrayBuffer(file);
                    }
                })
            },
            fileReader: function (metaData) {
                // https://developer.mozilla.org/en-US/docs/Web/API/URL/createObjectURL
                // https://developer.mozilla.org/en-US/docs/Web/API/File/Using_files_from_web_applications#Example_Using_object_URLs_to_display_images
                // https://developer.mozilla.org/en-US/docs/Web/API/HTMLMediaElement/srcObject

                if (typeof FileReader != "function") return false;

                /*                metaData:{
                                        file:myfile,
                                        element:div,
                                        width:150,
                                        height:150,
                                        class : "col-7"
                                        maxSizeByMB:50,
                                        isArrayBuffer:true,
                                        videoDuration:function(video){}
                                }*/

                var file = metaData.file;
                var element = metaData.element;
                var width = (metaData.width === undefined || metaData.width == null) ? null : metaData.width;
                var videoDuration = (metaData.videoDuration === undefined || metaData.videoDuration == null) ? null : metaData.videoDuration;
                var className = (metaData.class === undefined || metaData.class == null) ? null : metaData.class;
                var height = (metaData.height === undefined || metaData.height == null) ? null : metaData.height;
                var maxSizeByMB = (metaData.maxSizeByMB === undefined || metaData.maxSizeByMB == null) ? 30 : metaData.maxSizeByMB;
                var isArrayBuffer = (metaData.isArrayBuffer === undefined || metaData.isArrayBuffer == null) ? false : true;

                file = file.files[0];
                var size = Math.round(file.size / (1024 * 1024));
                var show = true;
                if (maxSizeByMB != "" && size > maxSizeByMB) show = false;

                var type = file.type;


                var obj = {}
                var reader = new FileReader();


                reader.onerror = function (event) {
                    obj.error = event.target.error.code;
                    alert("File could not be read! Code " + event.target.error.code);
                }

                if (type.indexOf("text") > -1) {
                    reader.readAsText(file);
                } else {
                    if (isArrayBuffer) {
                        reader.readAsArrayBuffer(file);
                    } else {
                        reader.readAsDataURL(file);
                    }
                }


                obj.contentType = type;
                obj.size = file.size;
                obj.bigSizeByMB = size;
                obj.name = file.name;

                var _this = this;


                reader.onload = function (event) {
                    obj.content = event.target.result;//base64
                    if (type == "" || type == null) {
                        type = "application-slash-octet-stream";

                        try {
                            obj.content = _this.arrayBufferToString(obj.content);
                            type = "text/plain";
                        } catch (e) {

                        }

                    }


                    if (type.indexOf("text") == 0 && show) {
                        //var x=reader.readAsText(file);
                        var text = document.createElement("div");
                        //text.innerHTML = dataUri;

                        text.innerText = obj.content; //file;
                        var letWidth = "";
                        var letHeight = "";

                        if (width != null || height != null) {
                            letWidth = width != null ? "width: " + width + "px" : "";
                            letHeight = height != null ? "height: " + height + "px" : "";
                            if (letWidth != "") {
                                letWidth += ";";
                            }
                        }
                        text.setAttribute("style", "resize: both;overflow-x: scroll;overflow-y: scroll;" + letWidth + letHeight);


                        if (className != null) {
                            text.className = className;
                        }

                        element.appendChild(text);
                    } else if (type.indexOf("image") == 0 && show) {
                        var img = document.createElement("img");

                        if (width != null) {
                            img.setAttribute("style", "max-width:" + width + ";max-height:" + height + ";");
                        }

                        if (className != null) {
                            img.className = className;
                        }
                        try {
                            img.setAttribute("src", window.URL.revokeObjectURL(file));
                        } catch (e) {
                        }
                        img.setAttribute("src", window.URL.createObjectURL(file));
                        //img.src = dataUri;
                        element.appendChild(img);
                    } else if ((type.indexOf("audio") > -1 || type.indexOf("video") > -1) && show) {
                        var format = (type.indexOf("video") > -1) ? "video" : "audio";
                        var video = document.createElement(format);
                        video.preload = "metadata";
                        video.controls = "controls";
                        video.type = type;
                        video.size = obj.size;

                        if (className != null) {
                            video.className = className;
                        }

                        if (height != null) {
                            video.setAttribute("style", "max-width:" + width + ";max-height:" + height + ";");
                        }
                        var sources = video.getElementsByTagName("source");
                        var source = null;
                        if (sources.length > 0) {
                            source = sources[0];
                        } else {
                            source = document.createElement("source");
                            video.appendChild(source);
                        }
                        source.type = type;

                        try {
                            source.setAttribute("src", window.URL.revokeObjectURL(file));
                        } catch (e) {
                        } finally {
                            source.setAttribute("src", window.URL.createObjectURL(file));
                        }
                        /*

                                                var canvas = document.createElement("canvas");
                                                var ctx = canvas.getContext("2d");
                                                video.currentTime = 400;
                                                ctx.drawImage(video,320,240);
                                                var blob = ctx.toBlob();
                        */


                        element.appendChild(video);

                        video.oncanplay = function () {
                            obj.duration = video.duration;
                            if (videoDuration != null) {
                                videoDuration(video);
                            }
                        }


                    } else if (type.indexOf("pdf") > -1) {
                        if (typeof window.URL !== "undefined" && typeof window.URL.createObjectURL !== "undefined") {

                            var embed = document.createElement("embed");

                            if (className != null) {
                                embed.className = className;
                            }

                            try {
                                embed.setAttribute("src", window.URL.revokeObjectURL(file));
                            } catch (e) {
                            }
                            embed.setAttribute("src", window.URL.createObjectURL(file));
                            embed.setAttribute("type", "application/pdf");
                            if (width != null) {
                                embed.style.maxWidth = width;
                            }

                            if (height != null) {
                                embed.style.maxHeight = height;
                            }

                            element.appendChild(embed);
                        } else {
                            if (typeof window.URL === 'undefined') {
                                window.navigator.msSaveBlob(xhr.response, filename);
                                return;
                            }
                        }

                    }

                }


                return obj;
                //reader.readAsText(file);// baraye khandan besourat text .mohtaviat matn dar dataUri zakhire mishavad

            },
            arrayBufferToBase64: function (buffer) {
                var binary = '';
                var bytes = new Uint8Array(buffer);
                var len = bytes.byteLength;
                for (var i = 0; i < len; i++) {
                    binary += String.fromCharCode(bytes[i]);
                }
                return window.btoa(binary);
            },
            toBlob: function (buffer, contentType) {
                if (buffer.constructor === Blob) {
                    return buffer;
                } else if (buffer.constructor === ArrayBuffer) {
                    buffer = new Uint8Array(buffer);
                } else if (buffer.constructor === String) {
                    try {
                        buffer = this.arrayBufferToString(buffer);  //this.base64ToBinary(buffer);
                    } catch (e) {
                        buffer = this.base64ToBinary(buffer);
                    }

                }
                return new Blob([buffer], {type: contentType});
            },
            // b64EncodeUnicode: function (str) {
            //     // first we use encodeURIComponent to get percent-encoded UTF-8,
            //     // then we convert the percent encodings into raw bytes which
            //     // can be fed into btoa.
            //     return btoa(encodeURIComponent(str).replace(/%([0-9A-F]{2})/g,
            //         function toSolidBytes(match, p1) {
            //             return String.fromCharCode('0x' + p1);
            //         }));
            // },
            // b64DecodeUnicode: function (str) {
            //     // Going backwards: from bytestream, to percent-encoding, to original string.
            //     return decodeURI(atob(str).split('').map(function (c) {
            //         return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            //     }).join(''));
            // },
            blobTobase64: function (blob) {
                var reader = new FileReader();
                reader.readAsDataURL(blob); // converts the blob to base64 and calls onload

                reader.onload = function () {
                    return reader.result; // data url  base64
                };
            },
            Buffer: {
                from: function (value, type) {
                    if (value == null || value == "") {
                        return value;
                    }
                    type = type.toLowerCase();
                    if (value.constructor === String && type == 'utf-8') {
                        return new TextEncoder().encode(value);
                    }
                },
                toString: function (buffer) {
                    if (buffer.constructor === ArrayBuffer) {
                        return new TextDecoder().decode(buffer);
                    }
                },
                concat: function (arrayBuffer, bufferLength) {

                    if (arguments.length == 1) {
                        var bufferLength = 0;
                        for (var i = 0; i < arrayBuffer.length; i++) {
                            if (arrayBuffer[i].constructor == String) {
                                arrayBuffer[i] = new TextEncoder().encode(arrayBuffer[i]).buffer;
                            } else if (arrayBuffer[i].constructor == Number) {
                                arrayBuffer[i] = Uint8Array.from([arrayBuffer[i]]).buffer;
                            }
                            bufferLength += arrayBuffer[i].byteLength;
                        }

                        var allBuffer = new Uint8Array(bufferLength);
                        var readData = 0;
                        var length = arrayBuffer.length;
                        for (var i = 0; i < length; i++) {
                            var buffer = arrayBuffer[i];
                            allBuffer.set(new Uint8Array(buffer), readData);
                            readData += buffer.byteLength;
                        }
                        return allBuffer;
                    }

                    var allBuffer = new Uint8Array(bufferLength).buffer.byteLength;
                    var readData = 0;
                    var length = arrayBuffer.length;
                    for (var i = 0; i < length; i++) {
                        if (arrayBuffer[i].constructor == String) {
                            arrayBuffer[i] = new TextEncoder().encode(arrayBuffer[i]).buffer;
                        } else if (arrayBuffer[i].constructor == Number) {
                            arrayBuffer[i] = Uint8Array.from([arrayBuffer[i]]).buffer;
                        }
                        var buffer = new Uint8Array(arrayBuffer[i]).buffer;
                        allBuffer.set(new Uint8Array(buffer), readData);
                        readData += buffer.byteLength;
                    }
                    return allBuffer;
                }
            },
            base64ToBinary: function (base64) {
                var BASE64_MARKER = ';base64,';
                var base64Index = 0;
                var base64Index = base64.indexOf(BASE64_MARKER);
                if (base64Index > -1) {
                    base64Index = base64Index + BASE64_MARKER.length;
                    base64 = base64.substring(base64Index);
                }
                var raw = window.atob(base64);
                var rawLength = raw.length;
                var array = new Uint8Array(new ArrayBuffer(rawLength));

                for (var i = 0; i < rawLength; i++) {
                    array[i] = raw.charCodeAt(i);
                }
                return array;
            },
            download: function (method, url, isDownload, callbackPercent, callbackXHR) {

                var query = null;
                var address = url;
                var index = url.indexOf("?");
                if (index > -1) {
                    address = url.substring(0, index);
                    query = url.substring(index + 1, url.length);
                }

                if (typeof window.URL !== "undefined" || typeof window.navigator.msSaveBlob !== 'undefined') {


                    var that = this;

                    var args = arguments;


                    var xhr;
                    if (typeof XMLHttpRequest !== 'undefined') {
                        xhr = new XMLHttpRequest();
                    } else {
                        var versions = [
                            "MSXML2.XmlHttp.6.0",
                            "MSXML2.XmlHttp.5.0",
                            "MSXML2.XmlHttp.4.0",
                            "MSXML2.XmlHttp.3.0",
                            "MSXML2.XmlHttp.2.0",
                            "Microsoft.XmlHttp"
                        ];

                        for (var i = 0; i < versions.length; i++) {

                            try {
                                xhr = new ActiveXObject(versions[i]);
                                break;
                            } catch (e) {
                            }

                        }
                    }

                    xhr.open(method.toUpperCase(), address, true);

                    if (typeof window.URL !== "undefined" && typeof window.URL.createObjectURL !== "undefined") {
                        xhr.responseType = "blob";
                    }

                    //xhr.responseType = "arraybuffer";
                    xhr.onreadystatechange = function () {
                        if (xhr.readyState === 4 && xhr.status === 200) {
                            var filename = xhr.getResponseHeader("Content-Disposition").split(";")[1].split("=")[1];
                            filename = filename.convertUnicode2Char();
                            if (args.length > 4) {
                                callbackXHR(xhr);
                            }


                            if (isDownload == true) {
                                if (typeof window.chrome !== 'undefined') {
                                    // Chrome version
                                    var link = document.createElement('a');
                                    link.href = window.URL.createObjectURL(xhr.response);
                                    link.download = filename;
                                    link.click();
                                } else if (typeof window.navigator.msSaveBlob !== 'undefined') {
                                    // IE version
                                    var blob = new Blob([xhr.response], {type: 'application/force-download'});
                                    window.navigator.msSaveBlob(blob, filename);
                                } else if (typeof window.URL !== "undefined" && typeof window.URL.createObjectURL !== "undefined") {
                                    // Firefox version
                                    var file = new File([xhr.response], filename, {type: 'application/force-download'});
                                    window.open(URL.createObjectURL(file));
                                }


                            }


                        }
                    };


                    xhr.addEventListener("progress", function (evt) {
                        if (evt.lengthComputable) {
                            var digit = parseInt(evt.loaded / evt.total);

                            if (args.length > 3) {
                                callbackPercent(digit);
                            }

                        }
                    }, false);
                    xhr.setRequestHeader("Content-type", "application/x-www-form-urlencoded");

                    //xhr.setRequestHeader("OPENED", "bytes=0-" + xhr.getResponseHeader("Content-Length"));
                    //xhr.setRequestHeader("OPENED", "bytes=0-" + 1024*1024*3);
                    if (query == null) {
                        xhr.send();
                    } else {
                        xhr.send(query);
                    }


                }/////???****
                else {
                    var from = document.createElement("from");
                    from.setAttribute("action", url);
                    from.setAttribute("method", method.toUpperCase());
                    var input = document.createElement("input");
                    input.setAttribute("type", "submit");
                    input.setAttribute("value", "send");
                    from.appendChild(input);
                    input.click();
                }

            },

            sendRequest: function (obj) {

                var method = obj.method;
                var url = obj.url;
                var success = obj.success;
                var headers = (obj.headers == undefined || obj.headers == null) ? {} : obj.headers;
                var data = (obj.data == undefined || obj.data == null) ? null : obj.data;

                // method, url, data, contentType, success, headers

                var xhr;
                if (typeof XMLHttpRequest !== 'undefined') {
                    xhr = new XMLHttpRequest();
                } else {
                    var versions = [
                        "MSXML2.XmlHttp.6.0",
                        "MSXML2.XmlHttp.5.0",
                        "MSXML2.XmlHttp.4.0",
                        "MSXML2.XmlHttp.3.0",
                        "MSXML2.XmlHttp.2.0",
                        "Microsoft.XmlHttp"
                    ];

                    for (var i = 0; i < versions.length; i++) {

                        try {
                            xhr = new ActiveXObject(versions[i]);
                            break;
                        } catch (e) {
                        }

                    }
                }

                xhr.open(method.toUpperCase(), url, true);
                //xhr.setRequestHeader('Content-Type', contentType);
                if (headers != null) {
                    for (var key in headers) {
                        xhr.setRequestHeader(key, headers[key]);
                    }
                }


                if (headers != null && headers['Accept'] != undefined && headers['Accept'] != null) {
                    if (headers.Accept.toLowerCase().indexOf('application/json') > -1) {
                        xhr.send(data != null ? JSON.stringify(data) : "");
                    } else {
                        xhr.send(data);
                    }
                } else {
                    xhr.send(data);
                }
                //xhr.onloadend
                xhr.onreadystatechange = function (event) {
                    if (xhr.readyState == 4 && xhr.status == 200) {
                        //var target = event.target;
                        var contentTypeResponse = xhr.getResponseHeader("Content-Type");
                        var data = null;
                        if (contentTypeResponse != null) {
                            if (contentTypeResponse.indexOf('application/json') > -1) {
                                try {
                                    data = JSON.parse(xhr.responseText);
                                } catch (e) {
                                    data = "";
                                }

                            } else if (contentTypeResponse.indexOf('application/xml') > -1) {
                                data = xhr.responseXML;
                            } else {
                                data = xhr.responseText;
                            }
                        } else {
                            data = xhr.responseText;
                        }
                        success(data);
                    }
                };
            },
            sendAsJSON: function (obj) {
                obj.headers.Accept = 'application/json; charset=UTF-8';
                this.sendRequest(obj);
            },
            sendAsXML: function (obj) {
                obj.headers.Accept = 'application/xml; charset=UTF-8';
                this.sendRequest(obj);
            },
            send: function (obj) {
                //obj.headers.Accept = 'application/x-www-form-urlencoded; charset=UTF-8';
                this.sendRequest(obj);
            },
            clone: function (obj) {

                if (arguments.length == 0) {
                    var obj = {};
                    for (var attr in this) {
                        obj[attr] = this[attr];
                    }
                    return obj;
                }

                var newObject = {};
                for (var attr in obj) {
                    newObject[attr] = obj[attr];
                }
                return newObject;
            },
            base64ToUnicode: function (sBase64) {
                return decodeURIComponent(atob(sBase64).split('').map(function (c) {
                    return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
                }).join(''));
            },
            UTF16ToBase64: function (sString) {

                return btoa(encodeURIComponent(sString).replace(/%([0-9A-F]{2})/g,
                    function toSolidBytes(match, p1) {
                        return String.fromCharCode('0x' + p1);
                    }));
            }


        }
    }

})();
