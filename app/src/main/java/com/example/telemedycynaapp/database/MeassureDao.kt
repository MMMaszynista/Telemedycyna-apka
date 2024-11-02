package com.example.telemedycynaapp.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MeassureDao {
    @Insert
    suspend fun insert(meassure: Meassure)

    @Update
    suspend fun update(note: Meassure)

    @Delete
    suspend fun delete(note: Meassure)

    @Query("SELECT * FROM meassures")
    fun getAllNotes(): LiveData<List<Meassure>>
}