package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.YouTubeOAuthHelper
import com.example.ui.components.MyVideoBrandLogo
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioCrimson
import com.example.ui.theme.StudioEmerald
import com.example.ui.viewmodel.MyVideoViewModel

private enum class AuthTab { LOGIN, REGISTER, RECOVER }

@Composable
fun AuthScreen(
    viewModel: MyVideoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val youtubeClientId by viewModel.youtubeClientId.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(AuthTab.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var channelName by remember { mutableStateOf("") }
    var recoveryCodeInput by remember { mutableStateOf("") }
    var generatedCodeNotice by remember { mutableStateOf<String?>(null) }
    var newPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showGoogleOAuthDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            MyVideoBrandLogo()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Creator Studio & Video Platform",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(22.dp))

            // Tab Switcher
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = activeTab == AuthTab.LOGIN,
                    onClick = { activeTab = AuthTab.LOGIN; errorMessage = null },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    label = { Text("Sign In") },
                    modifier = Modifier.testTag("auth_tab_login")
                )
                SegmentedButton(
                    selected = activeTab == AuthTab.REGISTER,
                    onClick = { activeTab = AuthTab.REGISTER; errorMessage = null },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    label = { Text("Register") },
                    modifier = Modifier.testTag("auth_tab_register")
                )
                SegmentedButton(
                    selected = activeTab == AuthTab.RECOVER,
                    onClick = { activeTab = AuthTab.RECOVER; errorMessage = null },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    label = { Text("Recover") },
                    modifier = Modifier.testTag("auth_tab_recover")
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    AnimatedContent(targetState = activeTab, label = "auth_form") { tab ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            when (tab) {
                                AuthTab.LOGIN -> {
                                    Text("Welcome back", style = MaterialTheme.typography.headlineSmall)
                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("Email or Gmail") },
                                        leadingIcon = { Icon(Icons.Outlined.Email, null) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("login_email_input")
                                    )
                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text("Password") },
                                        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                                        trailingIcon = {
                                            IconButton(onClick = { showPassword = !showPassword }) {
                                                Icon(
                                                    if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                    "Toggle password visibility"
                                                )
                                            }
                                        },
                                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("login_password_input")
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { activeTab = AuthTab.RECOVER }) {
                                            Text("Forgot password?")
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            isLoading = true
                                            errorMessage = null
                                            viewModel.loginWithEmail(email, password) { res ->
                                                isLoading = false
                                                res.onFailure { errorMessage = it.message }
                                            }
                                        },
                                        enabled = !isLoading,
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("login_submit_btn")
                                    ) {
                                        Text("Sign In")
                                    }
                                }

                                AuthTab.REGISTER -> {
                                    Text("Create MY VIDEO Account", style = MaterialTheme.typography.headlineSmall)
                                    OutlinedTextField(
                                        value = displayName,
                                        onValueChange = { displayName = it },
                                        label = { Text("Full Name") },
                                        leadingIcon = { Icon(Icons.Outlined.Person, null) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("reg_name_input")
                                    )
                                    OutlinedTextField(
                                        value = channelName,
                                        onValueChange = { channelName = it },
                                        label = { Text("Creator Channel Name") },
                                        leadingIcon = { Icon(Icons.Outlined.Tv, null) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("reg_channel_input")
                                    )
                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("Gmail or Email") },
                                        leadingIcon = { Icon(Icons.Outlined.Email, null) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("reg_email_input")
                                    )
                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text("Password (min 6 chars)") },
                                        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { showPassword = !showPassword }) {
                                                Icon(
                                                    if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                    "Toggle password visibility"
                                                )
                                            }
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("reg_password_input")
                                    )
                                    Button(
                                        onClick = {
                                            isLoading = true
                                            errorMessage = null
                                            viewModel.registerAccount(email, displayName, password, channelName) { res ->
                                                isLoading = false
                                                res.onFailure { errorMessage = it.message }
                                            }
                                        },
                                        enabled = !isLoading,
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("register_submit_btn")
                                    ) {
                                        Text("Create Account & Channel")
                                    }
                                }

                                AuthTab.RECOVER -> {
                                    Text("Password Recovery", style = MaterialTheme.typography.headlineSmall)
                                    Text(
                                        "Request a 6-digit recovery code for your registered MY VIDEO account.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("Registered Email") },
                                        leadingIcon = { Icon(Icons.Outlined.Email, null) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            errorMessage = null
                                            viewModel.requestRecoveryCode(email) { res ->
                                                res.onSuccess { code ->
                                                    generatedCodeNotice = "Recovery code issued: $code"
                                                }.onFailure { errorMessage = it.message }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Send Recovery Code")
                                    }
                                    if (generatedCodeNotice != null) {
                                        Surface(
                                            color = StudioEmerald.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = generatedCodeNotice!!,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = StudioEmerald,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                    OutlinedTextField(
                                        value = recoveryCodeInput,
                                        onValueChange = { recoveryCodeInput = it },
                                        label = { Text("6-Digit Recovery Code") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = newPassword,
                                        onValueChange = { newPassword = it },
                                        label = { Text("New Password") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = {
                                            errorMessage = null
                                            viewModel.resetPasswordWithCode(email, recoveryCodeInput, newPassword) { res ->
                                                res.onSuccess {
                                                    statusMessage = "Password reset! You can now sign in."
                                                    activeTab = AuthTab.LOGIN
                                                }.onFailure { errorMessage = it.message }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp)
                                    ) {
                                        Text("Reset Password")
                                    }
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (statusMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = statusMessage!!,
                            color = StudioEmerald,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Official Google OAuth button (never asks for Google password in-app)
                    OutlinedButton(
                        onClick = { showGoogleOAuthDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("google_oauth_btn")
                    ) {
                        Icon(Icons.Filled.AccountCircle, null, tint = StudioBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Continue with Google OAuth")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Official OAuth 2.0 consent — MY VIDEO never asks for your Google password.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Instant Role Switcher for Testing Seeded Creator & Admin Accounts
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Quick Access (Pre-seeded Profiles)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.quickSignInDemo("user_creator_1") },
                            modifier = Modifier.weight(1f).testTag("quick_creator_btn")
                        ) {
                            Icon(Icons.Outlined.VideoLibrary, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Creator", maxLines = 1)
                        }
                        FilledTonalButton(
                            onClick = { viewModel.quickSignInDemo("user_admin_1") },
                            modifier = Modifier.weight(1f).testTag("quick_admin_btn")
                        ) {
                            Icon(Icons.Outlined.AdminPanelSettings, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Admin", maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    if (showGoogleOAuthDialog) {
        var clientIdInput by remember { mutableStateOf(youtubeClientId) }
        var oauthEmail by remember { mutableStateOf("") }
        var oauthName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showGoogleOAuthDialog = false },
            title = { Text("Google OAuth 2.0 Sign-In") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "MY VIDEO uses official Google OAuth 2.0 PKCE in your browser. We never collect or store your Google password.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = clientIdInput,
                        onValueChange = { clientIdInput = it },
                        label = { Text("Google OAuth Client ID (Optional)") },
                        placeholder = { Text("xxxx.apps.googleusercontent.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (clientIdInput.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                viewModel.saveYoutubeOAuthClientId(clientIdInput)
                                YouTubeOAuthHelper.launchBrowserOAuth(
                                    context = context,
                                    clientId = clientIdInput.trim(),
                                    scopes = listOf("openid", "email", "profile", YouTubeOAuthHelper.YOUTUBE_READONLY_SCOPE)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.OpenInBrowser, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Google Consent Screen")
                        }
                    }
                    HorizontalDivider()
                    Text(
                        "Or link your Gmail identity for this device session:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    OutlinedTextField(
                        value = oauthEmail,
                        onValueChange = { oauthEmail = it },
                        label = { Text("Gmail Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = oauthName,
                        onValueChange = { oauthName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (oauthEmail.isNotBlank() && oauthEmail.contains("@")) {
                            viewModel.signInWithGoogleOAuth(oauthEmail, oauthName) {
                                showGoogleOAuthDialog = false
                            }
                        }
                    },
                    enabled = oauthEmail.contains("@")
                ) {
                    Text("Complete Sign-In")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleOAuthDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
