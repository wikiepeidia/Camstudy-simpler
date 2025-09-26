// PhotoMagic App JavaScript
let currentPage = 'main';
let upgradeInProgress = false;

// Page Navigation
function showPage(pageId) {
    // Hide all pages
    document.querySelectorAll('.page').forEach(page => {
        page.classList.remove('active');
    });
    
    // Show selected page
    document.getElementById(pageId + '-page').classList.add('active');
    
    // Update navigation
    document.querySelectorAll('.nav-item').forEach(nav => {
        nav.classList.remove('active');
    });
    
    // Find and activate current nav item
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(nav => {
        const span = nav.querySelector('span');
        if (span && span.textContent.toLowerCase() === pageId.toLowerCase()) {
            nav.classList.add('active');
        }
        // Special case for main page (Home)
        if (pageId === 'main' && span && span.textContent === 'Home') {
            nav.classList.add('active');
        }
    });
    
    currentPage = pageId;
}

// Camera Settings Toggle
function toggleCameraSettings() {
    const settings = document.getElementById('camera-settings');
    settings.classList.toggle('active');
}

// Dark Mode Toggle
function toggleDarkMode() {
    document.body.classList.toggle('dark-mode');
    localStorage.setItem('darkMode', document.body.classList.contains('dark-mode'));
}

// Logout Confirmation
function showLogoutConfirm() {
    if (confirm('Are you sure you want to logout? You will be signed out of your PhotoMagic account.')) {
        // Show logout animation
        showNotification('Logging out...', 'info');
        
        // Simulate logout process
        setTimeout(() => {
            showNotification('Successfully logged out!', 'success');
            // Here you would typically redirect to login page
            // For now, we'll just go to main page
            showPage('main');
        }, 1500);
    }
}

// Notification System
function showNotification(message, type = 'info') {
    // Remove existing notification
    const existingNotification = document.querySelector('.notification');
    if (existingNotification) {
        existingNotification.remove();
    }

    // Create notification
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.innerHTML = `
        <div class="notification-content">
            <i class="fas ${getNotificationIcon(type)}"></i>
            <span>${message}</span>
        </div>
    `;

    // Add to page
    document.body.appendChild(notification);

    // Show animation
    setTimeout(() => {
        notification.classList.add('show');
    }, 100);

    // Auto remove
    setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => {
            if (notification.parentNode) {
                notification.remove();
            }
        }, 300);
    }, 3000);
}

function getNotificationIcon(type) {
    switch(type) {
        case 'success': return 'fa-check-circle';
        case 'error': return 'fa-exclamation-circle';
        case 'warning': return 'fa-exclamation-triangle';
        default: return 'fa-info-circle';
    }
}

// Additional Camera Functions (from original app)

// Select Java Version
function selectJavaVersion(version) {
    // Remove previous selection
    document.querySelectorAll('.version-card').forEach(card => {
        card.classList.remove('selected');
    });
    
    // Select current card
    const selectedCard = document.querySelector(`[data-version="${version}"]`);
    selectedCard.classList.add('selected');
    
    // Update selected version
    selectedJavaVersion = version;
    
    // Show upgrade actions
    const upgradeActions = document.getElementById('upgrade-actions');
    const targetVersionSpan = document.getElementById('target-version');
    
    upgradeActions.style.display = 'block';
    targetVersionSpan.textContent = version;
    
    // Scroll to upgrade actions
    upgradeActions.scrollIntoView({ behavior: 'smooth', block: 'start' });
    
    showNotification(`Java ${version} selected for upgrade`, 'info');
}

// Start Upgrade Process
function startUpgrade() {
    if (upgradeInProgress) return;
    
    upgradeInProgress = true;
    const startButton = document.getElementById('start-upgrade-btn');
    startButton.textContent = 'Upgrading...';
    startButton.disabled = true;
    
    // Reset all steps
    document.querySelectorAll('.step-item').forEach((step, index) => {
        step.classList.remove('active', 'completed');
        const status = step.querySelector('.step-status');
        status.innerHTML = '<i class="fas fa-clock"></i>';
    });
    
    // Start step sequence
    executeUpgradeStep(1);
}

