# **Music Streaming and Management Application**

### **Overview**

The Music Streaming and Management Application is a full-featured project that serves as a music streaming and management platform. It is designed to connect artists with listeners while giving administrators control over content quality and user management. The system delivers an engaging Android app for users to explore and enjoy music and functional web interfaces for artists to manage their work and for admins to moderate and maintain the platform effectively.

---

### **Key Features**

#### **🎵 For Artists (Web Portal)**

- **Content Publishing**: Upload songs, create and manage playlists and albums with ease.
- **Statistics Dashboard**: View real-time data on track plays, likes, and follower trends.
- **Duplicate Detection**: Integrated system detects duplicates that automatically checks uploaded audio against the platform’s database to prevent duplicates.
- **Genre Prediction**: AI-powered service that automatically suggests genre tags for newly uploaded songs based on lyrics and audio analysis.

#### **🛠️ For Admins (Web Portal)**

- **User and Artist Management**: Full oversight of all accounts on the platform.
- **Content Moderation**: Review queue for accepting or declining uploaded songs, playlists, and albums to ensure quality.

#### **🎧 For Users (Android App)**

- **Music Experience**: Native Android experience built with Jetpack Compose for smooth performance.
- **Engagement**: Like songs, follow artists, create playlists, and view lyrics.
- **Background Playback**: Continues playing music even when the app is in the background or screen is off.

---

### **System Architecture**

The system follows a microservices-inspired architecture, separating the core backend, prediction services, and client applications.

<img width="2038" height="1917" alt="System Architecture Diagram" src="https://github.com/user-attachments/assets/a61e361e-926d-4dcf-9472-1b24929c6881" />

---

### **Technology Stack**

#### **📱 Android App (User Client)**

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM (Model-View-ViewModel) with Clean Architecture principles
- **Dependency Injection**: Hilt
- **Networking**: Retrofit + OkHttp
- **Media Playback**: Media3 (ExoPlayer)
- **Image Loading**: Coil
- **Animations**: Lottie
- **asynchronous Programming**: Coroutines & Flow
- **Other**: Firebase (Messaging, Analytics), WorkManager.

#### **☕ Backend Service (Core API)**

- **Language**: Java 17
- **Framework**: Spring Boot 3.4.4
- **Security**: Spring Security 6, JWT (JSON Web Tokens) for authentication.
- **Data Access**: Spring Data JPA (Hibernate)
- **Database**: PostgreSQL
- **Cloud Storage**: Cloudinary (for image/song cover storage)
- **Notifications**: Firebase Admin SDK
- **Build Tool**: Maven

#### **🔮 Prediction Service (AI/ML)**

- **Language**: Python 3.9+
- **Framework**: FastAPI (Async Web Framework)
- **Machine Learning**: scikit-learn (RandomForest for genre classification), joblib
- **NLP**: SpaCy (English model), Lingua (Language detection)
- **Audio Processing**:
  - **Whisper (OpenAI)**: For accurate lyrics transcription.
  - **Demucs**: For separating vocals from audio tracks.
  - **FFmpeg**: For audio format conversion, slowdown effects, and silence removal.
- **Data Handling**: Pandas, SQLAlchemy
- **Task Queue**: Python `multiprocessing` & `asyncio` for handling heavy compute tasks.

#### **💻 Frontend (Artist & Admin Web Portals)**

- **Core**: HTML5, CSS3, JavaScript (ES6+)
- **Styling**: Custom CSS & Bootstrap
- **Communication**: Fetch API interacting with Spring Boot Backend

---

### **Project Structure**

```
Music-Streaming-and-Management-Application/
├── MSMA_App/               # Android Application Source Code
│   ├── app/                # Main app module
│   └── build.gradle.kts    # Build configuration
├── MSMA_Backend/           # Spring Boot Backend Source Code
│   ├── src/                # Java source files and resources
│   └── pom.xml             # Maven dependencies
├── MSMA_Frontend/          # Web Frontend Source Code
│   └── MSMA_Frontend/      # Source files (html, css, js)
├── MSMA_Prediction/        # Python AI/ML Service
│   ├── GenrePrediction.py  # Main FastAPI application
│   └── pkl/                # Pre-trained models (TF-IDF, RFC)
└── README.md               # Project Documentation
```

---

### **Setup & Installation**

#### **1. Prerequisites**

- **Java**: JDK 17 or higher
- **Database**: PostgreSQL installed and running
- **Python**: Version 3.9 or higher
- **Node.js** (Optional, for frontend tooling)
- **Android Studio**: Latest version (Koala Feature Drop or later recommended)
- **FFmpeg**: Must be installed and added to system PATH (required for Prediction Service).

#### **2. Backend Setup (`MSMA_Backend`)**

1.  Navigate to `MSMA_Backend`.
2.  Configure database and cloud credentials in `application.properties` or environment variables (`.env` file if supported).
3.  Run the application:
    ```bash
    ./mvnw spring-boot:run
    ```
    The server typically starts on port `8080`.

#### **3. Prediction Service Setup (`MSMA_Prediction`)**

1.  Navigate to `MSMA_Prediction`.
2.  Install Python dependencies (create a virtual environment recommended):
    ```bash
    pip install fastapi uvicorn scikit-learn spacy pandas sqlalchemy psycopg2-binary joblib pydub
    python -m spacy download en_core_web_sm
    ```
    _(Note: Ensure all dependencies from imports in `GenrePrediction.py` are installed)._
3.  Run the FastAPI server:
    ```bash
    uvicorn GenrePrediction:app --reload --port 8000
    ```

#### **4. Frontend Setup (`MSMA_Frontend`)**

1.  Navigate to `MSMA_Frontend/MSMA_Frontend`.
2.  These are static files. You can serve them using a simple HTTP server (e.g., Live Server in VS Code) or deploy them to a web server (Nginx, Apache).
3.  Ensure the API endpoints in the JavaScript files point to your running Backend URL (e.g., `http://localhost:8080`).

#### **5. Android App Setup (`MSMA_App`)**

1.  Open the `MSMA_App` folder in Android Studio.
2.  Allow Gradle to sync and download dependencies.
3.  Ensure you have a `google-services.json` file in the `app/` directory if you want Firebase features to work.
4.  Connect an Android device or start an emulator.
5.  Run the app (Shift + F10).

---

### **Demo**

(https://drive.google.com/drive/folders/1z6KgQgd_eARIe2gF5ygXLNVGbE3FNoFM?usp=drive_link)

---

### **Developed by Pham Hoang Anh**
