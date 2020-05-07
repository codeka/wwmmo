package au.com.codeka.warworlds.planetrender

class TemplateException : Exception {
  constructor() {}
  constructor(cause: Throwable?) : super(cause) {}
  constructor(message: String?) : super(message) {}
  constructor(message: String?, cause: Throwable?) : super(message, cause) {}

  companion object {
    private const val serialVersionUID = 1L
  }
}