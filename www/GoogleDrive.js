var googleDrive = {
    uploadFile: function(successCallback, errorCallback, filePath) {
		cordova.exec( successCallback,
		            errorCallback,
					"GoogleDrive",
					"uploadFile",
					filePath
					);
    }
}

module.exports = googleDrive;