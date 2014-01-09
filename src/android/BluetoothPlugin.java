package org.apache.cordova.bluetooth;

import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.util.Log;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.annotation.TargetApi;


/**
 * Bluetooth interface for Cordova 2.6.0 (PhoneGap).
 * 
 * @version 	0.9.1
 * @author  	Taneli Hartikainen
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class BluetoothPlugin extends CordovaPlugin 
{	
	private static final String LOG_TAG					= "BluetoothPlugin";
	
	private static final String ACTION_IS_BT_ENABLED 	= "isEnabled";
	private static final String ACTION_ENABLE_BT		= "enable";
	private static final String ACTION_DISABLE_BT		= "disable";

	private static final String ACTION_IS_DISCOVERING	= "isDiscovering";
	private static final String ACTION_START_DISCOVERY	= "startDiscovery";
	private static final String ACTION_STOP_DISCOVERY	= "stopDiscovery";
	
	private static final String ACTION_IS_PAIRED		= "isPaired";
	private static final String ACTION_PAIR				= "pair";
	private static final String ACTION_UNPAIR			= "unpair";
	
	private static final String ACTION_GET_PAIRED		= "getPaired";
	private static final String ACTION_GET_UUIDS		= "getUuids";
	
	private static final String ACTION_IS_CONNECTED		= "isConnected";
	private static final String ACTION_IS_READING		= "isConnectionManaged";
	private static final String ACTION_CONNECT 			= "connect";
	private static final String ACTION_DISCONNECT 		= "disconnect";
	
	private	static final String ACTION_START_READING	= "startConnectionManager";
	private	static final String ACTION_STOP_READING		= "stopConnectionManager";
	
	private static final String ACTION_WRITE			= "write";

	/**
	 * Bluetooth interface
	 */
	private BluetoothWrapper _bluetooth;
	
	/**
	 * Callback context for device discovery actions.
	 */
	private CallbackContext _discoveryCallback;
	
	/**
	 * Callback context for pairing devices.
	 */
	private CallbackContext _pairingCallback;
	
	/**
	 * Callback context for fetching UUIDs.
	 */
	private CallbackContext	_uuidCallback;
	
	/**
	 * Callback context for the asynchronous connection attempt.
	 */
	private CallbackContext _connectCallback;
	
	/**
	 * Callback context for the asynchronous (and continuous) read operation.
	 */
	private CallbackContext _ioCallback;
	
	/**
	 * Is set to true when a discovery process is canceled or a new one is started when
	 * there is a discovery process still in progress (cancels the old one).
	 */
	private boolean _wasDiscoveryCanceled;
	
	
	/**
	 * Initialize the Plugin, Cordova handles this.
	 * 
	 * @param cordova	Used to get register Handler with the Context accessible from this interface 
	 * @param view		Passed straight to super's initialization.
	 */
	public void initialize(CordovaInterface cordova, CordovaWebView view)
	{
		super.initialize(cordova, view);

		_bluetooth = new BluetoothWrapper(cordova.getActivity().getBaseContext(), _handler);
		_wasDiscoveryCanceled = false;
	}

	/**
	 * Executes the given action.
	 * 
	 * @param action		The action to execute.
	 * @param args			Potential arguments.
	 * @param callbackCtx	Babby call home.
	 */
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackCtx)
	{	
		if(ACTION_IS_BT_ENABLED.equals(action))
		{
			isEnabled(args, callbackCtx);
		}
		else if(ACTION_ENABLE_BT.equals(action))
		{
			enable(args, callbackCtx);
		}
		else if(ACTION_DISABLE_BT.equals(action))
		{
			disable(args, callbackCtx);
		}
		else if(ACTION_IS_DISCOVERING.equals(action))
		{
			isDiscovering(args, callbackCtx);
		}
		else if(ACTION_START_DISCOVERY.equals(action))
		{
			startDiscovery(args, callbackCtx);
		}
		else if(ACTION_STOP_DISCOVERY.equals(action))
		{
			stopDiscovery(args, callbackCtx);
		}
		else if(ACTION_IS_PAIRED.equals(action))
		{
			isPaired(args, callbackCtx);
		}
		else if(ACTION_PAIR.equals(action))
		{
			pair(args, callbackCtx);
		}
		else if(ACTION_UNPAIR.equals(action))
		{
			unpair(args, callbackCtx);
		}
		else if(ACTION_GET_PAIRED.equals(action))
		{
			getPaired(args, callbackCtx);
		}
		else if(ACTION_GET_UUIDS.equals(action))
		{
			getUuids(args, callbackCtx);
		}
		else if(ACTION_IS_CONNECTED.equals(action))
		{
			isConnected(args, callbackCtx);
		}
		else if(ACTION_CONNECT.equals(action))
		{
			connect(args, callbackCtx);
		}
		else if(ACTION_DISCONNECT.equals(action))
		{
			disconnect(args, callbackCtx);
		}
		else if(ACTION_IS_READING.equals(action))
		{
			isConnectionManaged(args, callbackCtx);
		}
		else if(ACTION_START_READING.equals(action))
		{
			startConnectionManager(args, callbackCtx);
		}
		else if(ACTION_STOP_READING.equals(action))
		{
			stopConnectionManager(args, callbackCtx);
		}
		else if(ACTION_WRITE.equals(action))
		{
			write(args, callbackCtx);
		}
		else
		{
			Log.e(LOG_TAG, "Invalid Action[" + action + "]");
			callbackCtx.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
		}
		
		return true;
	}
	
	/**
	 * Send an error to given CallbackContext containing the error code and message.
	 * 
	 * @param ctx	Where to send the error.
	 * @param msg	What seems to be the problem.
	 * @param code	Integer value as a an error "code"
	 */
	private void error(CallbackContext ctx, String msg, int code)
	{
		try
		{
			JSONObject result = new JSONObject();
			result.put("message", msg);
			result.put("code", code);
			
			ctx.error(result);
		}
		catch(Exception e)
		{
			Log.e(LOG_TAG, "Error with... error raising, " + e.getMessage());
		}
	}
	
	/**
	 * Is Bluetooth on.
	 * 
	 * @param args			Arguments given.
	 * @param callbackCtx	Where to send results.
	 */
	private void isEnabled(JSONArray args, CallbackContext callbackCtx)
	{
		try 
		{
			callbackCtx.sendPluginResult(new PluginResult(PluginResult.Status.OK, _bluetooth.isEnabled()));
		} 
		catch(Exception e) 
		{
			this.error(callbackCtx, e.getMessage(), BluetoothError.ERR_UNKNOWN);
		}
	}
	
	/**
	 * Turn Bluetooth on.
	 * 
	 * @param args			Arguments given.
	 * @param callbackCtx	Where to send results.
	 */
	private void enable(JSONArray args, CallbackContext callbackCtx)
	{
		// TODO Add options to enable with Intent
		
		try
		{
			_bluetooth.enable();
			callbackCtx.success();
		}
		catch(Exception e)
		{
			this.error(callbackCtx, e.getMessage(), BluetoothError.ERR_UNKNOWN);
		}
	}

	/**
	 * Turn Bluetooth off.
	 * 
	 * @param args			Arguments given.
	 * @param callbackCtx	Where to send results.
	 */
	private void disable(JSONArray args, CallbackContext callbackCtx)
	{
		try
		{
			_bluetooth.disable();
			callbackCtx.success();
		}
		catch(Exception e)
		{
			this.error(callbackCtx, e.getMessage(), BluetoothError.ERR_UNKNOWN);
		}
	}

	/**
	 * See if a device discovery process is in progress.
	 * 
	 * @param args			Arguments given.
	 * @param callbackCtx	Where to send results.
	 */
	private void isDiscovering(JSONArray args, CallbackContext callbackCtx)
	{
		try 
		{
			callbackCtx.sendPluginResult(new PluginResult(PluginResult.Status.OK, _bluetooth.isDiscovering()));
		} 
		catch(Exception e) 
		{
			this.error(callbackCtx, e.getMessage(), BluetoothError.ERR_UNKNOWN);
		}
	}

	/**
	 * Start a device discovery.
	 * 
	 * @param args			Arguments given.
	 * @param callbackCtx	Where to send results.
	 */
	private void startDiscovery(JSONArray args, CallbackContext callbackCtx)
	{
		// TODO Someday add an option to fetch UUIDs at the same time
		
		try
		{
			if(_bluetooth.isConnecting())
			{
				this.error(callbackCtx, "A Connection attempt is in progress.", BluetoothError.ERR_CONNECTING_IN_PROGRESS);
			}
			else
			{
				if(_bluetooth.isDiscovering())
				{
					_wasDiscoveryCanceled = true;
					_bluetooth.stopDiscovery();
					
					if(_discoveryCallback != null)
					{
						this.error(_discoveryCallback, 
							"Discovery was stopped because a new discovery was started.", 
							BluetoothError.ERR_DISCOVERY_RESTARTED
						);
						_discoveryCallback = null;
					}
				}
				
				_bluetooth.startDiscovery();
				
				PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
				result.setKeepCallback(true);
				callbackCtx.sendPluginResult(result);
	
				_discoveryCallback = callbackCtx;
			}
		}
		catch(Exception e)
		{
			this.error(callbackCtx, e.getMessage(), BluetoothError.ERR_UNKNOWN);
		}
	}

	/**
	 * Stop device discovery.
	 * 
	 * @param args			Arguments given.
	 * @param callbackCtx	Where to send results.
	 */
	private void stopDiscovery(JSONArray args, CallbackContext callbackCtx)
	{
		try
		{
			if(_bluetooth.isDiscovering())
			{
				_wasDiscoveryCanceled = true;
				_bluetooth.stopDiscovery();
				
				if(_discoveryCallback != null)
				{
					this.error(_discoveryCallback, 
						"Discovery was cancelled.", 
						BluetoothError.ERR_DISCOVERY_CANCELED
					);
					
					_discoveryCallback = null;
				}
				
				callbackCtx.success();
			}
			else
			{
				this.error(callbackCtx, "There is no discovery to cancel.", BluetoothError.ERR_UNKNOWN);
			}
		}
		catch(Exception e)
		{
			this.error(callbackCtx, e.getMessage(), BluetoothError.ERR_UNKNOWN);
		}
	}

	/**
	 * See if the device is paired with the device in the given address.
	 * 
	 * @param args			Arguments given. First argument should be the address in String format.
	 * @param callbackCtx	Where to send results.
	 */
	private void isPaired(JSONArray args, CallbackContext callbackCtx)
	{
		try
		{
			String address = args.getString(0);
			callbackCtx.sendPluginResult(new PluginResult(PluginResult.Status.OK, _bluetooth.isBonded(address)));
		}
		catch(Exception e)
		{
			this.error(callbackCtx, e.getMessage(), BluetoothError.ERR_UNKNOWN);
		}
	}

	/**
	 * Pair the device with the device in the given address.
	 * 
	 * @param args			Arguments given. First argument should be the address in String format.
	 * @param callbackCtx	Where to send results.
	 */
	private void pair(JSONArray args, CallbackContext callbackCtx)
	{
		// TODO Add a timeout function for pairing
		
		if(_pairingCallback != null)
		{
			this.error(callbackCtx, "Pairing process is already in progress.", BluetoothError.ERR_PAIRING_IN_PROGRESS);
		}
		else
		{
			try
			{
				String address = args.getString(0);
				_bluetooth.createBond(address);
				_pairingCallback = callbackCtx;
			}
			catch(Exception e)
			{
				_pairingCallback = null;
				this.error(callbackCtx, e.getMessage(), BluetoothError.ERR_UNKNOWN);
			}
		}
	}

	/**
	 * Unpair with the device in the given address.
	 * 
	 * @param args			Arguments given. First argument should be the address in String format.
	 * @param callbackCtx	Where to send results.
	 */
	private void unpair(JSONArray args, CallbackContext callbackCtx)
	{
		try
		{
			String address = args.getString(0);
			_bluetooth.removeBond(address);
			callbackCtx.success();
		}
		catch(Exception e)
		{
			this.error(callbackCtx, e.getMessage(), BluetoothError.ERR_UNKNOWN);
		}
	}
	

	/**
	 * Get the devices paired with this device.
	 * 
	 * @param args			Arguments given.
	 * @param callbackCtx	Where to send results.
	 */
	private void getPaired(JSONArray args, CallbackContext callbackCtx)
	{
		try
		{
			JSONArray devices 						= new JSONArray();
			ArrayList<Pair<String>> bondedDevices 	= _bluetooth.getBondedDevices();
			
			for(Pair<String> deviceInfo : bondedDevices)
			{
				JSONObject device = new JSONObject();
				device.put("name", deviceInfo.a);
				device.put("address", deviceInfo.b);
				devices.put(device);
			}
			
			callbackCtx.success(devices);
		}
		catch(Exception e)
		{
			this.error(callbackCtx, e.getMessage(), BluetoothError.ERR_UNKNOWN);
		}
	}

	/**
	 * Get the UUID(s) of the device at given address.
	 * 
	 * @param args			Arguments given. First argument should be the address in String format.
	 * @param callbackCtx	Where to send results.
	 */
	private void getUuids(JSONArray args, CallbackContext callbackCtx)
	{
		if(_uuidCallback != null)
		{
			this.error(callbackCtx, 
				"Could not start UUID fetching because there is already one in progress.", 
				BluetoothError.ERR_UUID_FETCHING_IN_PROGRESS
			);
		}
		else
		{
			try
			{
				String address = args.getString(0);
				_bluetooth.fetchUuids(address);
				_uuidCallback = callbackCtx;
				
			}
			catch(Exception e)
			{
				_uuidCallback = null;
				this.error(callbackCtx, e.getMessage(), BluetoothError.ERR_UNKNOWN);
			}
		}
	}

	/**
	 * See if we have a connection.
	 * 
	 * @param args			Arguments given.
	 * @param callbackCtx	Where to send results.
	 */
	private void isConnected(JSONArray args, CallbackContext callbackCtx)
	{
		try 
		{	
			callbackCtx.sendPluginResult(new PluginResult(PluginResult.Status.OK, _bluetooth.isConnected()));
		} 
		catch(Exception e)
		{
			this.error(callbackCtx, e.getMessage(), BluetoothError.ERR_UNKNOWN);
		}
	}

	/**
	 * Attempt to connect to a device.
	 * 
	 * @param args			Arguments given. [Address, UUID, ConnectionType(Secure, Insecure, Hax)], String format.
	 * @param callbackCtx	Where to send results.
	 */
	private void connect(JSONArray args, CallbackContext callbackCtx)
	{
		boolean isConnecting 	= _bluetooth.isConnecting();
		boolean isConnected		= _bluetooth.isConnected(); 
		
		if(isConnecting)
		{
			this.error(callbackCtx, "There is already a connection attempt in progress.", BluetoothError.ERR_CONNECTING_IN_PROGRESS);
		}
		else if(isConnected)
		{
			this.error(callbackCtx, "There is already a connection in progress.", BluetoothError.ERR_CONNECTION_ALREADY_EXISTS);
		}
		else
		{
			try
			{
				if(_bluetooth.isDiscovering())
				{
					_wasDiscoveryCanceled = true;
					_bluetooth.stopDiscovery();
					
					if(_discoveryCallback != null)
					{
						this.error(_discoveryCallback, "Discovery stopped because a connection attempt was started.", BluetoothError.ERR_DISCOVERY_CANCELED);
					}
				}
			
				String address 		= args.getString(0);
				String uuid			= args.getString(1);
				String connTypeStr	= args.getString(2);
				
				_bluetooth.connect(address, uuid, connTypeStr);
				
				PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
				result.setKeepCallback(true);
				callbackCtx.sendPluginResult(result);
				
				_connectCallback = callbackCtx;
			}
			catch(Exception e)
			{
				_connectCallback = null;
				
				this.error(callbackCtx, e.getMessage(), BluetoothError.ERR_UNKNOWN);
			}
		}
	}

	/**
	 * Disconnect from the device currently connected to.
	 * 
	 * @param args			Arguments given.
	 * @param callbackCtx	Where to send results.
	 */
	private void disconnect(JSONArray args, CallbackContext callbackCtx)
	{
		try 
		{
			_bluetooth.disconnect();
			callbackCtx.success();
		} 
		catch(Exception e) 
		{
			this.error(callbackCtx, e.getMessage(), BluetoothError.ERR_UNKNOWN);
		}
	}

	/**
	 * See if we have a managed connection active (allows read/write).
	 * 
	 * @param args			Arguments given.
	 * @param callbackCtx	Where to send results.
	 */
	private void isConnectionManaged(JSONArray args, CallbackContext callbackCtx)
	{
		callbackCtx.sendPluginResult(new PluginResult(PluginResult.Status.OK, _bluetooth.isConnectionManaged()));
	}

	/**
	 * Start a managed connection, allowing read and write operations.
	 * 
	 * @param args			Arguments given.
	 * @param callbackCtx	Where to send results.
	 */
	private void startConnectionManager(JSONArray args, CallbackContext callbackCtx)
	{
		if(_ioCallback != null)
		{
			this.error(callbackCtx, "There is already an active connection.", BluetoothError.ERR_CONNECTION_ALREADY_EXISTS);
		}
		else
		{
			try
			{
				_bluetooth.startConnectionManager();
				_ioCallback = callbackCtx;
			}
			catch(Exception e)
			{
				_ioCallback = null;
				this.error(callbackCtx, e.getMessage(), BluetoothError.ERR_UNKNOWN);
			}
		}
	}
	
	/**
	 * Stop the managed connection, preventing further read or write operations.
	 * 
	 * @param args			Arguments given.
	 * @param callbackCtx	Where to send results.
	 */
	private void stopConnectionManager(JSONArray args, CallbackContext callbackCtx)
	{
		try
		{
			if(_bluetooth.isConnectionManaged())
			{
				_bluetooth.stopConnectionManager();
				callbackCtx.success();
			}
			else
			{
				this.error(callbackCtx, 
					"There is no connection being managed.", 
					BluetoothError.ERR_CONNECTION_DOESNT_EXIST
				);
			}
		}
		catch(Exception e)
		{
			this.error(callbackCtx, e.getMessage(), BluetoothError.ERR_UNKNOWN);
		}
	}

	/**
	 * Write given data to the managed connection.
	 * 
	 * @param args			Arguments given. First argument should be the data you want to write.
	 * @param callbackCtx	Where to send results.
	 */
	private void write(JSONArray args, CallbackContext callbackCtx)
	{
		Log.d(LOG_TAG, "write-method called");
		
		try 
		{
			Object data 		= args.get(0);
			String encoding 	= args.getString(1);
			boolean forceString = args.getBoolean(2); 
			
			byte[] defaultBytes = new byte[4];
			ByteBuffer buffer = ByteBuffer.wrap(defaultBytes);
			
			if(forceString || data.getClass() == String.class)
			{
				String dataString = (String)data;
				buffer = ByteBuffer.wrap(dataString.getBytes(encoding));
			}
			else if(data.getClass().equals(Integer.class))
			{	
				byte[] bytes = new byte[4];
				buffer = ByteBuffer.wrap(bytes);
				buffer.putInt((Integer)data);
			}
			else if(data.getClass().equals(Double.class))
			{
				byte[] bytes = new byte[8];
				buffer = ByteBuffer.wrap(bytes);
				buffer.putDouble((Double)data);
			}
			else
			{
				this.error(callbackCtx, "Unknown data-type", BluetoothError.ERR_UNKNOWN);
				return;
			}
			
			if(!_bluetooth.isConnected())
			{
				this.error(callbackCtx, "There is no managed connection to write to.", BluetoothError.ERR_CONNECTION_DOESNT_EXIST);
			}
			else
			{
				buffer.rewind();
				_bluetooth.write(buffer.array());
				callbackCtx.success();
			}
		} 
		catch (Exception e) 
		{
			this.error(callbackCtx, e.getMessage(), BluetoothError.ERR_UNKNOWN);
		}
	}
	
	/**
	 * Handle messages from BluetoothWrapper. BluetoothWrapper does a lot of asynchronous
	 * work, so the main way of communicating between BluetoothPlugin and BluetoothWrapper 
	 * is to use callback Messages.
	 * 
	 * @see Handler
	 * @see Message
	 * @see BluetoothWrapper
	 */
	private final Handler _handler = new Handler(new Handler.Callback() 
	{	
		@Override
		public boolean handleMessage(Message msg) 
		{	
			switch(msg.what)
			{
				case BluetoothWrapper.MSG_DISCOVERY_STARTED:
					
					_wasDiscoveryCanceled = false;
					
					break;
					
				case BluetoothWrapper.MSG_DISCOVERY_FINISHED:
					
					if(!_wasDiscoveryCanceled)
					{
						if(_discoveryCallback != null)
						{
							PluginResult result = new PluginResult(PluginResult.Status.OK, false);
							_discoveryCallback.sendPluginResult(result);
							_discoveryCallback = null;
						}
					}
					
					break;

				case BluetoothWrapper.MSG_DEVICE_FOUND:
					
					try
					{
						String name 	= msg.getData().getString(BluetoothWrapper.DATA_DEVICE_NAME);
						String address 	= msg.getData().getString(BluetoothWrapper.DATA_DEVICE_ADDRESS);
						
						JSONObject device = new JSONObject();
						device.put("name", name);
						device.put("address", address);
						
						// Send one device at a time, keeping callback to be used again
						if(_discoveryCallback != null)
						{
							PluginResult result = new PluginResult(PluginResult.Status.OK, device);
							result.setKeepCallback(true);
							_discoveryCallback.sendPluginResult(result);
						}
						else
						{
							Log.e(LOG_TAG, "CallbackContext for discovery doesn't exist.");
						}
					}
					catch(JSONException e)
					{
						if(_discoveryCallback != null)
						{
							BluetoothPlugin.this.error(_discoveryCallback,
								e.getMessage(),
								BluetoothError.ERR_UNKNOWN
							);
							_discoveryCallback = null;
						}
					}

					break;

				case BluetoothWrapper.MSG_UUIDS_FOUND:
					
					try
					{
						if(_uuidCallback != null)
						{
							String name 			= msg.getData().getString(BluetoothWrapper.DATA_DEVICE_NAME);
							String address 			= msg.getData().getString(BluetoothWrapper.DATA_DEVICE_ADDRESS);
							ArrayList<String> uuids = msg.getData().getStringArrayList(BluetoothWrapper.DATA_UUIDS);
							
							JSONObject deviceInfo = new JSONObject();
							JSONArray deviceUuids = new JSONArray(uuids);
							
							deviceInfo.put("name", name);
							deviceInfo.put("address", address);
							deviceInfo.put("uuids", deviceUuids);
							
							_uuidCallback.success(deviceInfo);
							_uuidCallback = null;
						}
						else
						{
							Log.e(LOG_TAG, "CallbackContext for uuid fetching doesn't exist.");
						}	
					}
					catch(Exception e)
					{
						if(_uuidCallback != null)
						{
							BluetoothPlugin.this.error(_uuidCallback,
								e.getMessage(), BluetoothError.ERR_UNKNOWN
							);
							_uuidCallback = null;
						}
					}
					
					break;
					
				case BluetoothWrapper.MSG_DEVICE_BONDED:
					
					try
					{
						String name 	= msg.getData().getString(BluetoothWrapper.DATA_DEVICE_NAME);
						String address 	= msg.getData().getString(BluetoothWrapper.DATA_DEVICE_ADDRESS);
						
						JSONObject bondedDevice = new JSONObject();
						bondedDevice.put("name", name);
						bondedDevice.put("address", address);
						
						if(_pairingCallback != null)
						{
							_pairingCallback.success(bondedDevice);
							_pairingCallback = null;
						}
						else
						{
							Log.e(LOG_TAG, "CallbackContext for pairing doesn't exist.");
						}	
					}
					catch(Exception e)
					{
						if(_pairingCallback != null)
						{
							BluetoothPlugin.this.error(_pairingCallback, 
								e.getMessage(), BluetoothError.ERR_PAIRING_FAILED
							);
							_pairingCallback = null;
						}
					}
					
					break;
					
				case BluetoothWrapper.MSG_CONNECTION_ESTABLISHED:
					
					if(_connectCallback != null)
					{
						_connectCallback.success();
						_connectCallback = null;
					}
					else
					{
						Log.e(LOG_TAG, "CallbackContext for connection doesn't exist.");
					}
					
					break;
					
				case BluetoothWrapper.MSG_CONNECTION_FAILED:

					String error = msg.getData().getString(BluetoothWrapper.DATA_ERROR);
					
					if(_connectCallback != null)
					{
						BluetoothPlugin.this.error(_connectCallback, 
							error, BluetoothError.ERR_CONNECTING_FAILED
						);
						_connectCallback = null;
					}
					else
					{
						Log.e(LOG_TAG, "CallbackContext for connection doesn't exist.");
					}
					
					break;
					
				case BluetoothWrapper.MSG_CONNECTION_LOST:
					
					if(_connectCallback != null)
					{
						BluetoothPlugin.this.error(_connectCallback,
							"Connection lost.", BluetoothError.ERR_CONNECTION_LOST
						);
						_connectCallback = null;
					}
					
					if(_ioCallback != null)
					{
						BluetoothPlugin.this.error(_ioCallback,
							"Connection lost.", BluetoothError.ERR_CONNECTION_LOST
						);
						_ioCallback = null;
					}
					
					break;
					
				case BluetoothWrapper.MSG_CONNECTION_STOPPED:
					
					if(_connectCallback != null)
					{
						BluetoothPlugin.this.error(_connectCallback,
							"Disconnected.", BluetoothError.ERR_DISCONNECTED
						);
						_connectCallback = null;
					}
					
					if(_ioCallback != null)
					{
						BluetoothPlugin.this.error(_ioCallback,
							"Disconnected.", BluetoothError.ERR_DISCONNECTED
						);
						_ioCallback = null;
					}
					
					break;
					
				case BluetoothWrapper.MSG_READ:
					
					String data = new String(
						msg.getData().getByteArray(BluetoothWrapper.DATA_BYTES), 
						Charset.forName("UTF-8")
					);
					
					if(_ioCallback != null)
					{
						PluginResult result = new PluginResult(PluginResult.Status.OK, data);
						result.setKeepCallback(true);
						_ioCallback.sendPluginResult(result);
					}
					else
					{
						Log.e(LOG_TAG, "CallbackContext for IO doesn't exist.");
					}
					
					break;
					
				case BluetoothWrapper.MSG_BLUETOOTH_LOST:
					
					if(_discoveryCallback != null)
					{
						BluetoothPlugin.this.error(_discoveryCallback,
							"Bluetooth lost.", BluetoothError.ERR_BLUETOOTH_LOST
						);
						_discoveryCallback = null;
					}

					if(_pairingCallback != null)
					{
						BluetoothPlugin.this.error(_pairingCallback,
							"Bluetooth lost.", BluetoothError.ERR_BLUETOOTH_LOST
						);
						_pairingCallback = null;
					}
					
					if(_uuidCallback != null)
					{
						BluetoothPlugin.this.error(_uuidCallback,
							"Bluetooth lost.", BluetoothError.ERR_BLUETOOTH_LOST
						);
						_uuidCallback = null;
					}
					
					if(_connectCallback != null)
					{
						BluetoothPlugin.this.error(_connectCallback,
							"Bluetooth lost.", BluetoothError.ERR_BLUETOOTH_LOST
						);
						_connectCallback = null;
					}
					
					if(_ioCallback != null)
					{
						BluetoothPlugin.this.error(_ioCallback,
							"Bluetooth lost.", BluetoothError.ERR_BLUETOOTH_LOST
						);
						_ioCallback = null;
					}
					
					break;
					
				default:
					
					Log.e(LOG_TAG, "Message type could not be resolved.");
					
					break;
			}
			
			return true;
		}
	});
}
