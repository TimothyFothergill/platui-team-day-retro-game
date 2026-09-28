package models

import javax.inject.Singleton
import scala.util.Random

@Singleton
class GameEngine {
  private var playerState: PlayerState = _
  private var currentEnemy: Option[Enemy] = None
  private var combatActive: Boolean = false
  private var turnCount: Int = 0

  val enemies: List[Enemy] = List(
    Enemy("Slimy Slime",                                30     , 1,  5,  10, 5, "slimey.png"),
    Enemy("Goshman",                                    40     , 1,  5,  10, 5, "goshman.gif"),
    Enemy("Goblin (some) Biscuits",                     50     , 2,  8,  20, 10, "goblinbiscuits.png"),
    Enemy("The Rib Tickler",                            70     , 2, 12,  35, 10, "ribtickler.gif"),
    Enemy("PEGAsus",                                    100 , 3, 15,  50, 15, "pegasus.png"),
    Enemy("Some Dragon Who Was Awoken From Its Slumber",100 , 3, 15,  50, 15, "bruhvarg.png"),
    Enemy("Shy-Hulud",                                  200 , 5, 25, 100, 20, "shy-hulud.png"),
    Enemy("The Real Shai-Hulud",                        500 , 7, 40, 250, 50, "shai-hulud.png")
     )

  def initPlayer(name: String): PlayerState = {
    playerState = PlayerState.newPlayer(name)
    playerState
     }

  def getPlayer: PlayerState = {
    val state = playerState
    if (state == null) return PlayerState.newPlayer("Unknown")
    if (state.resources.isEmpty) {
      state.copy(
        resources = Map(
              "caffeine" -> state.inventory.count(_ == Resource.Caffeine.name),
              "chips" -> state.inventory.count(_ == Resource.Chips.name),
              "documentation" -> state.inventory.count(_ == Resource.Documentation.name)
           )
          )
     } else {
      state
         }
       }

  def setCurrentArea(area: String): Unit = {
    if (playerState != null) {
      playerState = playerState.copy(currentArea = area)
          }
        }

  def startCombat(): Either[String, Enemy] = {
    if (playerState == null) return Left("No player found")
    currentEnemy = Some(enemies(Random.nextInt(enemies.size)))
    combatActive = true
    turnCount = 0
    currentEnemy.toRight("No enemy available")
          }

  def getCombatInfo(): Option[CombatInfo] = {
    if (!combatActive || currentEnemy.isEmpty) return None
    val enemy = currentEnemy.get
    Some(CombatInfo(enemy, playerState.hp, playerState.maxHp, turnCount))
      }

  def playerAttack(damageMultiplier: Double = 1.0): CombatResult = {
    turnCount += 1
    val weaponDamage = playerState.equipment.weapon.map(_.damage).getOrElse(0)
    val baseDamage = 5
    val totalDamage = (baseDamage + weaponDamage) * damageMultiplier
    val damageDealt = math.floor(totalDamage)
          val newEnemyHp = math.max(0, currentEnemy.get.hp - damageDealt.toInt)
    currentEnemy.foreach(e => currentEnemy = Some(currentEnemy.get.copy(hp = newEnemyHp)))

    val enemyDead = newEnemyHp <= 0
    if (enemyDead) {
      val goldReward = currentEnemy.get.goldReward
      playerState = playerState.copy(gold = playerState.gold + goldReward)
      combatActive = false
      CombatResult(s"You defeated ${currentEnemy.get.name}!", currentEnemy.get.name, true, goldReward, 0, newEnemyHp)
     } else {
        val enemyDamage = currentEnemy.get.damage
              val armorValue = playerState.equipment.armour.map(_.armour).getOrElse(0)
              val takenDamage = math.max(1, enemyDamage - armorValue)
        playerState = playerState.copy(hp = playerState.hp - takenDamage)
      CombatResult(s"You hit the ${currentEnemy.get.name} for $damageDealt damage. It hits back for $takenDamage!", currentEnemy.get.name, false, 0, damageDealt.toInt, newEnemyHp)
     }
      }

