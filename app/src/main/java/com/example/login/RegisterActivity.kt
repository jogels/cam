package com.example.login

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.login.databinding.ActivityRegisterBinding
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Completable.merge
import io.reactivex.Flowable.merge
import io.reactivex.Maybe.merge
import io.reactivex.Single.merge
import java.awt.font.TextAttribute
import java.util.*
import io.reactivex.*
import io.reactivex.Observable

@SuppressLint("CheckResult")
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val helper: FirebaseHelper = FirebaseHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        helper.initFirebase()
        showCity()
// Fullname Validation
        val nameStream = RxTextView.textChanges(binding.etFullname)
            .skipInitialValue()
            .map { name ->
                name.isEmpty()
            }
        nameStream.subscribe {
            showNameExistAlert(it)
        }

//        email Validation
        val emailStream = RxTextView.textChanges(binding.etEmail)
            .skipInitialValue()
            .map { email ->
                !Patterns.EMAIL_ADDRESS.matcher(email).matches()
            }
        emailStream.subscribe {
            showEmailValidAlert(it)
        }
//        Password Validation
        val passwordStream = RxTextView.textChanges(binding.etPassword)
            .skipInitialValue()
            .map { password ->
                password.length < 4
            }
        passwordStream.subscribe {
            showTextMinimalAlert(it, "Password")
        }
//        Confirm Password Validation
        val passwordConfirmStream = Observable.merge(
            RxTextView.textChanges(binding.etPassword)
                .skipInitialValue()
                .map { password ->
                    password.toString() != binding.etConfirmPassword.text.toString()
                },
            RxTextView.textChanges(binding.etConfirmPassword)
                .skipInitialValue()
                .map { confirmPassword ->
                    confirmPassword.toString() != binding.etPassword.text.toString()
                })
        passwordConfirmStream.subscribe {
            showPasswordConfirmAlert(it)
        }

//        Button Enable true or false
        val invalidFieldsStream = Observable.combineLatest(
            nameStream,
            emailStream,
            passwordStream,
            passwordConfirmStream
        ) { nameInvalid: Boolean, emailInvalid: Boolean, passwordInvalid: Boolean, passwordConfirmInvalid: Boolean ->
            !nameInvalid && !emailInvalid && !passwordInvalid && !passwordConfirmInvalid
        }
        invalidFieldsStream.subscribe { isValid ->
            if (isValid) {
                binding.btnRegister.isEnabled = true
                binding.btnRegister.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.primary_color)
            } else {
                binding.btnRegister.isEnabled = false
                binding.btnRegister.backgroundTintList =
                    ContextCompat.getColorStateList(this, android.R.color.darker_gray)
            }
        }
//        click
        binding.btnRegister.setOnClickListener {
            helper.registerUser(
                email = binding.etEmail.text.toString(),
                password = binding.etPassword.text.toString(),
                onSuccess = {
                    helper.saveDataUser(
                        id_user = it?.uid.orEmpty(),
                        area = binding.spnCity.selectedItem.toString(),
                        fullname = binding.etFullname.text.toString(),
                        email = binding.etEmail.text.toString(),
                        password = binding.etPassword.text.toString(),
                        onSuccess = {
                            startActivity(Intent(this, LoginActivity::class.java))
                        },
                        onFailure = {
                            Toast.makeText(this, "Error:$it", Toast.LENGTH_LONG).show()
                        }
                    )

                },
                onFailure = {
                    Toast.makeText(this, "Error:$it", Toast.LENGTH_LONG).show()
                }
            )
        }
        binding.tvHaveAccount.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun showCity() {
        val city = arrayListOf<String>("Jakarta", "Bandung", "Surabaya")
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, city)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnCity.adapter = adapter
    }

    private fun showNameExistAlert(isNotValid: Boolean) {
        binding.etFullname.error = if (isNotValid) "Nama tidak boleh kosong" else null
    }

    private fun showTextMinimalAlert(isNotValid: Boolean, text: String) {
        if (text == "Password")
            binding.etPassword.error = if (isNotValid) "$text harus lebih dari 4 huruf" else null
    }

    private fun showEmailValidAlert(isNotValid: Boolean) {
        binding.etEmail.error = if (isNotValid) "Email tidak valid" else null
    }

    private fun showPasswordConfirmAlert(isNotValid: Boolean) {
        binding.etConfirmPassword.error = if (isNotValid) "Password tidak sama" else null
    }


}