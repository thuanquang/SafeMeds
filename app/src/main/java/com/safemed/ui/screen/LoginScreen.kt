package com.safemed.ui.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safemed.R
import com.safemed.ui.theme.EmeraldGreen
import com.safemed.ui.theme.TextSecondary

// Custom Colors for this screen matching the design
private val LoginBgColor = Color(0xFFF3F4F6)
private val CardGreenStart = Color(0xFF134E42)
private val CardGreenEnd = Color(0xFF09221D)
private val LimeGreenAccent = Color(0xFFCCFF00) // The bright button color
private val InputLabelColor = Color(0xFF9CA3AF)
private val GoogleBtnColor = Color.White

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    // Lấy Activity context để truyền vào Google Sign-In
    val context = LocalContext.current
    val activity = context as Activity

    // Collect UI state từ ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Handle navigation khi đăng nhập thành công
    LaunchedEffect(uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) {
            onLoginSuccess()
            viewModel.onNavigateHandled()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginBgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // ===== 1. Welcome Card =====
            WelcomeCard()

            Spacer(modifier = Modifier.height(32.dp))

            // ===== 2. Form Section =====
            
            // Email Input
            CustomLoginTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = "EMAIL ADDRESS",
                placeholder = "example@email.com",
                icon = Icons.Default.Email,
                keyboardType = KeyboardType.Email,
                isError = uiState.emailError != null
            )
            if (uiState.emailError != null) {
                Text(
                    text = uiState.emailError ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Password Input
            var isPasswordVisible by remember { mutableStateOf(false) }
            CustomLoginTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = "PASSWORD",
                placeholder = "••••••••",
                icon = Icons.Default.Lock,
                keyboardType = KeyboardType.Password,
                isPassword = true,
                isPasswordVisible = isPasswordVisible,
                onPasswordVisibilityChange = { isPasswordVisible = !isPasswordVisible },
                isError = uiState.passwordError != null
            )
            if (uiState.passwordError != null) {
                Text(
                    text = uiState.passwordError ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp)
                )
            }

            // Forgot Password Link
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "Forgot Password?",
                    style = MaterialTheme.typography.labelMedium,
                    color = InputLabelColor,
                    modifier = Modifier.clickable { viewModel.onForgotPasswordClick() }
                )
            }

            // General Error
            uiState.generalError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
            }

            // ===== 3. Login Button =====
            Button(
                onClick = viewModel::onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(28.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LimeGreenAccent,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(28.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "LOGIN",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ===== 4. Divider =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f), color = Color.LightGray)
                Text(
                    text = "OR",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Divider(modifier = Modifier.weight(1f), color = Color.LightGray)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ===== 5. Google Sign In =====
            Button(
                onClick = { viewModel.onGoogleSignInClick(activity) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(28.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoogleBtnColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(28.dp),
                enabled = !uiState.isLoading
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Placeholder icon or real Google icon if available. 
                    // Assuming R.drawable.ic_google is not guaranteed, using created icon or text.
                    // But looking at previous code, it used `GoogleSignInButton` which might have resources.
                    // I'll try to use a simple text or localized icon if I can't find the resource id.
                    // Previous code: `GoogleSignInButton`. Using that same generic button logic here manually?
                    // Or I can copy the implementation of GoogleSignInButton here to match the style?
                    // The design requires a white button with shadow.
                    // I will look for R.drawable.ic_google_logo if it exists in the project context? 
                    // Safest path: Use a colored 'G' text or just "Sign in with Google".
                    // The design shows a logo. I'll check if I can import a painter.
                    // Previous code called `GoogleSignInButton`. Let's see if I can re-style it?
                    // Or I just re-implement it.
                    
                    // For now, I'll use a placeholder Box/Icon.
                    // If R.drawable.ic_google_logo exists I would use it.
                    // I'll assume we can use a generic "G" if no icon.
                    // But actually, typically there is an icon.
                    // Let's use `painterResource(id = R.drawable.ic_google_logo)` (common name) 
                    // But I don't know the ID.
                    // I will check if `com.safemed.R` was imported. Yes.
                    // I'll leave a TODO or use a grey box if I can't see the resource file.
                    // Actually, I can keep using `GoogleSignInButton` if I wrap it or modify it. 
                    // But the styles are different (User wants this design).
                    // I'll implement the button as shown in design.
                    
                    // Box(Modifier.size(24.dp).background(Color.Red)) // Debug placeholder
                     Text(
                        text = "Sign in with Google",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ===== 6. Footer =====
            val annotatedString = buildAnnotatedString {
                withStyle(style = SpanStyle(color = TextSecondary)) {
                    append("Don't have an account? ")
                }
                pushStringAnnotation(tag = "REGISTER", annotation = "REGISTER")
                withStyle(style = SpanStyle(
                    color = CardGreenStart, // Using dark green for link
                    fontWeight = FontWeight.Bold
                )) {
                    append("Sign up now")
                }
                pop()
            }
            
            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun WelcomeCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp), // Adjust height as needed
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(CardGreenStart, CardGreenEnd)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Pill Icon container
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFFFFF).copy(alpha = 0.1f)), // Semi-transparent circle
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        // Try to use Medication icon, or fallback
                        imageVector = Icons.Rounded.Medication, 
                        contentDescription = "Logo",
                        tint = LimeGreenAccent,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Welcome back to",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "SafeMed",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = LimeGreenAccent
                )
            }
        }
    }
}

@Composable
fun CustomLoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordVisibilityChange: (() -> Unit)? = null,
    isError: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp) // Taller to accommodate label and input
            .shadow(
                elevation = 8.dp, 
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = InputLabelColor, // Grey icon
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = InputLabelColor
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.Black
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    singleLine = true,
                    visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                )
            }
            
            // Show/Hide Password Icon
            if (isPassword && onPasswordVisibilityChange != null) {
                IconButton(onClick = onPasswordVisibilityChange) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                        tint = InputLabelColor
                    )
                }
            }
        }
    }
}


