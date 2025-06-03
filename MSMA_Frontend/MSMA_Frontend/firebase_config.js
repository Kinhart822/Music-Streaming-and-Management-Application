// Import the functions you need from the SDKs you need
import {initializeApp} from "https://www.gstatic.com/firebasejs/11.8.1/firebase-app.js";
import { getMessaging, getToken , onMessage} from "https://www.gstatic.com/firebasejs/11.8.1/firebase-messaging.js";

const firebaseConfig = {
    apiKey: "AIzaSyBLOaSSpliRqKMABSqU3ZaFy7DZxe0v8dc",
    authDomain: "musicnotifications-9b666.firebaseapp.com",
    projectId: "musicnotifications-9b666",
    storageBucket: "musicnotifications-9b666.firebasestorage.app",
    messagingSenderId: "616877216067",
    appId: "1:616877216067:web:0d49d62877f5aba2679ff6",
    measurementId: "G-BZV1PSDC2L"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Initialize Firebase Cloud Messaging
const messaging = getMessaging(app);

export {messaging, getToken, onMessage};