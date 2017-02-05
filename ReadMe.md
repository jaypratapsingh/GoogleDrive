# *********Cordova : Google Drive*************


By using this plugin you can upload or download file from Google Drive

## Install this plugin using:

cordova plugin add cordova-plugin-googledrive


## Remove Plugins :

cordova plugin remove cordova-plugin-googledrive


## Put the below code in your javascript code to run plugin:

### For Upload File:

googleDrive.uploadFile(
	function(success) {
		alert("All file has been successfully uploaded");
    },
	function(error) {
		alert("Something went wrong. Please try again...");
    }, filePath);
	
	
	
	
*filePath : Full File path* 


### For Download File:

googleDrive.downloadFile(
	function(success) {
		alert(success);
    },
	function(error) {
		alert(error);
    }, "");



##

> GitHub URL: https://github.com/jaypratapsingh/GoogleDrive

> npm url : https://www.npmjs.com/package/GoogleDrive
