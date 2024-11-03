package com.example.telemedycynaapp.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meassures")
data class Meassure(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,
    val humidity: Float
)