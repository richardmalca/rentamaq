package pe.rentamaq.core.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "equipment")
data class Equipment(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val zone: String,
    val hourlyRate: Int,
    val dailyRate: Int,
    @ColumnInfo(defaultValue = "''") val category: String
)

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val equipmentName: String,
    val start: String,
    val end: String,
    val total: Long,
    @ColumnInfo(defaultValue = "'PENDING'") val status: String = "PENDING",
    val expiresAt: String? = null,
    val providerPhone: String? = null,
    val paidAmount: Long? = null,
    val cancellationFee: Long? = null,
    val refundWindowDays: Int? = null
)

@Dao
interface CatalogDao {
    @Query("SELECT * FROM equipment ORDER BY id")
    fun equipment(): Flow<List<Equipment>>

    @Query("SELECT * FROM bookings ORDER BY id DESC")
    fun bookings(): Flow<List<Booking>>

    @Upsert
    suspend fun seed(equipment: List<Equipment>)

    @Insert
    suspend fun insert(booking: Booking): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seedBookings(bookings: List<Booking>)

    @Query("UPDATE bookings SET status = :next WHERE id = :id AND status = :expected")
    suspend fun transitionBooking(id: Long, expected: String, next: String): Int

    @Query("DELETE FROM bookings WHERE id = :id")
    suspend fun remove(id: Long)
}

@Database(entities = [Equipment::class, Booking::class], version = 3, exportSchema = false)
abstract class RentamaqDatabase : RoomDatabase() {
    abstract fun catalog(): CatalogDao
}

val migrateCatalogCategory = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE equipment ADD COLUMN category TEXT NOT NULL DEFAULT ''")
    }
}

val migrateBookingStatus = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE bookings ADD COLUMN status TEXT NOT NULL DEFAULT 'PENDING'")
        db.execSQL("ALTER TABLE bookings ADD COLUMN expiresAt TEXT")
        db.execSQL("ALTER TABLE bookings ADD COLUMN providerPhone TEXT")
        db.execSQL("ALTER TABLE bookings ADD COLUMN paidAmount INTEGER")
        db.execSQL("ALTER TABLE bookings ADD COLUMN cancellationFee INTEGER")
        db.execSQL("ALTER TABLE bookings ADD COLUMN refundWindowDays INTEGER")
    }
}
