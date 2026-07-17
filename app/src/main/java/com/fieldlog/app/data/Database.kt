package com.fieldlog.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Job::class, TimeEntry::class, Expense::class],
    version = 1,
    exportSchema = true
)
abstract class FieldLogDatabase : RoomDatabase() {

    abstract fun jobDao(): JobDao
    abstract fun timeEntryDao(): TimeEntryDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var instance: FieldLogDatabase? = null

        /**
         * One database for the whole app, created the first time it's asked for.
         * The file lives in the app's private storage on the phone — it is never
         * uploaded anywhere, and it works with the phone in airplane mode.
         */
        fun get(context: Context): FieldLogDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FieldLogDatabase::class.java,
                    "fieldlog.db"
                ).build().also { instance = it }
            }
    }
}
