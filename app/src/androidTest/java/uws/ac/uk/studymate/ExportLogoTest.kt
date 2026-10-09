package uws.ac.uk.studymate

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter

@RunWith(AndroidJUnit4::class)
class ExportLogoTest {
    @Test
    fun exportLogo() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_studymate_logo)!!
        
        // Color it gold #C4A24A
        drawable.colorFilter = PorterDuffColorFilter(0xFFC4A24A.toInt(), PorterDuff.Mode.SRC_IN)
        
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        
        val dir = File("/sdcard/Download")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "logo_gold.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
