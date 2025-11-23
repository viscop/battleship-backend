package ch.battleship.battleshipbackend.service;

import ch.battleship.battleshipbackend.domain.Board;
import ch.battleship.battleshipbackend.domain.Game;
import ch.battleship.battleshipbackend.domain.GameConfiguration;
import ch.battleship.battleshipbackend.domain.enums.GameStatus;
import ch.battleship.battleshipbackend.domain.Player;
import ch.battleship.battleshipbackend.repository.GameRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class GameService {

    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public Game createNewGame() {
        String gameCode = UUID.randomUUID().toString();
        GameConfiguration config = GameConfiguration.defaultConfig();
        Game game = new Game(gameCode, config);
        return gameRepository.save(game);
    }

    public Optional<Game> getByGameCode(String gameCode) {
        return gameRepository.findByGameCode(gameCode);
    }

    /**
     * Spieler tritt einem Spiel bei.
     * - Game muss existieren
     * - Game muss im Status WAITING sein
     * - Max. 2 Spieler
     * - Beim 2. Spieler wechselt das Game in den Status RUNNING
     */
    public Game joinGame(String gameCode, String username) {
        Game game = gameRepository.findByGameCode(gameCode)
                .orElseThrow(() -> new EntityNotFoundException("Game not found: " + gameCode));

        if (game.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException("Cannot join a game that is not in WAITING state");
        }

        int currentPlayers = game.getPlayers().size();
        if (currentPlayers >= 2) {
            throw new IllegalStateException("Game already has 2 players");
        }

        // neuen Player anlegen
        Player player = new Player(username);
        game.addPlayer(player);

        // passendes Board für diesen Player erzeugen
        int width = game.getConfig().getBoardWidth();
        int height = game.getConfig().getBoardHeight();

        Board board = new Board(width, height, player);

        game.addBoard(board);

        // zweiter Spieler startet das Game
        if (currentPlayers + 1 == 2) {
            game.setStatus(GameStatus.RUNNING);
        }

        return gameRepository.save(game);
    }
}
