# Android Input File

## Overview

Android Input File plugin for Cordova.

Install with Cordova CLI:

	$ cordova plugin add cordova-plugin-android-input-file

Supported Platforms:

* Android

## API

	/**
	 * Displays native prompt for user to select a file.
	 *
	 * @param accept Optional MIME type filter (e.g. 'image/gif,video/*').
	 *
	 * @returns Promise containing selected file's raw binary data,
	 * base64-encoded data: URI, MIME type, display name, and original URI.
	 *
	 * If user cancels, promise will be resolved as undefined.
	 * If error occurs, promise will be rejected.
	 */
	inputFile.getFile(accept?: string) : Promise<undefined|{
		data: Uint8Array;
		dataURI: string;
		mediaType: string;
		name: string;
		uri: string;
	}>
	
	/**
	 * Displays native prompt for user to select a file.
	 *
	 * @param accept Optional MIME type filter (e.g. 'image/gif,video/*').
	 *
	 * @returns Promise containing selected file's MIME type, display name,
	 * and original URI.
	 *
	 * If user cancels, promise will be resolved as undefined.
	 * If error occurs, promise will be rejected.
	 */
	inputFile.getFileMetadata(accept?: string) : Promise<undefined|{
		mediaType: string;
		name: string;
		uri: string;
	}>

## Example Usage 
  
  ```
  <input type="button" (click)="inputFile(accept)"/>
  ```

  ```
  function inputFile(accept : string){
    (async () => {
      const file = await inputFile.getFile(accept);
      console.log(file ? file.name : 'canceled');
    })();
  }
  ```


## Platform-Specific Notes

The following must be added to config.xml to prevent crashing when selecting large files
on Android:

```
<platform name="android">
	<edit-config
		file="app/src/main/AndroidManifest.xml"
		mode="merge"
		target="/manifest/application"
	>
		<application android:largeHeap="true" />
	</edit-config>
</platform>
```

If it isn't present already, you'll also need the attribute `xmlns:android="http://schemas.android.com/apk/res/android"` added to your `<widget>` tag in order for that to build successfully.
