package pe.edu.fisi.AgentProject;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class FederationAgent extends Agent {
    private final String[] friendTeams = TeamRegistry.getFriendTeamNames();

    @Override
    protected void setup() {
        AgentServices.register(this, AgentServices.FEDERATION);
        System.out.println(getLocalName() + " disponible. Equipos amigos: "
                + String.join(", ", friendTeams));
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                MessageTemplate template = MessageTemplate.MatchConversationId(
                        MatchProtocol.CONVERSATION_ID);
                ACLMessage request = myAgent.receive(template);
                if (request == null) {
                    block();
                    return;
                }

                ACLMessage reply = request.createReply();
                try {
                    String[] message = MatchProtocol.fields(request.getContent(), 3);
                    if (!"MATCH".equals(message[0])) {
                        throw new IllegalArgumentException("Solicitud desconocida.");
                    }
                    String first = TeamRegistry.normalizeTeamName(message[1]);
                    String second = TeamRegistry.normalizeTeamName(message[2]);
                    TeamRegistry.validateMatch(first, second);
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("TEAMS|" + first + "|"
                            + MatchProtocol.teamStatus(TeamRegistry.isFriend(first)) + "|"
                            + second + "|"
                            + MatchProtocol.teamStatus(TeamRegistry.isFriend(second)));
                } catch (IllegalArgumentException exception) {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("ERROR|" + exception.getMessage());
                }
                myAgent.send(reply);
            }
        });
    }

    @Override
    protected void takeDown() {
        AgentServices.deregister(this);
    }
}
