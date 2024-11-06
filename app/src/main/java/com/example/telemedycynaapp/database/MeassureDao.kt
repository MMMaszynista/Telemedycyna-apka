package com.example.telemedycynaapp.database
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
    fun getAllMeassures(): List<Meassure>

    @Query("SELECT COUNT(*) FROM meassures")
    fun getRecordsCount(): Int

    @Query("SELECT * FROM meassures WHERE id=:id")
    fun getMeassureById(id: Int): Meassure?

    @Query("DELETE FROM meassures")
    fun deleteAll()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'meassures'")
    fun resetId()
}