package models

case class PlayerState(
  playerName: String,
  hp: Int,
  maxHp: Int,
  gold: Int,
  level: Int,
  currentXp: Int,
  currentArea: String,
  inventory: List[String],
  equipment: Equipment
)

object PlayerState {
  def newPlayer(name: String): PlayerState = PlayerState(
    playerName = name,
    hp = 100,
    maxHp = 100,
    gold = 50,
    level = 1,
    currentXp = 1,
    currentArea = "publicZone",
    inventory = List(),
    equipment = Equipment.newEquipment
  )
}
