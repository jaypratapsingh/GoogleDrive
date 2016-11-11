var googleDrive = {
    uploadFile: function(successCallback, errorCallback, filePath) {
		cordova.exec( successCallback,
		            errorCallback,
					"GoogleDrive",
					"uploadFile",
					filePath
					);
    },
	
	downloadFile: function(successCallback, errorCallback, filePath) {
    		cordova.exec( successCallback,
    		            errorCallback,
    					"GoogleDrive",
    					"downloadFile",
    					""
    					);
        }
}

module.exports = googleDrive;