package br.com.documentshield.security;

import br.com.documentshield.DocumentShieldApplication;
import br.com.documentshield.service.ApiKeyManagementService;
import br.com.documentshield.service.ApiKeyManagementService.ApiKeyView;
import br.com.documentshield.service.ApiKeyManagementService.CreatedApiKey;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;

/** Ferramenta administrativa local. Segredos são exibidos uma única vez no stdout e nunca em logs. */
public final class ApiKeyProvisioningTool {
    private ApiKeyProvisioningTool() {}

    public static void main(String[] args) {
        int exitCode = 0;
        System.setProperty("debug", "false");
        System.setProperty("logging.level.root", "WARN");
        System.setProperty("logging.level.br.com.documentshield", "WARN");
        System.setProperty("logging.level.org.hibernate", "WARN");
        System.setProperty("spring.main.banner-mode", "off");
        SpringApplication application = new SpringApplication(DocumentShieldApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setLogStartupInfo(false);
        try (ConfigurableApplicationContext context = application.run()) {
            run(context.getBean(ApiKeyManagementService.class), args);
        } catch (IllegalArgumentException exception) {
            System.err.println("Erro: " + exception.getMessage());
            printUsage();
            exitCode = 2;
        } catch (Exception exception) {
            System.err.println("Erro: não foi possível concluir a operação administrativa.");
            exitCode = 1;
        }
        if (exitCode != 0) System.exit(exitCode);
    }

    static void run(ApiKeyManagementService service, String[] args) {
        if (args.length == 0) throw new IllegalArgumentException("Informe um comando.");
        String command = args[0].toLowerCase(Locale.ROOT);
        Map<String, String> options = options(Arrays.copyOfRange(args, 1, args.length));
        switch (command) {
            case "create" -> printCreated(service.create(required(options, "name"),
                    instant(options.get("expires-at"), false),
                    options.getOrDefault("scope", ApiKeyManagementService.DOCUMENT_ANALYZE_SCOPE),
                    integer(options.get("rate-limit"))));
            case "list" -> printList(service.list());
            case "revoke" -> printRevoked(service.revoke(uuid(required(options, "id"))));
            case "rotate" -> {
                boolean replacesExpiration = options.containsKey("expires-at");
                printCreated(service.rotate(uuid(required(options, "id")),
                        instant(options.get("expires-at"), true), replacesExpiration));
            }
            default -> throw new IllegalArgumentException("Comando desconhecido: " + command);
        }
    }

    private static Map<String, String> options(String[] args) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String argument : args) {
            if (!argument.startsWith("--") || !argument.contains("=")) {
                throw new IllegalArgumentException("Use opções no formato --nome=valor.");
            }
            int separator = argument.indexOf('=');
            String key = argument.substring(2, separator);
            String value = argument.substring(separator + 1);
            if (key.isBlank() || value.isBlank() || result.put(key, value) != null) {
                throw new IllegalArgumentException("Opção inválida ou repetida: " + argument);
            }
        }
        return result;
    }

    private static String required(Map<String, String> options, String key) {
        String value = options.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Informe --" + key + ".");
        return value;
    }

    private static Instant instant(String value, boolean acceptsNever) {
        if (value == null || acceptsNever && "never".equalsIgnoreCase(value)) return null;
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Data inválida. Use ISO-8601, por exemplo 2027-12-31T23:59:59Z.");
        }
    }

    private static Integer integer(String value) {
        if (value == null) return null;
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("O rate-limit deve ser um número inteiro.");
        }
    }

    private static UUID uuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("O id deve ser um UUID válido.");
        }
    }

    private static void printCreated(CreatedApiKey created) {
        System.out.println("API Key criada com sucesso.");
        System.out.println("ID: " + created.credential().id());
        System.out.println("Integração: " + created.credential().name());
        System.out.println("Identificador público: " + created.credential().publicId());
        System.out.println("Token: " + created.token());
        System.out.println("ATENÇÃO: copie este token agora. Ele não poderá ser recuperado posteriormente.");
    }

    private static void printList(List<ApiKeyView> credentials) {
        if (credentials.isEmpty()) {
            System.out.println("Nenhuma integração cadastrada.");
            return;
        }
        for (ApiKeyView value : credentials) {
            System.out.printf(Locale.ROOT,
                    "ID=%s | integração=%s | identificador=%s | status=%s | escopo=%s | criada=%s | expira=%s | último uso=%s | revogada=%s | limite/min=%s%n",
                    value.id(), value.name(), value.publicId(), status(value), value.scope(),
                    value.createdAt(), display(value.expiresAt()), display(value.lastUsedAt()),
                    display(value.revokedAt()), display(value.requestsPerMinute()));
        }
    }

    private static void printRevoked(ApiKeyView value) {
        System.out.println("API Key revogada com sucesso.");
        System.out.println("ID: " + value.id());
        System.out.println("Integração: " + value.name());
        System.out.println("Revogada em: " + value.revokedAt());
    }

    private static String status(ApiKeyView value) {
        if (value.revokedAt() != null || !value.active()) return "REVOGADA";
        if (value.expiresAt() != null && !value.expiresAt().isAfter(Instant.now())) return "EXPIRADA";
        return "ATIVA";
    }

    private static String display(Object value) {
        return value == null ? "-" : value.toString();
    }

    private static void printUsage() {
        System.err.println("Comandos:");
        System.err.println("  create --name=Nome [--expires-at=ISO-8601] [--rate-limit=N]");
        System.err.println("  list");
        System.err.println("  revoke --id=UUID");
        System.err.println("  rotate --id=UUID [--expires-at=ISO-8601|never]");
    }
}
