
var exec = require('cordova/exec');

var PLUGIN_NAME = 'MyCordovaPlugin';

var MyCordovaPlugin = {
  openCamera:function(duration,cb){
    exec(cb, null, PLUGIN_NAME, 'openCamera', [duration]);
  },
};

module.exports = MyCordovaPlugin;
