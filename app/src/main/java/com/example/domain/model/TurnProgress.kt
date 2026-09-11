package com.example.domain.model

/**
 * Represents the lifecycle stages of a single story turn.
 * Emitted by [StoryTurnEngine] during generation and processing.
 */
sealed class TurnProgress {
  /** The model is deliberating / reasoning before outputting tokens. */
  data object Thinking : TurnProgress()

  /** Tokens are actively streaming to the user interface. */
  data object Streaming : TurnProgress()

  /** Background extraction of world state, inventory, NPCs, and milestones. */
  data object ExtractingState : TurnProgress()

  /** Turn is fully processed and persisted to the local database. */
  data object Completed : TurnProgress()

  /** An error occurred during generation or processing. */
  data class Failed(val error: String) : TurnProgress()
}
