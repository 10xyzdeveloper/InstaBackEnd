import io.ktor.http.content.PartData
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val part: PartData.FileItem = TODO()
    val bytes = part.provider().toByteArray()
}
