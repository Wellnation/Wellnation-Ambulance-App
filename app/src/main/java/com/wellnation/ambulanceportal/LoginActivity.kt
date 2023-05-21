package com.wellnation.ambulanceportal

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wellnation.ambulanceportal.databinding.ActivityLoginBinding
import kotlin.math.log

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get a reference to the shared preferences object
        sharedPreferences = getSharedPreferences("myPref", Context.MODE_PRIVATE)
        val value = sharedPreferences.getBoolean("authStatus", false)
        val id = sharedPreferences.getString("ambulanceid", " ")
        Log.d("id from Sharepref",id.toString())
        if (value){
            val db = id?.let { FirebaseFirestore.getInstance().collection("ambulance").document(it)}
            db?.get()?.addOnSuccessListener {
                val ambulancedata = it.toObject(Ambulance::class.java)
                if (ambulancedata != null) {
                    ambulanceData.id = ambulancedata.id
                }
                if (ambulancedata != null) {
                    ambulanceData.driverName = ambulancedata.driverName
                }
                if (ambulancedata != null) {
                    ambulanceData.vechilenumber = ambulancedata.vechilenumber
                }
                if (ambulancedata != null) {
                    ambulanceData.contact = ambulancedata.contact
                }
                Log.d("id from Companion",ambulanceData.id.toString())
            }
            //startActivity(Intent(this, MainActivity::class.java))
            //finish()
        }
        binding.btnLogin.setOnClickListener {
            loginDriver()
        }
    }

    private fun loginDriver() {
        val id = binding.etemail.text.toString()
        val pass = binding.etPass.text.toString()
        val db = FirebaseFirestore.getInstance().collection("ambulance")
        val query = db.whereEqualTo("vechilenumber", id)

        query.get().addOnSuccessListener { result ->
            if (result.isEmpty) {
                Toast.makeText(this, "Invalid vehicle number", Toast.LENGTH_SHORT).show()
            } else {
                val ambulancedata = result.documents[0].toObject(Ambulance::class.java)
                if (ambulancedata != null) {
                    ambulanceData.id = ambulancedata.id
                    ambulanceData.driverName = ambulancedata.driverName
                    ambulanceData.vechilenumber = ambulancedata.vechilenumber
                    ambulanceData.contact = ambulancedata.contact
                }
                val password = result.documents[0].getString("authpass")
                if (pass == password) {
                    // Store the authentication status in shared preferences
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("authStatus", true)
                    editor.putString("ambulanceid",ambulanceData.id)
                    editor.apply()
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
