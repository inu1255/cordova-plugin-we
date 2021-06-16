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
var events = {};
function callEvent(type, data) {
	var listens = events[type];
	if (!listens) return;
	listens.forEach(function (cb, i) {
		try {
			cb(data);
		} catch (e) {
			console.error(e);
		}
		if (cb.once) listens.splice(i, 1);
	});
}
__we.ondata = function (msg) {
	var i = msg.indexOf("\n");
	var data = {};
	if (i < 0) {
		data.type = msg;
	} else {
		data.type = msg.slice(0, i);
		data.data = msg.slice(i + 1);
		try {
			data.data = JSON.parse(data.data);
		} catch (e) {}
	}
	var ss = data.type.split(":");
	var type = ss[0];
	// if (we.config.dev) console.log("ondata:", data.type, data.data);
	callEvent(type, data);
	for (var i_1 = 1; i_1 < ss.length; i_1++) {
		type += ":" + ss[i_1];
		callEvent(type, data);
	}
};

__we.on = function (type, cb) {
	type = type.trim();
	if (!type) throw "cannot listen empty type";
	if (typeof cb === "function") {
		var listens = events[type] || (events[type] = []);
		if (listens.indexOf(cb) < 0) listens.push(cb);
	}
};
__we.once = function (type, cb) {
	type = type.trim();
	if (!type) return Promise.resolve();
	return new Promise(function (resolve, reject) {
		function fn(v) {
			resolve(typeof cb === "function" ? cb.apply(this, arguments) : v && v.data);
			we.off(type, fn);
		}
		(events[type] || (events[type] = [])).push(fn);
	});
};
__we.off = function (type, cb) {
	type = type.trim();
	var listens = events[type];
	if (!listens) return;
	if (!cb) {
		listens.length = 0;
		return;
	}
	var i = listens.indexOf(cb);
	if (i >= 0) listens.splice(i, 1);
};
__we.offAll = function () {
	events = {};
};

module.exports = __we;
