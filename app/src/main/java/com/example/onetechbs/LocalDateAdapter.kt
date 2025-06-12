import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LocalDateAdapter : TypeAdapter<LocalDate>() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun write(out: JsonWriter, value: LocalDate?) {
        out.value(value?.format(DateTimeFormatter.ISO_LOCAL_DATE))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun read(`in`: JsonReader): LocalDate? {
        return if (`in`.peek() == com.google.gson.stream.JsonToken.NULL) {
            `in`.nextNull()
            null
        } else {
            val dateStr = `in`.nextString()
            LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)  // ✅ safer for "yyyy-MM-dd"
        }
    }
}