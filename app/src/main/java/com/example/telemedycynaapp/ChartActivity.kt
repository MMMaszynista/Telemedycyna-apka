package com.example.telemedycynaapp

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.telemedycynaapp.databinding.ActivityChartBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter




class ChartActivity : ComponentActivity() {
    private lateinit var binding: ActivityChartBinding
    private lateinit var lineChart: LineChart
    private lateinit var dataSet: LineDataSet
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
        binding = ActivityChartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lineChart = binding.humidityChart
        lineChart.setDrawGridBackground(false)
        lineChart.setTouchEnabled(true)
        lineChart.description.isEnabled = false
        lineChart.setPinchZoom(true)
        lineChart.axisLeft.axisMaximum=60f;
        lineChart.axisLeft.textColor=Color.WHITE
        lineChart.axisLeft.axisLineColor=Color.WHITE
        lineChart.axisLeft.textSize=16f
        lineChart.axisLeft.axisMinimum=0f;
        lineChart.axisLeft.granularity=10f; // Ustawienie odstępu co 10 jednostek
        lineChart.axisLeft.isGranularityEnabled = true;
        lineChart.xAxis.valueFormatter = TimeValueFormatter();
        lineChart.xAxis.textColor=Color.WHITE
        lineChart.xAxis.axisLineColor=Color.WHITE
        lineChart.xAxis.textSize=16f
        lineChart.xAxis.position=XAxis.XAxisPosition.BOTTOM;
        lineChart.axisRight.isEnabled=false;
        lineChart.legend.textColor=Color.WHITE
        lineChart.legend.textSize=18f

        val marker=Marker(this)
        marker.chartView=lineChart
        lineChart.marker=marker

        val description = Description().apply {
            text = "Czas"
            textSize = 16f
            textColor = Color.WHITE
            //setPosition(2f, - 2f)  // pozycja w prawym dolnym rogu
        }

        /*lineChart.description.text = "Czas (sekundy)"
        lineChart.description.textSize = 16f
        lineChart.description.textColor = Color.WHITE*/
        /*lineChart.description.position.x=500f
        lineChart.description.position.y=50f*/

        lineChart.description=description
        val filter = IntentFilter("DATA_RECEIVED")
        registerReceiver(dataReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(dataReceiver)
        setResult(RESULT_CANCELED)
    }

    private fun processData(data: Float) {
        if (data > 55f) {
            lineChart.axisLeft.axisMaximum = 100f;
            lineChart.axisLeft.labelCount=10;
        }
        entries.add(Entry(dataCount.toFloat(), data))
        dataCount+=2
        dataSet = LineDataSet(entries, "Wilgotność")
        prepDataSet()
        refreshChart()
    }

    private fun prepDataSet() {
        dataSet.setDrawCircles(false)
        dataSet.setDrawCircleHole(false)
        dataSet.setDrawValues(false)
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 15f
        dataSet.color = Color.YELLOW
        dataSet.lineWidth=3f

    }

    private fun refreshChart() {
        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.invalidate()
    }
}
class TimeValueFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        val totalSeconds = value.toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}