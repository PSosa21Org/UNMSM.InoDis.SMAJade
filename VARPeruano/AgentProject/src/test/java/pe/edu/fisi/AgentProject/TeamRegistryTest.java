package pe.edu.fisi.AgentProject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamRegistryTest {
    @Test
    void recognizesFriendTeamsWithoutCaseSensitivity() {
        assertTrue(TeamRegistry.isFriend("bOyS"));
        assertTrue(TeamRegistry.isFriend("juan pablo ii"));
    }

    @Test
    void acceptsAnyTwoDistinctTeamsAndRejectsInvalidPairs() {
        TeamRegistry.validateMatch("Alianza Lima", "Sporting Cristal");
        assertThrows(IllegalArgumentException.class,
                () -> TeamRegistry.validateMatch("Juan Pablo II", "juan pablo ii"));
        assertThrows(IllegalArgumentException.class,
                () -> TeamRegistry.validateMatch(" ", "Boys"));
    }

    @Test
    void exposesTheFriendTeams() {
        assertArrayEquals(new String[]{"Universitario", "Juan Pablo II", "Boys"},
                TeamRegistry.getFriendTeamNames());
    }
}
