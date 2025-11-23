package ch.battleship.battleshipbackend.web.api.controller;

import ch.battleship.battleshipbackend.domain.Game;
import ch.battleship.battleshipbackend.domain.enums.GameStatus;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class GameDto {
    private UUID id;
    private String gameCode;
    private GameStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static GameDto from(Game game) {
        GameDto dto = new GameDto();
        dto.setId(game.getId());
        dto.setGameCode(game.getGameCode());
        dto.setStatus(game.getStatus());
        dto.setCreatedAt(game.getCreatedAt());
        dto.setUpdatedAt(game.getUpdatedAt());
        return dto;
    }
}
