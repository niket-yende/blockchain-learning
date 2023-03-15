var fs = require('fs');
var cryptoMod = require('crypto');
var counterFile = './server/trade-counter.txt';

function readCounter() {
	var data = fs.readFileSync(counterFile);
	return data ? (isNaN(parseInt(data)) ? 0 : parseInt(data)) : 0;
}

function getAndUpdateCounter() {
	var currentVal = readCounter();
	var newCounter = currentVal + 1;
	fs.writeFileSync(counterFile, newCounter);
	return newCounter;
}

function readFile(path, cb) {
	return fs.readFile(path, cb);
}

function getDocHash(file) {
	var hash = cryptoMod.createHash('sha256');
	hash.update(file);
	var digest = hash.digest('hex');
	console.log(digest);
	return digest;
}

module.exports = {
	readCounter: readCounter,
	getAndUpdateCounter: getAndUpdateCounter,
	readFile: readFile,
	getDocHash: getDocHash
};