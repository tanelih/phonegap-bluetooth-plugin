var exec = require('cordova/exec');

/**
 * Create a new instance of Bluetooth(Plugin).
 * 
 * @class 		Bluetooth
 * @classdesc	BluetoothPlugin for cordova 2.6.0 (PhoneGap).
 */
var Bluetooth = function() 
{
	this.platforms = [ "android" ];
};

/**
 * Check if the API is supported on this platform.
 *
 * @memberOf Bluetooth
 * 
 * @returns {boolean}
 */
Bluetooth.prototype.isSupported = function() 
{
	return this.platforms.indexOf(device.platform.toLowerCase()) > -1;
}

/**
 * Generic success callback for the Bluetooth API. Indicates only that the
 * action was executed succesfully.
 *
 * @callback Bluetooth~onSuccess
 */

/**
 * Generic error callback for the Bluetooth API.
 * 
 * @callback  Bluetooth~onError
 * 
 * @param  {object} 	error 			The error object that contains information on what's what.
 * @param  {number} 	error.code 		The error code.
 * @param  {string} 	error.message 	The error message.
 */

/**
 * Generic Success callback that takes a boolean flag as parameter. Is often
 * used in conjunction with methods like isConnected and so on.
 * 
 * @callback  Bluetooth~onResult
 * 
 * @param  {boolean}  isTrue  Flag indicating the result.
 */

/**
 * Check if the device has Bluetooth enabled.
 *
 * @memberOf Bluetooth
 * 
 * @param  {Bluetooth~onResult} 	onSuccess 	Callback on successful execution.
 * @param  {Bluetooth~onError} 		onError 	Callback on failed execution.
 */
Bluetooth.prototype.isEnabled = function(onSuccess, onError)
{
	exec(onSuccess, onError, "Bluetooth", "isEnabled", []);
}

/**
 * Enable Bluetooth on the device.
 *
 * @memberOf Bluetooth
 * 
 * @param  {Bluetooth~onSuccess} 	onSuccess 	Callback on successful execution.
 * @param  {Bluetooth~onError} 		onError 	Callback on failed execution.
 */
Bluetooth.prototype.enable = function(onSuccess, onError) 
{
    exec(onSuccess, onError, "Bluetooth", "enable", []);
}

/**
 * Disable Bluetooth on the device.
 *
 * @memberOf Bluetooth
 * 
 * @param  {Bluetooth~onSuccess} 	onSuccess 	Callback on successful execution.
 * @param  {Bluetooth~onError} 		onError 	Callback on failed execution.
 */
Bluetooth.prototype.disable = function(onSuccess, onError) 
{
    exec(onSuccess, onError, "Bluetooth", "disable", []);
}

/**
 * Check if the device is in the process of discovering devices.
 *
 * @memberOf Bluetooth
 * 
 * @param  {Bluetooth~onResult} onSuccess 	Callback on successful execution.
 * @param  {Bluetooth~onError} 	onError 	Callback on failed execution.	
 */
Bluetooth.prototype.isDiscovering = function(onSuccess, onError)
{
	exec(onSuccess, onError, "Bluetooth", "isDiscovering", []);
}

/**
 * Represents a BluetoothDevice with name and address.
 *
 * @typedef		Bluetooth~BluetoothDevice
 * @type 		{object}
 * 
 * @property  {string}  name 	 Name of the device.
 * @property  {string}  address  Hardware address of the device.
 */

/**
 * Invoked when a new device is found.
 *
 * @callback Bluetooth~onDeviceDiscovered
 *
 * @param  {Bluetooth~BluetoothDevice}  device  The discovered device.
 */

/**
 * Start the device discovery process.
 *
 * @memberOf Bluetooth
 * 
 * @param  {Bluetooth~onDeviceDiscovered} 	onDeviceDiscovered 		Invoked when a device is found.
 * @param  {Bluetooth~onSuccess} 			onDiscoveryFinished 	Invoked when discovery finishes succesfully.
 * @param  {Bluetooth~onError} 				onError 				Invoked if there is an error, or the discovery finishes prematurely.
 */
Bluetooth.prototype.startDiscovery = function(onDeviceDiscovered, onDiscoveryFinished, onError) 
{
	var timeout = function()
	{
		onError({ code: 9001, message: "Request timed out" });
	}

	this.timeout = setTimeout(timeout, 15000);

	var self = this;
    exec(function(result)
	{
		if(result === false)
		{
			clearTimeout(self.timeout);
			onDiscoveryFinished();
		}
		else
		{
			onDeviceDiscovered(result);
		}
	}, 
	function(error)
	{	
		clearTimeout(self.timeout);
		onError(error);
	}, 
	"Bluetooth", "startDiscovery", []);
}

