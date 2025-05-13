package vn.edu.usth.msma.utils.constants

enum class IP(private val ip: String) {
    KINHART822("192.168.1.13"),
    KINHART822_4G("172.20.10.4"),
    KINHART822_ZEN8LABS("192.168.1.192");

    fun getIp(): String {
        return "http://" + this.ip + ":8080/"
    }
}