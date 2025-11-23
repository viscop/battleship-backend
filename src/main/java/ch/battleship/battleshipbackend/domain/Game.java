package ch.battleship.battleshipbackend.domain;

import ch.battleship.battleshipbackend.domain.common.BaseEntity;
import ch.battleship.battleshipbackend.domain.enums.GameStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Game extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status;

    @Column(nullable = false, unique = true, length = 64)
    private String gameCode;

    @Embedded
    private GameConfiguration config;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "game_id") // FK in players-Tabelle, aber Player kennt Game nicht
    private List<Player> players = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "game_id") // FK in boards-Tabelle, Board kennt Game nicht
    private List<Board> boards = new ArrayList<>();

    public Game(String gameCode, GameConfiguration config) {
        this.status = GameStatus.WAITING;
        this.gameCode = gameCode;
        this.config = config;
    }

    public void addPlayer(Player player) {
        this.players.add(player);
    }

    public void addBoard(Board board) {
        this.boards.add(board);
    }

}
