package com.example.login

import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.login.model.Gerai
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.io.File

class FirebaseHelper {
    private lateinit var auth: FirebaseAuth

    private lateinit var db: FirebaseFirestore

    private lateinit var storage: FirebaseStorage

    private lateinit var  storageReference: StorageReference

    fun initFirebase() {
        auth = Firebase.auth
        db = Firebase.firestore
        storage = Firebase.storage
        storageReference = storage.reference

    }

    fun checkUserLogin(onUserLoggedIn: () -> Unit) {

        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            onUserLoggedIn.invoke()
        }
    }

    fun registerUser(
        email: String, password: String, onSuccess: (user: FirebaseUser?) -> Unit,
        onFailure: (errorMessage: String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val user = auth.currentUser
                onSuccess.invoke(user)
            }
            .addOnFailureListener {
                val errorMessage = it.message ?: "Error Not Found"
                onFailure.invoke(errorMessage)
            }
    }

    fun saveDataUser(id_user:String, area:String, fullname:String, email: String, password: String,
                     onSuccess: () -> Unit, onFailure: (errorMessage: String) -> Unit){
        val user = hashMapOf(
            "id_user" to id_user,
            "area" to area,
            "fullname" to fullname,
            "email" to email,
            "password" to password
        )

        db.collection("users")
            .document(id_user)
            .set(user)
            .addOnSuccessListener { documentReference ->
                onSuccess.invoke()
            }
            .addOnFailureListener { e ->
                val errorMessage = e.message ?: "Error Not Found"
                onFailure.invoke(errorMessage)
            }
    }

    fun loginUser(
        email: String, password: String, onSuccess: (user: FirebaseUser?) -> Unit,
        onFailure: (errorMessage: String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val user = auth.currentUser
                onSuccess.invoke(user)
            }
            .addOnFailureListener {
                val errorMessage = it.message ?: "Error Not Found"
                onFailure.invoke(errorMessage)
            }
    }

    fun getUserData(user_id : String, onSuccess: (user: Map<String,Any>) -> Unit, onFailure: (errorMessage: String) -> Unit){

        val docRef = db.collection("users").document(user_id)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                onSuccess.invoke(document.data!!)
                } else {
                    onFailure.invoke("User Not Found")
                }
            }
            .addOnFailureListener { exception ->
                onFailure.invoke(exception.message?:"error not found")
            }

    }



    fun uploadVideo(videoPath:String, area:String, onFailure: (errorMessage: String) -> Unit,
                    onSuccess: (videoUrl: String) -> Unit){
        var file = Uri.fromFile(File(videoPath))
        val videoRef = storageReference.child("$area/${file.lastPathSegment}")
        val uploadTask = videoRef.putFile(file)

// Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener {
            // Handle unsuccessful uploads
            onFailure.invoke(it.message?: "Error Not Found")
        }.addOnSuccessListener { taskSnapshot ->
            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                videoRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    onSuccess.invoke(downloadUri.toString())
                } else {
                    // Handle failures
                    onFailure.invoke(
                        task.exception?.message?: "Error Not Found"
                    )
                }
            }
        }
    }

    fun saveDataVideo(id_user:String, title:String, videoUrl:String,
                     onSuccess: () -> Unit, onFailure: (errorMessage: String) -> Unit){
        val user_id = auth.currentUser?.uid?:"id"
        val video = hashMapOf(
            "id_user" to id_user,
            "title" to title,
            "videoUrl" to videoUrl
        )

        db.collection("videos")
            .document(id_user)
            .set(video)
            .addOnSuccessListener { documentReference ->
                onSuccess.invoke()
            }
            .addOnFailureListener { e ->
                val errorMessage = e.message ?: "Error Not Found"
                onFailure.invoke(errorMessage)
            }
    }

    fun getDataGerai(
         onSuccess: (geraiList: List<Gerai>) -> Unit, onFailure: (errorMessage: String) -> Unit){

            val docRef = db.collection("gerai")
            docRef.get()
                .addOnSuccessListener { query->
                    val geraiList = query.toObjects(Gerai::class.java)
                    if(geraiList.isNotEmpty())
                    { onSuccess.invoke(geraiList) }
                    else
                    {
                        onFailure.invoke("Data Not Found")
                    }
                }
                .addOnFailureListener { exception ->
                    onFailure.invoke(exception.message?:"error not found")
                }

        }

    fun logout(){
        auth.signOut()
    }
}