/**
 * Stop the device discovery process.
 *
 * @memberOf Bluetooth
 * 
 * @param  {Bluetooth~onSuccess} 	onSuccess 	Invoked on succesful completion.
 * @param  {Bluetooth~onError} 		onError 	Invoked if there is an error (for example there is no discovery in progress).
 */
Bluetooth.prototype.stopDiscovery = function(onSuccess, onError)
{
	exec(onSuccess, onError, "Bluetooth", "stopDiscovery", []);
}

/**
 * Check if the device at given address is paired with this device.
 *
 * @memberOf Bluetooth
 * 
 * @param  {Bluetooth~onResult} onSuccess 	Invoked with a flag indicating if the device is paired, passed in as a parameter.
 * @param  {Bluetooth~onError} 	onError 	Invoked if there is an error (for example wrong address).
 * @param  {string} 			address		Address of the device to check against.
 */
Bluetooth.prototype.isPaired = function(onSuccess, onError, address)
{
	exec(onSuccess, onError, "Bluetooth", "isPaired", [address]);
}

/**
 * Invoked when a device is paired with this device.
 *
 * @callback Bluetooth~onDevicePaired
 *
 * @param  {Bluetooth~BluetoothDevice}  device  The device which this device was paired with.
 */

/**
 * Attempt to pair with the device at given address.
 *
 * @memberOf Bluetooth
 * 
 * @param  {Bluetooth~onDevicePaired} 	onSuccess 	Invoked when the device at given address is paired.
 * @param  {Bluetooth~onError} 			onError 	Invoked if there is an error (for example wrong address).
 * @param  {string} 					address 	Address of the device to pair with.
 */
Bluetooth.prototype.pair = function(onSuccess, onError, address)
{
	exec(onSuccess, onError, "Bluetooth", "pair", [address]);
}

/**
 * Unpair with the device at given address.
 *
 * @memberOf Bluetooth
 * 
 * @param  {Bluetooth~onSuccess} 	onSuccess 	Invoked when unpairing was succesful.
 * @param  {Bluetooth~onError} 		onError 	Invoked when there is an error (for example the devices are not paired).
 * @param  {string} 				address 	The address of the device you wish to unpair with.
 */
Bluetooth.prototype.unpair = function(onSuccess, onError, address)
{
	exec(onSuccess, onError, "Bluetooth", "unpair", [address]);
}

/**
 * Invoked when paired devices are succesfully retrieved.
 *
 * @callback Bluetooth~onDevicesRetrieved
 *
 * @param  {Array<Bluetooth~BluetoothDevice>}  devices  The paired devices.
 */

/**
 * Get the devices paired with this device.
 *
 * @memberOf Bluetooth
 * 
 * @param  {Bluetooth~onDevicesRetrieved} 	onSuccess 	Invoked when the paired devices are retrieved.
 * @param  {Bluetooth~onError} 				onError 	Invoked if there is an error retrieving the paired devices.
 */
Bluetooth.prototype.getPaired = function(onSuccess, onError)
{
	exec(onSuccess, onError, "Bluetooth", "getPaired", []);
}

/**
 * Callback for retrieving UUIDs of a given device.
 *
 * @callback Bluetooth~onUuidsRetrieved
 *
 * @param  {object}  	device 			Information of the device, including the UUIDs.
 * @param  {string}  	device.name 	Name of the device.
 * @param  {string}  	device.address 	Hardware address of the device.
 * @param  {string[]} 	device.uuids 	UUIDs fetched from the device.
 */

/**
 * Get the UUIDs of the device at given address.
 *
 * @memberOf Bluetooth
 * 
 * @param  {Bluetooth~onUuidsRetrieved} onSuccess 	Invoked when UUIDs are fetched.
 * @param  {Bluetooth~onError}	 		onError 	Invoked if there is an error (for example invalid address).
 * @param  {string} 					address 	Address of the device you wish to perform the query on.
 */
Bluetooth.prototype.getUuids = function(onSuccess, onError, address)
{
	exec(onSuccess, onError, "Bluetooth", "getUuids", [address]);
}

