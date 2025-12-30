package com.safemed.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.actionCodeSettings
import com.google.firebase.firestore.FirebaseFirestore
import com.safemed.R
import com.safemed.data.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository xử lý Authentication với Firebase
 * Hỗ trợ: Email/Password, Google Sign-In và Email Link (passwordless)
 */
@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    companion object {
        private const val TAG = "AuthRepository"
        private const val USERS_COLLECTION = "users"
    }

    // ==================== COMMON AUTH METHODS ====================

    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    fun isUserLoggedIn(): Boolean = auth.currentUser != null
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // ==================== EMAIL/PASSWORD AUTHENTICATION ====================

    /**
     * Đăng nhập bằng Email và Password
     */
    suspend fun signInWithEmailPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User is null after sign in"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Email/Password sign-in failed", e)
            Result.failure(e)
        }
    }

    /**
     * Đăng ký bằng Email và Password
     */
    suspend fun createUserWithEmailPassword(
        email: String,
        password: String,
        fullName: String,
        phone: String
    ): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user

            if (user != null) {
                // Lưu thông tin user vào Firestore
                val newUser = User(
                    uid = user.uid,
                    email = email,
                    fullName = fullName,
                    avatarUrl = "",
                    createdAt = System.currentTimeMillis()
                )
                db.collection(USERS_COLLECTION).document(user.uid).set(newUser).await()
                Log.d(TAG, "User created in Firestore: ${user.uid}")
                Result.success(user)
            } else {
                Result.failure(Exception("User is null after registration"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Email/Password registration failed", e)
            Result.failure(e)
        }
    }

    /**
     * Gửi email reset mật khẩu
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send password reset email", e)
            Result.failure(e)
        }
    }

    // ==================== GOOGLE SIGN-IN ====================

    /**
     * Tạo nonce cho Google Sign-In (bảo mật)
     */
    private fun generateNonce(): String {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    /**
     * Đăng nhập bằng Google sử dụng Credential Manager
     * QUAN TRỌNG: Cần truyền Activity context để Credential Manager hoạt động đúng
     */
    suspend fun signInWithGoogle(activityContext: Activity): Result<FirebaseUser> {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setNonce(generateNonce())
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Sử dụng Activity context thay vì Application context
            val credManager = CredentialManager.create(activityContext)
            val result = credManager.getCredential(activityContext, request)
            handleGoogleSignInResult(result)
        } catch (e: NoCredentialException) {
            Log.e(TAG, "No Google account found on device", e)
            Result.failure(Exception("Không tìm thấy tài khoản Google trên thiết bị. Vui lòng thêm tài khoản Google trong Settings."))
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google Sign-In failed: ${e.type}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    private suspend fun handleGoogleSignInResult(result: GetCredentialResponse): Result<FirebaseUser> {
        val credential = result.credential

        return when {
            credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse Google ID token", e)
                    Result.failure(e)
                }
            }
            else -> {
                Log.w(TAG, "Credential is not of type Google ID!")
                Result.failure(Exception("Invalid credential type"))
            }
        }
    }

    private suspend fun firebaseAuthWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user

            if (user != null) {
                saveUserToFirestore(user, authResult.additionalUserInfo?.isNewUser ?: false)
                Result.success(user)
            } else {
                Result.failure(Exception("User is null after sign in"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase auth with Google failed", e)
            Result.failure(e)
        }
    }

    // ==================== EMAIL LINK AUTHENTICATION ====================

    /**
     * Gửi email link để đăng nhập (passwordless)
     */
    suspend fun sendSignInLinkToEmail(email: String): Result<Unit> {
        return try {
            val actionCodeSettings = actionCodeSettings {
                url = "https://safemed-ueh.firebaseapp.com/finishSignUp"
                handleCodeInApp = true
                setAndroidPackageName("com.safemed", true, "24")
            }
            auth.sendSignInLinkToEmail(email, actionCodeSettings).await()
            Log.d(TAG, "Email link sent to $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send email link", e)
            Result.failure(e)
        }
    }

    fun isSignInWithEmailLink(emailLink: String): Boolean = auth.isSignInWithEmailLink(emailLink)

    suspend fun signInWithEmailLink(email: String, emailLink: String): Result<FirebaseUser> {
        return try {
            if (!auth.isSignInWithEmailLink(emailLink)) {
                return Result.failure(Exception("Invalid email link"))
            }
            val authResult = auth.signInWithEmailLink(email, emailLink).await()
            val user = authResult.user
            if (user != null) {
                saveUserToFirestore(user, authResult.additionalUserInfo?.isNewUser ?: false)
                Result.success(user)
            } else {
                Result.failure(Exception("User is null after sign in"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Email link sign-in failed", e)
            Result.failure(e)
        }
    }

    // ==================== SIGN OUT ====================

    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            val clearRequest = ClearCredentialStateRequest()
            credentialManager.clearCredentialState(clearRequest)
            Log.d(TAG, "User signed out successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed", e)
            Result.failure(e)
        }
    }

    // ==================== HELPER ====================

    private suspend fun saveUserToFirestore(firebaseUser: FirebaseUser, isNewUser: Boolean) {
        try {
            val userDoc = db.collection(USERS_COLLECTION).document(firebaseUser.uid)
            if (isNewUser) {
                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    fullName = firebaseUser.displayName ?: "",
                    avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
                    createdAt = System.currentTimeMillis()
                )
                userDoc.set(user).await()
            } else {
                val updates = hashMapOf<String, Any>(
                    "email" to (firebaseUser.email ?: ""),
                    "fullName" to (firebaseUser.displayName ?: ""),
                    "avatarUrl" to (firebaseUser.photoUrl?.toString() ?: "")
                )
                userDoc.update(updates).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user to Firestore", e)
        }
    }
}
