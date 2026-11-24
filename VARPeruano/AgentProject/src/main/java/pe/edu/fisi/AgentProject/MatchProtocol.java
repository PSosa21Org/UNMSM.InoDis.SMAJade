package pe.edu.fisi.AgentProject;

final class MatchProtocol {
    static final String CONVERSATION_ID = "var-peruano-match";
    private static final String FRIEND = "FRIEND";
    private static final String NON_FRIEND = "NON_FRIEND";

    private MatchProtocol() {
    }

    static String[] fields(String content, int expectedCount) {
        String[] parts = content.split("\\|", -1);
        if (parts.length != expectedCount) {
            throw new IllegalArgumentException("Mensaje JADE con formato no válido: " + content);
        }
        return parts;
    }

    static String teamStatus(boolean isFriend) {
        return isFriend ? FRIEND : NON_FRIEND;
    }

    static boolean isFriendStatus(String status) {
        if (FRIEND.equals(status)) {
            return true;
        }
        if (NON_FRIEND.equals(status)) {
            return false;
        }
        throw new IllegalArgumentException("Estado de equipo no válido: " + status);
    }
}
