package domain.auth

object AuthSecret {
    val ALGORITHM = "PBKDF2WithHmacSHA512"
    val ITERATIONS = 120_000
    val KEY_LENGTH = 256
    val SECRET = "!!SomeRa1ndomSec_ret"
}