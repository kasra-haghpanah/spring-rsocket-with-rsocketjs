/**
 * Created by kasra.haghpanah on 03/04/2016.
 */
//"use strict";
(function () {

    // window.onload = function () {
    if (typeof window.demoApp === 'undefined') {
        window.demoApp = angular.module("demoApp", ['ngResource', 'ngRoute', 'pascalprecht.translate', 'ngLocale', 'ngTreeview']);

    }
    // }


})();


demoApp.config(['$routeProvider', '$httpProvider', '$sceDelegateProvider', function ($routeProvider, $httpProvider, $sceDelegateProvider) {

    $sceDelegateProvider.resourceUrlWhitelist([
        // Allow same origin resource loads.
        'self',
        // Allow loading from our assets domain. **.
        'http://localhost:8095/**'
    ]);


    $routeProvider
        .when('/signin', {
            controller: 'SigninController',
            templateUrl: '../view/signin.htm'
        })
        .when('/forgot', {
            controller: 'SignupController',
            templateUrl: '../view/forgot.htm'
        })
        .when('/signup', {
            controller: 'SignupController',
            templateUrl: '../view/signup.htm'
        })
        .when('/change/password/:phone/:code', {
            controller: 'SignupController',
            templateUrl: '../view/change.htm'
        })
        .when('/tree', {
            controller: 'TreeController',
            templateUrl: '../view/tree.htm'
        })
        .when('/content', {
            controller: 'ContentController',
            templateUrl: '../view/content.htm'
        })
        .otherwise({
            redirectTo: '/signin'
        });


    $httpProvider.interceptors.push(function ($q, $rootScope, $location) {

        return {
            // optional method
            'request': function (config) {
                // do something on success
                var headers = config.headers;
                if ($rootScope.secret != undefined) {
                    headers['Authorization'] = $rootScope.secret;
                }
                return config;
            },

            // optional method
            'requestError': function (rejection) {
                // do something on error
                return $q.reject(rejection);
            },

            // optional method
            'response': function (response) {
                // do something on success
                //console.log(response.headers());
                var serverUrl = response.config.url;
                var url = $location.protocol() + "://" + $location.host() + ":" + $location.port() + "/login";
                var headers = response.config.headers;
                var data = response.data;
                var method = response.config.method;

                /*                if (response.status == 200) {
                                    if (method == "PUT" && serverUrl == url) {
                                        $rootScope.token = data.token;
                                    }
                                }*/
                return response;
            },

            // optional method
            'responseError': function (rejection) {
                // do something on error
                if (rejection.status == 401) {
                    $location.path("/login");
                }

                return $q.reject(rejection);
            }


        };
    });


    //$httpProvider.defaults.headers.get[window.headerName] = window.token;

    // var value = window.basicAuthorization(window.username, window.password);
    //
    // $httpProvider.defaults.headers.post[window.headerName] = window.token;
    // $httpProvider.defaults.headers.post["Authorization"] = value;
    //
    // $httpProvider.defaults.headers.put[window.headerName] = window.token;
    // $httpProvider.defaults.headers.put["Authorization"] = value;
    //
    // $httpProvider.defaults.headers.patch[window.headerName] = window.token;
    // $httpProvider.defaults.headers.patch["Authorization"] = value;

}]);

