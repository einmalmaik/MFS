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

  /**
   * Die Erzählung steht, aber der Spielstand konnte nicht fortgeschrieben werden.
   *
   * Kein Fehlerzustand: Der Text ist da und der alte Checkpoint unbeschädigt. Nur Ort, Zeit,
   * Inventar, Verletzungen und Erinnerungen dieses Zuges fehlen — und das muss der Spieler
   * erfahren, sonst wundert er sich später über eine Welt, die stehen geblieben ist.
   */
  data object StateFrozen : TurnProgress()

  /** An error occurred during generation or processing. */
  data class Failed(val error: String) : TurnProgress()
}
