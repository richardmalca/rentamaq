package pe.rentamaq.core.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "equipment")
data class Equipment(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val zone: String,
    val hourlyRate: Int,
    val dailyRate: Int
)

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val equipmentName: String,
    val start: String,
    val end: String,
    val total: Long
)

@Dao
interface CatalogDao {
    @Query("SELECT * FROM equipment ORDER BY id")
    fun equipment(): Flow<List<Equipment>>

    @Query("SELECT * FROM bookings ORDER BY id DESC")
    fun bookings(): Flow<List<Booking>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seed(equipment: List<Equipment>)

    @Insert
    suspend fun insert(booking: Booking): Long

    @Query("DELETE FROM bookings WHERE id = :id")
    suspend fun remove(id: Long)
}

@Database(entities = [Equipment::class, Booking::class], version = 1, exportSchema = false)
abstract class RentamaqDatabase : RoomDatabase() {
    abstract fun catalog(): CatalogDao
}
