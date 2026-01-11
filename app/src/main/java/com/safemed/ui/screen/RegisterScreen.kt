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
import androidx.compose.material.icons.filled.*
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
import com.safemed.ui.theme.TextSecondary
import com.safemed.ui.component.*

// Custom Colors matching Login Screen
private val LoginBgColor = Color(0xFFF3F4F6)
private val CardGreenStart = Color(0xFF134E42)
private val CardGreenEnd = Color(0xFF09221D)
private val LimeGreenAccent = Color(0xFFCCFF00)
private val InputLabelColor = Color(0xFF9CA3AF)
private val GoogleBtnColor = Color.White

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isRegisterSuccess) {
        if (uiState.isRegisterSuccess) {
            onRegisterSuccess()
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
            WelcomeRegisterCard()

            Spacer(modifier = Modifier.height(32.dp))

            // ===== 2. Form Section =====
            
            // Full Name Input
            CustomRegisterTextField(
                value = uiState.fullName,
                onValueChange = viewModel::onFullNameChange,
                label = "FULL NAME",
                placeholder = "Nguyen Van A",
                icon = Icons.Default.Person
            )
            if (uiState.fullNameError != null) {
                ErrorMessage(uiState.fullNameError)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Email Input
            CustomRegisterTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = "EMAIL ADDRESS",
                placeholder = "example@email.com",
                icon = Icons.Default.Email,
                keyboardType = KeyboardType.Email
            )
            if (uiState.emailError != null) {
                ErrorMessage(uiState.emailError)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Phone Input
            CustomRegisterTextField(
                value = uiState.phone,
                onValueChange = viewModel::onPhoneChange,
                label = "PHONE NUMBER",
                placeholder = "0123 456 789",
                icon = Icons.Default.Phone,
                keyboardType = KeyboardType.Phone
            )
            if (uiState.phoneError != null) {
                ErrorMessage(uiState.phoneError)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Password Input
            var isPasswordVisible by remember { mutableStateOf(false) }
            CustomRegisterTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = "PASSWORD",
                placeholder = "••••••••",
                icon = Icons.Default.Lock,
                keyboardType = KeyboardType.Password,
                isPassword = true,
                isPasswordVisible = isPasswordVisible,
                onPasswordVisibilityChange = { isPasswordVisible = !isPasswordVisible }
            )
            if (uiState.passwordError != null) {
                ErrorMessage(uiState.passwordError)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password Input
            var isConfirmPasswordVisible by remember { mutableStateOf(false) }
            CustomRegisterTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = "CONFIRM PASSWORD",
                placeholder = "••••••••",
                icon = Icons.Default.Lock,
                keyboardType = KeyboardType.Password,
                isPassword = true,
                isPasswordVisible = isConfirmPasswordVisible,
                onPasswordVisibilityChange = { isConfirmPasswordVisible = !isConfirmPasswordVisible }
            )
            if (uiState.confirmPasswordError != null) {
                ErrorMessage(uiState.confirmPasswordError)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Terms Checkbox
            SafeMedCheckboxWithLinks(
                checked = uiState.agreeToTerms,
                onCheckedChange = viewModel::onAgreeToTermsChange,
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp)
            )
            if (uiState.termsError != null) {
                ErrorMessage(uiState.termsError, modifier = Modifier.padding(start = 16.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // General Error
            uiState.generalError?.let {
                ErrorMessage(it, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ===== 3. Register Button =====
            Button(
                onClick = viewModel::onRegisterClick,
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
                            text = "REGISTER",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 5. Google Sign Up Button =====
            Button(
                onClick = { viewModel.onGoogleSignUpClick(activity) },
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
                 Text(
                    text = "Sign up with Google",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ===== 6. Footer =====
            val annotatedString = buildAnnotatedString {
                withStyle(style = SpanStyle(color = TextSecondary)) {
                    append("Already have an account? ")
                }
                pushStringAnnotation(tag = "LOGIN", annotation = "LOGIN")
                withStyle(style = SpanStyle(
                    color = CardGreenStart,
                    fontWeight = FontWeight.Bold
                )) {
                    append("Sign in now")
                }
                pop()
            }
            
            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun WelcomeRegisterCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
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
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFFFFF).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Medication, 
                        contentDescription = "Logo",
                        tint = LimeGreenAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White
                )
                
                Text(
                    text = "Join SafeMed today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LimeGreenAccent
                )
            }
        }
    }
}

@Composable
fun CustomRegisterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordVisibilityChange: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = InputLabelColor,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
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

@Composable
fun ErrorMessage(text: String?, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    if (text != null) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = textAlign,
            modifier = modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp)
        )
    }
}

