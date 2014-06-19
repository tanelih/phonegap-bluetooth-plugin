$.fn.extend({
	enable: function() {
		this.removeAttr('disabled');
	},
	disable: function() {
		this.attr('disabled', 'disabled');
	}
});

var BluetoothState = Backbone.Model.extend({}, {
	Off:       1,
	Busy:      2,
	Ready:     3,
	Connected: 4
});

var Bluetooth = new BluetoothState({
	state: BluetoothState.Busy
});

var Device = Backbone.Model.extend({
	defaults: {
		name:        'name',
		address:     'address',
		isConnected: false
	}
});

var DeviceCollection = Backbone.Collection.extend({
	model: Device
});

var DeviceView = Backbone.View.extend({
	template: templates.device,

	events: {
		'click .btn-bt-connect':    'connect',
		'click .btn-bt-disconnect': 'disconnect'
	},

	initialize: function() {
		this.model.on('change', this.render, this);
	},

	render: function() {
		this.$el.html(_.template(this.template, {
			name: this.model.get('name'),
			isConnected: this.model.get('isConnected')
		}));
		return this;
	},

	connect: function() {
		var self = this;

		Bluetooth.set({
			state: BluetoothState.Busy
		});

		self.$('.btn-bt-connect').button('loading');

		var onFail = function() {
			Bluetooth.set({
				state: BluetoothState.Ready
			});
			self.$('.btn-bt-connect').button('reset');
		}

		var gotUuids = function(device) {
			console.log('got UUID\'s for device', device);
			var onConnection = function() {
				console.log('got connection');
				self.model.set({
					isConnected: true
				});

				var onConnectionLost = function() {
					console.log('lost connection');
					self.model.set({
						isConnected: false
					});
					onFail();
				}
				console.log('output data to console...');
				window.bluetooth.startConnectionManager(
					console.log, onConnectionLost);
			}

			window.bluetooth.connect(onConnection, onFail, {
				uuid:    device.uuids[0],
				address: self.model.get('address')
			});
		}

		window.bluetooth.getUuids(gotUuids, onFail, self.model.get('address'));
	},

	disconnect: function() {
		var self = this;

		var onDisconnected = function() {
			console.log('disconnected');
			self.model.set({
				isConnected: false
			});
			Bluetooth.set({
				state: BluetoothState.Ready
			});
		}

		Bluetooth.set({
			state: BluetoothState.Busy
		});

		window.bluetooth.disconnect(onDisconnected);
	}
});

var DeviceListView = Backbone.View.extend({
	el: '#list-devices',

	initialize: function() {
		this.collection.on('reset add', this.render, this);
	},

	render: function() {
		this.$el.html('');

		var self = this;
		self.collection.each(function(device) {
			self.$el.append(
				new DeviceView({ model: device }).render().el);
		});
	}
});

var onDeviceReady = function() {
	var devices = new DeviceListView({
		collection: new DeviceCollection()
	});

	var onBluetoothStateChanged = function() {

		console.log('state changed', Bluetooth.get('state'));

		switch(Bluetooth.get('state')) {
			case BluetoothState.Off:
				$('#btn-bt-on').enable();
				$('#btn-bt-off').disable();
				$('#btn-bt-discover').disable();
				$('.btn-bt-connect').disable();
				$('.btn-bt-disconnect').disable();
				break;

			case BluetoothState.Busy:
				$('#btn-bt-on').disable();
				$('#btn-bt-off').disable();
				$('#btn-bt-discover').disable();
				$('.btn-bt-connect').disable();
				$('.btn-bt-disconnect').disable();
				break;

			case BluetoothState.Ready:
				$('#btn-bt-on').disable();
				$('#btn-bt-off').enable();
				$('#btn-bt-discover').enable();
				$('.btn-bt-connect').enable();
				$('.btn-bt-disconnect').enable();
				break;

			case BluetoothState.Connected:
				$('#btn-bt-on').disable();
				$('#btn-bt-off').disable();
				$('#btn-bt-discover').disable();
				$('.btn-bt-connect').disable();
				$('.btn-bt-disconnect').enable();
				break;
		}
	}

	var onToggleOn = function() {
		Bluetooth.set({
			state: BluetoothState.Busy
		});

		var onBluetoothEnabled = function() {
			console.log('bluetooth enabled');
			Bluetooth.set({
				state: BluetoothState.Ready
			});
		}

		window.bluetooth.enable(onBluetoothEnabled);
	}

	var onToggleOff = function() {
		Bluetooth.set({
			state: BluetoothState.Busy
		});

		var onBluetoothDisabled = function() {
			console.log('bluetooth disabled');
			Bluetooth.set({
				state: BluetoothState.Off
			});
		}

		window.bluetooth.disable(onBluetoothDisabled);
	}

	var onDiscover = function() {
		console.log('starting discovery...');
		Bluetooth.set({
			state: BluetoothState.Busy
		});

		var onDeviceDiscovered = function(device) {
			console.log('found device', device);
			devices.collection.add(new Device(device));
		}

		var onDiscoveryFinished = function() {
			console.log('discovery finished');
			Bluetooth.set({
				state: BluetoothState.Ready
			});
			$('#btn-bt-discover').button('reset');
		}

		$('#btn-bt-discover').button('loading');

		devices.collection.reset();

		window.bluetooth.startDiscovery(
			onDeviceDiscovered, onDiscoveryFinished, onDiscoveryFinished);
	}

	$('#btn-bt-on').on('click', onToggleOn);
	$('#btn-bt-off').on('click', onToggleOff);
	$('#btn-bt-discover').on('click', onDiscover);

	Bluetooth.on('change', onBluetoothStateChanged);

	window.bluetooth.isEnabled(function(isEnabled) {
		Bluetooth.set({
			state: isEnabled ? BluetoothState.Ready : BluetoothState.Off
		});
	});
}

$(document).on('deviceready', onDeviceReady);
