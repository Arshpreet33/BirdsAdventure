package com.example.birdsadventure;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.service.media.MediaBrowserService;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UploadFragment extends Fragment {
    public static final int CAMERA_CODE = 101;
    public static final int CAMERA_REQUEST_CODE = 102;
    public static final int GALLERY_REQUEST_CODE = 105;
    //static final int REQUEST_TAKE_PHOTO = 1;
    ImageView imageView;
    Button camerabtn, gallerybtn;

    String currentPhotoPath;
      //  Firebase Storage Reference
     StorageReference storageReference;
    public UploadFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate (R.layout.fragment_upload, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated (view, savedInstanceState);

        //initialize storage reference
        storageReference = FirebaseStorage.getInstance().getReference();

        imageView = getActivity ().findViewById (R.id.imageView);
        camerabtn = getActivity ().findViewById (R.id.cameraBtn);
        gallerybtn = getActivity ().findViewById (R.id.galleryBtn);


        camerabtn.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick(View v) {
                askcamerapermission ();
              //  Toast.makeText (getActivity (), "Camera btn Clicked", Toast.LENGTH_LONG).show ();

            }


        });

        gallerybtn.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick(View v) {
              //  Toast.makeText (getActivity (), "Gallery btn CLicked", Toast.LENGTH_LONG).show ();

                Intent galllery=new Intent (Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult (galllery, GALLERY_REQUEST_CODE);
            }
        });


    }


    //ask user to grant camera access permissions
    private void askcamerapermission() {
        if (ContextCompat.checkSelfPermission (getActivity ().getApplicationContext (), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {

            ActivityCompat.requestPermissions (getActivity (), new String[]{Manifest.permission.CAMERA}, CAMERA_CODE);
        } else {
            dispatchTakePictureIntent ();
        }
    }


    //check user's permissions to access camera
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent ();
            } else {

                Toast.makeText (getActivity ().getApplicationContext (), "Camera permissions are needed", Toast.LENGTH_SHORT).show ();
            }
        }
    }


    //to capture & save an image to gallery image
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        //Camera
        if (requestCode == CAMERA_REQUEST_CODE)
        {
       if (resultCode== Activity.RESULT_OK)
       {
           File f =new File(currentPhotoPath);
           imageView.setImageURI (Uri.fromFile (f));
           Log.d ("tag" ,"Absolute url "+Uri.fromFile (f));

           Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
           Uri contentUri = Uri.fromFile(f);
           mediaScanIntent.setData(contentUri);
           getActivity ().sendBroadcast(mediaScanIntent);

         //  uploadImageToFirebase (f.getName (),contentUri);
       }
        }

        //Gallery
        if (requestCode == GALLERY_REQUEST_CODE)
        {
            if (resultCode== Activity.RESULT_OK)
            {
                Uri contentUri=data.getData ();
                String timeStamp = new SimpleDateFormat ("yyyyMMdd_HHmmss").format(new Date ());
                String imageFileName = "JPEG_" + timeStamp + "."+getFileExt(contentUri);
                Log.d ("tag","gallery image uri"+imageFileName);
                imageView.setImageURI (contentUri);

                uploadImageToFirebase (imageFileName,contentUri);

            }
        }
    }


    //Upload an image to firebase
    private void uploadImageToFirebase(String name, Uri contentUri)
    {
        final StorageReference image= storageReference.child ("pictures/"+name);
        image.putFile (contentUri).addOnSuccessListener (new OnSuccessListener< UploadTask.TaskSnapshot > () {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
            {
                image.getDownloadUrl ().addOnSuccessListener (new OnSuccessListener< Uri > () {
                    @Override
                    public void onSuccess(Uri uri)
                    {
                        Log.d ("tag","onSuccess: Upload Image URI is "+ uri);
                    }
                });
                Toast.makeText (getActivity (),"image has been successfully uploaded",Toast.LENGTH_LONG).show ();
            }
        }).addOnFailureListener (new OnFailureListener ()
        {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                Toast.makeText (getActivity (),"Upload Failed",Toast.LENGTH_LONG).show ();
            }
        });
    }

    private String getFileExt(Uri contentUri)
    {
        ContentResolver c =getActivity ().getContentResolver () ;
        MimeTypeMap mime =MimeTypeMap.getSingleton ();
        return mime.getExtensionFromMimeType (c.getType (contentUri));
    }


    //Create a file for the image
    private File createImageFile() throws IOException
    {
        // Create an image file name
        String timeStamp = new SimpleDateFormat ("yyyyMMdd_HHmmss").format(new Date ());
        String imageFileName = "JPEG_" + timeStamp + "_";
       // File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
       File storageDir=Environment.getExternalStoragePublicDirectory (Environment.DIRECTORY_PICTURES);
       // (it will work for actual device)
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


   //Camera Capture Action,create a Photo file
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getActivity ().getPackageManager ()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex)
            {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(getActivity (),
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }
}