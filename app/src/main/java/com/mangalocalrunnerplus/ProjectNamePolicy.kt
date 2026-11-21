package com.mangalocalrunnerplus

object ProjectNamePolicy {
    private val allowedName = Regex("[A-Za-z0-9][A-Za-z0-9 _.-]{0,49}")

    fun validate(rawName: String): String {
        val name = rawName.trim()
        require(name.isNotEmpty()) { "Project name cannot be empty." }
        require(name.length <= 50) { "Project name must be 50 characters or fewer." }
        require(name != "." && name != ".." && allowedName.matches(name)) {
            "Use a name starting with a letter or number; only letters, numbers, spaces, dot, underscore and hyphen are allowed."
        }
        return name
    }
}
