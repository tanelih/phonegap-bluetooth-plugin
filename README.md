PhoneGap BluetoothPlugin
========================
Bluetooth plugin for PhoneGap (Android). Tested on versions 2.6.0 and 3.0.0.

This plugin was created as part of an EU funded (Rural Development Programme for Mainland Finland 2007-2013) 
[project](http://blogit.jamk.fi/metsaapuilta/en/).

Installation
------------
Check out PhoneGap CLI [docs](http://docs.phonegap.com/en/3.0.0/guide_cli_index.md.html#The%20Command-line%20Interface) before starting out.

To install this plugin on 3.0.0, use the phonegap CLI.

```
phonegap local plugin add https://github.com/tanelih/phonegap-bluetooth-plugin.git
```

Remember to build the project afterwards.

Installation below 3.0.0 version should be done manually. Copy the contents of `manual/<platform>/src` and `manual/www` to their respective locations on your project. Remember to add plugin specification to `config.xml` and permissions to `AndroidManifest.xml`.

In `config.xml`...
```
<plugin name="Bluetooth" value="org.apache.cordova.bluetooth.BluetoothPlugin" />
```

In `AndroidManifest.xml`...
```
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
```

Usage
-----

If you installed the plugin with plugman in 3.0.0 environment, the plugin is accessible from `window.bluetooth`.

If you installed the plugin manually, you need to add the `bluetooth.js` script to your app. Then require the plugin after the `deviceready` event.
```
window.bluetooth = cordova.require("cordova/plugin/bluetooth");
```

License
-------
This plugin is available under MIT. See LICENSE for details.
