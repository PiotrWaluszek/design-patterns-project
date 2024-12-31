package orm.logger

class Logger {
    fun log(message: String) = println("[LOG] $message")
    fun error(message: String, exception: Exception? = null) {
        println("[ERROR] $message")
        exception?.let { println("[ERROR] Exception: ${it.message}") }
    }
    
    companion object {
        private var instance: Logger? = null
        fun getInstance(): Logger = instance ?: Logger().also { instance = it }
    }
}