 def getAvailableQuestions(): List[String] = List(
       "What has gone well?",
       "What could have been better?",
       "What do we do well as a team?",
       "What could we improve? (team working, process, ceremonies etc)",
       "Gratitude: Say thanks to someone and why.",
       "What do you wish you knew more about in PlatUI, MDTP or wider?",
       "What is your favourite Slack emoji?"
     )

  def answerQuestion(questionId: String, answer: String): (String, Int) = {
    val points = Random.nextInt(20) + 10
    playerState = playerState.copy(gold = playerState.gold + points)
    combatActive = false
          (s"Correct! You earned $points gold.", points)
           }

  def endCombat(): Unit = {
    combatActive = false
    currentEnemy = None
    turnCount = 0
         }

  def sellItem(itemName: String): (Boolean, Int) = {
    val hasItem = playerState.inventory.contains(itemName)
    if (hasItem) {
      val sellPrice = itemName match {
        case "A cuppa tea"                                          => 20
        case "Small Goods Coffee"                                   => 50
        case "Single Target Rock Imbued Dense Egg (S.T.R.I.D.E)" => 10
        case "Macbook M1 Pro"                                       => 30
        case "Macbook M2 Pro"                                       => 100
        case "Macbook M3 Pro"                                       => 250
        case "Macbook M4 Pro"                                       => 500
        case "Macbook M5 Pro"                                       => 1000
        case "TechMerch"                                            => 10
        case "Smart Armor"                                          => 30
        case "Smarter Armor"                                        => 100
        case "Smarterer Armor"                                      => 250
        case "Smartererer Armor"                                    => 500
        case "Smartest Armor"                                       => 1000
        case _                                                      => 10
            }
      playerState = playerState.copy(
          inventory = playerState.inventory.filterNot(_ == itemName),
          gold = playerState.gold + sellPrice
          )
            (true, sellPrice)
            } else {
            (false, 0)
            }
          }

  def buyItem(itemName: String): Either[String, Boolean] = {
    if (playerState == null) return Left("No player")

    val costs = Map(
             "A cuppa tea"          -> 20,
             "Small Goods Coffee"-> 35,
             "Macbook M1 Pro"       -> 50,
             "Smart Armor"          -> 50
           )

    val cost = costs.get(itemName)
    if (cost.isEmpty) return Left("Item not found")

    if (playerState.gold < cost.get) return Left("Not enough gold!")

    playerState = playerState.copy(
      gold = playerState.gold - cost.get,
      inventory = playerState.inventory :+ itemName
           )
    Right(true)
          }

     def getResource(item: Resource, qty: Int): Either[String, Boolean] = {
       if (playerState == null) return Left("No player!?")
       var i = qty
       while(i > 0) {
         playerState = playerState.copy(
           inventory = playerState.inventory :+ item.name,
           resources = playerState.resources + (item.name -> (playerState.resources.getOrElse(item.name, 0) + 1))
             )
           i = i - 1
            }
       Right(true)
          }

  def restAtInn(): Either[String, Boolean] = {
    if (playerState == null) return Left("No player? How did this happen?")
    val cost = 20
    if (playerState.gold < cost) return Left("Not enough gold! Go gathering, make some things and make some money!")
    playerState = playerState.copy(gold = playerState.gold - cost, hp = playerState.maxHp)
    Right(true)
      }

  def gatherResource(resourceType: String): String = {
    resourceType match {
      case "caffeine"          => val n = Random.nextInt(3) + 1; getResource(Resource.Caffeine, n); s"You got $n caffeine... things!"
      case "chips"             => val n = Random.nextInt(3) + 1; getResource(Resource.Chips, n); s"You hoarded $n chips!"
      case "documentation"     => val n = Random.nextInt(3) + 1; getResource(Resource.Documentation, n); s"You produced $n pages of documentation!"
      case _                   => "You gathered nothing useful, this should not happen D:"
          }
        }

