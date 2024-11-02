package com.example.telemedycynaapp

import android.content.Context
import android.widget.TextView
import com.example.telemedycynaapp.databinding.MarkerLayoutBinding
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class Marker(context: Context) : MarkerView(context, R.layout.marker_layout) {
    private var cordinates : TextView = findViewById(R.id.coordinateTextView)

    // Metoda wywoływana przy każdym przesunięciu kursora na wykresie
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            cordinates.text = "X: ${it.x}, Y: ${it.y}" // Ustawienie tekstu z współrzędnymi
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2).toFloat(), -height.toFloat())
    }
}