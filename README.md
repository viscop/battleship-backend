@startuml
skinparam classAttributeIconSize 0
 
' =========================
' Klassen (Entities)
' =========================
 
class Game {
  +id
  +status : GameStatus
  +gameCode
  +createdAt
  +updatedAt
  +config : GameConfiguration
}
 
class Player {
  +id
  +username
  ' optional: +email
}
 
class Board {
  +id
  +width
  +height
  +createdAt
  +owner : Player
  +placements : List<ShipPlacement>
}
 
class Ship {
  +id
  +type : ShipType
  +size
}
 
class ShipPlacement {
  +id
  +orientation : Orientation
  +start : Coordinate
  +ship : Ship
}
 
class Shot {
  +id
  +coordinate : Coordinate
  +result : ShotResult
  +timestamp
  +shooter : Player
  +targetBoard : Board
}
 
class ChatMessage {
  +id
  +text
  +timestamp
  +sender : Player
}
 
class GameConfiguration {
  +boardWidth
  +boardHeight
  +fleetDefinition
  ' z.B. 2x2, 2x3, 1x4, 1x5
}
 
' =========================
' Value Object
' =========================
class Coordinate {
  +x
  +y
}
 
' =========================
' Enums
' =========================
class GameStatus <<enumeration>> {
  WAITING
  RUNNING
  PAUSED
  FINISHED
}
 
class Orientation <<enumeration>> {
  HORIZONTAL
  VERTICAL
}
 
class ShotResult <<enumeration>> {
  MISS
  HIT
  SUNK
  ALREADY_SHOT
}
 
class ShipType <<enumeration>> {
  DESTROYER  ' size 2
  CRUISER    ' size 3
  BATTLESHIP ' size 4
  CARRIER    ' size 5
}
 
' =========================
' Beziehungen
' =========================
 
' Game als Root
Game "1" -- "*" Player : players
Game "1" -- "2" Board : boards
Game "1" -- "*" Shot : shots
Game "1" -- "*" ChatMessage : messages
Game "1" -- "1" GameConfiguration : config
 
' Board-Umfeld
Board "1" o-- "*" ShipPlacement : placements
ShipPlacement "*" -- "1" Ship : ship
 
' Shots & Chat
Shot "*" -- "1" Player : shooter
Shot "*" -- "1" Board : targetBoard
Shot "*" -- "1" Coordinate : coordinate
Shot "*" -- "1" ShotResult : result
 
ChatMessage "*" -- "1" Player : sender
ChatMessage "*" -- "1" Game : game
 
' Enums & Value Objects
Ship "1" -- "1" ShipType : type
ShipPlacement "*" -- "1" Orientation : orientation
ShipPlacement "*" -- "1" Coordinate : start
Game "1" -- "1" GameStatus : status
 
@enduml