  def craftItem(recipeName: String): Either[String, String] = {
    if (playerState == null) return Left("No player found")

    val caffeineCount = playerState.resources.getOrElse("caffeine", 0)
    val chipsCount = playerState.resources.getOrElse("chips", 0)
    val docCount = playerState.resources.getOrElse("documentation", 0)
    val hasM4Pro = playerState.inventory.contains("Macbook M4 Pro")

    recipeName match {
      case "A cuppa tea"              if caffeineCount >= 5           => doCraft(recipeName, Map("caffeine" -> 5), None, None)
      case "Small Goods coffee"       if caffeineCount >= 10           => doCraft(recipeName, Map("caffeine" -> 10), None, None)
      case "Monster Energy Drink"     if caffeineCount >= 30           => doCraft(recipeName, Map("caffeine" -> 30), None, None)
      case "Macbook M1 Pro"           if chipsCount >= 10                => doCraft(recipeName, Map("chips" -> 10), Some(Weapon(recipeName, 2)), None)
      case "Macbook M2 Pro"           if (chipsCount >= 11)               => doCraft(recipeName, Map("chips" -> 11), Some(Weapon(recipeName, 5)), None)
      case "Macbook M3 Pro"           if (chipsCount >= 12)               => doCraft(recipeName, Map("chips" -> 12), Some(Weapon(recipeName, 9)), None)
      case "Macbook M4 Pro"           if (chipsCount >= 13)               => doCraft(recipeName, Map("chips" -> 13), Some(Weapon(recipeName, 18)), None)
      case "Macbook M5 Pro"           if (chipsCount >= 15)               => doCraft(recipeName, Map("chips" -> 15), Some(Weapon(recipeName, 40)), None)
      case "Smart Armour"             if docCount >= 10                                => doCraft(recipeName, Map("documentation" -> 10), None, Some(Armour(recipeName, 2)))
      case "Smarter Armour"           if (docCount >= 11 && playerState.inventory.contains("Smart Armour"))    => doCraft(recipeName, Map("documentation" -> 11), None, Some(Armour(recipeName, 5)))
      case "Smarterer Armour"         if (docCount >= 12 && playerState.inventory.contains("Smarter Armour"))  => doCraft(recipeName, Map("documentation" -> 12), None, Some(Armour(recipeName, 9)))
      case "Smartererer Armour"       if (docCount >= 13 && playerState.inventory.contains("Smarterer Armour"))=> doCraft(recipeName, Map("documentation" -> 13), None, Some(Armour(recipeName, 18)))
      case "Smartest Armour"          if (docCount >= 15 && playerState.inventory.contains("Smartererer Armour"))=> doCraft(recipeName, Map("documentation" -> 15), None, Some(Armour(recipeName, 40)))
      case _                              => Left("Unknown recipe or missing materials!")
        }
       }

  private def doCraft(recipeName: String, costs: Map[String, Int], maybeWeapon: Option[Weapon], maybeArmour: Option[Armour]): Either[String, String] = {
     // Remove the required resource items from inventory
    var filteredInv = playerState.inventory
    costs.foreach { case (resName, count) =>
         (0 until count).foreach { _ =>
        val idx = filteredInv.indexOf(resName)
          if (idx >= 0) filteredInv = filteredInv.take(idx) ++ filteredInv.drop(idx + 1)
             }
            }
     // Update equipment: set weapon or armour if this is a craftable item
    val newEquipment = maybeWeapon match {
      case Some(w) => playerState.equipment.copy(weapon = Some(w))
      case None => maybeArmour match {
        case Some(a) => playerState.equipment.copy(armour = Some(a))
        case None => playerState.equipment
          }
        }
    playerState = playerState.copy(
      inventory = filteredInv :+ recipeName,
      equipment = newEquipment,
      resources = costs.foldLeft(playerState.resources) { (res, kv) =>
         res + (kv._1 -> (res.getOrElse(kv._1, 0) - kv._2))
            }
         )
     // Build the success message with stat boosts for equipment
    val weaponMsg = maybeWeapon.map(w => s" Your base damage is now ${w.damage}!").getOrElse("")
    val armourMsg = maybeArmour.map(a => s" Your armour value is now ${a.armour}!").getOrElse("")
     Right(s"Crafted $recipeName!$weaponMsg$armourMsg")
      }
}

case class CombatInfo(enemy: Enemy, playerHp: Int, playerMaxHp: Int, turnCount: Int)
case class CombatResult(message: String, enemyName: String, killed: Boolean, reward: Int, damageDealt: Int, currentEnemyHp: Int)
