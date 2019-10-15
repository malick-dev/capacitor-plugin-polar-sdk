# HNl capacitor-plugin-polar


## Test with demo app

cd [pluginDir]

npm install

cd [pluginDir]/demoApp

npm install

ionic build

npx cap open android

* after each modif on demo or plugin source code, re-execute 

npx cap sync android

## TASK LIST

DEMO APP
* [x] test polar sdk android sample
* [x] set up ionic test app
* [ ] test all plugin scenario with test app
* [ ] update view when event trigger

PLUGIN 
-[x] create plugin
-[ ] publish plugin in npm registry

PLUGIN - ANDROID
* [ ] polar connect
    -[x] hr
    -[ ] ppi
* [ ] polar disconnect
* [ ] bluetooth permissions
* [ ] trigger generic event name
* [ ] error exception handling
    - [ ] when device not enabled
    - [ ] when disconnection happened
    - [ ] when connection error

PLUGIN - IOS
* [ ] polar connect
    -[ ] hr
    -[ ] ppi
* [ ] polar disconnect
* [ ] trigger generic event name
* [ ] error exception handling
    - [ ] when disconnection happened
    - [ ] when connection error

## links

* https://capacitor.ionicframework.com/docs/plugins

* https://www.joshmorony.com/creating-a-local-capacitor-plugin-to-access-native-functionality-ios-swift/

* https://www.joshmorony.com/running-custom-native-ios-code-in-ionic-with-capacitor/



