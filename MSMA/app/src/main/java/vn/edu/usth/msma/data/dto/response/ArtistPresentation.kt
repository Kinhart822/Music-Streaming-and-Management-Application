package vn.edu.usth.msma.data.dto.response

import vn.edu.usth.msma.utils.constants.UserType

data class ArtistPresentation(
    val id: Long,
    val avatar: String,
    val firstName: String,
    val lastName: String,
    val artistName: String,
    val description: String,
    val image: String,
    val backgroundImage: String,
    val countListen: Long,
    val numberOfFollowers: Long,
    val email: String,
    val gender: String,
    val birthDay: String,
    val phone: String,
    val status: Int,
    val createdBy: Long,
    val lastModifiedBy: Long,
    val createdDate: String,
    val lastModifiedDate: String,
    val userType: UserType
)
