package com.jetbrains.space.import.space

sealed interface SpaceBoardCustomIdentifier {
    class Id(val id: String) : SpaceBoardCustomIdentifier
    class Name(val name: String) : SpaceBoardCustomIdentifier
}