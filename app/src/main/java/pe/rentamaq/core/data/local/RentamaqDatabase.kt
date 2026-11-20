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
    @ColumnInfo(defaultValue = "0") val owned: Boolean = false,
    val photos: String? = null,
    val blockedFrom: String? = null,
    val blockedUntil: String? = null,
    val serviceDate: String? = null,
    val serviceHours: Int? = null,
    val serviceNotes: String? = null,
    val serviceProvider: String? = null,
    val nextServiceDate: String? = null,
    val serviceDocument: String? = null,
    @ColumnInfo(defaultValue = "'catalog-provider'") val ownerId: String = "catalog-provider"
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
    val cancelledAt: String? = null,
    val equipmentId: Int? = null,
    val refundState: String? = null,
    @ColumnInfo(defaultValue = "'initial'") val customerId: String = "initial",
    @ColumnInfo(defaultValue = "'catalog-provider'") val providerId: String = "catalog-provider"
)

@Entity(tableName = "equipment_blocks", foreignKeys = [
    ForeignKey(entity = Equipment::class, parentColumns = ["id"], childColumns = ["equipmentId"], onDelete = ForeignKey.CASCADE)
], indices = [Index("equipmentId")])
data class EquipmentBlock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val equipmentId: Int,
    val firstDay: String,
    val lastDay: String
)

@Entity(tableName = "service_records", foreignKeys = [
    ForeignKey(entity = Equipment::class, parentColumns = ["id"], childColumns = ["equipmentId"], onDelete = ForeignKey.CASCADE)
], indices = [Index("equipmentId")])
data class ServiceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val equipmentId: Int,
    val date: String,
    val hours: Int,
    val notes: String,
    val responsible: String?,
    val nextReview: String?,
    val document: String?
)

@Entity(tableName = "booking_messages")
data class BookingMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookingId: Long,
    val text: String,
    val fromProvider: Boolean
)

@Dao
interface CatalogDao {
    @Query("SELECT * FROM equipment ORDER BY id")
    fun equipment(): Flow<List<Equipment>>

    @Query("SELECT * FROM bookings ORDER BY id DESC")
    fun bookings(): Flow<List<Booking>>

    @Query("SELECT * FROM bookings")
    suspend fun bookingSnapshot(): List<Booking>

    @Query("SELECT * FROM equipment WHERE id = :id")
    suspend fun equipmentById(id: Int): Equipment?

    @Query("SELECT * FROM equipment_blocks ORDER BY firstDay")
    fun blocks(): Flow<List<EquipmentBlock>>

    @Query("SELECT * FROM equipment_blocks WHERE equipmentId = :id")
    suspend fun blockSnapshot(id: Int): List<EquipmentBlock>

    @Insert
    suspend fun insertBlock(block: EquipmentBlock)

    @Delete
    suspend fun deleteBlock(block: EquipmentBlock): Int

    @Query("SELECT * FROM service_records ORDER BY date DESC, id DESC")
    fun services(): Flow<List<ServiceRecord>>

    @Query("SELECT * FROM service_records WHERE id = :id")
    suspend fun serviceById(id: Long): ServiceRecord?

    @Upsert
    suspend fun saveService(record: ServiceRecord)

    @Query("SELECT * FROM booking_messages WHERE bookingId = :bookingId ORDER BY id")
    fun messages(bookingId: Long): Flow<List<BookingMessage>>

    @Insert
    suspend fun addMessage(message: BookingMessage)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seed(equipment: List<Equipment>)

    @Query("UPDATE equipment SET category = :category, zone = :zone WHERE id = :id AND category = ''")
    suspend fun completeLegacyEquipment(id: Int, category: String, zone: String)

    @Query("SELECT COALESCE(MAX(id), 0) + 1 FROM equipment")
    suspend fun nextEquipmentId(): Int

    @Insert
    suspend fun insertEquipment(equipment: Equipment)

    @Update
    suspend fun updateEquipment(equipment: Equipment): Int

    @Transaction
    suspend fun addPublication(equipment: Equipment): Int {
        val id = nextEquipmentId()
        insertEquipment(equipment.copy(id = id, owned = true))
        return id
    }

    @Query("DELETE FROM equipment WHERE id = :id AND ownerId = :ownerId AND NOT EXISTS (SELECT 1 FROM bookings WHERE equipmentId = :id)")
    suspend fun removePublication(id: Int, ownerId: String)

    @Insert
    suspend fun insert(booking: Booking): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seedBookings(bookings: List<Booking>)

    @Query("UPDATE bookings SET status = :next, cancelledAt = :cancelledAt, refundState = :refund WHERE id = :id AND status = :expected")
    suspend fun transitionBooking(id: Long, expected: String, next: String, cancelledAt: String?, refund: String?): Int