/**
 * Check if there is a connection. Please note this does not indicate
 * whether the connection is managed or not.
 *
 * @memberOf Bluetooth
 * 
 * @param  {Bluetooth~onResult} 	onSuccess 	Invoked with a flag indicating whether there is a connection or not.
 * @param  {Bluetooth~onError} 		onError 	Invoked if there is an error.
 */
Bluetooth.prototype.isConnected = function(onSuccess, onError)
{
	exec(onSuccess, onError, "Bluetooth", "isConnected", []);
}

/**
 * Check if there is a connection and that it is currently managed.
 * This means that you will receive callbacks with read data, and
 * that you can write to the connection.
 *
 * @memberOf Bluetooth
 * 
 * @param  {Bluetooth~onResult} onSuccess  Invoked with a flag indicating whether the connection is managed or not.
 * @param  {Bluetooth~onError} 	onError    Invoked when there is an error. 
 *
 * @see Bluetooth~onDataRead
 * @see write
 */
Bluetooth.prototype.isConnectionManaged = function(onSuccess, onError)
{
	exec(onSuccess, onError, "Bluetooth", "isConnectionManaged", []);
}

/**
 * Attempt to connect with another device.
 *
 * @memberOf Bluetooth
 * 
 * @param  {Bluetooth~onSuccess} 	onSuccess  		Invoked when the connection is established. 
 * @param  {Bluetooth~onError} 		onError    		Invoked if there is an error while connecting (for example invalid address).
 * @param  {json} 					opts 	   		Options for the connection.
 * @param  {string}     			opts.address 	Target address.
 * @param  {string}     			opts.uuid 		Usually the target listens using some UUID, this is that UUID.
 * @param  {string}					[opts.conn] 	Type of connection, Secure by default.
 */
Bluetooth.prototype.connect = function(onSuccess, onError, opts)
{
	var conn = (typeof opts.conn === "undefined") ? "Secure" : opts.conn;
	
	exec(onSuccess, onError, "Bluetooth", "connect", [opts.address, opts.uuid, conn]);
}

/**
 * Disconnect from any current connection.
 *
 * @memberOf Bluetooth
 * 
 * @param  {Bluetooth~onSuccess} 	onSuccess 	Invoked if disconnecting was succesful.
 * @param  {Bluetooth~onError} 		onError 	Invoked if there was an error disconnecting (for example no connection).
 */
Bluetooth.prototype.disconnect = function(onSuccess, onError)
{
	exec(onSuccess, onError, "Bluetooth", "disconnect", []);
}

/**
 * Callback for a managed connection to send read data.
 *
 * @callback Bluetooth~onDataRead
 *
 * @param  {string}  data  The data received from the managed connection.
 */

/**
 * Start managing the connection, allowing reading and writing.
 *
 * @memberOf Bluetooth
 * 
 * @param  {Bluetooth~onDataRead} 	onDataRead 	Invoked when data is received from the managed connection.
 * @param  {Bluetooth~onError} 		onError 	Invoked if there is an error with the managed connection (connection lost, error reading data).
 *
 * @see stopConnectionManager
 */
Bluetooth.prototype.startConnectionManager = function(onDataRead, onError)
{
	exec(onDataRead, onError, "Bluetooth", "startConnectionManager", []);
}

/**
 * Stop the managed connection. Please note that this does not invoke disconnect,
 * so you can reinvoke startConnectionManager without having to invoke connect first.
 *
 * @memberOf Bluetooth
 * 
 * @param  {Bluetooth~onSuccess} 	onSuccess 	Invoked if stopping the managed connection was succesful.
 * @param  {Bluetooth~onError} 		onError 	Invoked if there was an error stopping the managed connection.
 */
Bluetooth.prototype.stopConnectionManager = function(onSuccess, onError)
{
	exec(onSuccess, onError, "Bluetooth", "stopConnectionManager", []);
}

/**
 * Write to the managed connection.
 *
 * @memberOf Bluetooth
 *
 * @param  {Bluetooth~onSuccess}  	onSuccess 	Invoked if writing was succesful.
 * @param  {Bluetooth~onError} 		onError 	Invoked if there was an error writing (for example there is no managed connection).
 * @param  {?} 						data 		The data to be written to the managed connection.
 */
Bluetooth.prototype.write = function(onSuccess, onError, data, encoding, forceString)
{
	encoding = encoding || "UTF-8";
	forceString = forceString || false;

	exec(onSuccess, onError, "Bluetooth", "write", [data, encoding, forceString]);
}

var bluetooth 	= new Bluetooth();
module.exports 	= bluetooth;
