// Templates of our example BT app, uses underscore.js templating
var templates = {

    // Single BluetoothDevice using Bootstrap components to
    // display nicely
    device:
        "<div class='panel panel-default' style='margin-top:16px;'>" +
            "<div class='panel-body'>" +
                "<div class='row'>" +
                    "<div class='col-xs-8'><b><%= name %></b></div>" +
                    "<div class='col-xs-4'>" +
                        "<% if(isConnected) { %>" +
                            "<button type='button' class='btn btn-danger btn-block btn-bt btn-bt-disconnect'>" +
                                "<span class='glyphicon glyphicon-remove'></span> Disconnect" +
                            "</button>" +
                        "<% } else { %>" +
                            "<button type='button' class='btn btn-default btn-block btn-bt btn-bt-connect' data-loading-text='Connecting...' disabled>" +
                                "<span class='glyphicon glyphicon-transfer'></span> Connect" +
                            "</button>" +
                        "<% } %>" +
                    "</div>" +
                "</div>" +
            "</div>" +
        "</div>"
}
