package controllers

import javax.inject._
import play.mvc._
import play.api.mvc._
import play.twirl.api.Html
import play.api.libs.json.{Json, JsObject}
import models.GameEngine
import scala.concurrent.{Await, ExecutionContext}
import java.lang.ProcessBuilder.Redirect
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import models.PlayerState

import scala.language.postfixOps
import play.api.mvc.{Cookie, Request, Result}

import play.api.Logging

class MainController @Inject()(
  cc: MessagesControllerComponents,
  gameEngine: GameEngine
)(implicit executionContext: ExecutionContext)
  extends MessagesAbstractController(cc) {

  def welcome() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.welcome())
  }

  def welcomeSubmit() = Action { implicit request: Request[AnyContent] =>
    val playerName = request.body.asFormUrlEncoded.flatMap(_.get("playerName")).flatMap(_.headOption).getOrElse("Adventurer")
    gameEngine.initPlayer(playerName)
    Redirect(routes.MainController.index())
  }

  def index() = Action { implicit request: Request[AnyContent] =>
    val playerState = gameEngine.getPlayer
    Ok(views.html.game(playerState))
  }

  def getPlayerState() = Action { implicit request: Request[AnyContent] =>
    val state = gameEngine.getPlayer
      val json = Json.obj(
         "playerName"        -> state.playerName,
         "hp"                -> state.hp,
         "maxHp"             -> state.maxHp,
         "gold"              -> state.gold,
         "currentArea"       -> state.currentArea,
         "inventory"         -> state.inventory,
         "equipment"         -> state.equipment,
         "resources"         -> state.resources
        )
    Ok(json)
     }

  def navigate(area: String) = Action { implicit request: Request[AnyContent] =>
    gameEngine.setCurrentArea(area)
    val json = Json.obj("success" -> true, "currentArea" -> area)
    Ok(json)
  }

  def startCombat() = Action { implicit request: Request[AnyContent] =>
    gameEngine.startCombat() match {
      case Right(enemy) =>
        val enemyJson = Json.obj(
          "name"           -> enemy.name,
          "hp"             -> enemy.hp,
          "level"          -> enemy.level,
          "damage"         -> enemy.damage,
          "goldReward"     -> enemy.goldReward,
          "xpReward"       -> enemy.xpReward,
          "imagePath"      -> enemy.imagePath,
        )
        val result = Json.obj(
          "combatActive"        -> true,
          "enemy"               -> enemyJson,
          "playerHp"            -> gameEngine.getPlayer.hp,
          "playerMaxHp"         -> gameEngine.getPlayer.maxHp
        )
        Ok(result)
      case Left(error) =>
        val errorJson = Json.obj("error" -> error)
        BadRequest(errorJson)
      }
    }

  def playerAttack() = Action { implicit request: Request[AnyContent] =>
    val multiplierOpt = request.queryString.get("multiplier").flatMap(_.headOption).map(m => m.toDouble)
    val multiplier = if (multiplierOpt.exists(_ > 1.0)) multiplierOpt.get else 1.0

       // Log to terminal
    if (multiplier >= 2.0) {
      println(s"✅ Player answered a question! Damage: x${multiplier}")
      } else {
      println("➡ Player attacked without answering. Normal damage.")
      }
    gameEngine.playerAttack(multiplier) match {
      case result =>
        val json = Json.obj(
           "message"              -> result.message,
           "enemyName"            -> result.enemyName,
           "killed"               -> result.killed,
           "reward"               -> result.reward,
           "damageDealt"          -> result.damageDealt,
           "currentEnemyHp"       -> result.currentEnemyHp,
           "playerHp"             -> gameEngine.getPlayer.hp,
           "maxHp"                -> gameEngine.getPlayer.maxHp
          )
        Ok(json)
       }
     }

  def answerQuestion() = Action { implicit request: Request[AnyContent] =>
    val questionId = request.queryString.get("questionId").flatMap(_.headOption).getOrElse("")
    val answer = request.body.asFormUrlEncoded.flatMap(_.get("answer")).flatMap(_.headOption).getOrElse("")
    val (message, points) = gameEngine.answerQuestion(questionId, answer)
    val json = Json.obj(
        "message"               -> message,
        "points"                -> points,
        "newGold"               -> gameEngine.getPlayer.gold,
        "combatActive"          -> false
      )
    Ok(json)
    }

  def getQuestions() = Action { implicit request: Request[AnyContent] =>
    val questions = gameEngine.getAvailableQuestions()
    val json = Json.obj("questions" -> questions)
    Ok(json)
    }

  def endCombat() = Action { implicit request: Request[AnyContent] =>
    gameEngine.endCombat()
    val json = Json.obj("success" -> true)
    Ok(json)
  }

  def buyItem(itemName: String) = Action { implicit request: Request[AnyContent] =>
    gameEngine.buyItem(itemName) match {
      case Right(true)                  =>
        val json = Json.obj("success" -> true, "message" -> s"Purchased $itemName!")
        Ok(json)
      case _                           =>
        val errorJson = Json.obj("error" -> "Purchase failed")
        BadRequest(errorJson)
      }
    }

  def sellItem(itemName: String) = Action { implicit request: Request[AnyContent] =>
    val (success, price) = gameEngine.sellItem(itemName)
    if (success) {
      val json = Json.obj("success" -> true, "message" -> s"Sold $itemName for $price gold!", "gold" -> gameEngine.getPlayer.gold)
      Ok(json)
              } else {
      val errorJson = Json.obj("error" -> "You don't have that item - Awkward...")
      BadRequest(errorJson)
    }
  }

  def restAtInn() = Action { implicit request: Request[AnyContent] =>
    gameEngine.restAtInn() match {
      case Right(true)                 =>
        val json = Json.obj("success" -> true, "message" -> "You rested and fully healed! (20 gold)", "gold" -> gameEngine.getPlayer.gold)
        Ok(json)
      case _                          =>
        val errorJson = Json.obj("error" -> "Rest failed - Are you broke? Go gathering and sell stuff.")
        BadRequest(errorJson)
      }
    }

    def gatherResource(resourceType: String) = Action { implicit request: Request[AnyContent] =>
      val result = gameEngine.gatherResource(resourceType)
      val json = Json.obj("message" -> result)
      Ok(json)
    }

    def craftItem(recipeName: String) = Action { implicit request: Request[AnyContent] =>
      gameEngine.craftItem(recipeName) match {
        case Right(message)           => Ok(Json.obj("success" -> true,  "message" -> message))
        case Left(error)              => BadRequest(Json.obj("success" -> false, "message" -> error))
       }
     }

      def getAvailableQuestions() = Action { implicit request: Request[AnyContent] =>
        val questions = List(
          "What has gone well?",
          "What could have been better?",
          "What do we do well as a team?",
          "What could we improve? (team working, process, ceremonies etc)",
          "Gratitude: Say thanks to someone and why.",
          "What do you wish you knew more about in PlatUI, MDTP or wider?",
          "What is your favourite Slack emoji?"
        )
        val json = Json.obj("questions" -> questions)
        Ok(json)
       }
}
