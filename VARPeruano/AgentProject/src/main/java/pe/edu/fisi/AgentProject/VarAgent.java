package pe.edu.fisi.AgentProject;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class VarAgent extends Agent {
    @Override
    protected void setup() {
        AgentServices.register(this, AgentServices.VAR);
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage request = myAgent.receive(
                        MessageTemplate.MatchConversationId(MatchProtocol.CONVERSATION_ID));
                if (request == null) {
                    block();
                    return;
                }
                review(request);
            }
        });
    }

    private void review(ACLMessage request) {
        String[] fields = MatchProtocol.fields(request.getContent(), 7);
        if (!"REVIEW".equals(fields[0]) || request.getPerformative() != ACLMessage.REQUEST) {
            throw new IllegalArgumentException("Solicitud VAR con formato no válido.");
        }
        boolean firstIsFriend = MatchProtocol.isFriendStatus(fields[3]);
        boolean secondIsFriend = MatchProtocol.isFriendStatus(fields[5]);
        String correctedCall = MatchRules.correctedTeamChargedWithFoul(
                fields[2], firstIsFriend, fields[4], secondIsFriend);
        ACLMessage response = request.createReply();
        response.setPerformative(ACLMessage.INFORM);
        response.setContent("VAR_RESULT|" + fields[1] + "|" + correctedCall);
        send(response);
        if (firstIsFriend != secondIsFriend) {
            String friendTeam = MatchRules.friendTeam(
                    fields[2], firstIsFriend, fields[4], secondIsFriend);
            System.out.println("VAR: falta #" + fields[1] + "; decisión corregida a favor de "
                    + friendTeam + " (cobro contra " + correctedCall + ").");
        } else {
            System.out.println("VAR: falta #" + fields[1]
                    + "; decisión aleatoria, cobro contra " + correctedCall + ".");
        }
    }

    @Override
    protected void takeDown() {
        AgentServices.deregister(this);
    }
}
