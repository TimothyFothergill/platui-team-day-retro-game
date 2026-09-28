package models

case class PlayerState(
  playerName: String,
  hp: Int,
  maxHp: Int,
  gold: Int,
  currentXp: Int,
  currentArea: String,
  inventory: List[String],
  equipment: Equipment,
  resources: Map[String, Int]
) {
  def withResources(resources: Map[String, Int]): PlayerState = this.copy(resources = resources)
}

object PlayerState {
  def newPlayer(name: String): PlayerState = PlayerState(
    playerName = name,
    hp = 100,
    maxHp = 100,
    gold = 50,
    currentXp = 0,
    currentArea = "home",
    inventory = Nil,
    equipment = Equipment.newEquipment,
    resources = Map(
      "caffeine" -> 0,
      "chips" -> 0,
      "documentation" -> 0
    )
  )
}
