package dev.anhquocs.truelab.core.data.local.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteTransactionListener
import android.os.CancellationSignal
import android.util.Pair
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import java.util.Locale

internal data class TableSchema(
    val name: String,
    val columns: MutableMap<String, String> = mutableMapOf(),
    val foreignKeys: MutableList<String> = mutableListOf()
)

internal data class IndexSchema(
    val name: String,
    val tableName: String,
    val columns: List<String>,
    val isUnique: Boolean
)

internal class TestSupportSQLiteDatabase : SupportSQLiteDatabase {
    val executedSqls = mutableListOf<String>()
    val tables = mutableMapOf<String, TableSchema>()
    val indexes = mutableMapOf<String, IndexSchema>()
    val rows = mutableMapOf<String, MutableList<MutableMap<String, Any?>>>()

    override fun execSQL(sql: String) {
        executedSqls.add(sql)
        val trimmed = sql.trim()
        val upper = trimmed.uppercase()

        when {
            upper.startsWith("CREATE TABLE") -> parseCreateTable(trimmed)
            upper.startsWith("CREATE INDEX") || upper.startsWith("CREATE UNIQUE INDEX") -> parseCreateIndex(trimmed)
            upper.startsWith("ALTER TABLE") -> parseAlterTable(trimmed)
        }
    }

    override fun execSQL(sql: String, bindArgs: Array<out Any?>) = execSQL(sql)

    private fun parseCreateTable(sql: String) {
        val cleanSql = sql.replace("`", "")
        val match = Regex("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)\\s*\\((.*)\\)", RegexOption.DOT_MATCHES_ALL).find(cleanSql)
        if (match != null) {
            val tableName = match.groupValues[1].trim()
            val body = match.groupValues[2].trim()
            val schema = TableSchema(name = tableName)
            val lines = body.split(",\n", ",\r\n", ",").map { it.trim() }
            for (line in lines) {
                val upperLine = line.uppercase()
                if (upperLine.startsWith("FOREIGN KEY")) {
                    schema.foreignKeys.add(line)
                } else if (!upperLine.startsWith("PRIMARY KEY") && line.isNotBlank()) {
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        schema.columns[parts[0].trim()] = parts[1].trim()
                    }
                }
            }
            tables[tableName] = schema
            rows.getOrPut(tableName) { mutableListOf() }
        }
    }

    private fun parseCreateIndex(sql: String) {
        val isUnique = sql.uppercase().contains("UNIQUE")
        val cleanSql = sql.replace("`", "")
        val match = Regex("CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)\\s+ON\\s+(\\w+)\\s*\\(([^)]+)\\)").find(cleanSql)
        if (match != null) {
            val indexName = match.groupValues[1].trim()
            val tableName = match.groupValues[2].trim()
            val cols = match.groupValues[3].split(",").map { it.trim() }
            indexes[indexName] = IndexSchema(indexName, tableName, cols, isUnique)
        }
    }

    private fun parseAlterTable(sql: String) {
        val cleanSql = sql.replace("`", "")
        val match = Regex("ALTER\\s+TABLE\\s+(\\w+)\\s+ADD\\s+COLUMN\\s+(\\w+)\\s+([^\\s,]+)(.*)", RegexOption.IGNORE_CASE).find(cleanSql)
        if (match != null) {
            val tableName = match.groupValues[1].trim()
            val colName = match.groupValues[2].trim()
            val colType = match.groupValues[3].trim()
            val extra = match.groupValues[4].trim()
            val table = tables[tableName]
            if (table != null) {
                table.columns[colName] = colType
                if (extra.contains("REFERENCES", ignoreCase = true)) {
                    table.foreignKeys.add("FOREIGN KEY($colName) $extra")
                }
                rows[tableName]?.forEach { row ->
                    if (!row.containsKey(colName)) row[colName] = null
                }
            }
        }
    }

    fun insertRow(tableName: String, data: Map<String, Any?>) {
        rows.getOrPut(tableName) { mutableListOf() }.add(data.toMutableMap())
    }

    override fun beginTransaction() {}
    override fun beginTransactionNonExclusive() {}
    override fun beginTransactionWithListener(transactionListener: SQLiteTransactionListener) {}
    override fun beginTransactionWithListenerNonExclusive(transactionListener: SQLiteTransactionListener) {}
    override fun endTransaction() {}
    override fun setTransactionSuccessful() {}
    override fun inTransaction(): Boolean = false
    override val isDbLockedByCurrentThread: Boolean get() = false
    override fun yieldIfContendedSafely(): Boolean = false
    override fun yieldIfContendedSafely(sleepAfterYieldDelayMillis: Long): Boolean = false
    override var version: Int = 2
    override val maximumSize: Long get() = 0L
    override fun setMaximumSize(numBytes: Long): Long = 0L
    override var pageSize: Long get() = 0L; set(value) {}
    override fun compileStatement(sql: String): SupportSQLiteStatement = throw UnsupportedOperationException()
    override fun query(query: String): Cursor = throw UnsupportedOperationException()
    override fun query(query: String, bindArgs: Array<out Any?>): Cursor = throw UnsupportedOperationException()
    override fun query(query: SupportSQLiteQuery): Cursor = throw UnsupportedOperationException()
    override fun query(query: SupportSQLiteQuery, cancellationSignal: CancellationSignal?): Cursor = throw UnsupportedOperationException()
    override fun insert(table: String, conflictAlgorithm: Int, values: ContentValues): Long = 0L
    override fun delete(table: String, whereClause: String?, whereArgs: Array<out Any?>?): Int = 0
    override fun update(table: String, conflictAlgorithm: Int, values: ContentValues, whereClause: String?, whereArgs: Array<out Any?>?): Int = 0
    override fun needUpgrade(newVersion: Int): Boolean = false
    override val path: String? get() = ":memory:"
    override val isReadOnly: Boolean get() = false
    override val isOpen: Boolean get() = true
    override fun close() {}
    override fun disableWriteAheadLogging() {}
    override fun enableWriteAheadLogging(): Boolean = false
    override val isWriteAheadLoggingEnabled: Boolean get() = false
    override val attachedDbs: List<Pair<String, String>>? get() = emptyList()
    override val isDatabaseIntegrityOk: Boolean get() = true
    override fun setForeignKeyConstraintsEnabled(enabled: Boolean) {}
    override fun setLocale(locale: Locale) {}
    override fun setMaxSqlCacheSize(cacheSize: Int) {}
}
