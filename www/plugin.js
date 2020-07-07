
var exec = require('cordova/exec');

var PLUGIN_NAME = 'MyCordovaPlugin';

var MyCordovaPlugin = {
  openCamera:function(cb){
    console.log('Starting camera......')
    exec(cb, null, PLUGIN_NAME, 'openCamera', []);
  }
};

module.exports = MyCordovaPlugin;