demoApp.config(['$translateProvider', function ($translateProvider) {

    $translateProvider
        .useStaticFilesLoader({
            prefix: '/translations/locale-',
            suffix: '.json'
        })
        .preferredLanguage('fa')
        //.useLocalStorage()
        .useSanitizeValueStrategy('escape')
        .useMissingTranslationHandlerLog();

    $translateProvider.translations('en', {
        ElectionWebSite: 'Election web site',
        Code: "Code",
        BUTTON_LANG_EN: 'english',
        BUTTON_LANG_FA: 'فارسی',
        FirstName: "firstname",
        LastName: "lastname",
        Title: "Title",
        AddPerson: "Add person",
        EditPerson: "Edit person",
        Form: "Form",
        Health: "Health",
        Persons: "Persons",
        Delete: "Delete",
        Edit: "Edit",
        UserName: "Username",
        Phone: "Phone",
        Password: "Password",
        RepeatPassword: "Repeat Password",
        Send: "Send",
        Signup: "Signup",
        Signin: "Signin",
        Note: "Note",
        AddUser: "Add sub user",
        User: "User",
        EditUser: "Edit user",
        TeamLeader: "Team leader",
        ErrorSignInMessage: "Your password not match with username!",
        SubGroupCount: "Subgroup count",
        ActiveSubGroupCount: "Active subgroup count",
        Content: "Content",
        UsersManagement: "Users management",
        Settings: "Settings",
        SelectFile: "Select file",
        Text: "Text",
        Upload: "Upload",
        Close: "Close",
        TreeType: "Tree type",
        Theme: "Theme",
        ShowDottedLine: "Show dotted line in the tree",
        TreeSize: "Tree size",
        Language: "Language",
        isExistsUser: "This phone has given",
        duplicateKey: "This phone has given",
        ChangePassword: "Change password",
        Logout: "Logout",
        ForgotPassword: "Forgot password",
        SendSMS: "Send SMS to your phone soon",
        NotExistsUser: "Does not exist user with this phone",
        Save: "Save",
        IncreaseRole: "Increase role",
        DecreaseRole: "Decrease role",
        Success: "Your request has done successfully",
        Faild: "Your request has faild",
        SendLink:"Sending link for registration",
        CopyLink:"Copy link",
        Saved:"Link is saved",
        SuperGroupUser:"Super group user"
    });


    $translateProvider.translations('fa', {
        ElectionWebSite: 'وب سایت انتخابات',
        Code: "رمز",
        BUTTON_LANG_EN: 'english',
        BUTTON_LANG_FA: 'فارسی',
        FirstName: "نام",
        LastName: "نام خانوادگی",
        Title: "عنوان",
        AddPerson: "افزودن کاربر",
        EditPerson: "ویرایش کاربر",
        Form: "فرم",
        Health: "سلامت",
        Persons: "اشخاص",
        Delete: "حذف",
        Edit: "ویرایش",
        UserName: "نام کاربری",
        Phone: "شماره همراه",
        Password: "رمز عبور",
        RepeatPassword: "تکرار رمز عبور",
        Send: "ارسال",
        Signup: "ثبت نام",
        Signin: "ورود",
        Note: "توجه",
        AddUser: "افزودن کاربر زیر مجموعه",
        User: "کاربر",
        TeamLeader: "سرگروه",
        EditUser: "ویرایش کاربر",
        ErrorSignInMessage: "نام کاربری یا رمز عبور درست نمی باشد!",
        SubGroupCount: "تعداد زیر گروه",
        ActiveSubGroupCount: "تعداد زیر گروه فعال",
        Content: "محتوا",
        UsersManagement: "مدیریت کاربران",
        Settings: "تنظیمات",
        SelectFile: "انتخاب فایل",
        Text: "پیام",
        Upload: "آپلود",
        Close: "بستن",
        TreeType: "نوع نمایش درختی",
        Theme: "رنگ پس زمینه",
        ShowDottedLine: "نمایش نقطه چنین در درخت",
        TreeSize: "سایز درخت",
        Language: "زبان",
        isExistsUser: "این شماره همراه از قبل گرفته شده است",
        duplicateKey: "این شماره همراه از قبل گرفته شده است",
        ChangePassword: "تغییر رمز عبور",
        Logout: "خروج",
        ForgotPassword: "فراموشی رمز عبور",
        SendSMS: "به زودی پیامکی برای شما ارسال خواهد شد",
        NotExistsUser: "کاربری با شماره تلفن وارد شده در سیستم موجود نیس",
        Save: "ذخیره",
        IncreaseRole: "افزایش سطح دسترسی",
        DecreaseRole: "کاهش سطح دسترسی",
        Success: "درخواست شما با موفقیت انجام شده است",
        Faild: "درخواست شما با خطا مواجه شده است",
        SendLink:"لینک ارسالی جهت ثبت نام",
        CopyLink:"کپی لینک",
        Saved:"لینک ذخیره شد",
        SuperGroupUser:"کاربر سرگروه"
    });

    $translateProvider.preferredLanguage('en');
    //$translateProvider.useLocalStorage();


}]);
