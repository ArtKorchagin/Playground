package io.github.korchagin.playground

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.ac_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ac_main)
        sample_text.text = stringFromJNI()
    }

    external fun stringFromJNI(): String

    companion object {

        init {
            System.loadLibrary("native-lib")
        }
    }
}
