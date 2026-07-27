package com.aelion.aero.common.control;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Response for {@code GET /v1/players}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ControlPlayersResponse {

    private List<ControlPlayerEntry> players = new ArrayList<>();

    public ControlPlayersResponse() {
    }

    public ControlPlayersResponse(List<ControlPlayerEntry> players) {
        setPlayers(players);
    }

    public static ControlPlayersResponse of(List<ControlPlayerEntry> players) {
        return new ControlPlayersResponse(players);
    }

    public static ControlPlayersResponse empty() {
        return new ControlPlayersResponse(Collections.emptyList());
    }

    public List<ControlPlayerEntry> getPlayers() {
        return players;
    }

    public void setPlayers(List<ControlPlayerEntry> players) {
        this.players = players == null ? new ArrayList<>() : new ArrayList<>(players);
    }
}
