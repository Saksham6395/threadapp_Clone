package com.example.threadapp.Screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.threadapp.item_view.ThreadItem
import com.example.threadapp.model.UserModel
import com.example.threadapp.utils.sharedPref
import com.example.threadapp.viewmodel.AuthState
import com.example.threadapp.viewmodel.AuthViewModel
import com.example.threadapp.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun Profile(navController: NavController, authViewModel: AuthViewModel){
    val authState=authViewModel.authState.observeAsState()
    val context = LocalContext.current
    val profileViewModel: ProfileViewModel = viewModel()
    val threads by profileViewModel.thread.observeAsState()
    val user=UserModel(
        name = sharedPref.getName(context),
        username = sharedPref.getUserName(context),
        email = sharedPref.getEmail(context),
        imageUrl = sharedPref.getImage(context),
        dob = sharedPref.getDob(context),
        password = sharedPref.getPassword(context)
    )
    if(FirebaseAuth.getInstance().currentUser!=null){
    profileViewModel.fetchThreads(FirebaseAuth.getInstance().currentUser!!.uid)}
    LaunchedEffect(authState.value){
        when(authState.value){
            is AuthState.Unauthenticated ->navController.navigate("login")
            else -> Unit
        }
    }
    LazyColumn {
       item {
           ConstraintLayout(modifier = Modifier.padding(16.dp)) {
               val (
                   userImage,
                   username,
                   name,
                   logout,
                   follower,
                   following
               ) = createRefs()
               Image(painter = rememberAsyncImagePainter(model = sharedPref.getImage(context)),
                   contentDescription = null, modifier = Modifier
                       .constrainAs(userImage) {
                           top.linkTo(parent.top)
                           end.linkTo(parent.end, margin = 0.dp )
                       }
                       .size(100.dp)
                       .clip(CircleShape),
                   contentScale = ContentScale.Crop)
               Text(text = sharedPref.getName(context),
                   style = TextStyle(fontSize = 24.sp),
                   fontWeight = FontWeight.ExtraBold,
                   modifier = Modifier
                       .constrainAs(name) {
                           top.linkTo(parent.top)
                           start.linkTo(parent.start)
                       }
                       .fillMaxWidth()
               )
               Text(text = sharedPref.getUserName(context),
                   style = TextStyle(fontSize = 20.sp),
                   modifier = Modifier
                       .constrainAs(username) {
                           top.linkTo(name.bottom)
                           start.linkTo(parent.start)
                       }
               )
               Text(text = "0  Followers", style = TextStyle(fontSize = 20.sp), modifier = Modifier
                   .constrainAs(follower) {
                       top.linkTo(username.bottom)
                       start.linkTo(parent.start)
                   }
               )
               Text(text = "0 Following", style = TextStyle(fontSize = 20.sp), modifier = Modifier
                   .constrainAs(following) {
                       top.linkTo(follower.bottom)
                       start.linkTo(parent.start)
                   }
               )
               ElevatedButton(
                   onClick = { authViewModel.signout() },
                   modifier = Modifier.constrainAs(logout) {
                       top.linkTo(following.bottom)
                       start.linkTo(parent.start)
                   }) {
                   Text(text = "Logout")
               }

           }
       }

        item{
            Divider(color = Color.Gray, thickness = 1.dp)
        }

        items(threads ?: emptyList()) {item ->
            ThreadItem(
                thread = item,
                users = user,
                navController,
                sharedPref.getUserName(context))

        }
    }
}