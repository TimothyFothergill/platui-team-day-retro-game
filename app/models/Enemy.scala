package models

case class Enemy(
  name: String,
  hp: Int,
  level: Int,
  damage: Int,
  goldReward: Int,
  xpReward: Int,
  imagePath: String = ""
)
