package org.apache.cordova.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.reflect.Method;

import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;

import android.annotation.TargetApi;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothAdapter;

import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.AsyncTask;
import android.os.Parcelable;

import android.util.Log;


/**
 * Wrapper for the standard Bluetooth API found in Android. Contains threads for
 * connecting and managing a connection. Please note that this is designed as a
 * thin wrapper around Android's native Bluetooth API and doesn't contain any
 * internal checks against overwriting active connections with new ones etc.
 *
 * @see BluetoothAdapter
 * @see BluetoothDevice
 * @see BluetoothSocket
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
public class BluetoothWrapper 
{	
	private static final String LOG_TAG = "BluetoothWrapper";
	
	public static final int MSG_DISCOVERY_STARTED		= 0;
	public static final int MSG_DISCOVERY_FINISHED		= 1;
	public static final int MSG_DEVICE_FOUND			= 2;
	public static final int MSG_CONNECTION_ESTABLISHED	= 3;
	public static final int MSG_CONNECTION_FAILED		= 4;
	public static final int MSG_CONNECTION_STOPPED		= 5;
	public static final int MSG_CONNECTION_LOST			= 6;
	public static final int MSG_READ					= 8;
	public static final int MSG_BLUETOOTH_LOST			= 9;
	public static final int MSG_UUIDS_FOUND				= 10;
	public static final int MSG_DEVICE_BONDED			= 11;
	
	public static final String DATA_DEVICE_ADDRESS 		= "DeviceAddress";
	public static final String DATA_DEVICE_NAME			= "DeviceName";
	public static final String DATA_BYTES				= "Bytes";
	public static final String DATA_BYTES_READ			= "BytesRead";
	public static final String DATA_UUIDS				= "Uuids";
	public static final String DATA_ERROR				= "Error";
	
	/**
	 * Is used to send messages back to the user of this class.
	 * Message types are specified above with the prefix MSG
	 */
	private Handler 			_handler;			
	
	/**
	 * Android's BluetoothAdapter
	 */
	private BluetoothAdapter 	_adapter;
	
	/**
	 * Socket that is synchronized between threads to allow stopping ConnectionManager
	 * while retaining the connection itself.
	 */
	private BluetoothSocket		_socket;	
	
	/**
	 * Thread for attempting a connection. When successful, initializes a connected socket to the
	 * <b>_socket</b> member of BluetoothWrapper.
	 */
	private ConnectionAttempt	_connectionAttempt;
	
	/**
	 * Thread for managing an active connection. Requires a connected socket to perform
	 * read/write operations.
	 */
	private ConnectionManager 	_connectionManager;
	
	/**
	 * Enumeration for various types of connections we can attempt.
	 * 
	 * @see BluetoothDevice
	 * @see BluetoothSocket
	 */
	public enum EConnectionType
	{
		/**
		 * Create a standard secure connection.
		 */
		Secure,
		
		/**
		 * Create a standard insecure connection.
		 */
		Insecure,
		
		/**
		 * Use reflection to create the socket, seems very volatile.
		 */
		Hax
	}
	
	/**
	 * Constructor for the BluetoothWrapper class. Registers correct receivers for Bluetooth events.
	 * 
	 * @param ctx       Application context, used to register receiver for various bluetooth related events.
	 * @param handler	A Handler that is sent Messages using the codes specified in this class.
	 * 
	 * @see Context
	 * @see Handler
	 * @see Message
	 */
	public BluetoothWrapper(Context ctx, Handler handler)
	{
		_handler = handler;
		_adapter = BluetoothAdapter.getDefaultAdapter();
		
		IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
		ctx.registerReceiver(_receiver, filter);
		
		filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		ctx.registerReceiver(_receiver, filter);
		
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		ctx.registerReceiver(_receiver, filter);
		
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		ctx.registerReceiver(_receiver, filter);
		
		filter = new IntentFilter(BluetoothDevice.ACTION_FOUND); 
		ctx.registerReceiver(_receiver, filter);
		
		filter = new IntentFilter(BluetoothDevice.ACTION_UUID);
		ctx.registerReceiver(_receiver, filter);
	}
	
	/**
	 * Check whether Bluetooth is on or off.
	 * 
	 * @return Flag indicating if Bluetooth is enabled on this device.
	 * @throws Exception When there is an error deducing adapter state.
	 */
	public boolean isEnabled() throws Exception
	{
		try
		{
			return _adapter.getState() == BluetoothAdapter.STATE_ON;
		}
		catch(Exception e)
		{	
			throw e;
		}
	}

	
	/**
	 * Enable Bluetooth without direct user consent. Careful!
	 * 
	 * @throws Exception When there is an error enabling Bluetooth.
	 */
	public void enable() throws Exception
	{
		try
		{
			if(!_adapter.enable())
			{
				if(isEnabled())
				{
					throw new Exception("Bluetooth is already on.");
				}
				else
				{
					throw new Exception("Error enabling Bluetooth.");
				}
			}
		}
		catch(Exception e)
		{
			throw e;
		}
	}

	
	/**
	 * Disable Bluetooth without direct user consent.
	 * 
	 * @throws Exception When there is an error disabling Bluetooth.
	 */
	public void disable() throws Exception
	{
		try
		{
			if(!_adapter.disable())
			{
				if(!isEnabled())
				{
					throw new Exception("Bluetooth is already off.");
				}
				else
				{
					throw new Exception("Error disabling Bluetooth.");
				}
			}
		}
		catch(Exception e)
		{
			throw e;
		}
	}

	
	/**
	 * See if there is an ongoing device discovery process going on.
	 * 
	 * @return True if Bluetooth is on and device discovery is in progress. Otherwise false.
	 * @throws Exception If there is an error checking whether the discovery process is in progress.
	 */
	public boolean isDiscovering() throws Exception
	{
		try
		{
			return _adapter.isEnabled() && _adapter.isDiscovering();
		}
		catch(Exception e)
		{
			throw e;
		}
	}

	/**
	 * Start a device discovery process. Results are broadcasted to the
	 * Handler registered to this class. This will not cancel any current
	 * discovery process, but you should do it anyways.
	 * 
	 * @throws Exception If there is an error starting the discovery process.
	 * 
	 * @see BluetoothDevice
	 */
	public void startDiscovery() throws Exception
	{
		try
		{
			if(!_adapter.startDiscovery())
			{
				throw new Exception("Error starting discovery.");
			}
		}
		catch(Exception e)
		{
			throw e;
		}
	}

	
	/**
	 * Cancel the current discovery process.
	 * 
	 * @throws Exception If there is an error with canceling the current discovery process.
	 */
	public void stopDiscovery() throws Exception
	{
		try
		{
			if(!_adapter.cancelDiscovery())
			{
				if(!_adapter.isDiscovering())
				{
					throw new Exception("There is no discovery process in progress.");
				}
				else
				{
					throw new Exception("Error canceling the discovery process.");
				}
			}
		}
		catch(Exception e)
		{
			throw e;
		}
	}

	
	/**
	 * Check if the device at given address is bonded with this device.
	 * 
	 * @param address The device we want to check against.
	 * @return Flag indicating whether the devices are bonded.
	 * @throws Exception If there is a problem deducing the bond state. A wrong address might also cause this. :)
	 * 
	 * @see BluetoothDevice
	 */
	public boolean isBonded(String address) throws Exception
	{
		try
		{
			BluetoothDevice device = _adapter.getRemoteDevice(address);
			return device.getBondState() == BluetoothDevice.BOND_BONDED;
		}
		catch(Exception e)
		{
			throw e;
		}
	}
	
	
	/**
	 * Get the devices bonded with this device.
	 * 
	 * @return a list of Pairs. Each pair contains members <b>a</b> and <b>b</b>: 
	 * <b>a</b> is the device name and <b>b</b> the device address.
	 * 
	 * @see Pair
	 */
	public ArrayList<Pair<String>> getBondedDevices()
	{
		Set<BluetoothDevice> bondedDevices 	= _adapter.getBondedDevices();
		ArrayList<Pair<String>> devices 	= new ArrayList<Pair<String>>();
		
		for(BluetoothDevice device : bondedDevices)
		{
			devices.add(new Pair<String>(device.getName(), device.getAddress()));
		}
		
		return devices;
	}
	
	
	/**
	 * Attempt to bond with the device at given address.
	 * 
	 * @param address The address of the device to bond with.
	 * @throws Exception If there is an error bonding with the device.
	 * 
	 * @see BluetoothDevice
	 */
	public void createBond(String address) throws Exception
	{
		try
		{
			BluetoothDevice device = _adapter.getRemoteDevice(address);
			if(device.getBondState() == BluetoothDevice.BOND_BONDED)
			{
				throw new Exception("The device is alraedy paired.");
			}
			
			Method createBond = device.getClass().getMethod("createBond");
			if(!(Boolean)createBond.invoke(device))
			{
				throw new Exception("Failed to start the bonding process with given device.");
			}
		}
		catch(Exception e)
		{
			throw e;
		}
	}
	
	
	/**
	 * Remove the bond between the device at given address and this device.
	 * 
	 * @param address The address of the device to be unbound.
	 * @throws Exception If there is an error removing the bond between this and that device.
	 * 
	 * @see BluetoothDevice
	 */
	public void removeBond(String address) throws Exception
	{
		try
		{	
			BluetoothDevice device = _adapter.getRemoteDevice(address);
			if(device.getBondState() != BluetoothDevice.BOND_BONDED)
			{
				throw new Exception("Device at given address is not bonded.");
			}
			
			Method removeBond = device.getClass().getMethod("removeBond");
			if(!(Boolean)removeBond.invoke(device))
			{
				throw new Exception("Failed to remove bond with given device.");
			}
		}
		catch(Exception e)
		{
			throw e;
		}
	}
	
	
	/**
	 * Fetch the UUID's of the device at given address.
	 * 
	 * @param address The address of the device to fetch UUIDs from.
	 * @throws Exception If there was an error starting the fetching process.
	 * 
	 * @see UUID
	 */
	public void fetchUuids(String address) throws Exception
	{
		try
		{
			BluetoothDevice device = _adapter.getRemoteDevice(address);
			if(!device.fetchUuidsWithSdp())
			{
				throw new Exception("Failed to start fetching UUIDs for the device at given address.");
			}
		}
		catch(Exception e)
		{
			throw e;
		}
	}
	
	/**
	 * Check if there is an ongoing connection attempt.
	 * 
	 * @return True if a connection attempt is in progress.
	 */
	public boolean isConnecting()
	{
		if(_connectionAttempt != null)
		{
			return _connectionAttempt.getStatus() == AsyncTask.Status.RUNNING;
		}
		return false;
	}
	
	/**
	 * Check if there is a connected socket.
	 * 
	 * @return A flag indicating whether there is a Connected Socket
	 *
	 * @see BluetoothSocket
	 */
	public boolean isConnected()
	{
		if(_socket != null)
		{
			synchronized(_socket)
			{
				return _socket.isConnected();	
			}
		}
		return false;
	}
	
	
	/**
	 * Check if there is a connected socket that is managed (allows read/write operations).
	 * 
	 * @return Flag indicating whether there is an active managed connection
	 */
	public boolean isConnectionManaged()
	{
		if(_connectionManager != null)
		{
			return _connectionManager.isAlive();
		}
		return false;
	}
	
	
	/**
	 * Attempts a connection to the specified address. Please note that this does not disconnect
	 * any current connections, and you have to do that manually. 
	 * 
	 * @param address The address of the device you want to connect to.
	 * @param uuidStr The UUID you want to connect with, or to.
	 * @param connTypeStr The type of connection you want to attempt.
	 * @throws Exception If there is an error starting the connection attempt.
	 * 
	 * @see ConnectionAttempt
	 * @see ConnectionManager
	 */
	public void connect(String address, String uuidStr, String connTypeStr) throws Exception
	{
		try
		{
			BluetoothDevice device 		= _adapter.getRemoteDevice(address);
			UUID uuid					= UUID.fromString(uuidStr); 
			EConnectionType connType 	= EConnectionType.valueOf(connTypeStr);
			
			_connectionAttempt = new ConnectionAttempt(device, uuid, connType);
			_connectionAttempt.execute();
		}
		catch(Exception e)
		{
			throw e;
		}
	}
	
	
	/**
	 * Attempts to disconnect the current connection. Closes the socket if it is open.
	 * 
	 * @throws Exception If there is an error disconnecting (no connection to close).
	 */
	public void disconnect() throws Exception
	{
		try
		{	
			if(isConnecting() || isConnected())
			{
				if(_connectionAttempt != null)
				{
					if(_connectionAttempt.getStatus() != AsyncTask.Status.FINISHED)
					{
						_connectionAttempt.cancel(true);
					}
				}
				
				if(_connectionManager != null)
				{
					if(_connectionManager.isAlive())
					{
						_connectionManager.kill();
					}
				}
			
				_handler.obtainMessage(MSG_CONNECTION_STOPPED).sendToTarget();
			}
			else
			{
				throw new Exception("Nothing to disconnect from.");
			}
		}
		catch(Exception e)
		{
			throw e;
		}
		finally
		{
			try
			{
				if(_socket != null)
				{
					synchronized(_socket)
					{
						_socket.close();
					}
				}
			}
			catch(IOException ioe)
			{
				Log.e(LOG_TAG, "Failed to close socket. " + ioe.getMessage());
				throw ioe;
			}
		}
	}
	
	
	/**
	 * Starts a thread which manages the connected socket.
	 * 
	 * @throws Exception If there is an error starting the managed connection. 
	 * 
	 * @see ConnectionManager
	 */
	public void startConnectionManager() throws Exception
	{
		try
		{
			if(_socket == null)
			{
				throw new Exception("There is no socket.");
			}
			else if(!_socket.isConnected())
			{
				throw new Exception("Socket has no active connection.");
			}
			else
			{
				_connectionManager = new ConnectionManager(_socket);
				_connectionManager.start();
			}
		}
		catch(Exception e)
		{
			throw e;
		}
	}
	
	
	/**
	 * Stops the thread managing a connected socket.
	 * 
	 * @throws Exception If there is a problem stopping the thread (it doesn't exist).
	 */
	public void stopConnectionManager() throws Exception
	{
		try
		{
			if(_connectionManager != null)
			{
				if(_connectionManager.isAlive())
				{
					_connectionManager.kill();
				}
				else
				{
					throw new Exception("There is no active ConnectionManager to stop.");
				}
			}
			else
			{
				throw new Exception("There is no ConnectionManager to stop.");
			}
		}
		catch(Exception e)
		{
			throw e;
		}
	}
	
	
	/**
	 * Writes data to the managed connection.
	 * 
	 * @param data The data you want to write. Will be converted into a byte array (byte[]).
	 * @throws Exception If there is an error writing the data.
	 */
	public void write(byte[] bytes) throws Exception
	{
		try
		{
			if(_connectionManager == null)
			{
				throw new Exception("There is no managed connection to write to.");
			}
			else if(!_connectionManager.isAlive())
			{
				throw new Exception("There is no active managed connection to write to.");
			}
			else
			{
				_connectionManager.write(bytes);
			}
		}
		catch(Exception e)
		{
			throw e;
		}
	}
	
	
	/**
	 * Receiver registered for various Bluetooth based events.
	 */
	private final BroadcastReceiver _receiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context ctx, Intent intent)
		{
			String action = intent.getAction();
			
			if(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action))
			{	
				int connState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
				if(connState == BluetoothAdapter.STATE_TURNING_OFF || connState == BluetoothAdapter.STATE_OFF)
				{
					_handler.obtainMessage(MSG_BLUETOOTH_LOST).sendToTarget();
				}
			}
			else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action))
			{
				int bondState 			= intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0);
				BluetoothDevice device 	= intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				
				if(bondState == BluetoothDevice.BOND_BONDED)
				{
					String name 	= device.getName(); 
					String address 	= device.getAddress();
					
					Bundle bundle = new Bundle();
					bundle.putString(DATA_DEVICE_NAME, name);
					bundle.putString(DATA_DEVICE_ADDRESS, address);
				
					Message msg = _handler.obtainMessage(MSG_DEVICE_BONDED);
					msg.setData(bundle);
					msg.sendToTarget();
				}
			}
			else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
			{
				_handler.obtainMessage(MSG_DISCOVERY_STARTED).sendToTarget();
			}
			else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
			{
				_handler.obtainMessage(MSG_DISCOVERY_FINISHED).sendToTarget();
			}
			else if(BluetoothDevice.ACTION_FOUND.equals(action))
			{
				try
				{
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					
					Bundle bundle = new Bundle();
					bundle.putString(DATA_DEVICE_NAME, device.getName());
					bundle.putString(DATA_DEVICE_ADDRESS, device.getAddress());
				
					Message msg = _handler.obtainMessage(MSG_DEVICE_FOUND);
					msg.setData(bundle);
					msg.sendToTarget();
				}
				catch(Exception e)
				{
					Log.e(LOG_TAG, "Exception" + e.getMessage());
				}
			}
			else if(BluetoothDevice.ACTION_UUID.equals(action))
			{
				BluetoothDevice device 			= intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Parcelable[] uuids 				= intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
				ArrayList<String> uuidStrings 	= new ArrayList<String>();
				
				if(uuids != null)
				{
					for(Parcelable uuid : uuids)
					{
						uuidStrings.add(uuid.toString());
					}
				}
				
				Bundle bundle = new Bundle();
				bundle.putString(DATA_DEVICE_NAME, device.getName());
				bundle.putString(DATA_DEVICE_ADDRESS, device.getAddress());
				bundle.putStringArrayList(DATA_UUIDS, uuidStrings);
			
				Message msg = _handler.obtainMessage(MSG_UUIDS_FOUND);
				msg.setData(bundle);
				msg.sendToTarget();
			}
		}
	};
	
	/** 
	 * Attempts a connection at the specified device. Sets the private field <b>_socket</b>
	 * for BluetoothWrapper on successful connection attempt.
	 * 
	 * @see AsyncTask
	 * @see BluetoothWrapper
	 * @see BluetoothSocket
	 */
	private class ConnectionAttempt extends AsyncTask<Void, Void, BluetoothSocket>
	{
		private static final String LOG_TAG = "[BluetoothService]ConnectTask";
		
		private final UUID 				_uuid;
		private final BluetoothSocket 	_socket;
		
		private String _error;
		
		/**
		 * Constructor for ConnectionAttempt: creates a socket for the given device and other parameters.
		 * 
		 * @param device Target of this connection attempt
		 * @param uuid UUID for creating the socket
		 * @param connType Type of connection, eg. secure or insecure
		 * @throws Exception If there is a problem creating the socket with given parameters.
		 */
		public ConnectionAttempt(BluetoothDevice device, UUID uuid, EConnectionType connType) throws Exception 
		{
			UUID 			tmpUuid 	= null;
			BluetoothSocket tmpSocket 	= null;
			
			try
			{
				if(uuid == null)
				{
					throw new Exception("No UUID given for ConnectionAttempt.");
				}
				else
				{
					tmpUuid = uuid;
				}
			}
			catch(Exception e)
			{
				throw e;
			}
			
			_uuid = tmpUuid;
			
			try
			{
				switch(connType)
				{
				case Secure:
					tmpSocket = device.createRfcommSocketToServiceRecord(_uuid);
					break;
					
				case Insecure:
					tmpSocket = device.createInsecureRfcommSocketToServiceRecord(_uuid);
					break;
					
				case Hax:
					Method createSocket = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
			        tmpSocket = (BluetoothSocket)createSocket.invoke(device, Integer.valueOf(1));	
					break;
				}
			}
			catch(Exception e)
			{
				throw e;
			}
			finally
			{
				this._socket = tmpSocket;
			}
		}
		
		@Override
		protected BluetoothSocket doInBackground(Void... params) 
		{
			if(this._socket == null)
			{
				this._error = "Socket not created correctly.";
				return null;
			}
			
			try
			{
				_socket.connect();
				return _socket;
			}
			catch(IOException eConnect)
			{
				this._error = eConnect.getMessage();
				
				try
				{
					_socket.close();
				}
				catch(IOException eClose)
				{
					this._error += " " + eClose.getMessage();
					Log.e(LOG_TAG, "Failed to close socket. " + eClose.getMessage());
				}
				return null;
			}
		}

		@Override
		protected void onPostExecute(BluetoothSocket resultingSocket) 
		{
			if(resultingSocket == null)
			{
				Bundle bundle = new Bundle();
				bundle.putString(DATA_ERROR, this._error);
				
				Message msg = _handler.obtainMessage(MSG_CONNECTION_FAILED);
				msg.setData(bundle);
				msg.sendToTarget();
			}
			else
			{
				if(BluetoothWrapper.this._socket != null)
				{
					synchronized(BluetoothWrapper.this._socket)
					{
						BluetoothWrapper.this._socket = resultingSocket;
					}
				}
				else
				{
					BluetoothWrapper.this._socket = resultingSocket;
				}
				
				_handler.obtainMessage(MSG_CONNECTION_ESTABLISHED).sendToTarget();				
			}	
		}
	}

	
	/**
	 * Manages an active connection, allowing read and write operations.  
	 */
	private class ConnectionManager extends Thread
	{
		private static final String LOG_TAG		= "[BluetoothWrapper]ConnectionManager";
		private static final int BUFFER_SIZE 	= 1024;
		
		private final BluetoothSocket 	_socket;
		private final InputStream 		_input;
		private final OutputStream 		_output;
		
		private volatile boolean _isAlive;
		
		/**
		 * Constructor for ConnectionManager, retrieves input and output streams from given socket.
		 * 
		 * @param socket A connected socket.
		 * @throws IOException If there is an error retrieving streams from the socket.
		 */
		public ConnectionManager(BluetoothSocket socket) throws IOException
		{
			_socket				= socket;
			InputStream input	= null;
			OutputStream output = null;
			
			try
			{
				input 	= _socket.getInputStream();
				output 	= _socket.getOutputStream();
			}
			catch(IOException e)
			{
				throw e;
			}
			
			_input 	= input;
			_output = output;
			
			_isAlive = true;
		}
		
		@Override
		public void run() 
		{
			int bytes;
			byte[] buffer = new byte[BUFFER_SIZE];
			
			while(_isAlive)
			{	
				try
				{
					bytes 		= _input.read(buffer);
					byte[] data = new byte[bytes];
					
					for(int i = 0; i < bytes; i++)
					{
						data[i] = buffer[i];
					}
					
					Bundle bundle = new Bundle();
					bundle.putByteArray(DATA_BYTES, data);

					Message msg = _handler.obtainMessage(MSG_READ);
					msg.setData(bundle);
					msg.sendToTarget();
				}
				catch(Exception e)
				{
					try 
					{
						if(BluetoothWrapper.this._socket != null)
						{
							synchronized(BluetoothWrapper.this._socket)
							{
								BluetoothWrapper.this._socket.close();
								BluetoothWrapper.this._socket = null;
							}
						}
					} 
					catch(Exception ioe) 
					{
						Log.e(LOG_TAG, "Failed to close socket after connection error." + ioe.getMessage());
					}
						
					Bundle bundle = new Bundle();
					bundle.putString(DATA_ERROR, "Error reading InputStream. " + e.getMessage());

					Message msg = _handler.obtainMessage(MSG_CONNECTION_LOST);
					msg.setData(bundle);
					msg.sendToTarget();
					
					break;
				}
			}
		}
		
		/**
		 * Write given data to the output stream.
		 * 
		 * @param bytes The data you wish to transmit to the output stream.
		 * @throws IOException If there is an error writing to the output stream.
		 */
		public void write(byte[] bytes) throws IOException
		{
			try
			{
				_output.write(bytes);
			}
			catch(IOException e)
			{
				throw e;
			}
		}
		
		/**
		 * Flags the thread so that it will not continue execution, essentially killing it.
		 */
		public void kill()
		{
			_isAlive = false;
		}
	}
}
