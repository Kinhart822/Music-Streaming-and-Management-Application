package vn.edu.usth.msma.utils.constants

enum class IP(private val ip: String) {
    KINHART822("192.168.33.100");

    fun getIp(): String {
        return "http://" + this.ip + ":8080/"
    }
}