// Execute Upgrade Steps
function executeUpgradeStep(stepNumber) {
    const step = document.getElementById(`step-${stepNumber}`);
    const status = step.querySelector('.step-status');
    
    // Activate current step
    step.classList.add('active');
    status.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
    
    // Simulate step duration
    const durations = [2000, 5000, 3000, 2000]; // Different durations for each step
    
    setTimeout(() => {
        // Complete current step
        step.classList.remove('active');
        step.classList.add('completed');
        status.innerHTML = '<i class="fas fa-check"></i>';
        
        // Special handling for download step
        if (stepNumber === 2) {
            simulateDownloadProgress();
        }
        
        // Move to next step or complete
        if (stepNumber < 4) {
            executeUpgradeStep(stepNumber + 1);
        } else {
            completeUpgrade();
        }
    }, durations[stepNumber - 1]);
}

// Simulate Download Progress
function simulateDownloadProgress() {
    const progressBar = document.getElementById('download-progress');
    let progress = 0;
    
    const interval = setInterval(() => {
        progress += Math.random() * 15;
        if (progress >= 100) {
            progress = 100;
            clearInterval(interval);
        }
        progressBar.style.width = progress + '%';
    }, 200);
}

// Complete Upgrade
function completeUpgrade() {
    upgradeInProgress = false;
    const startButton = document.getElementById('start-upgrade-btn');
    startButton.textContent = 'Upgrade Complete!';
    startButton.style.background = 'var(--success-color)';
    
    // Update current Java version
    const currentVersionElement = document.getElementById('current-version');
    const statusElement = document.getElementById('version-status');
    
    currentVersionElement.textContent = `Java ${selectedJavaVersion}.0.1`;
    statusElement.textContent = 'Up to Date';
    statusElement.className = 'version-status current';
    
    // Show success notification
    showNotification(`Successfully upgraded to Java ${selectedJavaVersion}!`, 'success');
    
    // Add new JDK to installed list (simulate)
    setTimeout(() => {
        addInstalledJDK(selectedJavaVersion);
        hideUpgradeActions();
    }, 2000);
}

// Cancel Upgrade
function cancelUpgrade() {
    if (upgradeInProgress) {
        upgradeInProgress = false;
        showNotification('Upgrade cancelled', 'warning');
    }
    hideUpgradeActions();
}

// Hide Upgrade Actions
function hideUpgradeActions() {
    document.getElementById('upgrade-actions').style.display = 'none';
    selectedJavaVersion = null;
    
    // Reset selection
    document.querySelectorAll('.version-card').forEach(card => {
        card.classList.remove('selected');
    });
    
    // Reset button
    const startButton = document.getElementById('start-upgrade-btn');
    startButton.textContent = 'Start Upgrade';
    startButton.disabled = false;
    startButton.style.background = '';
}

// Add Installed JDK (simulation)
function addInstalledJDK(version) {
    const jdkList = document.querySelector('.jdk-list');
    
    // Remove active class from current JDK
    document.querySelectorAll('.jdk-item').forEach(item => {
        item.classList.remove('active');
        const badge = item.querySelector('.active-badge');
        if (badge) {
            badge.remove();
        }
        const actions = item.querySelector('.jdk-actions');
        actions.innerHTML = `
            <button class="btn-small" onclick="setDefault(${version})">Set Default</button>
            <button class="btn-small danger" onclick="removeJDK(${version})">Remove</button>
        `;
    });
    
    // Create new JDK item
    const newJDK = document.createElement('div');
    newJDK.className = 'jdk-item active';
    newJDK.innerHTML = `
        <div class="jdk-info">
            <div class="jdk-icon">
                <i class="fab fa-java"></i>
            </div>
            <div class="jdk-details">
                <h4>Java ${version} (${version}.0.1)</h4>
                <p>/usr/lib/jvm/java-${version}-openjdk</p>
                <small>Eclipse Adoptium</small>
            </div>
        </div>
        <div class="jdk-actions">
            <span class="active-badge">Active</span>
            <button class="btn-small danger" onclick="removeJDK(${version})">Remove</button>
        </div>
    `;
    
    // Add to list
    jdkList.appendChild(newJDK);
    
    // Update environment variables
    updateEnvironmentVariables(version);
}

