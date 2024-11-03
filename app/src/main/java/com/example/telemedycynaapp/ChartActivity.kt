package com.example.telemedycynaapp

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.example.telemedycynaapp.database.AppDatabase
import com.example.telemedycynaapp.database.Meassure
import com.example.telemedycynaapp.databinding.ActivityChartBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class ChartActivity : ComponentActivity() {
    private lateinit var binding: ActivityChartBinding
    private lateinit var lineChart: LineChart
    private lateinit var dataSet: LineDataSet
    private lateinit var db: AppDatabase
    private var entries = mutableListOf<Entry>()
    private var timeSeconds = 0
    private var timeId = 1
    private var updateData = false
    private var maxMeassures = 500

    private val queue = object : LinkedHashMap<Float, Float>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Float, Float>): Boolean {
            return size > maxMeassures
        }
    }

    private val dataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val data = intent?.getFloatExtra("data", 0.0F)
            data?.let {
                processData(it)
            }
        }
    }

    private fun resetDb() {
        CoroutineScope(Dispatchers.IO).launch {
            db.meassureDao().deleteAll()
            db.meassureDao().resetId()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.getDatabase(this)
        resetDb()
        binding = ActivityChartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lineChart = binding.humidityChart
        lineChart.setDrawGridBackground(false)
        lineChart.setTouchEnabled(true)
        lineChart.description.isEnabled = false
        lineChart.setPinchZoom(true)
        lineChart.axisLeft.axisMaximum = 60f
        lineChart.axisLeft.textColor = Color.WHITE
        lineChart.axisLeft.axisLineColor = Color.WHITE
        lineChart.axisLeft.textSize = 16f
        lineChart.axisLeft.axisMinimum = 10f
        lineChart.axisLeft.granularity = 10f // Ustawienie odstępu co 10 jednostek
        lineChart.axisLeft.isGranularityEnabled = true
        lineChart.xAxis.valueFormatter = TimeValueFormatter()
        lineChart.xAxis.textColor = Color.WHITE
        lineChart.xAxis.axisLineColor = Color.WHITE
        lineChart.xAxis.textSize = 16f
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.axisRight.isEnabled = false
        lineChart.legend.textColor = Color.WHITE
        lineChart.legend.textSize = 18f

        binding.saveFileButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                saveDataToFile()
            }
        }

        val marker = Marker(this)
        marker.chartView = lineChart
        lineChart.marker = marker

        val description = Description().apply {
            text = "Czas"
            textSize = 16f
            textColor = Color.WHITE
            //setPosition(2f, - 2f)  // pozycja w prawym dolnym rogu
        }

        lineChart.description = description
        val filter = IntentFilter("DATA_RECEIVED")
        registerReceiver(dataReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(dataReceiver)
        setResult(RESULT_CANCELED)
    }

    private fun saveToDb(data: Float) {
        CoroutineScope(Dispatchers.IO).launch {
            val timeValueFormatter = TimeValueFormatter()
            if (timeId > maxMeassures) {
                timeId = 1
            }
            if (updateData) {
                val meassureToUpdate = db.meassureDao().getMeassureById(timeId)
                // Sprawdź, czy pomiar istnieje, a następnie zaktualizuj
                meassureToUpdate?.let {
                    val updatedMeassure = it.copy(
                        humidity = data,
                        date = timeValueFormatter.getFormattedValue(timeSeconds.toFloat())
                    ) // Utwórz nową instancję z nową wilgotnością
                    db.meassureDao().update(updatedMeassure) // Wywołaj metodę update
                }
            } else {
                db.meassureDao().insert(
                    Meassure(
                        date = timeValueFormatter.getFormattedValue(timeSeconds.toFloat()),
                        humidity = data
                    )
                )
                if (timeId == maxMeassures) {
                    updateData = true
                }
            }
            timeId++
        }
    }


    private fun processData(data: Float) {
        saveToDb(data)
        Log.v("timeID: ", timeId.toString())
        if (data > 55f) {
            lineChart.axisLeft.axisMaximum = 100f
            lineChart.axisLeft.labelCount = 10
        }
        queue.put(timeSeconds.toFloat(), data)
        //entries.add(Entry(timeSeconds.toFloat(), data))
        entries = queueToEntries(queue)
        timeSeconds += 2
        dataSet = LineDataSet(entries, "Wilgotność")
        prepDataSet()
        refreshChart()
    }

    private fun queueToEntries(queue: LinkedHashMap<Float, Float>): MutableList<Entry> {
        val entries = mutableListOf<Entry>()
        for (pair in queue) {
            entries.add(Entry(pair.key, pair.value))
        }
        return entries
    }

    private fun prepDataSet() {
        //dataSet.setDrawCircles(false)
        dataSet.setDrawCircleHole(false)
        dataSet.setDrawValues(false)
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 15f
        dataSet.setDrawCircles(true)
        dataSet.color = Color.YELLOW
        dataSet.lineWidth = 3f
    }

    private fun refreshChart() {
        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.invalidate()
    }

    private suspend fun saveDataToFile() {
        // Pobierz dane z bazy danych
        var measures = db.meassureDao().getAllMeassures()
        // Przygotuj ścieżkę do pliku
        val fileName = "measures.csv"
        val fileContent = StringBuilder()
        fileContent.append("ID,Date,Humidity\n") // Nagłówki
        // Dodaj dane do pliku
        for (measure in measures) {
            fileContent.append("${measure.id},${measure.date},${measure.humidity}\n")
        }
        // Zapisz do pliku
        withContext(Dispatchers.IO) {
            val file = File(getExternalFilesDir(null), fileName)
            file.writeText(fileContent.toString())
        }
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