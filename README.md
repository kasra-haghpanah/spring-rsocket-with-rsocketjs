# election-spring-rsocket-with-rsocket-js

# Notice

This application works on Spring 3.0.0 or later and JDK 17 or later

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.1.3</version>
    <relativePath/>
</parent>
```

# Technologies
- [x] [Spring Rsocket](https://github.com/benwilcock/spring-rsocket-demo)
- [x] [Spring Webflux](https://github.com/spring-projects/spring-framework/tree/main/spring-webflux)
- [x] [Spring Security](https://github.com/spring-projects/spring-security)
- [x] [Spring Rsocket Security](https://github.com/spring-tips/rsocket-security)
- [x] [Spring Data Mongodb Reactive](https://github.com/spring-projects/spring-data-mongodb/blob/main/src/main/asciidoc/reference/reactive-mongo-repositories.adoc)
- [x] [Springdoc Openapi](https://github.com/springdoc/springdoc-openapi)
- [x] [Spring Retry](https://github.com/spring-projects/spring-retry)
- [x] [Spring thymeleaf](https://github.com/thymeleaf/thymeleaf-spring)
- [x] [Spring Validation](https://github.com/spring-projects/spring-boot/blob/main/spring-boot-project/spring-boot-starters/spring-boot-starter-validation/build.gradle)
- [x] [JWT on Webflux & Rsocket](https://github.com/jwtk/jjwt)
- [x] [Reactor Netty](https://github.com/spring-projects/spring-boot/tree/main/spring-boot-project/spring-boot-starters/spring-boot-starter-reactor-netty)
- [x] [AngularJs](https://github.com/angular/angular.js?)
- [x] [browserified rsocket-js](https://github.com/rsocket/rsocket-js/tree/master)
- [x] [bootstrap](https://github.com/twbs)


# Prerequisites

First, install MongoDB 4.2 or later on your system

Second in the application.properties change the value of the adminUsers as a key  and insert your phone number

Now you can run this application

# Application URL
http://localhost:8095

# Springdoc Openapi URL
http://localhost:8095/webjars/swagger-ui/index.html#/

![01](https://github.com/kasra-haghpanah/spring-rsocket-with-rsocketjs/assets/53397941/8eec1f1d-deb4-4a1b-be87-fd58194cde1c)


# Features
First click The Signup button then create an account yourself and finally the login. Now you see the following image.

![image](https://user-images.githubusercontent.com/53397941/168492767-1c414c6e-a700-45ec-bf94-30ff7ad3d25d.png)

Now click the Upload tap then click the Send button.

![image](https://user-images.githubusercontent.com/53397941/168492923-9b7259e9-d947-42a9-aec5-e1c24817bac1.png)

For uploading, has been used the requestChannel and other users will be notified as well (broadcast)

The Users management's tree uses requestStream as well

For download, go to The Content tab, and click on the item you want. For download, it has been used requestStream.
