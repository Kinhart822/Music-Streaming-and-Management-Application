importScripts('https://www.gstatic.com/firebasejs/11.8.1/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/11.8.1/firebase-messaging-compat.js');

const firebaseConfig = {
    apiKey: "AIzaSyBLOaSSpliRqKMABSqU3ZaFy7DZxe0v8dc",
    authDomain: "musicnotifications-9b666.firebaseapp.com",
    projectId: "musicnotifications-9b666",
    storageBucket: "musicnotifications-9b666.firebasestorage.app",
    messagingSenderId: "616877216067",
    appId: "1:616877216067:web:0d49d62877f5aba2679ff6",
    measurementId: "G-BZV1PSDC2L"
};

firebase.initializeApp(firebaseConfig);
const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
    console.log('Received background message:', payload);
    const notificationTitle = payload.notification.title;
    const notificationOptions = {
        body: payload.notification.body,
        icon: '../imgs/logo.png'
    };
    self.registration.showNotification(notificationTitle, notificationOptions);
});