const fs = require("fs-extra");
const path = require("path");

async function main() {
	let list = [
		"crashshield-2.0.3-release.aar",
		"logger-2.0.3-release.aar",
		"main-2.0.3-release.aar",
		"phoneNumber-L-AuthSDK-2.12.1.aar",
	];
	console.log("一键登录模块");
	for (let key of list) {
		let srcpath = path.join(process.cwd(), "res/libs/", key);
		let dstpath = path.join("cordova/platforms/android/app/libs", key);
		if (await fs.pathExists(srcpath)) {
			let data = await fs.readFile(srcpath);
			await fs.writeFile(dstpath, data);
			console.log("复制", key);
		} else if (await fs.pathExists(dstpath)) {
			console.log("使用默认的", key);
		} else {
			console.error("缺少", key);
		}
	}
}

main();
