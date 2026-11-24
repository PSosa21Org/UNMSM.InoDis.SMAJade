package pe.edu.fisi.AgentProject;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

final class AgentServices {
    static final String REFEREE = "var-peruano.referee";
    static final String OPERATOR = "var-peruano.operator";
    static final String FEDERATION = "var-peruano.federation";
    static final String VAR = "var-peruano.var";

    private AgentServices() {
    }

    static void register(Agent agent, String serviceType) {
        DFAgentDescription agentDescription = new DFAgentDescription();
        agentDescription.setName(agent.getAID());

        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType(serviceType);
        serviceDescription.setName(serviceType);
        agentDescription.addServices(serviceDescription);

        try {
            DFService.register(agent, agentDescription);
        } catch (FIPAException exception) {
            throw new IllegalStateException(
                    "No se pudo registrar el servicio JADE " + serviceType + ".", exception);
        }
    }

    static AID find(Agent agent, String serviceType) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType(serviceType);
        template.addServices(serviceDescription);

        try {
            DFAgentDescription[] providers = DFService.search(agent, template);
            return providers.length == 0 ? null : providers[0].getName();
        } catch (FIPAException exception) {
            throw new IllegalStateException(
                    "No se pudo buscar el servicio JADE " + serviceType + ".", exception);
        }
    }

    static void deregister(Agent agent) {
        try {
            DFService.deregister(agent);
        } catch (FIPAException exception) {
            throw new IllegalStateException(
                    "No se pudo retirar el servicio JADE de " + agent.getLocalName() + ".",
                    exception);
        }
    }
}
