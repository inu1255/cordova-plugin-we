const fs = require("fs-extra");
const path = require("path");

async function main() {
	let list = [
		"crashshield-2.0.3-release.aar",
		"logger-2.0.3-release.aar",
		"main-2.0.3-release.aar",
		"phoneNumber-L-AuthSDK-2.12.1.aar",
	];
	for (let file of list) {
		if (await fs.pathExists(file)) {
			let data = await fs.readFile(file);
			await fs.writeFile(path.join("cordova/platforms/android/app/libs", file), data);
		}
	}
}

main();
