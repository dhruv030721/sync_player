package com.meetflix.party.domain.models

/**
 * Design Pattern: State / StateMachine
 * Represents distinct UI workflow modes for Party Mode screen.
 */
enum class PartyScreenMode {
    IDLE,
    HOSTING,
    DISCOVERING,
    JOINING,
    WATCHING
}
