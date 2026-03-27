package me.zhangls.entry.data.local

import me.zhangls.data.model.AccountModel


object LocalAccountsDataProvider {
  val allUserAccounts = listOf(
    AccountModel(
      id = 1L,
      firstName = "Jeff",
      lastName = "Hansen",
      email = "hikingfan@gmail.com",
      altEmail = "hkngfan@outside.com",
      avatar = "avatar_10",
    ),
    AccountModel(
      id = 2L,
      firstName = "Jeff",
      lastName = "H",
      email = "jeffersonloveshiking@gmail.com",
      altEmail = "jeffersonloveshiking@work.com",
      avatar = "avatar_2",
    ),
    AccountModel(
      id = 3L,
      firstName = "Jeff",
      lastName = "Hansen",
      email = "jeffersonc@google.com",
      altEmail = "jeffersonc@gmail.com",
      avatar = "avatar_9",
    ),
  )

  private val allUserContactAccounts = listOf(
    AccountModel(
      id = 4L,
      firstName = "Tracy",
      lastName = "Alvarez",
      email = "tracealvie@gmail.com",
      altEmail = "tracealvie@gravity.com",
      avatar = "avatar_1",
    ),
    AccountModel(
      id = 5L,
      firstName = "Allison",
      lastName = "Trabucco",
      email = "atrabucco222@gmail.com",
      altEmail = "atrabucco222@work.com",
      avatar = "avatar_3",
    ),
    AccountModel(
      id = 6L,
      firstName = "Ali",
      lastName = "Connors",
      email = "aliconnors@gmail.com",
      altEmail = "aliconnors@android.com",
      avatar = "avatar_5",
    ),
    AccountModel(
      id = 7L,
      firstName = "Alberto",
      lastName = "Williams",
      email = "albertowilliams124@gmail.com",
      altEmail = "albertowilliams124@chromeos.com",
      avatar = "avatar_0",
    ),
    AccountModel(
      id = 8L,
      firstName = "Kim",
      lastName = "Alen",
      email = "alen13@gmail.com",
      altEmail = "alen13@mountainview.gov",
      avatar = "avatar_7",
    ),
    AccountModel(
      id = 9L,
      firstName = "Google",
      lastName = "Express",
      email = "express@google.com",
      altEmail = "express@gmail.com",
      avatar = "avatar_express",
    ),
    AccountModel(
      id = 10L,
      firstName = "Sandra",
      lastName = "Adams",
      email = "sandraadams@gmail.com",
      altEmail = "sandraadams@textera.com",
      avatar = "avatar_2",
    ),
    AccountModel(
      id = 11L,
      firstName = "Trevor",
      lastName = "Hansen",
      email = "trevorhandsen@gmail.com",
      altEmail = "trevorhandsen@express.com",
      avatar = "avatar_8",
    ),
    AccountModel(
      id = 12L,
      firstName = "Sean",
      lastName = "Holt",
      email = "sholt@gmail.com",
      altEmail = "sholt@art.com",
      avatar = "avatar_6",
    ),
    AccountModel(
      id = 13L,
      firstName = "Frank",
      lastName = "Hawkins",
      email = "fhawkank@gmail.com",
      altEmail = "fhawkank@thisisme.com",
      avatar = "avatar_4",
    ),
  )

  fun getDefaultUserAccount() = allUserAccounts.first()

  fun getContactAccountByUid(accountId: Long): AccountModel {
    return allUserContactAccounts.first { it.id == accountId }
  }
}