// Set Default JDK
function setDefault(version) {
    // Remove active class from all items
    document.querySelectorAll('.jdk-item').forEach(item => {
        item.classList.remove('active');
        const badge = item.querySelector('.active-badge');
        if (badge) {
            badge.remove();
        }
        // Update actions
        const actions = item.querySelector('.jdk-actions');
        const details = item.querySelector('.jdk-details h4');
        const currentVersion = details.textContent.match(/Java (\d+)/)[1];
        actions.innerHTML = `
            <button class="btn-small" onclick="setDefault(${currentVersion})">Set Default</button>
            <button class="btn-small danger" onclick="removeJDK(${currentVersion})">Remove</button>
        `;
    });
    
    // Find and activate selected version
    const jdkItems = document.querySelectorAll('.jdk-item');
    jdkItems.forEach(item => {
        const versionText = item.querySelector('.jdk-details h4').textContent;
        if (versionText.includes(`Java ${version}`)) {
            item.classList.add('active');
            const actions = item.querySelector('.jdk-actions');
            actions.innerHTML = `
                <span class="active-badge">Active</span>
                <button class="btn-small danger" onclick="removeJDK(${version})">Remove</button>
            `;
        }
    });
    
    // Update current version display
    document.getElementById('current-version').textContent = `Java ${version}.0.1`;
    document.getElementById('version-status').textContent = 'Up to Date';
    document.getElementById('version-status').className = 'version-status current';
    
    // Update environment variables
    updateEnvironmentVariables(version);
    
    showNotification(`Java ${version} set as default`, 'success');
}

// Remove JDK
function removeJDK(version) {
    if (confirm(`Are you sure you want to remove Java ${version}?`)) {
        const jdkItems = document.querySelectorAll('.jdk-item');
        jdkItems.forEach(item => {
            const versionText = item.querySelector('.jdk-details h4').textContent;
            if (versionText.includes(`Java ${version}`)) {
                item.remove();
            }
        });
        
        showNotification(`Java ${version} removed successfully`, 'success');
    }
}

// Update Environment Variables
function updateEnvironmentVariables(version) {
    const javaHomeInput = document.querySelector('.env-item input[value*="java-"]');
    const pathInput = document.querySelector('.env-item input[value*="/bin"]');
    
    if (javaHomeInput) {
        javaHomeInput.value = `/usr/lib/jvm/java-${version}-openjdk`;
    }
    
    if (pathInput) {
        pathInput.value = `/usr/lib/jvm/java-${version}-openjdk/bin:...`;
    }
}

// Copy to Clipboard
function copyToClipboard(type) {
    const input = event.target.closest('.env-value').querySelector('input');
    const text = input.value;
    
    // Modern clipboard API
    if (navigator.clipboard) {
        navigator.clipboard.writeText(text).then(() => {
            showNotification('Copied to clipboard!', 'success');
        });
    } else {
        // Fallback for older browsers
        input.select();
        document.execCommand('copy');
        showNotification('Copied to clipboard!', 'success');
    }
    
    // Visual feedback
    const button = event.target.closest('button');
    const icon = button.querySelector('i');
    icon.classList.remove('fa-copy');
    icon.classList.add('fa-check');
    
    setTimeout(() => {
        icon.classList.remove('fa-check');
        icon.classList.add('fa-copy');
    }, 1000);
}

