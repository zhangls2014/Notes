package me.zhangls.entry.data.sample

import me.zhangls.data.model.AccountModel

object LocalAccountsDataProvider {
  val allUserAccounts = listOf(
    AccountModel(
      id = 1L,
      firstName = "Ada",
      lastName = "Lovelace",
      email = "ada@example.com",
      altEmail = "ada@example.net",
      avatar = "avatar_10",
      isDefault = true,
    ),
    AccountModel(
      id = 2L,
      firstName = "Jeff",
      lastName = "H",
      email = "jeffersonloveshiking@example.com",
      altEmail = "jeffersonloveshiking@example.net",
      avatar = "avatar_2",
    ),
    AccountModel(
      id = 3L,
      firstName = "Jeff",
      lastName = "Hansen",
      email = "jeffersonc@example.com",
      altEmail = "jeffersonc@example.net",
      avatar = "avatar_9",
    ),
  )

  private val allUserContactAccounts = listOf(
    AccountModel(
      id = 4L,
      firstName = "Tracy",
      lastName = "Alvarez",
      email = "tracealvie@example.com",
      altEmail = "tracealvie@example.net",
      avatar = "avatar_1",
    ),
    AccountModel(
      id = 5L,
      firstName = "Allison",
      lastName = "Trabucco",
      email = "atrabucco222@example.com",
      altEmail = "atrabucco222@example.net",
      avatar = "avatar_3",
    ),
    AccountModel(
      id = 6L,
      firstName = "Ali",
      lastName = "Connors",
      email = "aliconnors@example.com",
      altEmail = "aliconnors@example.net",
      avatar = "avatar_5",
    ),
    AccountModel(
      id = 7L,
      firstName = "Alberto",
      lastName = "Williams",
      email = "albertowilliams124@example.com",
      altEmail = "albertowilliams124@example.net",
      avatar = "avatar_0",
    ),
    AccountModel(
      id = 8L,
      firstName = "Kim",
      lastName = "Alen",
      email = "alen13@example.com",
      altEmail = "alen13@example.net",
      avatar = "avatar_7",
    ),
    AccountModel(
      id = 9L,
      firstName = "Google",
      lastName = "Express",
      email = "express@example.com",
      altEmail = "express@example.net",
      avatar = "avatar_express",
    ),
    AccountModel(
      id = 10L,
      firstName = "Sandra",
      lastName = "Adams",
      email = "sandraadams@example.com",
      altEmail = "sandraadams@example.net",
      avatar = "avatar_2",
    ),
    AccountModel(
      id = 11L,
      firstName = "Trevor",
      lastName = "Hansen",
      email = "trevorhandsen@example.com",
      altEmail = "trevorhandsen@example.net",
      avatar = "avatar_8",
    ),
    AccountModel(
      id = 12L,
      firstName = "Sean",
      lastName = "Holt",
      email = "sholt@example.com",
      altEmail = "sholt@example.net",
      avatar = "avatar_6",
    ),
    AccountModel(
      id = 13L,
      firstName = "Frank",
      lastName = "Hawkins",
      email = "fhawkank@example.com",
      altEmail = "fhawkank@example.net",
      avatar = "avatar_4",
    ),
  )

  fun getDefaultUserAccount() = allUserAccounts.first()

  fun getContactAccountByUid(accountId: Long): AccountModel = allUserContactAccounts.first { it.id == accountId }
}
