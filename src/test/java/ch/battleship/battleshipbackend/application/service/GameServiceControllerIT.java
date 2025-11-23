package ch.battleship.battleshipbackend.application.service;

import ch.battleship.battleshipbackend.domain.Game;
import ch.battleship.battleshipbackend.repository.GameRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GameControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameRepository gameRepository;

    @Test
    void createGame_and_then_loadByGameCode() throws Exception {
        // 1) POST /api/games -> Game erstellen
        String response = mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameCode").exists())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Kleiner Trick: wir holen den GameCode aus der DB statt aus dem JSON zu parsen
        // (für den Anfang ist das ok)
        Game game = gameRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Game not created"));
        String code = game.getGameCode();

        assertThat(code).isNotBlank();

        // 2) GET /api/games/{code} -> sollte das Spiel zurückgeben
        mockMvc.perform(get("/api/games/{gameCode}", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameCode").value(code))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }
}

