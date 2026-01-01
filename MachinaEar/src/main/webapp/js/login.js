/**
 * MachinaEar Login Module
 * Handles login, registration, and 2FA flows
 */

(function() {
    'use strict';

    // Configuration
    const contextPath = window.location.pathname.replace(/\/[^/]*$/, '');
    const API_URL = window.location.origin + contextPath + '/iam';
    const params = new URLSearchParams(window.location.search);
    const returnTo = params.get('returnTo');

    // State
    let requires2FA = false;
    let currentForm = 'login';

    // DOM Elements Cache
    const elements = {
        // Forms
        loginForm: null,
        registerForm: null,
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
        // Register form elements
        registerEmail: null,
        registerUsername: null,
        registerPassword: null,
        confirmPassword: null,
        registerBtn: null,
        passwordStrength: null,
        strengthFill: null,
        strengthLabel: null,
        // Titles
        formTitle: null,
        formSubtitle: null
    };

    /**
     * Initialize the module
     */
    function init() {
        cacheElements();
        bindEvents();
        checkInitialMode();
    }

    /**
     * Check if we should show register form based on URL parameter
     */
    function checkInitialMode() {
        const mode = params.get('mode');
        if (mode === 'register') {
            showRegisterForm();
        }
    }

    /**
     * Cache DOM elements
     */
    function cacheElements() {
        elements.loginForm = document.getElementById('loginForm');
        elements.registerForm = document.getElementById('registerForm');
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
        elements.registerEmail = document.getElementById('registerEmail');
        elements.registerUsername = document.getElementById('registerUsername');
        elements.registerPassword = document.getElementById('registerPassword');
        elements.confirmPassword = document.getElementById('confirmPassword');
        elements.registerBtn = document.getElementById('registerBtn');
        elements.passwordStrength = document.getElementById('passwordStrength');
        elements.strengthFill = document.getElementById('strengthFill');
        elements.strengthLabel = document.getElementById('strengthLabel');
        elements.formTitle = document.getElementById('formTitle');
        elements.formSubtitle = document.getElementById('formSubtitle');
    }

    /**
     * Bind event listeners
     */
    function bindEvents() {
        // Form submissions
        elements.loginForm?.addEventListener('submit', handleLogin);
        elements.registerForm?.addEventListener('submit', handleRegister);
        
        // Password strength
        elements.registerPassword?.addEventListener('input', handlePasswordInput);
        
        // 2FA toggle
        elements.toggle2FALink?.addEventListener('click', toggle2FASection);

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
        elements.loginForm?.classList.remove('hidden');
        elements.registerForm?.classList.add('hidden');
        if (elements.formTitle) elements.formTitle.textContent = 'Sign In';
        if (elements.formSubtitle) elements.formSubtitle.textContent = 'Enter your credentials to continue';
        hideAlerts();
        currentForm = 'login';
    }

    function showRegisterForm() {
        elements.loginForm?.classList.add('hidden');
        elements.registerForm?.classList.remove('hidden');
        if (elements.formTitle) elements.formTitle.textContent = 'Create Account';
        if (elements.formSubtitle) elements.formSubtitle.textContent = 'Fill in your details to get started';
        hideAlerts();
        currentForm = 'register';
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

        // Build request body
        const body = { email, password };
        if (totpCode) body.totpCode = parseInt(totpCode);
        if (recoveryCode) body.recoveryCode = recoveryCode;

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
            setTimeout(() => {
                window.location.href = returnTo || '/home';
            }, 500);

        } catch (err) {
            showError(err.message || 'Login failed');
            setButtonLoading(elements.loginBtn, false, 'Sign In');
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

        setButtonLoading(elements.registerBtn, true, 'Creating account...');

        try {
            const response = await fetch(`${API_URL}/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ email, username, password })
            });

            const result = await response.json();

            if (!response.ok) {
                throw new Error(result.error || 'Registration failed');
            }

            // Registration successful - auto login
            showSuccess('Account created! Signing you in...');

            const loginUrl = `${API_URL}/auth/login${returnTo ? `?returnTo=${encodeURIComponent(returnTo)}` : ''}`;
            const loginResponse = await fetch(loginUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ email, password }),
                redirect: 'manual'
            });

            if (loginResponse.type === 'opaqueredirect' || loginResponse.status === 302 || loginResponse.status === 303) {
                const location = loginResponse.headers.get('Location');
                window.location.href = location || window.location.href;
                return;
            }

            const loginResult = await loginResponse.json();

            if (!loginResponse.ok) {
                showSuccess('Account created! Please sign in.');
                setTimeout(showLoginForm, 1500);
                return;
            }

            setTimeout(() => {
                window.location.href = returnTo || '/home';
            }, 500);

        } catch (err) {
            showError(err.message || 'Registration failed');
            setButtonLoading(elements.registerBtn, false, 'Create Account');
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
        toggle2FASection
    };

})();
