package com.example.login

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.login.databinding.ActivityLoginBinding
import com.example.login.databinding.ActivityMainBinding
import com.example.login.databinding.ActivityRegisterBinding
import com.example.login.model.Gerai
import com.jakewharton.rxbinding2.widget.RxTextView
import java.util.*
import io.reactivex.*
import io.reactivex.Observable

@SuppressLint("CheckResult")
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val helper: FirebaseHelper = FirebaseHelper()
    private val helperReference: PreferenceHelper = PreferenceHelper()
    private var geraiList: List<Gerai> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        helper.initFirebase()
        helperReference.initPreference(this)
        showGeraiList()


//        Email validation
        val emailStream = RxTextView.textChanges(binding.etEmail)
            .skipInitialValue()
            .map { email ->
                email.isEmpty()
            }
        emailStream.subscribe {
            showTextMinimalAlert(it, "Email")
        }

//        Password Validation
        val passwordStream = RxTextView.textChanges(binding.etPassword)
            .skipInitialValue()
            .map { password ->
                password.isEmpty()
            }
        passwordStream.subscribe {
            showTextMinimalAlert(it, "Password")
        }

//Button Enable true or false
        val invalidFieldsStream = Observable.combineLatest(
            emailStream,
            passwordStream
        ) { emailInvalid: Boolean, passwordInvalid: Boolean ->
            !emailInvalid && !passwordInvalid
        }
        invalidFieldsStream.subscribe { isValid ->
            if (isValid) {
                binding.btnLogin.isEnabled = true
                binding.btnLogin.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_color)
            } else {
                binding.btnLogin.isEnabled = false
                binding.btnLogin.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.darker_gray)
            }
        }

//        click
        binding.btnLogin.setOnClickListener{
            helper.loginUser(
                email = binding.etEmail.text.toString(),
                password = binding.etPassword.text.toString(),
                onSuccess = {
                    helperReference.saveShopName(
                        shopName = binding.spnLocation.selectedItem.toString()
                    )
                    helperReference.saveVisit(
                        visit = binding.tilVisit.editText?.text.toString()
                    )
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                },
                onFailure = {
                    Toast.makeText(this, "Error:$it", Toast.LENGTH_LONG).show()
                }
            )


        }
        binding.tvHaventAccount.setOnClickListener{
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.spnLocation.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedGeraiName = binding.spnLocation.selectedItem.toString()
                val selectedGerai = geraiList.find{it.status==selectedGeraiName}
                selectedGerai?.let {
                    helperReference.saveKodeGerai(it.kode_unik_gerai)
                    helperReference.saveStatus(it.status)
                    helperReference.saveRegion(it.region)
                    helperReference.saveStoreName(it.store_name)

                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }
    }

    private fun showTextMinimalAlert(isNotValid: Boolean, text: String) {
        if (text == "Email")
            binding.etEmail.error = if (isNotValid) "$text tidak boleh kosong" else null
       else if (text == "Password")
            binding.etPassword.error = if (isNotValid) "$text harus lebih dari 4 huruf" else null
    }

    private fun showLocation(geraiNames:List<String>) {
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, geraiNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnLocation.adapter = adapter
    }

    fun showGeraiList(){
        helper.getDataGerai(
            onSuccess = {list ->
                geraiList = list
                val geraiNames = geraiList.map{
                    it.status

                }
                showLocation(geraiNames)
            },
            onFailure = {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        )
    }




}