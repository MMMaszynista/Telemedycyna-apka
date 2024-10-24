package com.example.telemedycynaapp

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import com.example.telemedycynaapp.databinding.ActivityChartBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet


class ChartActivity : ComponentActivity() {
    private lateinit var binding: ActivityChartBinding
    private lateinit var lineChart: LineChart
    private val entries = mutableListOf<Entry>()
    private var dataCount = 0
    private val dataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val data = intent?.getFloatExtra("data", 0.0F)
            data?.let {
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
        lineChart = binding.humidityChart
        // Konfiguracja wykresu
        lineChart.setDrawGridBackground(false)
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)
        val filter = IntentFilter("DATA_RECEIVED")
        registerReceiver(dataReceiver, filter)
        Log.v("AAA", "Connected and data processing...")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(dataReceiver)
    }

    private fun processData(data: Float) {
        entries.add(Entry(dataCount.toFloat(), data))
        dataCount++
        val dataSet = LineDataSet(entries, "Data")
        dataSet.color = Color.BLUE
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 10f

        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.invalidate()
    }
}