    @Query("UPDATE bookings SET paidAmount = total, cancellationFee = MIN(40, total), refundWindowDays = 5 WHERE id = :id AND paidAmount IS NULL AND status = 'CONFIRMED'")
    suspend fun pay(id: Long): Int

    @Query("UPDATE bookings SET refundState = :next WHERE id = :id AND status = 'CANCELLED' AND refundState = :expected")
    suspend fun advanceRefund(id: Long, expected: String, next: String): Int

    @Query("DELETE FROM bookings WHERE id = :id")
    suspend fun remove(id: Long)

    @Query("DELETE FROM booking_messages WHERE bookingId = :id")
    suspend fun removeMessages(id: Long)
}

@Database(entities = [Equipment::class, Booking::class, BookingMessage::class, EquipmentBlock::class, ServiceRecord::class],
    version = 7, exportSchema = false)
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

val migrateLocalWorkflows = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE equipment ADD COLUMN photos TEXT")
        db.execSQL("ALTER TABLE equipment ADD COLUMN blockedFrom TEXT")
        db.execSQL("ALTER TABLE equipment ADD COLUMN blockedUntil TEXT")
        db.execSQL("ALTER TABLE equipment ADD COLUMN serviceDate TEXT")
        db.execSQL("ALTER TABLE equipment ADD COLUMN serviceHours INTEGER")
        db.execSQL("ALTER TABLE equipment ADD COLUMN serviceNotes TEXT")
        db.execSQL("ALTER TABLE bookings ADD COLUMN equipmentId INTEGER")
        db.execSQL("ALTER TABLE bookings ADD COLUMN refundState TEXT")
        db.execSQL("UPDATE bookings SET refundState = 'REQUESTED' WHERE status = 'CANCELLED' AND paidAmount > COALESCE(cancellationFee, 0)")
        db.execSQL("CREATE TABLE IF NOT EXISTS booking_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, bookingId INTEGER NOT NULL, text TEXT NOT NULL, fromProvider INTEGER NOT NULL)")
    }
}

val migrateServiceDocuments = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE equipment ADD COLUMN serviceProvider TEXT")
        db.execSQL("ALTER TABLE equipment ADD COLUMN nextServiceDate TEXT")
        db.execSQL("ALTER TABLE equipment ADD COLUMN serviceDocument TEXT")
    }
}

val migrateMarketplaceAccounts = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE equipment ADD COLUMN ownerId TEXT NOT NULL DEFAULT 'catalog-provider'")
        db.execSQL("UPDATE equipment SET ownerId = 'initial' WHERE owned = 1")
        db.execSQL("ALTER TABLE bookings ADD COLUMN customerId TEXT NOT NULL DEFAULT 'initial'")
        db.execSQL("ALTER TABLE bookings ADD COLUMN providerId TEXT NOT NULL DEFAULT 'catalog-provider'")
        db.execSQL("UPDATE bookings SET equipmentId = (SELECT MIN(e.id) FROM equipment e WHERE e.name = bookings.equipmentName) WHERE equipmentId IS NULL")
        db.execSQL("UPDATE bookings SET providerId = COALESCE((SELECT ownerId FROM equipment WHERE id = bookings.equipmentId), 'catalog-provider')")
        db.execSQL("CREATE TABLE equipment_blocks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, equipmentId INTEGER NOT NULL, firstDay TEXT NOT NULL, lastDay TEXT NOT NULL, FOREIGN KEY(equipmentId) REFERENCES equipment(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX index_equipment_blocks_equipmentId ON equipment_blocks(equipmentId)")
        db.execSQL("INSERT INTO equipment_blocks(equipmentId, firstDay, lastDay) SELECT id, blockedFrom, blockedUntil FROM equipment WHERE blockedFrom IS NOT NULL AND blockedUntil IS NOT NULL")
        db.execSQL("CREATE TABLE service_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, equipmentId INTEGER NOT NULL, date TEXT NOT NULL, hours INTEGER NOT NULL, notes TEXT NOT NULL, responsible TEXT, nextReview TEXT, document TEXT, FOREIGN KEY(equipmentId) REFERENCES equipment(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX index_service_records_equipmentId ON service_records(equipmentId)")
        db.execSQL("INSERT INTO service_records(equipmentId, date, hours, notes, responsible, nextReview, document) SELECT id, serviceDate, serviceHours, serviceNotes, serviceProvider, nextServiceDate, serviceDocument FROM equipment WHERE serviceDate IS NOT NULL AND serviceHours IS NOT NULL AND serviceNotes IS NOT NULL")
    }
}
