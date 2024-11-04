package com.example.telemedycynaapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import java.security.AccessController.getContext


class ChartActivity : ComponentActivity() {
    private lateinit var binding: ActivityChartBinding
    private lateinit var lineChart: LineChart
    private lateinit var dataSet: LineDataSet
    private val requestedPermissions = Manifest.permission.WRITE_EXTERNAL_STORAGE
    private lateinit var db: AppDatabase
    private var entries = mutableListOf<Entry>()
    private var timeSeconds = 0
    private var timeId = 1
    private var updateData = false
    private var maxMeassures = 70

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
           saveDataToFile()
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


    private fun saveDataToFile() {
        // Pobierz dane z bazy danych
        if(!checkRequestPermission()){
            requestPermission.launch(requestedPermissions)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val measures = db.meassureDao().getAllMeassures()
            if (measures.isNotEmpty()) {

                val fileContent = StringBuilder()
                fileContent.append("ID,Date,Humidity\n") // Nagłówki

                for (measure in measures) {
                    fileContent.append("${measure.id},${measure.date},${measure.humidity}\n")
                }

                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){ //dla android >= 10
                    val resolver = contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, "measures.csv")
                        put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        resolver.openOutputStream(it)?.use { outputStream ->
                            outputStream.write(fileContent.toString().toByteArray())
                        }
                    }
                }else{ //dla android < 10
                    // Zapisz do pliku
                    val file = File(getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "measures.csv")
                    file.writeText(fileContent.toString())
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChartActivity, "Dane zostały zapisane do pliku", Toast.LENGTH_SHORT).show()
                }
            }else{
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChartActivity, "Aktualnie baza danych jest pusta", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun checkRequestPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(this, requestedPermissions) == PackageManager.PERMISSION_GRANTED)
    }
    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
            var permanentlyDenied = false

            if (!permission) {
                if (!shouldShowRequestPermissionRationale(requestedPermissions)) {
                    permanentlyDenied = true
                }
            }

            when {
                permanentlyDenied -> {
                    DialogManager.showSettingsDialog(this)
                }

                checkRequestPermission() -> {
                    saveDataToFile()
                }

                else -> {
                    DialogManager.showDialogPermDenied(this,::saveDataToFile)
                }
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