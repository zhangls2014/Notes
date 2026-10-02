package me.zhangls.database

// Frozen SQL fixtures from schemas/me.zhangls.database.AppDatabase/{1,2,3}.json.
// Keep these historical fixtures unchanged when current entities evolve.
internal val historicalSchemas = mapOf(
  1 to listOf(
    """CREATE TABLE IF NOT EXISTS `account` (`id` INTEGER NOT NULL, `firstName` TEXT NOT NULL, `lastName` TEXT NOT NULL, `email` TEXT NOT NULL, `altEmail` TEXT NOT NULL, `avatar` TEXT NOT NULL, PRIMARY KEY(`id`))""",
    """CREATE TABLE IF NOT EXISTS `email` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `senderId` INTEGER NOT NULL, `subject` TEXT NOT NULL, `body` TEXT NOT NULL, `isImportant` INTEGER NOT NULL, `isStarred` INTEGER NOT NULL, `mailbox` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, `parentEmailId` INTEGER)""",
    """CREATE INDEX IF NOT EXISTS `index_email_id` ON `email` (`id`)""",
    """CREATE INDEX IF NOT EXISTS `index_email_senderId` ON `email` (`senderId`)""",
    """CREATE INDEX IF NOT EXISTS `index_email_parentEmailId` ON `email` (`parentEmailId`)""",
    """CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)""",
    """INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f45b697d5c02ddc10f111ebcdd8d2569')""",
    """PRAGMA user_version = 1""",
  ),
  2 to listOf(
    """CREATE TABLE IF NOT EXISTS `account` (`id` INTEGER NOT NULL, `firstName` TEXT NOT NULL, `lastName` TEXT NOT NULL, `email` TEXT NOT NULL, `altEmail` TEXT NOT NULL, `avatar` TEXT NOT NULL, PRIMARY KEY(`id`))""",
    """CREATE TABLE IF NOT EXISTS `email` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `senderId` INTEGER NOT NULL, `recipientIds` TEXT NOT NULL DEFAULT '[]', `subject` TEXT NOT NULL, `body` TEXT NOT NULL, `isImportant` INTEGER NOT NULL, `isStarred` INTEGER NOT NULL, `mailbox` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, `parentEmailId` INTEGER)""",
    """CREATE INDEX IF NOT EXISTS `index_email_id` ON `email` (`id`)""",
    """CREATE INDEX IF NOT EXISTS `index_email_senderId` ON `email` (`senderId`)""",
    """CREATE INDEX IF NOT EXISTS `index_email_parentEmailId` ON `email` (`parentEmailId`)""",
    """CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)""",
    """INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '9c4f4367a00820b0c7ae9fa55a103439')""",
    """PRAGMA user_version = 2""",
  ),
  3 to listOf(
    """CREATE TABLE IF NOT EXISTS `account` (`id` INTEGER NOT NULL, `firstName` TEXT NOT NULL, `lastName` TEXT NOT NULL, `email` TEXT NOT NULL, `altEmail` TEXT NOT NULL, `avatar` TEXT NOT NULL, `isDefault` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))""",
    """CREATE TABLE IF NOT EXISTS `email` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `senderId` INTEGER NOT NULL, `recipientIds` TEXT NOT NULL DEFAULT '[]', `subject` TEXT NOT NULL, `body` TEXT NOT NULL, `isImportant` INTEGER NOT NULL, `isStarred` INTEGER NOT NULL, `mailbox` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, `parentEmailId` INTEGER)""",
    """CREATE INDEX IF NOT EXISTS `index_email_id` ON `email` (`id`)""",
    """CREATE INDEX IF NOT EXISTS `index_email_senderId` ON `email` (`senderId`)""",
    """CREATE INDEX IF NOT EXISTS `index_email_parentEmailId` ON `email` (`parentEmailId`)""",
    """CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)""",
    """INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'dec55581a0b7f86a8fdc84382d3b7db0')""",
    """PRAGMA user_version = 3""",
  )
)
