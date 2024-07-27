package com.example.threadapp.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.threadapp.model.UserModel
import com.example.threadapp.utils.sharedPref
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.util.UUID


class AuthViewModel:ViewModel() {
    val auth = FirebaseAuth.getInstance()
    private val _authState = MutableLiveData<AuthState>().apply { value = AuthState.Unauthenticated }
    val authState: LiveData<AuthState> = _authState

    private val userRef = FirebaseDatabase.getInstance().getReference("users")
    private val db = FirebaseDatabase.getInstance()
    val authRef = db.getReference("users")
    private val storageRef = Firebase.storage.reference
    private val imageRef = storageRef.child("images/${UUID.randomUUID()}.jpg")
    private val _firebaseUser = MutableLiveData<FirebaseUser>()
    val firebaseUser: MutableLiveData<FirebaseUser> get()=_firebaseUser
//    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
//        val currentUser = firebaseAuth.currentUser
//        Log.d("AuthViewModel", "AuthStateListener triggered. Current user: $currentUser")
//        if (currentUser == null) {
//            _authState.value = AuthState.Unauthenticated
//        } else {
//            _authState.value = AuthState.Authenticated
//        }
//    }
    init {
//        auth.addAuthStateListener(authStateListener)
        checkAuthStatus()
//        _firebaseUser.value=auth.currentUser
    }
    private fun checkAuthStatus(){
        val currentUser = auth.currentUser
        Log.d("AuthViewModel", "Current user: $currentUser")
        if (currentUser == null) {
            _authState.value = AuthState.Unauthenticated
        } else {
            _authState.value = AuthState.Authenticated
        }
    }

    fun login(email: String, password: String, context: Context) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("email or password can not be empty")
            return
        }
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Authenticated
                    getData(auth.currentUser?.uid,context)
                } else {
                    _authState.value = AuthState.Error(task.exception?.message?:"something went wrong")
                }
            }
    }

    private fun getData(uid: String?, context: Context) {

        userRef.child(uid!!).get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val snapshot = task.result
                    val userData: UserModel? = snapshot.getValue(UserModel::class.java)
                    if (userData != null) {
                        sharedPref.storeData(
                            userData.name,
                            userData.email,
                            userData.password,
                            userData.username,
                            userData.dob,
                            userData.imageUrl,
                            context
                        )
                    }
                    else{
                        Log.e("AuthViewModel", "Failed to read user data: ${task.exception?.message}")
                    }
        }
        }
    }
    fun register(
        name:String,
        email: String,
        password: String,
        username:String,
        dob:String,
        imageUri: Uri,
        context: Context
    ) {
        val fireStored=Firebase.firestore
        val documentId = UUID.randomUUID().toString()
        val followersRef=fireStored.collection("followers").document(documentId)
        val followingRef=fireStored.collection("following").document(documentId)

        followingRef.set(mapOf("followingIds" to listOf<String>()))
        followersRef.set(mapOf("followerIds" to listOf<String>()))

        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Authenticated
                    _firebaseUser.value=auth.currentUser
                    saveImage(name, email,password, username ,dob, imageUri, auth.currentUser?.uid,context)
                } else {
                    _authState.value = AuthState.Error(task.exception?.message?:"something went wrong")
                }
            }
    }


    fun signout(){
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }
    private fun saveImage(
        name: String,
        email: String,
        password: String,
        username: String,
        dob: String,
        imageUri: Uri,
        uid: String?,
        context: Context
    ) {
        val uploadTask = imageRef.putFile(imageUri)
        uploadTask.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener {
                saveData(name, email,password, username, dob, it.toString(),uid,context)
            }
        }
    }

    private fun saveData(
        name: String,
        email: String,
        password: String,
        username: String,
        dob: String,
        toString: String,
        uid: String?,
        context: Context
    ) {
        val userData = UserModel(name,email,password,username,dob,toString,uid!!)

        userRef.child(uid).setValue(userData)
            .addOnSuccessListener {
                if(it.toString()!=""){
                sharedPref.storeData(name, email,password, username, dob, it.toString(), context)
            }}
            .addOnFailureListener {
            }
    }
}
sealed class AuthState {
    data object Loading : AuthState()
    data object Authenticated : AuthState()
    data object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}