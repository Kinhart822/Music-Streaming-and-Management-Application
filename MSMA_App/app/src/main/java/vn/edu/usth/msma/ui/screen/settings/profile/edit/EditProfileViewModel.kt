package vn.edu.usth.msma.ui.screen.settings.profile.edit

import android.Manifest
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.network.ApiService
import vn.edu.usth.msma.utils.constants.Gender
import vn.edu.usth.msma.utils.constants.UserType
import vn.edu.usth.msma.utils.eventbus.Event.ProfileUpdatedEvent
import vn.edu.usth.msma.utils.eventbus.EventBus
import java.io.File
import javax.inject.Inject

data class EditProfileState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val userName: String? = null,
    val email: String? = null,
    val id: Long? = null,
    val avatar: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val gender: Gender = Gender.Male,
    val birthDay: String? = null,
    val phone: String? = null,
    val status: Int? = null,
    val createdBy: Long? = null,
    val lastModifiedBy: Long? = null,
    val createdDate: String? = null,
    val lastModifiedDate: String? = null,
    val userType: UserType? = null,
    val avatarUri: Uri? = null,
    val showGenderDropdown: Boolean = false,
    val pickerType: String = ""
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    init {
        fetchUserDetails()
    }

    fun onFirstNameChanged(firstName: String?) {
        _state.update { it.copy(firstName = firstName) }
    }

    fun onLastNameChanged(lastName: String?) {
        _state.update { it.copy(lastName = lastName) }
    }

    fun onGenderSelected(gender: Gender) {
        _state.update { it.copy(gender = gender, showGenderDropdown = false) }
    }

    fun toggleGenderDropdown() {
        _state.update { it.copy(showGenderDropdown = !it.showGenderDropdown) }
    }

    fun onDateOfBirthChanged(birthDay: String?) {
        _state.update { it.copy(birthDay = birthDay) }
    }

    fun onPhoneChanged(phone: String?) {
        _state.update { it.copy(phone = phone) }
    }

    fun onAvatarSelected(uri: Uri?) {
        _state.update { it.copy(avatarUri = uri) }
        Log.d("EditProfileViewModel", "Avatar selected: $uri")
    }

    fun requestAvatarPicker(permissionLauncher: ManagedActivityResultLauncher<String, Boolean>) {
        Log.d("EditProfileViewModel", "Requesting permission for avatar")
        _state.update { it.copy(pickerType = "avatar") }
        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
    }

    fun updateProfile() {
        _state.update { it.copy(isLoading = true, isSuccess = false, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Create MultipartBody.Part for avatar (optional)
                val avatarPart = _state.value.avatarUri?.let { uri ->
                    val file = uriToFile(context, uri)
                    MultipartBody.Part.createFormData(
                        "avatar",
                        file.name,
                        file.asRequestBody("image/*".toMediaTypeOrNull())
                    )
                }

                // Create RequestBody for text fields, using empty string for null values
                val descriptionBody = ("").toRequestBody("text/plain".toMediaTypeOrNull())
                val firstNameBody = (_state.value.firstName ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
                val lastNameBody = (_state.value.lastName ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
                val genderBody = _state.value.gender.name.toRequestBody("text/plain".toMediaTypeOrNull())
                val birthDayBody = (_state.value.birthDay ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
                val phoneBody = (_state.value.phone ?: "").toRequestBody("text/plain".toMediaTypeOrNull())

                // Call the API
                val response = apiService.getAccountApi().updateAccount(
                    avatar = avatarPart,
                    description = descriptionBody,
                    firstName = firstNameBody,
                    lastName = lastNameBody,
                    gender = genderBody,
                    dateOfBirth = birthDayBody,
                    phone = phoneBody
                )

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.status == "200") {
                        fetchUserDetails()
                        _state.update { it.copy(isLoading = false, isSuccess = true) }
                        // Notify that profile has been updated
                        EventBus.publish(ProfileUpdatedEvent)
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = "Update failed: ${apiResponse.message}"
                            )
                        }
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Update failed: ${response.message()}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("EditProfileViewModel", "Update error: ${e.message}", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Update error: ${e.message}"
                    )
                }
            }
        }
    }

    private fun fetchUserDetails() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val email = preferencesManager.currentUserEmailFlow.first() ?: run {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "No user logged in"
                        )
                    }
                    return@launch
                }
                val response = apiService.getAccountApi().getUserDetailByUser()
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    // Map gender string to Gender enum
                    val gender = when (user.gender) {
                        "Male" -> Gender.Male
                        "Female" -> Gender.Female
                        else -> Gender.Male // Default to Male if unknown
                    }
                    _state.update {
                        it.copy(
                            id = user.id,
                            avatar = user.avatar,
                            firstName = user.firstName,
                            lastName = user.lastName,
                            email = user.email,
                            gender = gender,
                            birthDay = user.birthDay,
                            phone = user.phone,
                            status = user.status,
                            createdBy = user.createdBy,
                            lastModifiedBy = user.lastModifiedBy,
                            createdDate = user.createdDate,
                            lastModifiedDate = user.lastModifiedDate,
                            userType = user.userType
                        )
                    }
                } else {
                    Log.e("EditProfileViewModel", "Failed to fetch user details: ${response.message()}")
                    _state.update {
                        it.copy(
                            error = "Failed to fetch user details: ${response.message()}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("EditProfileViewModel", "Error fetching user details: ${e.message}", e)
                _state.update {
                    it.copy(
                        error = "Error fetching user details: ${e.message}"
                    )
                }
            }
        }
    }

    private fun uriToFile(context: Context, uri: Uri): File {
        val fileName = getFileName(context, uri) ?: "temp_image_${System.currentTimeMillis()}.jpg"
        return try {
            val file = File(context.cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Input stream is null")
            Log.d("EditProfileViewModel", "File created: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("EditProfileViewModel", "Error converting Uri to File: ${e.message}", e)
            throw e
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }
}