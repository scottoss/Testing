package chat.stoat.persistence

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import chat.stoat.DawnChatApplication

object SqlStorage {
    val driver: SqlDriver = AndroidSqliteDriver(
        Database.Schema,
        DawnChatApplication.instance.applicationContext,
        "revolt.db"
    )
}