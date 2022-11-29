package io.github.kolod

import org.apache.logging.log4j.LogManager
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import javax.sound.sampled.*
import kotlin.math.log10

class Sound (private val name :String) {
    private val logger = LogManager.getLogger()

    companion object {
        @JvmField var volume = 100
        @JvmStatic private fun db() = log10(volume / 100f) * 20f
    }

    fun play() {
        if (volume > Float.MIN_VALUE) try {
            val stream = javaClass.getResourceAsStream(name) ?: return
            val bufferedStream: InputStream = BufferedInputStream(stream)
            val ais = AudioSystem.getAudioInputStream(bufferedStream) ?: return

            with (AudioSystem.getClip()) {
                open(ais)
                (getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl)?.value = db()
                framePosition = 0
                start()
            }

            ais.close()
        } catch (ex :IllegalArgumentException) {
            logger.error(ex.message, ex)
        } catch (ex: UnsupportedAudioFileException) {
            logger.error(ex.message, ex)
        } catch (ex: LineUnavailableException) {
            logger.error(ex.message, ex)
        } catch (ex: IOException) {
            logger.error(ex.message, ex)
        }
    }
}