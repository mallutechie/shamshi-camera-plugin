
var exec = require('cordova/exec');

var PLUGIN_NAME = 'MyCordovaPlugin';

var MyCordovaPlugin = {
  echo: function(phrase, cb) {
    exec(cb, null, PLUGIN_NAME, 'echo', [phrase]);
  },
  getDate: function(cb) {
    exec(cb, null, PLUGIN_NAME, 'getDate', []);
  },
  openCamera:function(cb){
    console.log('Starting camera......')
    exec(cb, null, PLUGIN_NAME, 'openCamera', []);
  }
};

module.exports = MyCordovaPlugin;
