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
    Enemy("Slimy Slime",              30  ,  5,  10),
    Enemy("Goblin the Biscuits",      50  ,  8,  20),
    Enemy("Dancing Skeleton",         70  , 12,  35),
    Enemy("Dark Wizards of ",         100 , 15,  50),
    Enemy("Shy-Hulud",                200 , 25, 100),
    Enemy("The Real Shai-Hulud",      500 , 40, 250)
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

  def playerAttack(damage: Int): CombatResult = {
    if (!combatActive) return CombatResult("No combat ongoing", "", false, 0, 0)

    turnCount += 1
    val newEnemyHp = math.max(0, currentEnemy.get.hp - damage)
    currentEnemy.foreach(e => currentEnemy = Some(currentEnemy.get.copy(hp = newEnemyHp)))

    val enemyDead = newEnemyHp <= 0
    if (enemyDead) {
      val goldReward = currentEnemy.get.goldReward
      playerState = playerState.copy(gold = playerState.gold + goldReward)
      combatActive = false
      CombatResult(s"You defeated ${currentEnemy.get.name}!", currentEnemy.get.name, true, goldReward, 0)
      } else {
      val takenDamage = currentEnemy.get.damage
      playerState = playerState.copy(hp = math.max(1, playerState.hp - takenDamage))
      CombatResult(s"You hit the ${currentEnemy.get.name} for $damage damage. It hits back for $takenDamage!", currentEnemy.get.name, false, 0, damage)
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
        case "Health Potion" => 5
        case "Super Potion"   => 30
        case "Attack Scroll" => 20
        case _                 => 10
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
        "Health Potion"     -> 10,
        "Super Potion"      -> 25,
        "Attack Scroll"     -> 20,
        "Iron Sword"        -> 50,
        "Steel Armor"       -> 75
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

  def restAtInn(): Either[String, Boolean] = {
    if (playerState == null) return Left("No player? How did this happen?")
    val cost = 20
    if (playerState.gold < cost) return Left("Not enough gold! Go dig and make some money!")
    playerState = playerState.copy(gold = playerState.gold - cost, hp = playerState.maxHp)
    Right(true)
    }

  def gatherResource(resourceType: String): String = {
    resourceType match {
      case "caffeine" => val n = Random.nextInt(2) + 1; s"You gathered $n herbs!"
      case "chips"    => val n = Random.nextInt(2) + 1; s"You found $n chips!"
      case "documentation"    => val n = Random.nextInt(1) + 1; s"You chopped $n tech conference !"
      case _         => "You gathered nothing useful."
      }
    }

  def craftItem(recipeName: String): String = {
    recipeName match {
      case "A cuppa tea"          if playerState.inventory.count(_ == "Caffeine") >= 10 => "Crafted Health Potion!"
      case "Small Goods coffee"   if playerState.inventory.count(_ == "Caffeine") >= 20 => "Crafted Super Potion!"
      case "Macbook M1 Pro"       if playerState.inventory.count(_ == "Chips")     >= 10 => "Crafted Macbook M1 Pro!"
      case "Macbook M2 Pro"       if (playerState.inventory.count(_ == "Chips")     >= 11 && playerState.inventory.count(_ == "Macbook M4 Pro") >= 1) => "Crafted Macbook M2 Pro, wow!"
      case "Macbook M3 Pro"       if (playerState.inventory.count(_ == "Chips")     >= 12 && playerState.inventory.count(_ == "Macbook M4 Pro") >= 1) => "Crafted Macbook M3 Pro, wow!"
      case "Macbook M4 Pro"       if (playerState.inventory.count(_ == "Chips")     >= 13 && playerState.inventory.count(_ == "Macbook M4 Pro") >= 1) => "Crafted Macbook M4 Pro, wow!"
      case "Macbook M5 Pro"       if (playerState.inventory.count(_ == "Chips")     >= 15 && playerState.inventory.count(_ == "Macbook M4 Pro") >= 1) => "Crafted Macbook M5 Pro, WOW!!!"
      case "Smart Armour"         if playerState.inventory.count(_ == "Documentation")     >= 10 => "Crafted Smart Armour"
      case "Smarter Armour"       if (playerState.inventory.count(_ == "Documentation")     >= 11 && playerState.inventory.count(_ == "Smart Armour") >= 1) => "Crafted Smarter Armour"
      case "Smarterer Armour"     if (playerState.inventory.count(_ == "Documentation")     >= 12 && playerState.inventory.count(_ == "Smarter Armour") >= 1) => "Crafted Smarterer Armour"
      case "Smartererer Armour"   if (playerState.inventory.count(_ == "Documentation")     >= 13 && playerState.inventory.count(_ == "Smarterer Armour") >= 1) => "Crafted Smartererer Armour"
      case "Smartest Armour"      if (playerState.inventory.count(_ == "Documentation")     >= 15 && playerState.inventory.count(_ == "Smartererer Armour") >= 1) => "Crafted The Smartest Armour, WOW!!!"
      case _                   => "Unknown recipe or missing materials!"
      }
    }
}

case class Enemy(name: String, hp: Int, damage: Int, goldReward: Int)
case class CombatInfo(enemy: Enemy, playerHp: Int, playerMaxHp: Int, turnCount: Int)
case class CombatResult(message: String, enemyName: String, killed: Boolean, reward: Int, damageDealt: Int)
