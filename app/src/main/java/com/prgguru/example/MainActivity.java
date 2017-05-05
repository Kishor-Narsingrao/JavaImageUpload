package com.prgguru.example;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.service.voice.VoiceInteractionService;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

@SuppressLint("NewApi")
public class MainActivity extends Activity {

	ProgressDialog prgDialog;
	String encodedString;
	RequestParams params = new RequestParams();
	String imgPath, fileName;
	Bitmap bitmap;
	Uri outputFileUri;
	String mCurrentPhotoPath;
	TextView tvMsg;
	private ArrayList<String> selectedImageArray;
	private static int RESULT_LOAD_IMG = 1;
	public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

		if (permissionCheck != PackageManager.PERMISSION_GRANTED)
		{
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
		}
		else if ( Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission( this, android.Manifest.permission.CAMERA ) != PackageManager.PERMISSION_GRANTED)
		{
			requestPermissions(new String[]{android.Manifest.permission.CAMERA},MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
		}

		selectedImageArray=new ArrayList<>();
		prgDialog = new ProgressDialog(this);
		// Set Cancelable as False
		prgDialog.setCancelable(false);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
				if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED))
					break;
			default:
				break;
		}
	}

	public void loadImagefromGallery(View view) {
		if(selectedImageArray.size() <5) {
			// Create intent to Open Image applications like Gallery, Google Photos
			Intent galleryIntent = new Intent(Intent.ACTION_PICK,
					android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			// Start the Intent
			startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
		}
		else
			Toast.makeText(this,"You can upload only 5 Images at a time ",Toast.LENGTH_LONG).show();
	}

	public void takePicUsingCamera(View v)	{

		if(selectedImageArray.size()<5) {
			try {
				Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					cameraIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					cameraIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
					outputFileUri = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider", createImageFile());
					cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
				} else {
					outputFileUri = Uri.fromFile(createImageFile());
					cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
				}
				startActivityForResult(cameraIntent, 100);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else
			Toast.makeText(this,"You can upload only 5 Images at a time ",Toast.LENGTH_LONG).show();
	}

	private File createImageFile() throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())+"_"+ System.currentTimeMillis();
		String imageFileName = "Img_" + timeStamp;
		File storageDir = new File(Environment.getExternalStorageDirectory(), "Images");

		File file=new File(storageDir,imageFileName+".png");
		// Save a file: path for use with ACTION_VIEW intents
		mCurrentPhotoPath = file.getAbsolutePath();
		return file;
	}

	// When Image is selected from Gallery
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		try {
			// When an Image is picked
			if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK
					&& null != data) {
				// Get the Image from data

				Uri selectedImage = data.getData();

//				getFilePath(selectedImage);
				getFilePath(getGallaryImageURL(selectedImage));

			}
			//This code for camera Intent
			else if(requestCode == 100 && resultCode == RESULT_OK)
			{
				addImageToGallery(mCurrentPhotoPath,this);

				try {
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {

						String filename=FilePath.getPath(this,outputFileUri);
						getFilePath(filename);
						Toast.makeText(this,filename,Toast.LENGTH_LONG).show();
					}
					else
					{
						getFilePath(mCurrentPhotoPath);
//						String filename=imgPath;
//						Toast.makeText(this,filename,Toast.LENGTH_LONG).show();
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				Toast.makeText(this, "You haven't picked Image",
						Toast.LENGTH_LONG).show();
			}
		} catch (Exception e) {
			Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG)
					.show();
		}

	}

	private String getGallaryImageURL(Uri selectedImage ){
		String[] filePathColumn = {MediaStore.Images.Media.DATA};
		// Get the cursor
		Cursor cursor = getContentResolver().query(selectedImage,
				filePathColumn, null, null, null);
		// Move to first row
		cursor.moveToFirst();

		int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
		return (cursor.getString(columnIndex));

	}

	private void getFilePath(String selectedImage){

		/*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {

			String[] filePathColumn = {MediaStore.Images.Media.DATA};

			// Get the cursor
			Cursor cursor = getContentResolver().query(selectedImage,
					filePathColumn, null, null, null);
			// Move to first row
			cursor.moveToFirst();

			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);*//*
//			imgPath = cursor.getString(columnIndex);
			imgPath =FilePath.getPath(this,outputFileUri);
//			cursor.close();
		}
		else

		{
			imgPath=mCurrentPhotoPath;
		}*/

//		imgPath=selectedImage;
		selectedImageArray.add(selectedImage);

		tvMsg=(TextView)findViewById(R.id.tvMsg);

		String fileName="";
		for(int i=0;i<selectedImageArray.size();i++)
		{

			String[] parts = selectedImageArray.get(i).split("/");
			final String RelativefileName = parts[parts.length-1];
			fileName=RelativefileName+"\n"+fileName;
		}
		tvMsg.setText(fileName);
	}

	public static void addImageToGallery(final String filePath, final Context context) {

		ContentValues values = new ContentValues();

		values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
		values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
		values.put(MediaStore.MediaColumns.DATA, filePath);

		context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
	}

	// When Upload button is clicked
	public void uploadImage(View v) {


		if(selectedImageArray.size()<=5 && selectedImageArray.size() !=0) {

			for (int i=0;i<selectedImageArray.size();i++) {

				imgPath=selectedImageArray.get(i);

			// Get the Image's file name
			String fileNameSegments[] = imgPath.split("/");
			fileName = fileNameSegments[fileNameSegments.length - 1];
			// Put file name in Async Http Post Param which will used in Java web app
			params.put("filename", fileName);


			// When Image is selected from Gallery
			if (imgPath != null && !imgPath.isEmpty()) {
				prgDialog.setMessage("Converting Image to Binary Data");
				prgDialog.show();
				// Convert image to String using Base64
				encodeImagetoString();
				// When Image is not selected from Gallery
			} else {
				Toast.makeText(
						getApplicationContext(),
						"You must select image from gallery before you try to upload",
						Toast.LENGTH_LONG).show();
			}
			}
		}
	}

	// AsyncTask - To convert Image to String
	public void encodeImagetoString( ) {

				/*new AsyncTask<Void, Void, String>() {

					protected void onPreExecute() {

					}

					@Override
					protected String doInBackground(Void... params) {*/
//				final String strImageName=params[0];

						BitmapFactory.Options options = null;
						options = new BitmapFactory.Options();
						options.inSampleSize = 3;
						bitmap = BitmapFactory.decodeFile(imgPath, options);
						ByteArrayOutputStream stream = new ByteArrayOutputStream();
						// Must compress the Image to reduce image size to make upload easy
						bitmap.compress(Bitmap.CompressFormat.PNG, 50, stream);
						byte[] byte_arr = stream.toByteArray();
						// Encode Image to String
						encodedString = Base64.encodeToString(byte_arr, 0);
						/*return "";
					}

					@Override
					protected void onPostExecute(String msg) {*/
						prgDialog.setMessage("Calling Upload");
						// Put converted Image string into Async Http Post param
						params.put("image", encodedString);
						// Trigger Image upload
						triggerImageUpload();
					/*}
				}.execute();*/
			}


	public void triggerImageUpload() {
		makeHTTPCall();
	}


	// Make Http call to upload Image to Java server
	public void makeHTTPCall() {
		prgDialog.setMessage("Invoking JSP");
		AsyncHttpClient client = new AsyncHttpClient();

		// Don't forget to change the IP address to your LAN address. Port no as well.
		client.post("http://192.168.1.171:1080/ImageUploadWebApp/uploadimg.jsp",
				params, new AsyncHttpResponseHandler() {

					// When the response returned by REST has Http
					// response code '200'
					@Override
					public void onSuccess(String response) {
						// Hide Progress Dialog
						prgDialog.hide();
						Toast.makeText(getApplicationContext(), response,
								Toast.LENGTH_LONG).show();
						selectedImageArray.clear();
						tvMsg.setText("");
					}

					// When the response returned by REST has Http
					// response code other than '200' such as '404',
					// '500' or '403' etc

					@Override
					public void onFailure(int statusCode, Throwable error,
										  String content) {
						// Hide Progress Dialog
						prgDialog.hide();
						// When Http response code is '404'
						if (statusCode == 404) {
							Toast.makeText(getApplicationContext(),
									"Requested resource not found",
									Toast.LENGTH_LONG).show();
						}
						// When Http response code is '500'
						else if (statusCode == 500) {
							Toast.makeText(getApplicationContext(),
									"Something went wrong at server end",
									Toast.LENGTH_LONG).show();
						}
						// When Http response code other than 404, 500
						else {
							Toast.makeText(
									getApplicationContext(),
									"Error Occured \n Most Common Error: \n1. Device not connected to Internet\n2. Web App is not deployed in App server\n3. App server is not running\n HTTP Status code : "
											+ statusCode, Toast.LENGTH_LONG)
									.show();
						}
					}
				});
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		// Dismiss the progress bar when application is closed
		if (prgDialog != null) {
			prgDialog.dismiss();
		}
	}
}
