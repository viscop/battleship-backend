package ch.battleship.battleshipbackend.application.service;

import ch.battleship.battleshipbackend.domain.Board;
import ch.battleship.battleshipbackend.domain.Game;
import ch.battleship.battleshipbackend.domain.GameConfiguration;
import ch.battleship.battleshipbackend.domain.Player;
import ch.battleship.battleshipbackend.domain.enums.GameStatus;
import ch.battleship.battleshipbackend.repository.GameRepository;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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

}

