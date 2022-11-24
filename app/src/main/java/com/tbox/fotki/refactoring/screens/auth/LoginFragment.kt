package com.tbox.fotki.refactoring.screens.auth

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.tbox.fotki.R
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


class LoginFragment : Fragment() {

    private val viewModel: SignViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }
/*

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.subscribeToEvents().observe(viewLifecycleOwner, Observer {
            when (it) {
                is OnSuccess -> openApp()

                is OnError -> {
                    showAlertDialog(
                        requireContext(),
                        getString(R.string.dialog_error_title),
                        it.message
                    )
                }
                is OnFailure -> {
                    showAlertDialog(
                        requireContext(),
                        getString(R.string.dialog_error_title),
                        getString(it.message)
                    )
                }
                is Loading -> {

                }
            }
        })
    }

    private fun openApp() {
        */
/* if (preferenceHelper.isTutorialFinished()) {
             replaceFragment(
                 R.id.fragment_container_view,
                 HomeFragment(), false
             )
         } else {
             replaceFragment(
                 R.id.fragment_container_view,
                 TutorialFragment.createNewInstance(TutorialFragment.FROM_SIGN_UP), false
             )
         }*//*

        replaceFragment(
            R.id.fragment_container_view,
            HomeFragment(), false
        )

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        */
/*signUp.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }*//*

        loginBtn.setOnClickListener {
            validate()
        }
        forgotText.setOnClickListener {
            replaceFragment(
                R.id.fragment_container_view,
                ForgotFragment(), true
            )
        }
        setIMEOptions()
        val mGoogleSignInClient = GoogleSignIn.getClient((activity as MainActivity), gso)

        connect_google.setOnClickListener {
            val signInIntent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, GOOGLE_SIGN_CODE)
        }

        connect_fb.setOnClickListener { fbLogin() }
        connect_apple.setOnClickListener { appleLogin() }

        val signSpanText = SpannableString(resources.getString(R.string.create_acc))
        generateColourSpan(signSpanText, "Sign Up")

        signUp.text = signSpanText
        signUp.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }
    }


    private fun appleLogin() {
        val provider = OAuthProvider.newBuilder("apple.com")
        provider.setScopes(listOf("email", "name"))

        val auth = FirebaseAuth.getInstance()

        val pending = auth.pendingAuthResult
        if (pending != null) {
            pending.addOnSuccessListener { authResult ->
                Log.d("apple login", "checkPending:onSuccess:$authResult")
                // Get the user profile with authResult.getUser() and
                // authResult.getAdditionalUserInfo(), and the ID
                // token from Apple with authResult.getCredential().
                val user = authResult.user

                val id = user?.uid ?: ""
                val email = user?.email ?: ""
                val fistName = user?.displayName ?: ""
                val latestName = user?.displayName ?: ""
                viewModel.startSocialAuth(
                    ApiModels.SocialNetworkNetwork.APPLE,
                    id,
                    email,
                    fistName,
                    latestName
                )

            }.addOnFailureListener { e ->
                Log.w("apple login", "checkPending:onFailure", e)
                showDialogMessage("Error", e.toString())
            }
        } else {
            Log.d("apple login", "pending: null")
            // create new login request
            auth.startActivityForSignInWithProvider(requireActivity(), provider.build())
                .addOnSuccessListener { authResult ->
                    // Sign-in successful!
                    Log.d("apple login", "activitySignIn:onSuccess:${authResult.user}")
                    val user = authResult.user

                    val id = user?.uid ?: ""
                    val email = user?.email ?: ""
                    val fistName = user?.displayName ?: ""
                    val latestName = user?.displayName ?: ""
                    viewModel.startSocialAuth(
                        ApiModels.SocialNetworkNetwork.APPLE,
                        id,
                        email,
                        fistName,
                        latestName
                    )
                }
                .addOnFailureListener { e ->
                    Log.w("apple login", "activitySignIn:onFailure", e)
                    showDialogMessage("Error", e.toString())
                }

        }

    }

    private fun generateColourSpan(
        spannable: SpannableString,
        str: String
    ) {
        val color = context?.getColor(R.color.purple_arrow)
        color?.let {
            spannable.setSpan(
                ForegroundColorSpan(color),
                spannable.indexOf(str),
                spannable.indexOf(str) + str.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun validate() {
        val email = emailText.text.toString()
        val pass = password.text.toString()
        val validEmail = isValidEmail(email)
        val validPass = isValidPass(pass)
        if (!validEmail) {
            emailText.error = getString(R.string.email_not_vaid)
        } else if (!validPass) {
            password.error = getString(R.string.pass_not_valid)
        } else {
            viewModel.login(email, pass)
        }

    }

    private fun setIMEOptions() {
        emailText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                password.requestFocus()
                true
            }
            false
        }
        password.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                loginBtn.performClick()
                true
            }
            false
        }

    }

    private fun fbLogin() {
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult?) {
                    result?.let {
                        val request = GraphRequest.newMeRequest(
                            AccessToken.getCurrentAccessToken()
                        ) { jsonObj, response ->
                            val userId = jsonObj.getSafeString("id")
                            val fbName = jsonObj.getSafeString("name")
                            val email = jsonObj.getSafeString("email")
                            val names = fbName.split(" ", ignoreCase = true, limit = 2)
                            var firstName = ""
                            var lastName = "names[1]"
                            if (names.size > 1) {
                                firstName = names[0]
                                lastName = names[1]
                            } else {
                                firstName = fbName
                                lastName = firstName
                            }

                            viewModel.startSocialAuth(
                                ApiModels.SocialNetworkNetwork.FACEBOOK,
                                userId,
                                email,
                                firstName,
                                lastName
                            )

                        }
                        request.parameters =
                            Bundle().apply { putString("fields", "id,name,email,gender") }
                        request.executeAsync()
                    }
                }

                override fun onCancel() {
                }

                override fun onError(error: FacebookException?) {
                    error.toString()
                }
            })
    }

    private val GOOGLE_SIGN_CODE = 123
    private val callbackManager = CallbackManager.Factory.create()

    private var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .build()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            GOOGLE_SIGN_CODE -> handleLogin(GoogleSignIn.getSignedInAccountFromIntent(data))
        }
    }

    private fun handleLogin(task: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
            // Signed in successfully, show authenticated UI.
            account.toString()
            account?.let { acc ->
                val id = acc.id ?: ""
                val email = acc.email ?: ""
                val fistName = acc.givenName ?: ""
                val latestName = acc.familyName ?: ""
                viewModel.startSocialAuth(
                    ApiModels.SocialNetworkNetwork.GOOGLE,
                    id,
                    email,
                    fistName,
                    latestName

                )
            }
        } catch (e: ApiException) {
            Log.w("ds", "signInResult:failed code=" + e.statusCode)
        }
    }
*/

}
