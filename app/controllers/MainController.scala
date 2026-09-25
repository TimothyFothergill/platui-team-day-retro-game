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
  def toJsonNode(obj: JsObject): JsonNode = {
      val mapper = new ObjectMapper()
      mapper.readTree(obj.toString())
    }

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
                      "playerName"       -> state.playerName,
                      "hp"               -> state.hp,
                      "maxHp"            -> state.maxHp,
                      "gold"             -> state.gold,
                      "level"            -> state.level,
                      "currentArea"      -> state.currentArea,
                      "inventory"        -> state.inventory,
                      "equipment"        -> state.equipment
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
                        "damage"         -> enemy.damage,
                        "goldReward"     -> enemy.goldReward
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
    gameEngine.playerAttack(10) match {
      case result =>
        val json = Json.obj(
                        "message"            -> result.message,
                        "enemyName"          -> result.enemyName,
                        "killed"             -> result.killed,
                        "reward"             -> result.reward,
                        "damageDealt"        -> result.damageDealt,
                        "playerHp"           -> gameEngine.getPlayer.hp,
                        "maxHp"              -> gameEngine.getPlayer.maxHp
                      )
        Ok(json)
      }
    }

  def answerQuestion() = Action { implicit request: Request[AnyContent] =>
    val (message, points) = gameEngine.answerQuestion("", "")
    val json = Json.obj(
                    "message"             -> message,
                    "points"              -> points,
                    "newGold"             -> gameEngine.getPlayer.gold,
                    "combatActive"        -> false
                  )
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
      val errorJson = Json.obj("error" -> "You don't have that item.")
      BadRequest(errorJson)
    }
  }

  def restAtInn() = Action { implicit request: Request[AnyContent] =>
    gameEngine.restAtInn() match {
      case Right(true)                 =>
        val json = Json.obj("success" -> true, "message" -> "You rested and fully healed! (20 gold)")
        Ok(json)
      case _                          =>
        val errorJson = Json.obj("error" -> "Rest failed")
        BadRequest(errorJson)
      }
    }

    def gatherResource(resourceType: String) = Action { implicit request: Request[AnyContent] =>
      val result = gameEngine.gatherResource(resourceType)
      val json = Json.obj("message" -> result)
      Ok(json)
    }

    def craftItem(recipeName: String) = Action { implicit request: Request[AnyContent] =>
      val result = gameEngine.craftItem(recipeName)
      val json = Json.obj("message" -> result)
      Ok(json)
      }

    def getAvailableQuestions() = Action { implicit request: Request[AnyContent] =>
      val q1 = Json.toJson(Json.obj("id" -> "q1", "question" -> "What is 7 x 8?", "options" -> List("54", "56", "58", "60")))
      val q2 = Json.toJson(Json.obj("id" -> "q2", "question" -> "Which element has atomic number 1?", "options" -> List("Helium", "Hydrogen", "Lithium", "Carbon")))
      val q3 = Json.toJson(Json.obj("id" -> "q3", "question" -> "How many legs does a spider have?", "options" -> List("6", "8", "10", "4")))
      val q4 = Json.toJson(Json.obj("id" -> "q4", "question" -> "What is the capital of France?", "options" -> List("London", "Berlin", "Paris", "Madrid")))
      val q5 = Json.toJson(Json.obj("id" -> "q5", "question" -> "What does HP stand for in games?", "options" -> List("High Power", "Hit Points", "Heavy Punch", "Happy Play")))
      val questions = List(q1, q2, q3, q4, q5)
      val json = Json.obj("questions" -> questions)
      Ok(json)
    }
}
