package pe.edu.fisi.AgentProject;

import java.util.concurrent.ThreadLocalRandom;

public final class MatchRules {
    private MatchRules() {
    }

    public static boolean shouldReview(
            String teamChargedWithFoul,
            String firstTeam,
            boolean firstIsFriend,
            String secondTeam,
            boolean secondIsFriend) {
        if (firstIsFriend == secondIsFriend) {
            return true;
        }
        String friendTeam = firstIsFriend ? firstTeam : secondTeam;
        return TeamRegistry.normalizeTeamName(teamChargedWithFoul)
                .equalsIgnoreCase(friendTeam);
    }

    public static String friendTeam(
            String firstTeam, boolean firstIsFriend, String secondTeam, boolean secondIsFriend) {
        if (firstIsFriend == secondIsFriend) {
            throw new IllegalArgumentException(
                    "Se requiere exactamente un equipo amigo para corregir la decisión.");
        }
        return firstIsFriend ? firstTeam : secondTeam;
    }

    public static String correctedTeamChargedWithFoul(
            String firstTeam,
            boolean firstIsFriend,
            String secondTeam,
            boolean secondIsFriend) {
        if (firstIsFriend == secondIsFriend) {
            return ThreadLocalRandom.current().nextBoolean() ? firstTeam : secondTeam;
        }
        String friendTeam = friendTeam(firstTeam, firstIsFriend, secondTeam, secondIsFriend);
        return friendTeam.equalsIgnoreCase(firstTeam) ? secondTeam : firstTeam;
    }
}
