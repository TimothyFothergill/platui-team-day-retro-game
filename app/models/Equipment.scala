package models

import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._

case class Equipment(
  weapon: Option[Weapon] = None,
  armour: Option[Armour] = None
)

object Equipment {
  implicit val equipmentFormat: OFormat[Equipment] = Json.format[Equipment]

  def newEquipment: Equipment = Equipment(
    weapon = Some(Weapon(
      name = "Single Target Rock Imbued Dense Egg (S.T.R.I.D.E)",
      damage = 1
    )),
    armour = Some(Armour(
      name = "Tech Merch",
      armour = 1
    ))
  )
}

case class Weapon(
  name: String,
  damage: Int
)

object Weapon {
  implicit val weaponFormat: OFormat[Weapon] = Json.format[Weapon]
}

case class Armour(
  name: String,
  armour: Int
)

object Armour {
  implicit val armourFormat: OFormat[Armour] = Json.format[Armour]
}
