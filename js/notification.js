const createNotificationElement = () => {
    const notification = document.createElement('div');
    notification.id = 'notification';
    notification.className = 'notification';
    notification.style.display = 'none';
    notification.innerHTML = `
        <span id="notification-message"></span>
        <span class="close-notification">×</span>
    `;
    document.body.appendChild(notification);
    notification.querySelector('.close-notification').addEventListener('click', () => {
        notification.style.display = 'none';
    });
    return notification;
};

// Show notification
const showNotification = (message, isError = false) => {
    const notification = document.getElementById('notification') || createNotificationElement();
    const messageSpan = document.getElementById('notification-message');
    messageSpan.textContent = message;
    notification.style.background = isError ? 'var(--error-color)' : 'var(--success-color)';
    notification.style.display = 'flex';
    setTimeout(() => {
        notification.style.display = 'none';
    }, 3000);
};

export { showNotification }
