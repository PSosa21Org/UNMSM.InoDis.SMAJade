package pe.edu.fisi.AgentProject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class RefereeAgent extends Agent {
    private static final int FOUL_COUNT = 20;
    private static final long FOUL_DELAY_MS = 1000;

    private enum State {
        START,
        WAIT_MATCH_READY,
        SEND_FOUL,
        WAIT_BETWEEN_FOULS,
        WAIT_RESULT
    }

    private State state = State.START;
    private String firstTeam;
    private String secondTeam;
    private boolean firstIsFriend;
    private boolean secondIsFriend;
    private int foulNumber;
    private final Map<String, Integer> committedFouls = new HashMap<>();
    private final Map<String, Integer> calledAgainst = new HashMap<>();

    public RefereeAgent(String firstTeam, String secondTeam) {
        this.firstTeam = firstTeam;
        this.secondTeam = secondTeam;
    }

    @Override
    protected void setup() {
        firstTeam = TeamRegistry.normalizeTeamName(firstTeam);
        secondTeam = TeamRegistry.normalizeTeamName(secondTeam);
        TeamRegistry.validateMatch(firstTeam, secondTeam);
        committedFouls.put(firstTeam, 0);
        committedFouls.put(secondTeam, 0);
        calledAgainst.put(firstTeam, 0);
        calledAgainst.put(secondTeam, 0);
        AgentServices.register(this, AgentServices.REFEREE);

        System.out.println("Árbitro: partido " + firstTeam + " vs " + secondTeam);
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                switch (state) {
                    case START:
                        if (!requestMatchSetup()) {
                            block(1000);
                        }
                        break;
                    case WAIT_MATCH_READY:
                        if (!receiveMatchSetup()) {
                            block();
                        }
                        break;
                    case SEND_FOUL:
                        sendFoul();
                        break;
                    case WAIT_BETWEEN_FOULS:
                        state = State.SEND_FOUL;
                        block(FOUL_DELAY_MS);
                        break;
                    case WAIT_RESULT:
                        if (!receiveResult()) {
                            block();
                        }
                        break;
                    default:
                        throw new IllegalStateException("Estado arbitral inesperado.");
                }
            }
        });
    }

    private boolean requestMatchSetup() {
        AID operator = AgentServices.find(this, AgentServices.OPERATOR);
        if (operator == null) {
            return false;
        }
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.addReceiver(operator);
        request.setConversationId(MatchProtocol.CONVERSATION_ID);
        request.setContent("MATCH|" + firstTeam + "|" + secondTeam + "|"
                + FOUL_COUNT + "|START");
        send(request);
        state = State.WAIT_MATCH_READY;
        return true;
    }

    private boolean receiveMatchSetup() {
        ACLMessage response = receive(
                MessageTemplate.MatchConversationId(MatchProtocol.CONVERSATION_ID));
        if (response == null) {
            return false;
        }
        String[] fields = MatchProtocol.fields(response.getContent(), 5);
        if (!"MATCH_READY".equals(fields[0]) || response.getPerformative() != ACLMessage.INFORM) {
            throw new IllegalStateException("No se pudo configurar el partido: "
                    + response.getContent());
        }
        firstTeam = fields[1];
        firstIsFriend = MatchProtocol.isFriendStatus(fields[2]);
        secondTeam = fields[3];
        secondIsFriend = MatchProtocol.isFriendStatus(fields[4]);
        state = State.SEND_FOUL;
        return true;
    }

    private void sendFoul() {
        if (foulNumber == FOUL_COUNT) {
            printSummary();
            doDelete();
            return;
        }

        String offender = randomTeam();
        String initialCall = randomTeam();
        committedFouls.compute(offender, (team, count) -> count + 1);
        foulNumber++;

        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        AID operator = AgentServices.find(this, AgentServices.OPERATOR);
        if (operator == null) {
            throw new IllegalStateException("El servicio del operador no está disponible.");
        }
        request.addReceiver(operator);
        request.setConversationId(MatchProtocol.CONVERSATION_ID);
        request.setContent("FOUL|" + foulNumber + "|" + offender + "|" + initialCall);
        send(request);
        System.out.println("Árbitro: falta #" + foulNumber + " cometida por " + offender
                + "; decisión inicial, cobro contra " + initialCall + ".");
        state = State.WAIT_RESULT;
    }

    private boolean receiveResult() {
        ACLMessage response = receive(
                MessageTemplate.MatchConversationId(MatchProtocol.CONVERSATION_ID));
        if (response == null) {
            return false;
        }
        String[] fields = MatchProtocol.fields(response.getContent(), 4);
        if (!"RESULT".equals(fields[0]) || response.getPerformative() != ACLMessage.INFORM) {
            throw new IllegalStateException("Respuesta del operador no válida: "
                    + response.getContent());
        }
        int resultNumber = Integer.parseInt(fields[1]);
        if (resultNumber != foulNumber) {
            throw new IllegalStateException("La respuesta no corresponde a la falta pendiente.");
        }
        calledAgainst.compute(fields[2], (team, count) -> count + 1);
        System.out.println("Árbitro: decisión final #" + foulNumber + " "
                + fields[3].toLowerCase() + "; cobro contra " + fields[2] + ".");
        state = foulNumber < FOUL_COUNT ? State.WAIT_BETWEEN_FOULS : State.SEND_FOUL;
        return true;
    }

    private String randomTeam() {
        return ThreadLocalRandom.current().nextBoolean() ? firstTeam : secondTeam;
    }

    private void printSummary() {
        System.out.println();
        System.out.println("=== Resumen del partido ===");
        System.out.println("Equipos amigos de referencia: "
                + String.join(", ", TeamRegistry.getFriendTeamNames()));
        System.out.println("Equipos del partido: " + firstTeam + " ("
                + (firstIsFriend ? "amigo" : "no amigo") + "), "
                + secondTeam + " ("
                + (secondIsFriend ? "amigo" : "no amigo") + ")");
        System.out.println("Faltas cometidas aleatoriamente:");
        System.out.println("  " + firstTeam + ": " + committedFouls.get(firstTeam));
        System.out.println("  " + secondTeam + ": " + committedFouls.get(secondTeam));
        System.out.println("Faltas cobradas en contra de cada equipo:");
        System.out.println("  " + firstTeam + ": " + calledAgainst.get(firstTeam));
        System.out.println("  " + secondTeam + ": " + calledAgainst.get(secondTeam));
        System.out.println("Total de faltas revisadas: " + foulNumber);
    }

    @Override
    protected void takeDown() {
        AgentServices.deregister(this);
    }
}
