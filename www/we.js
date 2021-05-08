const exec = require("cordova/exec");
const FEATURE_NAME = "we";

var __we = {};

function each(method) {
  Object.defineProperty(__we, method, {
    get: function () {
      return function () {
        var args = Array.from(arguments);
        return new Promise(function (resolve, reject) {
          exec(resolve, reject, FEATURE_NAME, method, args);
        });
      };
    },
  });
}

function init(methods) {
  methods.split(",").forEach(each);
}

function ondata() {
  __we.ondata && __we.ondata.apply(__we, arguments);
}
exec(ondata, console.error, FEATURE_NAME, "ondata", []);
exec(init, console.error, FEATURE_NAME, "init", []);

module.exports = __we;
