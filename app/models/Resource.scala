package models

sealed trait Resource {
  def name: String
}
object Resource {
  case object Caffeine extends Resource {
    override val name = "caffeine"
  }
  case object Chips extends Resource {
    override val name = "chips"
  }
  case object Documentation extends Resource {
    override val name = "documentation"
  }
}
