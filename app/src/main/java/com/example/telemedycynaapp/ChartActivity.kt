package com.example.telemedycynaapp

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.example.telemedycynaapp.databinding.ActivityChartBinding

class ChartActivity : ComponentActivity() {
    private lateinit var binding: ActivityChartBinding

    private val dataReceiver=object: BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val data = intent?.getDoubleExtra("data",0.0)
            data?.let{
                processData(it)
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        binding = ActivityChartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val filter = IntentFilter("DATA_RECEIVED")
        registerReceiver(dataReceiver, filter)
        Log.v("AAA","Connected and data processing...")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(dataReceiver)
    }

    private fun processData(data: Double){
            //rysowanei wykresu
    }
}