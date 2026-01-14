/**
 * MachinaEar Login Module
 * Handles login, registration, and 2FA flows
 */

(function () {
    'use strict';

    // Configuration
    const contextPath = window.location.pathname.replace(/\/[^/]*$/, '');
    const API_URL = window.location.origin + contextPath + '/iam';
    const ALTCHA_CHALLENGE_URL = API_URL + '/altcha/challenge';
    const params = new URLSearchParams(window.location.search);
    const returnTo = params.get('returnTo');
    // App URLs for OAuth flow
    const APP_ROOT_URL = 'https://machinaear.me';
    const OAUTH_CLIENT_ID = 'machina-ear-web';
    const OAUTH_REDIRECT_URI = APP_ROOT_URL + '/auth/callback';

    // ==================== PKCE Helper Functions ====================

    /**
     * Generate a cryptographically random code verifier for PKCE
     */
    function generateCodeVerifier() {
        const array = new Uint8Array(32);
        crypto.getRandomValues(array);
        return base64UrlEncode(array);
    }

    /**
     * Generate SHA-256 code challenge from verifier
     */
    async function generateCodeChallenge(verifier) {
        const encoder = new TextEncoder();
        const data = encoder.encode(verifier);
        const digest = await crypto.subtle.digest('SHA-256', data);
        return base64UrlEncode(new Uint8Array(digest));
    }

    /**
     * Base64 URL encode without padding
     */
    function base64UrlEncode(array) {
        const base64 = btoa(String.fromCharCode.apply(null, array));
        return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    }

    /**
     * Generate random state for CSRF protection
     */
    function generateState() {
        const array = new Uint8Array(16);
        crypto.getRandomValues(array);
        return base64UrlEncode(array);
    }

    /**
     * Store PKCE parameters in sessionStorage for the client app to use
     */
    function storePKCEParams(verifier, state) {
        sessionStorage.setItem('pkce_verifier', verifier);
        sessionStorage.setItem('pkce_state', state);
    }

    /**
     * Initiate OAuth flow - redirect to authorize endpoint
     * This function mirrors the client-side OAuth flow
     */
    async function initiateOAuthFlow() {
        const codeVerifier = generateCodeVerifier();
        const codeChallenge = await generateCodeChallenge(codeVerifier);
        const state = generateState();

        // Store PKCE params for the client app callback to use
        storePKCEParams(codeVerifier, state);

        const authParams = new URLSearchParams({
            response_type: 'code',
            client_id: OAUTH_CLIENT_ID,
            redirect_uri: OAUTH_REDIRECT_URI,
            code_challenge: codeChallenge,
            code_challenge_method: 'S256',
            state: state
        });

        // Redirect to authorize endpoint (same origin)
        window.location.href = `${API_URL}/auth/authorize?${authParams.toString()}`;
    }

    // State
    let requires2FA = false;
    let currentForm = 'login';
    let loginAltchaValue = null;
    let registerAltchaValue = null;
    let pendingVerificationEmail = null; // Email awaiting code verification

    // DOM Elements Cache
    const elements = {
        // Forms
        loginForm: null,
        registerForm: null,
        forgotPasswordForm: null,
        resetPasswordForm: null,
        verifyEmailPage: null,
        // Alerts
        errorAlert: null,
        infoAlert: null,
        successAlert: null,
        // Login form elements
        loginEmail: null,
        loginPassword: null,
        totpCode: null,
        recoveryCode: null,
        loginBtn: null,
        twoFactorSection: null,
        toggle2FALink: null,
        forgotPasswordLink: null,
        // Register form elements
        registerEmail: null,
        registerUsername: null,
        registerPassword: null,
        confirmPassword: null,
        registerBtn: null,
        passwordStrength: null,
        strengthFill: null,
        strengthLabel: null,
        // Forgot password elements
        forgotEmail: null,
        forgotPasswordBtn: null,
        // Reset password elements
        newPassword: null,
        confirmNewPassword: null,
        resetPasswordBtn: null,
        resetPasswordStrength: null,
        resetStrengthFill: null,
        resetStrengthLabel: null,
        // Verify email elements
        verifyEmailMessage: null,
        verifyEmailBackBtn: null,
        // Verification code elements
        verifyCodeForm: null,
        verificationCode: null,
        verifyCodeBtn: null,
        resendCodeBtn: null,
        verifyCodeEmail: null,
        // Titles
        formTitle: null,
        formSubtitle: null,
        // ALTCHA elements
        loginAltcha: null,
        registerAltcha: null
    };

    /**
     * Initialize the module
     */
    function init() {
        cacheElements();
        bindEvents();
        initAltchaWidgets();
        checkInitialMode();
    }

    /**
     * Check if we should show register form based on URL parameter
     */
    function checkInitialMode() {
        const mode = params.get('mode');
        const token = params.get('token');

        if (token) {
            // Check if this is an email verification or password reset
            const path = window.location.pathname;
            
            // New: Check mode parameter first (for new URLs)
            if (mode === 'reset-password') {
                showResetPasswordForm(token);
            } else if (mode === 'verify-email') {
                showVerifyEmailPage();
                handleEmailVerification(token);
            }
            // Legacy: Check path (for old URLs)
            else if (path.includes('verify-email')) {
                showVerifyEmailPage();
                handleEmailVerification(token);
            } else if (path.includes('reset-password')) {
                showResetPasswordForm(token);
            }
        } else if (mode === 'register') {
            showRegisterForm();
        }
    }

    /**
     * Cache DOM elements
     */
    function cacheElements() {
        elements.loginForm = document.getElementById('loginForm');
        elements.registerForm = document.getElementById('registerForm');
        elements.forgotPasswordForm = document.getElementById('forgotPasswordForm');
        elements.resetPasswordForm = document.getElementById('resetPasswordForm');
        elements.verifyEmailPage = document.getElementById('verifyEmailPage');
        elements.errorAlert = document.getElementById('errorAlert');
        elements.infoAlert = document.getElementById('infoAlert');
        elements.successAlert = document.getElementById('successAlert');
        elements.loginEmail = document.getElementById('loginEmail');
        elements.loginPassword = document.getElementById('loginPassword');
        elements.totpCode = document.getElementById('totpCode');
        elements.recoveryCode = document.getElementById('recoveryCode');
        elements.loginBtn = document.getElementById('loginBtn');
        elements.twoFactorSection = document.getElementById('twoFactorSection');
        elements.toggle2FALink = document.getElementById('toggle2FALink');
        elements.forgotPasswordLink = document.getElementById('forgotPasswordLink');
        elements.registerEmail = document.getElementById('registerEmail');
        elements.registerUsername = document.getElementById('registerUsername');
        elements.registerPassword = document.getElementById('registerPassword');
        elements.confirmPassword = document.getElementById('confirmPassword');
        elements.registerBtn = document.getElementById('registerBtn');
        elements.passwordStrength = document.getElementById('passwordStrength');
        elements.strengthFill = document.getElementById('strengthFill');
        elements.strengthLabel = document.getElementById('strengthLabel');
        elements.forgotEmail = document.getElementById('forgotEmail');
        elements.forgotPasswordBtn = document.getElementById('forgotPasswordBtn');
        elements.newPassword = document.getElementById('newPassword');
        elements.confirmNewPassword = document.getElementById('confirmNewPassword');
        elements.resetPasswordBtn = document.getElementById('resetPasswordBtn');
        elements.resetPasswordStrength = document.getElementById('resetPasswordStrength');
        elements.resetStrengthFill = document.getElementById('resetStrengthFill');
        elements.resetStrengthLabel = document.getElementById('resetStrengthLabel');
        elements.verifyEmailMessage = document.getElementById('verifyEmailMessage');
        elements.verifyEmailBackBtn = document.getElementById('verifyEmailBackBtn');
        elements.verifyCodeForm = document.getElementById('verifyCodeForm');
        elements.verificationCode = document.getElementById('verificationCode');
        elements.verifyCodeBtn = document.getElementById('verifyCodeBtn');
        elements.resendCodeBtn = document.getElementById('resendCodeBtn');
        elements.verifyCodeEmail = document.getElementById('verifyCodeEmail');
        elements.formTitle = document.getElementById('formTitle');
        elements.formSubtitle = document.getElementById('formSubtitle');
        elements.loginAltcha = document.getElementById('loginAltcha');
        elements.registerAltcha = document.getElementById('registerAltcha');
    }

    /**
     * Initialize ALTCHA widgets
     */
    function initAltchaWidgets() {
        // Set the challenge URL for both widgets
        if (elements.loginAltcha) {
            elements.loginAltcha.setAttribute('challengeurl', ALTCHA_CHALLENGE_URL);
            elements.loginAltcha.addEventListener('statechange', (e) => {
                if (e.detail && e.detail.state === 'verified') {
                    loginAltchaValue = e.detail.payload;
                }
            });
        }
        
        if (elements.registerAltcha) {
            elements.registerAltcha.setAttribute('challengeurl', ALTCHA_CHALLENGE_URL);
            elements.registerAltcha.addEventListener('statechange', (e) => {
                if (e.detail && e.detail.state === 'verified') {
                    registerAltchaValue = e.detail.payload;
                }
            });
        }
    }

    /**
     * Reset ALTCHA widget (called after failed attempt or form switch)
     */
    function resetAltcha(widgetElement) {
        if (widgetElement && typeof widgetElement.reset === 'function') {
            widgetElement.reset();
        }
        // Clear stored values
        if (widgetElement === elements.loginAltcha) {
            loginAltchaValue = null;
        } else if (widgetElement === elements.registerAltcha) {
            registerAltchaValue = null;
        }
    }

    /**
     * Bind event listeners
     */
    function bindEvents() {
        // Form submissions
        elements.loginForm?.addEventListener('submit', handleLogin);
        elements.registerForm?.addEventListener('submit', handleRegister);
        elements.forgotPasswordForm?.addEventListener('submit', handleForgotPassword);
        elements.resetPasswordForm?.addEventListener('submit', handleResetPassword);
        elements.verifyCodeForm?.addEventListener('submit', handleVerifyCode);
        elements.resendCodeBtn?.addEventListener('click', handleResendCode);

        // Password strength
        elements.registerPassword?.addEventListener('input', handlePasswordInput);
        elements.newPassword?.addEventListener('input', handleResetPasswordInput);

        // 2FA toggle
        elements.toggle2FALink?.addEventListener('click', toggle2FASection);

        // Forgot password link
        elements.forgotPasswordLink?.addEventListener('click', showForgotPasswordForm);

        // Verify email back button
        elements.verifyEmailBackBtn?.addEventListener('click', showLoginForm);

        // Form toggle links
        document.querySelectorAll('[data-action="show-login"]').forEach(el => {
            el.addEventListener('click', showLoginForm);
        });
        document.querySelectorAll('[data-action="show-register"]').forEach(el => {
            el.addEventListener('click', showRegisterForm);
        });
    }

    // ==================== Alert Functions ====================

    function showError(message) {
        hideAlerts();
        if (elements.errorAlert) {
            elements.errorAlert.textContent = message;
            elements.errorAlert.classList.remove('hidden');
        }
    }

    function showInfo(message) {
        hideAlerts();
        if (elements.infoAlert) {
            elements.infoAlert.textContent = message;
            elements.infoAlert.classList.remove('hidden');
        }
    }

    function showSuccess(message) {
        hideAlerts();
        if (elements.successAlert) {
            elements.successAlert.textContent = message;
            elements.successAlert.classList.remove('hidden');
        }
    }

    function hideAlerts() {
        elements.errorAlert?.classList.add('hidden');
        elements.infoAlert?.classList.add('hidden');
        elements.successAlert?.classList.add('hidden');
    }

    // ==================== Form Toggle Functions ====================

    function showLoginForm() {
        hideAllForms();
        elements.loginForm?.classList.remove('hidden');
        if (elements.formTitle) elements.formTitle.textContent = 'Sign In';
        if (elements.formSubtitle) elements.formSubtitle.textContent = 'Enter your credentials to continue';
        hideAlerts();
        currentForm = 'login';
        // Reset ALTCHA widgets when switching forms
        resetAltcha(elements.loginAltcha);
        resetAltcha(elements.registerAltcha);
        // Reset login form fields
        if (elements.loginEmail) elements.loginEmail.disabled = false;
        if (elements.loginPassword) elements.loginPassword.disabled = false;
        requires2FA = false;
        // Clear URL parameters
        window.history.replaceState({}, document.title, window.location.pathname);
    }

    function showRegisterForm() {
        hideAllForms();
        elements.registerForm?.classList.remove('hidden');
        if (elements.formTitle) elements.formTitle.textContent = 'Create Account';
        if (elements.formSubtitle) elements.formSubtitle.textContent = 'Fill in your details to get started';
        hideAlerts();
        currentForm = 'register';
        // Reset ALTCHA widgets when switching forms
        resetAltcha(elements.loginAltcha);
        resetAltcha(elements.registerAltcha);
    }

    function showForgotPasswordForm() {
        hideAllForms();
        elements.forgotPasswordForm?.classList.remove('hidden');
        if (elements.formTitle) elements.formTitle.textContent = 'Reset Password';
        if (elements.formSubtitle) elements.formSubtitle.textContent = 'Enter your email to receive a reset link';
        hideAlerts();
        currentForm = 'forgot-password';
    }

    function showResetPasswordForm(token) {
        hideAllForms();
        elements.resetPasswordForm?.classList.remove('hidden');
        if (elements.formTitle) elements.formTitle.textContent = 'Set New Password';
        if (elements.formSubtitle) elements.formSubtitle.textContent = 'Choose a strong password for your account';
        hideAlerts();
        currentForm = 'reset-password';
        // Store token for form submission
        elements.resetPasswordForm.dataset.token = token;
    }

    function showVerifyEmailPage() {
        hideAllForms();
        elements.verifyEmailPage?.classList.remove('hidden');
        if (elements.formTitle) elements.formTitle.textContent = 'Email Verification';
        if (elements.formSubtitle) elements.formSubtitle.textContent = 'Please wait while we verify your email';
        hideAlerts();
        currentForm = 'verify-email';
    }

    function hideAllForms() {
        elements.loginForm?.classList.add('hidden');
        elements.registerForm?.classList.add('hidden');
        elements.forgotPasswordForm?.classList.add('hidden');
        elements.resetPasswordForm?.classList.add('hidden');
        elements.verifyEmailPage?.classList.add('hidden');
        elements.verifyCodeForm?.classList.add('hidden');
    }

    function showVerificationCodeForm(email) {
        hideAllForms();
        elements.verifyCodeForm?.classList.remove('hidden');
        if (elements.formTitle) elements.formTitle.textContent = 'Verify Your Email';
        if (elements.formSubtitle) elements.formSubtitle.textContent = 'Enter the 6-digit code sent to your email';
        if (elements.verifyCodeEmail) elements.verifyCodeEmail.textContent = email;
        hideAlerts();
        currentForm = 'verify-code';
        pendingVerificationEmail = email;
        elements.verificationCode?.focus();
    }

    // ==================== 2FA Functions ====================

    function toggle2FASection() {
        const section = elements.twoFactorSection;
        const link = elements.toggle2FALink;
        if (!section || !link) return;

        const isOpen = section.classList.contains('open');
        if (isOpen) {
            section.classList.remove('open');
            link.textContent = 'I have a 2FA code';
        } else {
            section.classList.add('open');
            link.textContent = 'Hide 2FA fields';
            elements.totpCode?.focus();
        }
    }

    function show2FASection() {
        const section = elements.twoFactorSection;
        const link = elements.toggle2FALink;
        if (section) section.classList.add('open');
        if (link) link.textContent = 'Hide 2FA fields';
    }

    // ==================== Password Strength ====================

    function checkPasswordStrength(password) {
        let score = 0;
        if (password.length >= 8) score++;
        if (password.length >= 12) score++;
        if (/[a-z]/.test(password) && /[A-Z]/.test(password)) score++;
        if (/[0-9]/.test(password)) score++;
        if (/[^a-zA-Z0-9]/.test(password)) score++;

        if (score <= 1) return { level: 'weak', text: 'Weak' };
        if (score <= 2) return { level: 'fair', text: 'Fair' };
        if (score <= 3) return { level: 'good', text: 'Good' };
        return { level: 'strong', text: 'Strong' };
    }

    function handlePasswordInput(e) {
        const password = e.target.value;
        if (password.length > 0) {
            elements.passwordStrength?.classList.remove('hidden');
            const strength = checkPasswordStrength(password);
            if (elements.strengthFill) {
                elements.strengthFill.className = 'password-strength-fill strength-' + strength.level;
            }
            if (elements.strengthLabel) {
                elements.strengthLabel.textContent = strength.text;
            }
        } else {
            elements.passwordStrength?.classList.add('hidden');
        }
    }

    function handleResetPasswordInput(e) {
        const password = e.target.value;
        if (password.length > 0) {
            elements.resetPasswordStrength?.classList.remove('hidden');
            const strength = checkPasswordStrength(password);
            if (elements.resetStrengthFill) {
                elements.resetStrengthFill.className = 'password-strength-fill strength-' + strength.level;
            }
            if (elements.resetStrengthLabel) {
                elements.resetStrengthLabel.textContent = strength.text;
            }
        } else {
            elements.resetPasswordStrength?.classList.add('hidden');
        }
    }

    // ==================== Button State ====================

    function setButtonLoading(button, loading, text) {
        if (!button) return;
        button.disabled = loading;
        if (loading) {
            button.innerHTML = '<span class="spinner"></span>' + text;
        } else {
            button.textContent = text;
        }
    }

    // ==================== Login Handler ====================

    async function handleLogin(e) {
        e.preventDefault();
        hideAlerts();

        const email = elements.loginEmail?.value;
        const password = elements.loginPassword?.value;
        const totpCode = elements.totpCode?.value;
        const recoveryCode = elements.recoveryCode?.value;

        // Check ALTCHA verification (not required for 2FA retry)
        const is2FARetry = totpCode || (recoveryCode && recoveryCode.trim());
        if (!is2FARetry && !loginAltchaValue) {
            showError('Please complete the security verification');
            return;
        }

        // Build request body
        const body = { email, password };
        if (totpCode) body.totpCode = parseInt(totpCode);
        if (recoveryCode) body.recoveryCode = recoveryCode;
        if (!is2FARetry && loginAltchaValue) body.altcha = loginAltchaValue;

        setButtonLoading(elements.loginBtn, true, 'Signing in...');

        try {
            const url = `${API_URL}/auth/login${returnTo ? `?returnTo=${encodeURIComponent(returnTo)}` : ''}`;
            const response = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify(body),
                redirect: 'manual'
            });

            // Check for redirect
            if (response.type === 'opaqueredirect' || response.status === 302 || response.status === 303) {
                const location = response.headers.get('Location');
                window.location.href = location || window.location.href;
                return;
            }

            const result = await response.json();

            if (!response.ok) {
                throw new Error(result.error || 'Login failed');
            }

            // Check if 2FA is required
            if (result.twoFactorEnabled && !result.authenticated) {
                requires2FA = true;
                show2FASection();
                showInfo('Two-factor authentication required. Please enter your code.');
                setButtonLoading(elements.loginBtn, false, 'Sign In');
                if (elements.loginEmail) elements.loginEmail.disabled = true;
                if (elements.loginPassword) elements.loginPassword.disabled = true;
                elements.totpCode?.focus();
                return;
            }

            // Login successful
            showSuccess('Login successful! Redirecting...');
            setTimeout(async () => {
                // If returnTo points to OAuth authorize endpoint, use it (coming from OAuth flow)
                if (returnTo && returnTo.includes('/auth/authorize')) {
                    window.location.href = returnTo;
                } else {
                    // No returnTo or not an OAuth flow - redirect to app with auto_login flag
                    window.location.href = APP_ROOT_URL + '?auto_login=true';
                }
            }, 500);

        } catch (err) {
            showError(err.message || 'Login failed');
            setButtonLoading(elements.loginBtn, false, 'Sign In');
            // Reset ALTCHA widget on failure
            resetAltcha(elements.loginAltcha);
        }
    }

    // ==================== Register Handler ====================

    async function handleRegister(e) {
        e.preventDefault();
        hideAlerts();

        const email = elements.registerEmail?.value;
        const username = elements.registerUsername?.value;
        const password = elements.registerPassword?.value;
        const confirmPassword = elements.confirmPassword?.value;

        // Client-side validation
        if (password !== confirmPassword) {
            showError('Passwords do not match');
            return;
        }

        if (password.length < 8) {
            showError('Password must be at least 8 characters');
            return;
        }

        // Check ALTCHA verification
        if (!registerAltchaValue) {
            showError('Please complete the security verification');
            return;
        }

        setButtonLoading(elements.registerBtn, true, 'Sending verification code...');

        try {
            const response = await fetch(`${API_URL}/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ email, username, password, altcha: registerAltchaValue })
            });

            const result = await response.json();

            if (!response.ok) {
                throw new Error(result.error || 'Registration failed');
            }

            // Check if verification is required (new flow)
            if (result.verificationRequired) {
                setButtonLoading(elements.registerBtn, false, 'Create Account');
                showVerificationCodeForm(result.email || email);
                showSuccess(result.message || 'Verification code sent! Check your email.');
                return;
            }

            // Legacy flow (shouldn't happen with new backend)
            showSuccess('Account created! Please check your email for a verification link.');
            setButtonLoading(elements.registerBtn, false, 'Account Created');
            setTimeout(() => {
                showLoginForm();
                showInfo('Please verify your email before signing in.');
            }, 3000);

        } catch (err) {
            showError(err.message || 'Registration failed');
            setButtonLoading(elements.registerBtn, false, 'Create Account');
            // Reset ALTCHA widget on failure
            resetAltcha(elements.registerAltcha);
        }
    }

    // ==================== Verify Code Handler ====================

    async function handleVerifyCode(e) {
        e.preventDefault();
        hideAlerts();

        const code = elements.verificationCode?.value?.trim();

        if (!code || code.length !== 6) {
            showError('Please enter the 6-digit verification code');
            return;
        }

        if (!pendingVerificationEmail) {
            showError('No pending registration. Please start over.');
            showRegisterForm();
            return;
        }

        setButtonLoading(elements.verifyCodeBtn, true, 'Verifying...');

        try {
            const response = await fetch(`${API_URL}/auth/verify-code`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ 
                    email: pendingVerificationEmail, 
                    code: code 
                })
            });

            const result = await response.json();

            if (!response.ok) {
                throw new Error(result.error || 'Verification failed');
            }

            // Success - account created
            showSuccess(result.message || 'Account created successfully! You can now sign in.');
            pendingVerificationEmail = null;
            
            // Redirect to login after a short delay
            setTimeout(() => {
                showLoginForm();
                showSuccess('Your account is ready. Please sign in.');
            }, 2000);

        } catch (err) {
            showError(err.message || 'Verification failed');
            setButtonLoading(elements.verifyCodeBtn, false, 'Verify Code');
        }
    }

    // ==================== Resend Code Handler ====================

    async function handleResendCode(e) {
        e.preventDefault();
        hideAlerts();

        if (!pendingVerificationEmail) {
            showError('No pending registration. Please start over.');
            showRegisterForm();
            return;
        }

        setButtonLoading(elements.resendCodeBtn, true, 'Sending...');

        try {
            const response = await fetch(`${API_URL}/auth/resend-code`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ email: pendingVerificationEmail })
            });

            const result = await response.json();

            if (!response.ok) {
                throw new Error(result.error || 'Failed to resend code');
            }

            showSuccess(result.message || 'New verification code sent!');
            if (elements.verificationCode) elements.verificationCode.value = '';
            elements.verificationCode?.focus();

        } catch (err) {
            showError(err.message || 'Failed to resend code');
        } finally {
            setButtonLoading(elements.resendCodeBtn, false, 'Resend Code');
        }
    }

    // ==================== Forgot Password Handler ====================

    async function handleForgotPassword(e) {
        e.preventDefault();
        hideAlerts();

        const email = elements.forgotEmail?.value;

        if (!email) {
            showError('Please enter your email address');
            return;
        }

        setButtonLoading(elements.forgotPasswordBtn, true, 'Sending...');

        try {
            const response = await fetch(`${API_URL}/auth/forgot-password`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ email })
            });

            const result = await response.json();

            if (!response.ok) {
                throw new Error(result.error || 'Failed to send reset email');
            }

            // Show success message
            showSuccess(result.message || 'If an account exists with that email, a reset link has been sent.');

            // Clear form
            if (elements.forgotEmail) elements.forgotEmail.value = '';

            // Show back to login option after a delay
            setTimeout(() => {
                showInfo('Check your email for the reset link. You can close this page.');
            }, 2000);

        } catch (err) {
            showError(err.message || 'Failed to send reset email');
        } finally {
            setButtonLoading(elements.forgotPasswordBtn, false, 'Send Reset Link');
        }
    }

    // ==================== Email Verification Handler ====================

    async function handleEmailVerification(token) {
        if (!token) {
            showError('Verification token is missing');
            if (elements.verifyEmailMessage) {
                elements.verifyEmailMessage.textContent = '❌ Verification failed - invalid token';
            }
            return;
        }

        try {
            const response = await fetch(`${API_URL}/auth/verify-email?token=${encodeURIComponent(token)}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include'
            });

            const result = await response.json();

            if (!response.ok) {
                throw new Error(result.error || 'Email verification failed');
            }

            // Success
            if (elements.verifyEmailMessage) {
                elements.verifyEmailMessage.textContent = '✅ Email verified successfully!';
            }
            showSuccess(result.message || 'Email verified successfully! You can now sign in.');

            // Redirect to login after a delay
            setTimeout(() => {
                showLoginForm();
            }, 3000);

        } catch (err) {
            if (elements.verifyEmailMessage) {
                elements.verifyEmailMessage.textContent = '❌ Verification failed';
            }
            showError(err.message || 'Email verification failed');
        }
    }

    // ==================== Reset Password Handler ====================

    async function handleResetPassword(e) {
        e.preventDefault();
        hideAlerts();

        const token = elements.resetPasswordForm?.dataset.token;
        const newPassword = elements.newPassword?.value;
        const confirmNewPassword = elements.confirmNewPassword?.value;

        // Client-side validation
        if (!token) {
            showError('Reset token is missing');
            return;
        }

        if (newPassword !== confirmNewPassword) {
            showError('Passwords do not match');
            return;
        }

        if (newPassword.length < 8) {
            showError('Password must be at least 8 characters');
            return;
        }

        setButtonLoading(elements.resetPasswordBtn, true, 'Resetting password...');

        try {
            const response = await fetch(`${API_URL}/auth/reset-password?token=${encodeURIComponent(token)}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ newPassword })
            });

            const result = await response.json();

            if (!response.ok) {
                throw new Error(result.error || 'Password reset failed');
            }

            // Success
            showSuccess(result.message || 'Password reset successfully! Please sign in with your new password.');

            // Clear form
            if (elements.newPassword) elements.newPassword.value = '';
            if (elements.confirmNewPassword) elements.confirmNewPassword.value = '';

            // Show login form
            setTimeout(() => {
                showLoginForm();
                showInfo('Your password has been reset. Sign in below, or go to the app to sign in with OAuth.');
            }, 2000);

        } catch (err) {
            showError(err.message || 'Password reset failed');
            setButtonLoading(elements.resetPasswordBtn, false, 'Reset Password');
        }
    }

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    // Expose functions for inline handlers if needed
    window.MachinaEarLogin = {
        showLoginForm,
        showRegisterForm,
        toggle2FASection,
        showForgotPasswordForm
    };

})();