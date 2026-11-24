package pe.edu.fisi.AgentProject;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.util.leap.Properties;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;

public final class App {
    private static final String REFEREE_MODE = "referee";
    private static final String WORKERS_MODE = "workers";
    private static final String GUI_MODE = "gui";
    private static final String DEFAULT_MAIN_PORT = "1099";
    private static final int CONNECT_ATTEMPTS = 30;
    private static final long CONNECT_RETRY_DELAY_MS = 1000;

    private App() {
    }

    public static void main(String[] args) {
        if (args.length == 3 && REFEREE_MODE.equalsIgnoreCase(args[0])) {
            startReferee(args[1], args[2]);
            return;
        }
        if (args.length == 1 && WORKERS_MODE.equalsIgnoreCase(args[0])) {
            startWorkers();
            return;
        }
        if (args.length == 3 && GUI_MODE.equalsIgnoreCase(args[0])) {
            startGuiDemo(args[1], args[2]);
            return;
        }
        throw new IllegalArgumentException(
                "Uso: referee <equipo-1> <equipo-2> | workers | gui <equipo-1> <equipo-2>");
    }

    private static void startReferee(String firstTeam, String secondTeam) {
        firstTeam = TeamRegistry.normalizeTeamName(firstTeam);
        secondTeam = TeamRegistry.normalizeTeamName(secondTeam);
        TeamRegistry.validateMatch(firstTeam, secondTeam);

        Properties properties = createProfileProperties(true, false);
        AgentContainer container = Runtime.instance()
                .createMainContainer(new ProfileImpl(properties));
        try {
            container.acceptNewAgent(
                    "Agente1-Arbitro",
                    new RefereeAgent(firstTeam, secondTeam)).start();
        } catch (StaleProxyException exception) {
            throw new IllegalStateException("No se pudo iniciar el agente árbitro.", exception);
        }
    }

    private static void startGuiDemo(String firstTeam, String secondTeam) {
        firstTeam = TeamRegistry.normalizeTeamName(firstTeam);
        secondTeam = TeamRegistry.normalizeTeamName(secondTeam);
        TeamRegistry.validateMatch(firstTeam, secondTeam);

        AgentContainer container = Runtime.instance().createMainContainer(
                new ProfileImpl(createProfileProperties(true, true)));
        try {
            container.acceptNewAgent("Agente2-Operador", new OperatorAgent()).start();
            container.acceptNewAgent("Agente3-FPF", new FederationAgent()).start();
            container.acceptNewAgent("Agente4-VAR", new VarAgent()).start();
            container.acceptNewAgent(
                    "Agente1-Arbitro",
                    new RefereeAgent(firstTeam, secondTeam)).start();
        } catch (StaleProxyException exception) {
            throw new IllegalStateException(
                    "No se pudieron iniciar los agentes para la demostración GUI.", exception);
        }
    }

    private static void startWorkers() {
        String mainHost = System.getenv("JADE_MAIN_HOST");
        if (mainHost == null || mainHost.trim().isEmpty()) {
            throw new IllegalStateException("Debe configurarse la variable JADE_MAIN_HOST.");
        }

        String mainPort = environmentOrDefault("JADE_MAIN_PORT", DEFAULT_MAIN_PORT);
        Properties properties = createProfileProperties(false, false);
        properties.setProperty(Profile.MAIN_HOST, mainHost.trim());
        properties.setProperty(Profile.MAIN_PORT, mainPort);

        AgentContainer container = connectToMainContainer(properties);
        try {
            container.acceptNewAgent("Agente3-FPF", new FederationAgent()).start();
            container.acceptNewAgent("Agente4-VAR", new VarAgent()).start();
            container.acceptNewAgent("Agente2-Operador", new OperatorAgent()).start();
        } catch (StaleProxyException exception) {
            throw new IllegalStateException("No se pudieron iniciar los agentes de apoyo.", exception);
        }
    }

    private static Properties createProfileProperties(boolean main, boolean gui) {
        Properties properties = new Properties();
        properties.setProperty(Profile.GUI, Boolean.toString(gui));
        properties.setProperty(Profile.MAIN, Boolean.toString(main));
        properties.setProperty(Profile.MAIN_PORT,
                environmentOrDefault("JADE_MAIN_PORT", DEFAULT_MAIN_PORT));
        return properties;
    }

    private static AgentContainer connectToMainContainer(Properties properties) {
        Runtime runtime = Runtime.instance();
        for (int attempt = 1; attempt <= CONNECT_ATTEMPTS; attempt++) {
            AgentContainer container = runtime.createAgentContainer(new ProfileImpl(properties));
            if (container != null) {
                return container;
            }
            if (attempt < CONNECT_ATTEMPTS) {
                System.out.println("Esperando la plataforma JADE (" + attempt + "/"
                        + CONNECT_ATTEMPTS + ").");
                try {
                    Thread.sleep(CONNECT_RETRY_DELAY_MS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                            "Se interrumpió la conexión con la plataforma JADE.", exception);
                }
            }
        }
        throw new IllegalStateException(
                "No fue posible conectarse a la plataforma JADE en "
                        + properties.getProperty(Profile.MAIN_HOST) + ":"
                        + properties.getProperty(Profile.MAIN_PORT) + ".");
    }

    private static String environmentOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }
}
