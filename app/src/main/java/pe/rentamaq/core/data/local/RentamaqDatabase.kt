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
    @ColumnInfo(defaultValue = "''") val category: String,
    @ColumnInfo(defaultValue = "0") val owned: Boolean = false
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
    val refundWindowDays: Int? = null,
    val cancelledAt: String? = null
)

@Dao
interface CatalogDao {
    @Query("SELECT * FROM equipment ORDER BY id")
    fun equipment(): Flow<List<Equipment>>

    @Query("SELECT * FROM bookings ORDER BY id DESC")
    fun bookings(): Flow<List<Booking>>

    @Upsert
    suspend fun seed(equipment: List<Equipment>)

    @Query("SELECT COALESCE(MAX(id), 0) + 1 FROM equipment")
    suspend fun nextEquipmentId(): Int

    @Insert
    suspend fun insertEquipment(equipment: Equipment)

    @Transaction
    suspend fun addPublication(equipment: Equipment): Int {
        val id = nextEquipmentId()
        insertEquipment(equipment.copy(id = id, owned = true))
        return id
    }

    @Query("DELETE FROM equipment WHERE id = :id AND owned = 1")
    suspend fun removePublication(id: Int)

    @Insert
    suspend fun insert(booking: Booking): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seedBookings(bookings: List<Booking>)

    @Query("UPDATE bookings SET status = :next, cancelledAt = :cancelledAt WHERE id = :id AND status = :expected")
    suspend fun transitionBooking(id: Long, expected: String, next: String, cancelledAt: String?): Int

    @Query("DELETE FROM bookings WHERE id = :id")
    suspend fun remove(id: Long)
}

@Database(entities = [Equipment::class, Booking::class], version = 4, exportSchema = false)
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

val migrateAccountAndRefunds = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE equipment ADD COLUMN owned INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE bookings ADD COLUMN cancelledAt TEXT")
    }
}
