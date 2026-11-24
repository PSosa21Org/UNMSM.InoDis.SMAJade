package pe.edu.fisi.AgentProject;

import java.util.Arrays;

public final class TeamRegistry {
    private static final String[] FRIEND_TEAM_NAMES = {"Universitario", "Juan Pablo II", "Boys"};

    private TeamRegistry() {
    }

    public static String[] getFriendTeamNames() {
        return FRIEND_TEAM_NAMES.clone();
    }

    public static String normalizeTeamName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del equipo no puede estar vacío.");
        }
        String normalized = name.trim();
        if (normalized.contains("|")) {
            throw new IllegalArgumentException("El nombre del equipo no puede contener '|'.");
        }
        return normalized;
    }

    public static boolean isFriend(String teamName) {
        String normalized = normalizeTeamName(teamName);
        return Arrays.stream(FRIEND_TEAM_NAMES)
                .anyMatch(friend -> friend.equalsIgnoreCase(normalized));
    }

    public static void validateMatch(String firstTeam, String secondTeam) {
        String first = normalizeTeamName(firstTeam);
        String second = normalizeTeamName(secondTeam);
        if (first.equalsIgnoreCase(second)) {
            throw new IllegalArgumentException("Los dos equipos deben ser distintos.");
        }
    }
}
