PhoneGap BluetoothPlugin
========================
Bluetooth plugin for PhoneGap (Android). Tested on versions 2.6.0 and 3.0.0.

**NOTE** The plugin requires Android API version 15 (Ice Cream Sandwich) to function properly.

This plugin was created as part of an EU funded (Rural Development Programme for
Mainland Finland 2007-2013) [project](http://blogit.jamk.fi/metsaapuilta/en/).

Read this first!
----------------
I'm currently in a situation where I do not have any equipment to test this plugin on, 
or develop reliably. Due to this situation, I am unable to maintain this plugin properly. 
I will still respond to pull requests and issues if I can help with them.

If you are interested in developing this plugin, fork it and make a pull request that 
will mention your fork here in this `README`. If you have your own Bluetooth plugin, 
that you would like to be mentioned here, make a pull request and add a link to it in 
this `README`. Pull requests to fix and add stuff will always be appreciated!

Installation
------------
Check out PhoneGap CLI [docs](http://docs.phonegap.com/en/3.0.0/guide_cli_index.md.html#The%20Command-line%20Interface)
before starting out.

To install this plugin on 3.0.0, use the phonegap CLI.

```
phonegap local plugin add https://github.com/tanelih/phonegap-bluetooth-plugin.git
```

Remember to build the project afterwards.

Installation below 3.0.0 version should be done manually. Copy the contents of
`manual/<platform>/src` and `manual/www` to their respective locations on your
project. Remember to add plugin specification to `config.xml` and permissions to
`AndroidManifest.xml`.

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

If you installed the plugin with plugman in 3.0.0 environment, the plugin is
accessible from `window.bluetooth`.

If you installed the plugin manually, you need to add the `bluetooth.js` script
to your app. Then require the plugin after the `deviceready` event.

```
window.bluetooth = cordova.require("cordova/plugin/bluetooth");
```

License
-------
This plugin is available under MIT. See LICENSE for details.
