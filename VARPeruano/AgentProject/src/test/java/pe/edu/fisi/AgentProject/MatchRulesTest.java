package pe.edu.fisi.AgentProject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchRulesTest {
    @Test
    void reviewsCallsAgainstTheOnlyFriendRegardlessOfArgumentOrder() {
        assertFalse(MatchRules.shouldReview(
                "Alianza Lima", "Universitario", true, "Alianza Lima", false));
        assertTrue(MatchRules.shouldReview(
                "Universitario", "Universitario", true, "Alianza Lima", false));
        assertFalse(MatchRules.shouldReview(
                "Sporting Cristal", "Sporting Cristal", false, "Boys", true));
        assertTrue(MatchRules.shouldReview(
                "Boys", "Sporting Cristal", false, "Boys", true));
    }

    @Test
    void sendsBothFriendAndNoFriendMatchesToVar() {
        assertTrue(MatchRules.shouldReview(
                "Universitario", "Universitario", true, "Boys", true));
        assertTrue(MatchRules.shouldReview(
                "Alianza Lima", "Sporting Cristal", false, "Alianza Lima", false));
    }

    @Test
    void varCorrectionChargesTheNonFriend() {
        assertEquals("Alianza Lima",
                MatchRules.correctedTeamChargedWithFoul(
                        "Universitario", true, "Alianza Lima", false));
        assertEquals("Alianza Lima",
                MatchRules.correctedTeamChargedWithFoul(
                        "Alianza Lima", false, "Juan Pablo II", true));
        assertTrue(isEither(
                MatchRules.correctedTeamChargedWithFoul(
                        "Universitario", true, "Boys", true),
                "Universitario", "Boys"));
        assertTrue(isEither(
                MatchRules.correctedTeamChargedWithFoul(
                        "Alianza Lima", false, "Sporting Cristal", false),
                "Alianza Lima", "Sporting Cristal"));
    }

    private boolean isEither(String value, String first, String second) {
        return value.equals(first) || value.equals(second);
    }
}
