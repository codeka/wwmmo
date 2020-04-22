package au.com.codeka.warworlds.client.ui

import androidx.transition.*

/**
 * Helper class for creating transitions between fragments.
 */
object Transitions {
  fun transform(): Transform {
    return Transform()
  }

  fun fade(): Fade {
    return Fade()
  }

  class Transform : TransitionSet() {
    private fun init() {
      ordering = ORDERING_TOGETHER
      addTransition(ChangeBounds())
          .addTransition(ChangeTransform())
          .addTransition(ChangeImageTransform())
    }

    init {
      init()
    }
  }
}