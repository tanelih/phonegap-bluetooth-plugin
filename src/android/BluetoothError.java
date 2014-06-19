package org.apache.cordova.bluetooth;

/**
 * Container class for different error codes for Bluetooth.
 * These are the codes passed to the JavaScript API's onError
 * callback under <b>error.code</b>.
 *
 */
public class BluetoothError
{
	public static final int ERR_UNKNOWN							= 0;
	public static final int ERR_DISCOVERY_CANCELED 				= 1;
	public static final int ERR_DISCOVERY_RESTARTED				= 2;
	public static final int ERR_PAIRING_IN_PROGRESS				= 3;
	public static final int ERR_UUID_FETCHING_IN_PROGRESS		= 4;
	public static final int ERR_CONNECTION_ALREADY_EXISTS		= 5;
	public static final int ERR_CONNECTING_IN_PROGRESS			= 6;
	public static final int ERR_CONNECTION_DOESNT_EXIST			= 7;
	public static final int ERR_CONNECTION_LOST					= 8;
	public static final int ERR_CONNECTING_FAILED				= 9;
	public static final int ERR_PAIRING_FAILED					= 10;
	public static final int ERR_UUID_FETCH_FAILED				= 11;
	public static final int ERR_BLUETOOTH_LOST					= 12;
	public static final int ERR_MANAGED_CONNECTION_LOST			= 13;
	public static final int ERR_DISCONNECTED					= 14;
}