// Notification System
function showNotification(message, type = 'info') {
    // Remove existing notifications
    const existing = document.querySelector('.notification');
    if (existing) {
        existing.remove();
    }
    
    // Create notification
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.innerHTML = `
        <div class="notification-content">
            <i class="fas fa-${getNotificationIcon(type)}"></i>
            <span>${message}</span>
        </div>
    `;
    
    // Style notification
    Object.assign(notification.style, {
        position: 'fixed',
        top: '20px',
        left: '50%',
        transform: 'translateX(-50%)',
        zIndex: '10000',
        background: getNotificationColor(type),
        color: 'white',
        padding: '12px 20px',
        borderRadius: '8px',
        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
        fontSize: '14px',
        fontWeight: '500',
        maxWidth: '300px',
        textAlign: 'center'
    });
    
    // Add to DOM
    document.body.appendChild(notification);
    
    // Animate in
    notification.style.opacity = '0';
    notification.style.transform = 'translateX(-50%) translateY(-20px)';
    
    requestAnimationFrame(() => {
        notification.style.transition = 'all 0.3s ease';
        notification.style.opacity = '1';
        notification.style.transform = 'translateX(-50%) translateY(0)';
    });
    
    // Auto remove
    setTimeout(() => {
        notification.style.opacity = '0';
        notification.style.transform = 'translateX(-50%) translateY(-20px)';
        setTimeout(() => {
            if (notification.parentNode) {
                notification.remove();
            }
        }, 300);
    }, 3000);
}

function getNotificationIcon(type) {
    switch (type) {
        case 'success': return 'check-circle';
        case 'error': return 'exclamation-circle';
        case 'warning': return 'exclamation-triangle';
        default: return 'info-circle';
    }
}

function getNotificationColor(type) {
    switch (type) {
        case 'success': return '#10b981';
        case 'error': return '#ef4444';
        case 'warning': return '#f59e0b';
        default: return '#6366f1';
    }
}

// Version Card Selection Styles
function addVersionCardStyles() {
    const style = document.createElement('style');
    style.textContent = `
        .notification-content {
            display: flex;
            align-items: center;
            gap: 8px;
        }
        
        .notification-content i {
            font-size: 16px;
        }
    `;
    document.head.appendChild(style);
}

// Initialize App
document.addEventListener('DOMContentLoaded', function() {
    // Add custom styles
    addVersionCardStyles();
    
    // Load saved dark mode preference
    if (localStorage.getItem('darkMode') === 'true') {
        document.body.classList.add('dark-mode');
        const darkModeToggle = document.querySelector('input[onchange="toggleDarkMode()"]');
        if (darkModeToggle) {
            darkModeToggle.checked = true;
        }
    }
    
    // Add click handlers for tabs
    document.querySelectorAll('.tab-btn').forEach(tab => {
        tab.addEventListener('click', function() {
            document.querySelectorAll('.tab-btn').forEach(t => t.classList.remove('active'));
            this.classList.add('active');
        });
    });
    
    // Add click handlers for filter items
    document.querySelectorAll('.filter-item').forEach(filter => {
        filter.addEventListener('click', function() {
            document.querySelectorAll('.filter-item').forEach(f => f.classList.remove('active'));
            this.classList.add('active');
        });
    });
    
    console.log('PhotoMagic App loaded successfully!');
});

// Additional Camera Functions (from original app)
function capturePhoto() {
    showNotification('Photo captured!', 'success');
}

function switchCamera() {
    showNotification('Camera switched', 'info');
}

function openGallery() {
    showPage('history');
}

// Keyboard shortcuts
document.addEventListener('keydown', function(e) {
    // ESC to go back
    if (e.key === 'Escape' && currentPage !== 'main') {
        showPage('main');
    }
    
    // Number keys for quick navigation
    const pageMap = {
        '1': 'main',
        '2': 'camera', 
        '3': 'history',
        '4': 'settings'
    };
    
    if (pageMap[e.key]) {
        showPage(pageMap[e.key]);
    }
});

// Touch gestures for mobile
let touchStartX = 0;
let touchStartY = 0;

document.addEventListener('touchstart', function(e) {
    touchStartX = e.changedTouches[0].screenX;
    touchStartY = e.changedTouches[0].screenY;
});

document.addEventListener('touchend', function(e) {
    const touchEndX = e.changedTouches[0].screenX;
    const touchEndY = e.changedTouches[0].screenY;
    const deltaX = touchEndX - touchStartX;
    const deltaY = touchEndY - touchStartY;
    
    // Swipe right to go back (only if not on main page)
    if (deltaX > 50 && Math.abs(deltaY) < 100 && currentPage !== 'main') {
        showPage('main');
    }
});