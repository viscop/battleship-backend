package ch.battleship.battleshipbackend.application.service;

import ch.battleship.battleshipbackend.domain.*;
import ch.battleship.battleshipbackend.domain.enums.GameStatus;
import ch.battleship.battleshipbackend.domain.enums.Orientation;
import ch.battleship.battleshipbackend.domain.enums.ShipType;
import ch.battleship.battleshipbackend.domain.enums.ShotResult;
import ch.battleship.battleshipbackend.repository.GameRepository;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private ch.battleship.battleshipbackend.service.GameService gameService;

    @Test
    void createNewGame_shouldSetWaitingStatusAndGenerateGameCode_andSaveGame() {
        // Arrange
        when(gameRepository.save(any(Game.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);

        // Act
        Game game = gameService.createNewGame();

        // Assert: Rückgabewert
        assertThat(game).isNotNull();
        assertThat(game.getStatus()).isEqualTo(GameStatus.WAITING);
        assertThat(game.getGameCode()).isNotBlank();

        // Assert: Interaktion mit Repository
        verify(gameRepository, times(1)).save(gameCaptor.capture());
        Game saved = gameCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(GameStatus.WAITING);
        assertThat(saved.getGameCode()).isNotBlank();
    }

    @Test
    void getByGameCode_shouldDelegateToRepository() {
        GameConfiguration config = GameConfiguration.defaultConfig();
        // Arrange
        String gameCode = "TEST-CODE";
        Game existing = new Game(gameCode, config);
        when(gameRepository.findByGameCode(gameCode)).thenReturn(Optional.of(existing));

        // Act
        Optional<Game> result = gameService.getByGameCode(gameCode);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getGameCode()).isEqualTo(gameCode);
        verify(gameRepository, times(1)).findByGameCode(gameCode);
        verifyNoMoreInteractions(gameRepository);
    }

    @Test
    void joinGame_secondPlayerShouldStartGame() {
        GameConfiguration config = GameConfiguration.defaultConfig();
        // Arrange
        String code = "TEST-CODE";
        Game game = new Game(code, config);

        // erster Player ist schon drin
        game.addPlayer(new Player("FirstPlayer"));

        when(gameRepository.findByGameCode(code)).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Game result = gameService.joinGame(code, "SecondPlayer");

        // Assert
        assertThat(result.getPlayers()).hasSize(2);
        assertThat(result.getStatus()).isEqualTo(GameStatus.RUNNING);
        verify(gameRepository, times(1)).save(game);
    }

    @Test
    void joinGame_firstPlayer_shouldKeepStatusWaiting() {
        GameConfiguration config = GameConfiguration.defaultConfig();
        // Arrange
        String code = "TEST-CODE";
        Game game = new Game(code, config); // status = WAITING, players = empty
        when(gameRepository.findByGameCode(code)).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Game result = gameService.joinGame(code, "Player1");

        // Assert
        assertThat(result.getPlayers()).hasSize(1);
        assertThat(result.getPlayers().get(0).getUsername()).isEqualTo("Player1");
        assertThat(result.getStatus()).isEqualTo(GameStatus.WAITING); // noch nicht gestartet
        verify(gameRepository, times(1)).save(game);
    }

    @Test
    void joinGame_secondPlayer_shouldSetStatusRunning() {
        GameConfiguration config = GameConfiguration.defaultConfig();
        // Arrange
        String code = "TEST-CODE";
        Game game = new Game(code, config);
        game.addPlayer(new Player("Player1")); // es gibt schon einen Spieler

        when(gameRepository.findByGameCode(code)).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Game result = gameService.joinGame(code, "Player2");

        // Assert
        assertThat(result.getPlayers()).hasSize(2);
        assertThat(result.getPlayers().get(1).getUsername()).isEqualTo("Player2");
        assertThat(result.getStatus()).isEqualTo(GameStatus.RUNNING);
        verify(gameRepository, times(1)).save(game);
    }

    @Test
    void joinGame_thirdPlayer_shouldThrowIllegalStateException() {
        GameConfiguration config = GameConfiguration.defaultConfig();
        // Arrange
        String code = "TEST-CODE";
        Game game = new Game(code, config);
        game.addPlayer(new Player("Player1"));
        game.addPlayer(new Player("Player2"));

        when(gameRepository.findByGameCode(code)).thenReturn(Optional.of(game));

        // Act + Assert
        assertThatThrownBy(() -> gameService.joinGame(code, "Player3"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("2 players");

        verify(gameRepository, never()).save(any());
    }

    @Test
    void joinGame_nonExistingGame_shouldThrowEntityNotFound() {
        // Arrange
        String code = "UNKNOWN";
        when(gameRepository.findByGameCode(code)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> gameService.joinGame(code, "Player1"))
                .isInstanceOf(EntityNotFoundException.class);

        verify(gameRepository, never()).save(any());
    }

    @Test
    void joinGame_firstPlayer_shouldKeepStatusWaiting_andCreateBoard() {
        GameConfiguration config = GameConfiguration.defaultConfig();
        // Arrange
        String code = "TEST-CODE";
        Game game = new Game(code, config); // status = WAITING, players/boards = empty

        when(gameRepository.findByGameCode(code)).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Game result = gameService.joinGame(code, "Player1");

        // Assert
        assertThat(result.getPlayers()).hasSize(1);
        Player p1 = result.getPlayers().get(0);
        assertThat(p1.getUsername()).isEqualTo("Player1");

        assertThat(result.getBoards()).hasSize(1);
        Board b1 = result.getBoards().get(0);
        assertThat(b1.getOwner()).isEqualTo(p1);
        assertThat(b1.getWidth()).isEqualTo(10);
        assertThat(b1.getHeight()).isEqualTo(10);

        assertThat(result.getStatus()).isEqualTo(GameStatus.WAITING);
        verify(gameRepository, times(1)).save(game);
    }

    @Test
    void joinGame_secondPlayer_shouldSetStatusRunning_andCreateSecondBoard() {
        GameConfiguration config = GameConfiguration.defaultConfig();
        // Arrange
        String code = "TEST-CODE";
        Game game = new Game(code, config);

        // Konsistenter Startzustand: 1 Player + 1 Board
        Player existing = new Player("Player1");
        game.addPlayer(existing);
        game.addBoard(new Board(10, 10, existing));

        when(gameRepository.findByGameCode(code)).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Game result = gameService.joinGame(code, "Player2");

        // Assert
        assertThat(result.getPlayers()).hasSize(2);
        Player p2 = result.getPlayers().get(1);
        assertThat(p2.getUsername()).isEqualTo("Player2");

        assertThat(result.getBoards()).hasSize(2);
        // irgendein Board gehört p2
        assertThat(result.getBoards())
                .anyMatch(b -> b.getOwner().equals(p2));

        assertThat(result.getStatus()).isEqualTo(GameStatus.RUNNING);
        verify(gameRepository, times(1)).save(game);
    }

    @Test
    void joinGame_thirdPlayer_shouldThrowIllegalStateException_andNotChangeBoards() {
        GameConfiguration config = GameConfiguration.defaultConfig();
        // Arrange
        String code = "TEST-CODE";
        Game game = new Game(code, config);

        Player p1 = new Player("Player1");
        Player p2 = new Player("Player2");
        game.addPlayer(p1);
        game.addPlayer(p2);
        game.addBoard(new Board(10, 10, p1));
        game.addBoard(new Board(10, 10, p2));

        when(gameRepository.findByGameCode(code)).thenReturn(Optional.of(game));

        // Act + Assert
        assertThatThrownBy(() -> gameService.joinGame(code, "Player3"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(game.getPlayers()).hasSize(2);
        assertThat(game.getBoards()).hasSize(2);
        verify(gameRepository, never()).save(any());
    }

    @Test
    void createNewGame_shouldUseDefaultConfiguration() {
        // Arrange
        when(gameRepository.save(any(Game.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Game game = gameService.createNewGame();

        // Assert
        assertThat(game.getConfig()).isNotNull();
        assertThat(game.getConfig().getBoardWidth()).isEqualTo(10);
        assertThat(game.getConfig().getBoardHeight()).isEqualTo(10);
        assertThat(game.getConfig().getFleetDefinition()).isEqualTo("2x2,2x3,1x4,1x5");
    }

    @Test
    void canPlaceShip_shouldReturnFalse_whenOverlappingWithOneOfMultipleExistingPlacements() {
        // Arrange
        Board board = new Board(10, 10, null);
        Ship ship1 = new Ship(ShipType.DESTROYER);  // size 2
        Ship ship2 = new Ship(ShipType.CRUISER);    // size 3
        Ship newShip = new Ship(ShipType.DESTROYER);

        // Erstes Schiff: horizontal bei (0,0) -> (1,0)
        board.placeShip(ship1, new Coordinate(0, 0), Orientation.HORIZONTAL);

        // Zweites Schiff: horizontal bei (5,5) -> (7,5)
        board.placeShip(ship2, new Coordinate(5, 5), Orientation.HORIZONTAL);

        // Neues Schiff soll das zweite schneiden: vertikal durch (6,5)
        Coordinate startOverlapping = new Coordinate(6, 4); // (6,4) & (6,5)

        // Act
        boolean result = board.canPlaceShip(newShip, startOverlapping, Orientation.VERTICAL);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void fireShot_shouldReturnShotAndPersistGame_whenAllDataIsValid() {
        GameConfiguration config = GameConfiguration.defaultConfig();
        // Arrange
        String code = "TEST-CODE";
        Game game = new Game(code, config);
        game.setStatus(GameStatus.RUNNING);

        // Spieler & Board wie im echten Szenario
        Player attacker = new Player("Attacker");
        Player defender = new Player("Defender");
        game.addPlayer(attacker);
        game.addPlayer(defender);

        Board defenderBoard = new Board(
                game.getConfig().getBoardWidth(),
                game.getConfig().getBoardHeight(),
                defender
        );

        // Schiff platzieren: DESTROYER (2 Felder) bei (3,3) & (4,3)
        Ship ship = new Ship(ShipType.DESTROYER);
        defenderBoard.placeShip(ship, new Coordinate(3, 3), Orientation.HORIZONTAL);

        game.addBoard(defenderBoard);

        when(gameRepository.findByGameCode(code)).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Shot shot = gameService.fireShot(
                code,
                attacker.getId(),
                defenderBoard.getId(),
                3,
                3
        );

        // Assert
        assertThat(shot).isNotNull();
        assertThat(shot.getResult()).isEqualTo(ShotResult.HIT);
        assertThat(shot.getShooter()).isEqualTo(attacker);
        assertThat(shot.getTargetBoard()).isEqualTo(defenderBoard);
        assertThat(game.getShots()).hasSize(1);

        verify(gameRepository, times(1)).save(game);
    }

    @Test
    void fireShot_shouldReturnSunk_whenAllCoordinatesOfShipAreHit() {
        GameConfiguration config = GameConfiguration.defaultConfig();
        // Arrange
        String code = "TEST-CODE";
        Game game = new Game(code, config);
        game.setStatus(GameStatus.RUNNING);

        Player attacker = new Player("Attacker");
        Player defender = new Player("Defender");
        game.addPlayer(attacker);
        game.addPlayer(defender);

        Board defenderBoard = new Board(
                game.getConfig().getBoardWidth(),
                game.getConfig().getBoardHeight(),
                defender
        );

        Ship ship = new Ship(ShipType.DESTROYER); // size 2
        defenderBoard.placeShip(ship, new Coordinate(3, 3), Orientation.HORIZONTAL);

        game.addBoard(defenderBoard);

        when(gameRepository.findByGameCode(code)).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. Schuss -> HIT
        Shot first = gameService.fireShot(
                code,
                attacker.getId(),
                defenderBoard.getId(),
                3,
                3
        );
        assertThat(first.getResult()).isEqualTo(ShotResult.HIT);

        // 2. Schuss -> SUNK
        Shot second = gameService.fireShot(
                code,
                attacker.getId(),
                defenderBoard.getId(),
                4,
                3
        );

        // Assert
        assertThat(second.getResult()).isEqualTo(ShotResult.SUNK);
        assertThat(game.getShots()).hasSize(2);
        verify(gameRepository, atLeastOnce()).save(game);
    }


    @Test
    void fireShot_shouldThrowEntityNotFound_whenGameDoesNotExist() {
        // Arrange
        String code = "UNKNOWN";
        when(gameRepository.findByGameCode(code)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() ->
                gameService.fireShot(code, UUID.randomUUID(), UUID.randomUUID(), 0, 0)
        ).isInstanceOf(jakarta.persistence.EntityNotFoundException.class);

        verify(gameRepository, never()).save(any());
    }

    @Test
    void fireShot_shouldThrowIllegalState_whenGameIsNotRunning() {
        GameConfiguration config = GameConfiguration.defaultConfig();
        // Arrange
        String code = "TEST-CODE";
        Game game = new Game(code, config); // Status = WAITING per Default
        when(gameRepository.findByGameCode(code)).thenReturn(Optional.of(game));

        // Act + Assert
        assertThatThrownBy(() ->
                gameService.fireShot(code, UUID.randomUUID(), UUID.randomUUID(), 0, 0)
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not RUNNING");

        verify(gameRepository, never()).save(any());
    }

    @Test
    void fireShot_shouldThrowIllegalState_whenShooterIsNotPartOfGame() {
        GameConfiguration config = GameConfiguration.defaultConfig();
        // Arrange
        String code = "TEST-CODE";
        Game game = new Game(code, config);
        game.setStatus(GameStatus.RUNNING);

        Player somePlayer = new Player("SomePlayer");
        game.addPlayer(somePlayer);

        Board board = new Board(
                game.getConfig().getBoardWidth(),
                game.getConfig().getBoardHeight(),
                somePlayer
        );
        game.addBoard(board);

        UUID unknownShooterId = UUID.randomUUID();

        when(gameRepository.findByGameCode(code)).thenReturn(Optional.of(game));

        // Act + Assert
        assertThatThrownBy(() ->
                gameService.fireShot(code, unknownShooterId, board.getId(), 0, 0)
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Shooter does not belong");

        verify(gameRepository, never()).save(any());
    }

    @Test
    void fireShot_shouldThrowIllegalState_whenShootingOwnBoard() {
        GameConfiguration config = GameConfiguration.defaultConfig();
        // Arrange
        String code = "TEST-CODE";
        Game game = new Game(code, config);
        game.setStatus(GameStatus.RUNNING);

        Player attacker = new Player("Attacker");
        game.addPlayer(attacker);

        Board ownBoard = new Board(
                game.getConfig().getBoardWidth(),
                game.getConfig().getBoardHeight(),
                attacker
        );
        game.addBoard(ownBoard);

        when(gameRepository.findByGameCode(code)).thenReturn(Optional.of(game));

        // Act + Assert
        assertThatThrownBy(() ->
                gameService.fireShot(code, attacker.getId(), ownBoard.getId(), 0, 0)
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot shoot at own board");

        verify(gameRepository, never()).save(any());
    }

    @Test
    void fireShot_shouldThrowIllegalArgument_whenCoordinateOutOfBounds() {
        GameConfiguration config = GameConfiguration.defaultConfig();
        // Arrange
        String code = "TEST-CODE";
        Game game = new Game(code, config);
        game.setStatus(GameStatus.RUNNING);

        Player attacker = new Player("Attacker");
        Player defender = new Player("Defender");
        game.addPlayer(attacker);
        game.addPlayer(defender);

        Board defenderBoard = new Board(
                game.getConfig().getBoardWidth(),
                game.getConfig().getBoardHeight(),
                defender
        );
        game.addBoard(defenderBoard);

        when(gameRepository.findByGameCode(code)).thenReturn(Optional.of(game));

        int xOutOfBounds = defenderBoard.getWidth();   // gültig wäre 0..width-1
        int yOutOfBounds = defenderBoard.getHeight();  // gültig wäre 0..height-1

        // Act + Assert
        assertThatThrownBy(() ->
                gameService.fireShot(code, attacker.getId(), defenderBoard.getId(), xOutOfBounds, yOutOfBounds)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("out of board bounds");

        verify(gameRepository, never()).save(any());
    }

}

