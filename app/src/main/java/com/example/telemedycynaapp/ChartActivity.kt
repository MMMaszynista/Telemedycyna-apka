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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.LinkedList
import java.util.Locale
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue


class ChartActivity : ComponentActivity() { //klasa wykresu prezentujacego wyniki pomiarów wilgotnosci
    private lateinit var binding: ActivityChartBinding
    private lateinit var lineChart: LineChart
    private lateinit var dataSet: LineDataSet
    private val dbQueue=ConcurrentLinkedQueue<DbChartMeassure>()
    private val requestedPermissions = Manifest.permission.WRITE_EXTERNAL_STORAGE
    private lateinit var db: AppDatabase
    private var entries = mutableListOf<Entry>()
    private var timeSeconds = 0
    private var timeId = 1
    private var updateData = false
    private var maxMeassures = 50

    private val queue: Queue<Pair<Int, Float>> = LinkedList()

    private val dataReceiver = object : BroadcastReceiver() { //obiekt globalny przesylajacy z klasy gattManager zmierzona wartosc wilgotnosci
        override fun onReceive(context: Context?, intent: Intent?) {
            val data = intent?.getFloatExtra("data", 0.0F)
            data?.let {
                processData(it)
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) { //inicjalizacja pól klasy
        super.onCreate(savedInstanceState)

        db = AppDatabase.getDatabase(this)
        resetDb() //usuniecie zawartosci tabeli w bazie danych
        binding = ActivityChartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prepLineChart() //konfiguracja wygladu wykresu liniowego

        binding.saveFileButton.setOnClickListener { //przypisanie funkcji wywolywanej po nacisnieciu przycisku
            saveDataToFile()
        }

        val filter = IntentFilter("DATA_RECEIVED")
        registerReceiver(dataReceiver, filter)

        saveToDb(dbQueue) //funckja wywolywana w osobnym watku ktora zapisuje pomiary do bazy danych
    }

    override fun onDestroy() { //funckja wywoływana gdy aktywność (ekran) jest zamykany
        super.onDestroy()
        unregisterReceiver(dataReceiver)
        setResult(RESULT_CANCELED)
    }

    private fun saveToDb(dataQueue : ConcurrentLinkedQueue<DbChartMeassure>) { //funkcja zapisująca pomiary do bazy danych
        CoroutineScope(Dispatchers.IO).launch {
            while(true) {
                if (dbQueue.isNotEmpty()) {
                    var entry = dataQueue.poll() // Pobiera i usuwa pierwszy element
                    while (entry != null) { //jesli w buforze sa jakies wartosci to zapisuj kazdy do bazy danych
                        if (entry.update) { //zaktualizuj zapisana juz wartosc wilgotnosci w tabeli
                            if(db.meassureDao().getMeassureById(entry.meas.id)!=null) {
                                db.meassureDao().update(
                                    Meassure(
                                        id = entry.meas.id,
                                        humidity = entry.meas.humidity,
                                        date = entry.meas.date
                                    )
                                ) // Wywołaj metodę update
                            }
                        } else { //dodaaj nowa wartosc wilgotnosci do tabeli
                            db.meassureDao().insert(
                                Meassure(
                                    date = entry.meas.date,
                                    humidity = entry.meas.humidity
                                )
                            )
                        }
                        entry = dataQueue.poll()
                    }
                }else {
                    delay(500) // Dodanie opóźnienia, aby nie obciążać CPU
                }
            }
        }

    }

    private fun processData(data: Float) { //odbiera dane, dodaje do kolejki i wyświetla na wykresie

        if (data > 55f) {
            lineChart.axisLeft.axisMaximum = 100f
            lineChart.axisLeft.labelCount = 10
        }

        if (timeId > maxMeassures) {
            timeId = 1
        }

        if(updateData){
            queue.poll()
            queue.offer(Pair(timeSeconds, data))
        } else{
            queue.offer(Pair(timeSeconds, data))
        }

        dbQueue.add(DbChartMeassure(Meassure(id = timeId, date=TimeValueFormatter().getFormattedValue(timeSeconds.toFloat()), humidity = data), updateData))

        entries=queueToEntries(queue)
        dataSet = LineDataSet(entries, "Wilgotność")

        if (!updateData && timeId == maxMeassures) {
            updateData = true
        }

        timeSeconds += 2
        timeId++
        prepDataSet()
        refreshChart()
    }

    private fun queueToEntries(queue: Queue<Pair<Int, Float>>): MutableList<Entry> {
        val entries = mutableListOf<Entry>()
        for (pair in queue) {
            entries.add(Entry(pair.first.toFloat(), pair.second))
        }
        return entries
    }


    private fun resetDb() { //funkcja usuwa wszystkie dane z tabeli w bazie danych
        CoroutineScope(Dispatchers.IO).launch {
            db.meassureDao().deleteAll()
            db.meassureDao().resetId()
        }
    }
    
    private fun prepLineChart(){ //funkcja ustawia parametry wykresu - kolory, rozmiary punktów, wartości maksymalne / minimalne na osi X, Y

        lineChart = binding.humidityChart
        lineChart.setDrawGridBackground(false)
        lineChart.setTouchEnabled(true)
        lineChart.description.isEnabled = false
        lineChart.setPinchZoom(true)
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
        lineChart.xAxis.axisMinimum= 0F
        lineChart.xAxis.axisMaximum= (maxMeassures*2).toFloat()
        lineChart.isAutoScaleMinMaxEnabled=false

        lineChart.axisRight.isEnabled = false
        lineChart.legend.textColor = Color.WHITE
        lineChart.legend.textSize = 18f
        lineChart.setNoDataText("")
        dataSet=LineDataSet(entries, "Puste dane")
        val lineData = LineData(dataSet)
        lineChart.data = lineData

        val marker = Marker(this)
        marker.chartView = lineChart

        lineChart.marker = marker
        lineChart.axisLeft.axisMaximum = 60f

        val description = Description().apply {
            text = "Czas"
            textSize = 16f
            textColor = Color.WHITE
        }
        lineChart.description = description
        refreshChart()
    }
    private fun prepDataSet() { //przygotuj zestaw danych do wyswietlenia na wykresie
        dataSet.setDrawCircleHole(false)
        dataSet.setDrawValues(false)
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 15f
        dataSet.setDrawCircles(true)
        dataSet.color = Color.YELLOW
        dataSet.lineWidth = 3f
    }

    private fun refreshChart() { //funkcja odświeżająca wykres
        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.invalidate()
    }

    private fun saveDataToFile() { //funckja zapisująca zebrane dane do formatu CSV
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
                    fileContent.append("${measure.id}, ${measure.date}, ${measure.humidity}\n")
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
    
    private fun checkRequestPermission(): Boolean { //funkcja sprawdzająca czy użytkownik przyznał odpowiednie uprawnienia
        return (ContextCompat.checkSelfPermission(this, requestedPermissions) == PackageManager.PERMISSION_GRANTED)
    }
    
    private val requestPermission = //funkcja obsługi przyznania / nieprzyznania uprawnień przez użytkownika
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
data class DbChartMeassure(val meas: Meassure, val update : Boolean)

class TimeValueFormatter : ValueFormatter() { //klasa odpowiedzialna za wyświetlanie wartości na osi X w odpowiednim formacie godzina:minuta:sekunda
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