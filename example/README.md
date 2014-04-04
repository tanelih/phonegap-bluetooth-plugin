# PhoneGap Bluetooth Plugin - Example

Assuming you have your development environment setup correctly...

```
cd example/

bower install

cordova create build-example/ com.example.bt "BtExample"

(cd build-example/ && cordova platform add android)
(cd build-example/ && cordova plugin add https://github.com/tanelih/phonegap-bluetooth-plugin)

cp -r www/* build-example/www/

(cd build-example/ && cordova build android)
(cd build-example/ && cordova run)
```
