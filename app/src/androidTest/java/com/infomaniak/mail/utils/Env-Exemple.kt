/*
 * Infomaniak kDrive - Android
 * Copyright (C) 2022 Infomaniak Network SA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.infomaniak.mail.utils

/**
 * File to be copied and completed, it is also necessary to remove the -Example in the file name, so that the tests can work
 */
object `Env-Exemple` {
    /**
     * Specify here, if you want to use the account of the user already logged in the application
     * true to enable it, otherwise use the [TOKEN] field
     */
    const val USE_CURRENT_USER = false

    /**
     * Set it, if [USE_CURRENT_USER] is false
     */
    const val TOKEN = ""

    /**
     * The drive used for the tests
     * 410457 for simple empty drive without collaboration (must be logged with user below)
     * 140946 for infomaniak dev test drive
     */
    const val DRIVE_ID = 140946

    /**
     * User name used for share file invitation
     */
    const val INVITE_USER_NAME = "invite@infomaniak.com"

    /**
     * User id used for the addUser test (for assertion)
     */
    const val NEW_USER_ID = -1

    /**
     * User name used to connect on for addUser test (Disable 2FA !)
     */
    const val NEW_USER_NAME = "test@infomaniak.com"

    /**
     * User password used to connect on for addUser test
     */
    const val NEW_USER_PASSWORD = ""
}
