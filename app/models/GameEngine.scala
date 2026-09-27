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
    Enemy("Slimy Slime",                                30  , 1,  5,  10, 5, "bruhvarg.png"),
    Enemy("Some guy who says 'gosh'",                   40  , 1,  5,  10, 5, "bruhvarg.png"),
    Enemy("Goblin (some) Biscuits",                     50  , 2,  8,  20, 10, "bruhvarg.png"),
    Enemy("Dancing Skeleton",                           70  , 2, 12,  35, 10, "bruhvarg.png"),
    Enemy("Dark Wizards of the Other Platforms Realm",  100 , 3, 15,  50, 15, "bruhvarg.png"),
    Enemy("Shy-Hulud",                                  200 , 5, 25, 100, 20, "bruhvarg.png"),
    Enemy("The Real Shai-Hulud",                        500 , 7, 40, 250, 50, "bruhvarg.png")
  )

  def initPlayer(name: String): PlayerState = {
    playerState = PlayerState.newPlayer(name)
    playerState
  }

  def getPlayer: PlayerState = playerState

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

  def playerAttack(): CombatResult = {
    // if (!combatActive) return CombatResult("No combat ongoing", "", false, 0, 0)

    turnCount += 1
    val weaponDamage = playerState.equipment.weapon.map(_.damage).getOrElse(0)
          val newEnemyHp = math.max(0, currentEnemy.get.hp - weaponDamage)
    currentEnemy.foreach(e => currentEnemy = Some(currentEnemy.get.copy(hp = newEnemyHp)))

    val enemyDead = newEnemyHp <= 0
    if (enemyDead) {
      val goldReward = currentEnemy.get.goldReward
      playerState = playerState.copy(gold = playerState.gold + goldReward)
      combatActive = false
      CombatResult(s"You defeated ${currentEnemy.get.name}!", currentEnemy.get.name, true, goldReward, 0)
    } else {
        val enemyDamage = currentEnemy.get.damage
              val armorValue = playerState.equipment.armour.map(_.armour).getOrElse(0)
              val takenDamage = math.max(1, enemyDamage - armorValue)
        playerState = playerState.copy(hp = math.max(1, playerState.hp - takenDamage))
        CombatResult(s"You hit the ${currentEnemy.get.name} for $weaponDamage damage. It hits back for $takenDamage!", currentEnemy.get.name, false, 0, weaponDamage)
    }
  }

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
        case "A cuppa tea"                                       => 10
        case "Small Goods Coffee"                                => 20
        case "Single Target Rock Imbued Dense Egg (S.T.R.I.D.E)" => 10
        case "Macbook M1 Pro"                                    => 30
        case "Macbook M2 Pro"                                    => 100
        case "Macbook M3 Pro"                                    => 250
        case "Macbook M4 Pro"                                    => 500
        case "Macbook M5 Pro"                                    => 1000
        case "TechMerch"                                         => 10
        case "Smart Armor"                                       => 30
        case "Smarter Armor"                                     => 100
        case "Smarterer Armor"                                   => 250
        case "Smartererer Armor"                                 => 500
        case "Smartest Armor"                                    => 1000
        case _                                                   => 10
        }
      playerState = playerState.copy(inventory = playerState.inventory.filterNot(_ == itemName))
      (true, sellPrice)
      } else {
      (false, 0)
      }
    }

  def buyItem(itemName: String): Either[String, Boolean] = {
    if (playerState == null) return Left("No player")

    val costs = Map(
        "A cuppa tea"       -> 20,
        "Small Goods Coffee"-> 35,
        "Macbook M1 Pro"    -> 50,
        "Smart Armor"       -> 50
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
      if (playerState == null) return Left("No player?!")
      while(qty > 0) {
        playerState = playerState.copy(
          inventory = playerState.inventory :+ item.name
        )
       qty - 1
      }
      println(playerState.inventory)
    Right(true)
  }

  def restAtInn(): Either[String, Boolean] = {
    if (playerState == null) return Left("No player? How did this happen?")
    val cost = 20
    if (playerState.gold < cost) return Left("Not enough gold! Go gather, make some things and make some money!")
    playerState = playerState.copy(gold = playerState.gold - cost, hp = playerState.maxHp)
    Right(true)
  }

  def gatherResource(resourceType: String): String = {
    resourceType match {
      case "caffeine"       => val n = Random.nextInt(3) + 1; getResource(Resource.Caffeine, n); s"You got $n caffeine... things!"
      case "chips"          => val n = Random.nextInt(3) + 1; getResource(Resource.Chips, n); s"You hoarded $n chips!"
      case "documentation"  => val n = Random.nextInt(3) + 1; getResource(Resource.Documentation, n); s"You produced $n pages of documentation!"
      case _                => "You gathered nothing useful, this shouldn't happen D:"
      }
    }

  def craftItem(recipeName: String): String = {
    recipeName match {
      case "A cuppa tea"          if playerState.inventory.count(_ == "caffeine") >= 10 => "Crafted A cuppa tea!"
      case "Small Goods coffee"   if playerState.inventory.count(_ == "caffeine") >= 20 => "Crafted Small Goods Coffee!"
      case "Monster Energy Drink" if playerState.inventory.count(_ == "caffeine") >= 30 => "Crafted Monster Energy Drink!"
      case "Macbook M1 Pro"       if playerState.inventory.count(_ == "chips")     >= 10 => "Crafted Macbook M1 Pro!"
      case "Macbook M2 Pro"       if (playerState.inventory.count(_ == "chips")     >= 11 && playerState.inventory.count(_ == "Macbook M4 Pro") >= 1) => "Crafted Macbook M2 Pro, wow!"
      case "Macbook M3 Pro"       if (playerState.inventory.count(_ == "chips")     >= 12 && playerState.inventory.count(_ == "Macbook M4 Pro") >= 1) => "Crafted Macbook M3 Pro, wow!"
      case "Macbook M4 Pro"       if (playerState.inventory.count(_ == "chips")     >= 13 && playerState.inventory.count(_ == "Macbook M4 Pro") >= 1) => "Crafted Macbook M4 Pro, wow!"
      case "Macbook M5 Pro"       if (playerState.inventory.count(_ == "chips")     >= 15 && playerState.inventory.count(_ == "Macbook M4 Pro") >= 1) => "Crafted Macbook M5 Pro, WOW!!!"
      case "Smart Armour"         if playerState.inventory.count(_ == "documentation")     >= 10 => "Crafted Smart Armour"
      case "Smarter Armour"       if (playerState.inventory.count(_ == "documentation")     >= 11 && playerState.inventory.count(_ == "Smart Armour") >= 1) => "Crafted Smarter Armour"
      case "Smarterer Armour"     if (playerState.inventory.count(_ == "documentation")     >= 12 && playerState.inventory.count(_ == "Smarter Armour") >= 1) => "Crafted Smarterer Armour"
      case "Smartererer Armour"   if (playerState.inventory.count(_ == "documentation")     >= 13 && playerState.inventory.count(_ == "Smarterer Armour") >= 1) => "Crafted Smartererer Armour"
      case "Smartest Armour"      if (playerState.inventory.count(_ == "documentation")     >= 15 && playerState.inventory.count(_ == "Smartererer Armour") >= 1) => "Crafted The Smartest Armour, WOW!!!"
      case _                      => "Unknown recipe or missing materials!"
      }
    }
}

case class CombatInfo(enemy: Enemy, playerHp: Int, playerMaxHp: Int, turnCount: Int)
case class CombatResult(message: String, enemyName: String, killed: Boolean, reward: Int, damageDealt: Int)
