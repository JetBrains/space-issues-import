package com.jetbrains.space.import.common

sealed interface ExternalProjectProperty {
    class Id(val id: String) : ExternalProjectProperty
    class Name(val name: String) : ExternalProjectProperty
}

enum class ProjectPropertyType {
    Id, Name, Email
}

val defaultProjectPropertyType = ProjectPropertyType.Name
