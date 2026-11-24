package pe.edu.fisi.AgentProject;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class OperatorAgent extends Agent {
    private enum State {
        WAIT_MATCH,
        WAIT_FEDERATION,
        REVIEWING
    }

    private State state = State.WAIT_MATCH;
    private AID referee;
    private String firstTeam;
    private String secondTeam;
    private boolean firstIsFriend;
    private boolean secondIsFriend;
    private String pendingFoul;

    @Override
    protected void setup() {
        AgentServices.register(this, AgentServices.OPERATOR);
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage message = myAgent.receive(
                        MessageTemplate.MatchConversationId(MatchProtocol.CONVERSATION_ID));
                if (message == null) {
                    block();
                    return;
                }
                process(message);
            }
        });
    }

    private void process(ACLMessage message) {
        try {
            switch (state) {
                case WAIT_MATCH:
                    receiveMatch(message);
                    break;
                case WAIT_FEDERATION:
                    receiveTeams(message);
                    break;
                case REVIEWING:
                    receiveVarDecision(message);
                    break;
                default:
                    throw new IllegalStateException("Estado de revisión inesperado.");
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            ACLMessage failure = message.createReply();
            failure.setPerformative(ACLMessage.FAILURE);
            failure.setContent("ERROR|" + exception.getMessage());
            send(failure);
        }
    }

    private void receiveMatch(ACLMessage message) {
        String[] fields = MatchProtocol.fields(message.getContent(), 5);
        if (!"MATCH".equals(fields[0]) || message.getPerformative() != ACLMessage.REQUEST) {
            throw new IllegalArgumentException("Se esperaba la configuración del partido.");
        }
        referee = message.getSender();
        AID federation = AgentServices.find(this, AgentServices.FEDERATION);
        if (federation == null) {
            throw new IllegalStateException("El servicio de la FPF no está disponible.");
        }
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.addReceiver(federation);
        request.setConversationId(MatchProtocol.CONVERSATION_ID);
        request.setContent("MATCH|" + fields[1] + "|" + fields[2]);
        send(request);
        state = State.WAIT_FEDERATION;
    }

    private void receiveTeams(ACLMessage message) {
        String[] fields = MatchProtocol.fields(message.getContent(), 5);
        if (!"TEAMS".equals(fields[0]) || message.getPerformative() != ACLMessage.INFORM) {
            throw new IllegalArgumentException(
                    fields.length > 1 ? fields[1] : "La FPF no pudo validar los equipos.");
        }
        firstTeam = fields[1];
        firstIsFriend = MatchProtocol.isFriendStatus(fields[2]);
        secondTeam = fields[3];
        secondIsFriend = MatchProtocol.isFriendStatus(fields[4]);
        ACLMessage ready = new ACLMessage(ACLMessage.INFORM);
        ready.addReceiver(referee);
        ready.setConversationId(MatchProtocol.CONVERSATION_ID);
        ready.setContent("MATCH_READY|" + firstTeam + "|"
                + MatchProtocol.teamStatus(firstIsFriend) + "|" + secondTeam + "|"
                + MatchProtocol.teamStatus(secondIsFriend));
        send(ready);
        state = State.REVIEWING;
        System.out.println("Operador: " + firstTeam + " ("
                + (firstIsFriend ? "amigo" : "no amigo") + "), "
                + secondTeam + " ("
                + (secondIsFriend ? "amigo" : "no amigo") + ").");
    }

    private void receiveVarDecision(ACLMessage message) {
        String[] fields = message.getContent().split("\\|", -1);
        if ("FOUL".equals(fields[0]) && message.getPerformative() == ACLMessage.REQUEST) {
            fields = MatchProtocol.fields(message.getContent(), 4);
            reviewFoul(message, fields);
            return;
        }
        if ("VAR_RESULT".equals(fields[0]) && message.getPerformative() == ACLMessage.INFORM) {
            fields = MatchProtocol.fields(message.getContent(), 3);
            if (!fields[1].equals(pendingFoul)) {
                throw new IllegalStateException("La respuesta VAR no corresponde a la falta pendiente.");
            }
            sendResult(fields[1], fields[2], true);
            pendingFoul = null;
            return;
        }
        throw new IllegalArgumentException("Mensaje de revisión inesperado: " + fields[0]);
    }

    private void reviewFoul(ACLMessage message, String[] fields) {
        if (pendingFoul != null) {
            throw new IllegalStateException("Hay una revisión VAR pendiente.");
        }
        String foulNumber = fields[1];
        String calledAgainst = fields[3];
        if (!MatchRules.shouldReview(
                calledAgainst, firstTeam, firstIsFriend, secondTeam, secondIsFriend)) {
            sendResult(foulNumber, calledAgainst, false);
            System.out.println("Operador: falta #" + foulNumber + " revisada; decisión confirmada.");
            return;
        }
        pendingFoul = foulNumber;
        AID var = AgentServices.find(this, AgentServices.VAR);
        if (var == null) {
            throw new IllegalStateException("El servicio del VAR no está disponible.");
        }
        ACLMessage review = new ACLMessage(ACLMessage.REQUEST);
        review.addReceiver(var);
        review.setConversationId(MatchProtocol.CONVERSATION_ID);
        review.setContent("REVIEW|" + foulNumber + "|" + firstTeam + "|"
                + MatchProtocol.teamStatus(firstIsFriend) + "|" + secondTeam + "|"
                + MatchProtocol.teamStatus(secondIsFriend) + "|" + calledAgainst);
        send(review);
        System.out.println("Operador: falta #" + foulNumber + " enviada al VAR.");
    }

    private void sendResult(String foulNumber, String calledAgainst, boolean corrected) {
        ACLMessage result = new ACLMessage(ACLMessage.INFORM);
        result.addReceiver(referee);
        result.setConversationId(MatchProtocol.CONVERSATION_ID);
        result.setContent("RESULT|" + foulNumber + "|" + calledAgainst + "|"
                + (corrected ? "CORREGIDA" : "CONFIRMADA"));
        send(result);
    }

    @Override
    protected void takeDown() {
        AgentServices.deregister(this);
    }
}
