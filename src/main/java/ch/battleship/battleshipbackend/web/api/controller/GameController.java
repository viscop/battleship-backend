package ch.battleship.battleshipbackend.web.api.controller;

import ch.battleship.battleshipbackend.service.GameService;

import ch.battleship.battleshipbackend.domain.Game;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    // Neues Game anlegen
    @PostMapping
    public ResponseEntity<GameDto> createGame() {
        Game game = gameService.createNewGame();
        return ResponseEntity.ok(GameDto.from(game));
    }

    // Game per gameCode laden
    @GetMapping("/{gameCode}")
    public ResponseEntity<GameDto> getGame(@PathVariable String gameCode) {
        return gameService.getByGameCode(gameCode)
                .map(game -> ResponseEntity.ok(GameDto.from(game)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{gameCode}/join")
    public ResponseEntity<GameDto> joinGame(@PathVariable String gameCode,
                                            @RequestBody JoinGameRequest request) {
        try {
            Game game = gameService.joinGame(gameCode, request.username());
            return ResponseEntity.ok(GameDto.from(game));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build(); // später evtl. aussagekräftigere Fehler
        }
    }
}