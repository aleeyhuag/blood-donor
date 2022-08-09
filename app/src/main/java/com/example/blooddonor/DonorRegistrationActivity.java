package com.example.blooddonor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.example.blooddonor.R.id.backButton;
import static com.example.blooddonor.R.id.profile_image;

public class DonorRegistrationActivity extends AppCompatActivity {

    private TextView backButton;

    private CircleImageView profile_image;

    private TextInputEditText registerFullName, registerEmail, registerPhoneNumber, registerPassword;

    private Spinner bloodGroupSpinner;

    private Button registerButton;

    private Uri resultUri;

    private ProgressDialog loader;

    private FirebaseAuth mAuth; //adding firebase authentication
    private DatabaseReference userDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donor_registration);

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DonorRegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        profile_image = findViewById(R.id.profile_image);
        registerFullName = findViewById(R.id.registerFullName);
        registerEmail = findViewById(R.id.registerEmail);
        registerPhoneNumber = findViewById(R.id.registerPhoneNumber);
        registerPassword = findViewById(R.id.registerPassword);
        bloodGroupSpinner = findViewById(R.id.bloodGroupSpinner);
        registerButton = findViewById(R.id.registerButton);
        loader = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();

        //take user to gallery
        profile_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });

        //after filling the form and clicking register

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = registerEmail.getText().toString().trim();
                final String password = registerPassword.getText().toString().trim();
                final String fullName = registerFullName.getText().toString().trim();
                final String phoneNumber = registerPhoneNumber.getText().toString().trim();
                final String bloodGroup = bloodGroupSpinner.getSelectedItem().toString();

                if (TextUtils.isEmpty(email)){
                    registerEmail.setError("Email Required");
                    return;
                }

                if (TextUtils.isEmpty(password)){
                    registerPassword.setError("Password Required");
                    return;
                }

                if (TextUtils.isEmpty(fullName)){
                    registerFullName.setError("Fullname Required");
                    return;
                }

                if (TextUtils.isEmpty(phoneNumber)){
                    registerPhoneNumber.setError("Phone Number Required");
                    return;
                }

                if (bloodGroup.equals("Select your blood group")){
                    Toast.makeText(DonorRegistrationActivity.this, "Select Blood Group", Toast.LENGTH_SHORT).show();
                    return;
                }

                else {

                    loader.setMessage("Registering you...");
                    loader.setCanceledOnTouchOutside(false);
                    loader.show();

                    //goto tools and connect firebase after initializing loader
                    mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {


                            if (!task.isSuccessful()){
                                String error = task.getException().toString();
                                Toast.makeText(DonorRegistrationActivity.this, "Error" + error, Toast.LENGTH_SHORT).show();
                            }
                            else{
                                String currentUserId = mAuth.getCurrentUser().getUid();
                                userDatabaseRef = FirebaseDatabase.getInstance().getReference().child("users").child(currentUserId); //firebase folder user also called node
                                HashMap userInfo = new HashMap(); //
                                userInfo.put("id", currentUserId);
                                userInfo.put("name", fullName);
                                userInfo.put("email", email);
                                userInfo.put("phonenumber", phoneNumber);
                                userInfo.put("bloodgroup", bloodGroup);
                                userInfo.put("type", "donor");
                                userInfo.put("search", "donor"+bloodGroup);

                                userDatabaseRef.updateChildren(userInfo).addOnCompleteListener(new OnCompleteListener() {
                                    @Override
                                    public void onComplete(@NonNull Task task) {
                                        if (task.isSuccessful()){
                                            Toast.makeText(DonorRegistrationActivity.this, "Data saved successfulyy", Toast.LENGTH_SHORT).show();
                                        }else{
                                            Toast.makeText(DonorRegistrationActivity.this, task.getException().toString(), Toast.LENGTH_SHORT).show();
                                        }
                                        finish();
                                    }
                                });

                                // saving the picture
                                if(resultUri != null){
                                    final StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profile images").child(currentUserId);
                                    Bitmap bitmap = null;

                                    try {
                                        bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
                                    }catch (IOException e){
                                        e.printStackTrace();
                                    }
                                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 20, byteArrayOutputStream);
                                    byte [] data = byteArrayOutputStream.toByteArray();
                                    UploadTask uploadTask = filePath.putBytes(data); //upload image

                                    uploadTask.addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(DonorRegistrationActivity.this,"Image upload Failed", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                            if (taskSnapshot.getMetadata() != null && taskSnapshot.getMetadata().getReference() != null){
                                                Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                                                result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                    @Override
                                                    public void onSuccess(Uri uri) {
                                                           String imageUri = uri.toString();
                                                           Map newImageMap = new HashMap();
                                                           newImageMap.put("profilepictureurl", imageUri);

                                                           //saving image url to database
                                                           userDatabaseRef.updateChildren(newImageMap).addOnCompleteListener(new OnCompleteListener() {
                                                               @Override
                                                               public void onComplete(@NonNull Task task) {
                                                                   if (task.isSuccessful()){
                                                                       Toast.makeText(DonorRegistrationActivity.this,"Image url added to database", Toast.LENGTH_SHORT).show();
                                                                   }else{
                                                                       Toast.makeText(DonorRegistrationActivity.this,task.getException().toString(), Toast.LENGTH_SHORT).show();
                                                                   }
                                                               }
                                                           });

                                                           finish();
                                                    }
                                                });
                                            }
                                        }
                                    });

                                    Intent intent = new Intent(DonorRegistrationActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                    loader.dismiss();
                                }

                            }
                        }
                    });

                }
            }
        });
    }

    // contains what the user picked from gallery
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null){
            resultUri = data.getData();
            profile_image.setImageURI(resultUri);
        }
    }
}