package vn.edu.usth.msma.utils.constants

enum class UserType(val code: Int) {
    ADMIN(0),
    USER(1),
    ARTIST(2);

    companion object {
        fun getName(code: Int): String {
            return UserType.entries.find { it.code == code }?.name
                ?: throw IllegalArgumentException("Invalid code: $code")
        }
    }
}
