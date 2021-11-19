package pl.piotrnarecki.recordaudiodemo

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.slider.Slider
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.experimental.and


class MainActivity : AppCompatActivity() {

    var path = Environment.getExternalStorageDirectory().toString() + "/myrec.3gp"

    var isRecording = false

    val mr = MediaRecorder()
    var mp = MediaPlayer()

    val handler = Handler()

    var isSoundGenerating = false


    //generate sound

    private val duration = 0.5 // seconds

    private val sampleRate = 8000
    private val numSamples = duration * sampleRate
    private val sample = DoubleArray(numSamples.toInt())
    private var freqOfTone = 700.0  //440.0 // hz

    private val generatedSnd = ByteArray((2 * numSamples).toInt())


    var audioTrack = AudioTrack(
        AudioManager.STREAM_MUSIC,
        sampleRate, AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT, generatedSnd.size,
        AudioTrack.MODE_STATIC
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()

        button1.isEnabled = true
        button2.isEnabled = false
        button3.isEnabled = false
        button4.isEnabled = true
        button5.isEnabled = false


//start recording
        button1.setOnClickListener {
            startRecording()
        }
//stop recording
        button2.setOnClickListener {
            stopRecording()
        }
//play recording
        button3.setOnClickListener {
            playRecording()
        }

        // generate sound
        button4.setOnClickListener {
            generateSound()
        }
        //stop generated sound
        button5.setOnClickListener {
            stopSound()
        }


    }

    override fun onResume() {
        super.onResume()
//        startRecording()
    }

    override fun onPause() {
        super.onPause()
        stopRecording()

    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        stopSound()
    }


    fun startRecording() {


        Toast.makeText(applicationContext, "RECORDING START", Toast.LENGTH_SHORT).show()
        isRecording = true

        mr.setAudioSource(MediaRecorder.AudioSource.MIC)
        mr.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mr.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB)
        mr.setOutputFile(path)
        mr.prepare()
        mr.start()


        if (mp.isPlaying) {
            mp.stop()
        }


        button1.isEnabled = false
        button2.isEnabled = true
        button3.isEnabled = false
        button4.isEnabled = false
        button5.isEnabled = false


        handler.postDelayed(object : Runnable {
            override fun run() {
                val amplitude: Double = getAmplitude()
                text_view.setText(String.format(Locale.US, "%.1f", amplitude))
                handler.postDelayed(this, 1000)
            }
        }, 1000)


    }

    fun stopRecording() {

        Toast.makeText(applicationContext, "RECORDING STOP", Toast.LENGTH_SHORT).show()
        isRecording = false


        mr.stop()

//        if (mp.isPlaying) {
//            mp.stop()
//        }


        text_view.setText("")


        button1.isEnabled = true
        button2.isEnabled = false
        button3.isEnabled = true
        button4.isEnabled = true
        button5.isEnabled = false

    }


    fun playRecording() {

        Toast.makeText(applicationContext, "RECORDING PLAY", Toast.LENGTH_SHORT).show()
        isRecording = false;


        mp.setDataSource(path)
        mp.prepare()
        mp.start()


        button1.isEnabled = true
        button2.isEnabled = false
        button3.isEnabled = false
        button4.isEnabled = true
        button5.isEnabled = false

    }


    fun generateSound() {


        val thread = Thread {

            freqOfTone = freq_slider.value.toDouble()
//            text_view.setText(freqOfTone.toString() + " Hz")
            genTone(freqOfTone)
            handler.post { playSound() }
        }
        thread.start()
    }

    fun stopSound() {


        var isSoundGenerating = false
        audioTrack.flush();
        audioTrack.stop();
        audioTrack.release();


        button1.isEnabled = true
        button2.isEnabled = false
        button3.isEnabled = false
        button4.isEnabled = true
        button5.isEnabled = false

    }


    fun genTone(freqency: Double) {


        // fill out the array
        for (i in 0 until numSamples.toInt()) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / freqency))
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        var idx = 0
        for (dVal in sample) {
            // scale to maximum amplitude
            val value = (dVal * 32767).toInt().toShort()
            // in 16 bit wav PCM, first byte is the low order byte


            generatedSnd[idx++] = ((value and 0x00ff).toByte())
            generatedSnd[idx++] = ((value and 0xff00.toShort()).toByte())
        }
    }

    fun playSound() {
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate, AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT, generatedSnd.size,
            AudioTrack.MODE_STATIC
        )
        audioTrack.write(generatedSnd, 0, generatedSnd.size)
        audioTrack.play()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 111 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            button1.isEnabled = false
        }

    }


    fun checkPermissions() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 111
            )
        }

    }

    private fun getAmplitude(): Double {
        return if (mr != null) {
            val maxAmplitude: Double = mr.maxAmplitude.toDouble()
            20 * Math.log10(maxAmplitude / 0.0001)
        } else {
            0.0
        }
    }


}