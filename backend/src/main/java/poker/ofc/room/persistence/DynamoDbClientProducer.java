package poker.ofc.room.persistence;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/**
 * Produces the DynamoDB client used by {@link RoomRepository}. Points at
 * DynamoDB Local when {@code ofc.dynamodb.endpoint-override} is set (local
 * dev and tests) and at real AWS DynamoDB otherwise (staging/production),
 * using the task's IAM role via the default credentials chain.
 */
@ApplicationScoped
public class DynamoDbClientProducer {

    @ConfigProperty(name = "ofc.dynamodb.endpoint-override")
    Optional<String> endpointOverride;

    @ConfigProperty(name = "ofc.dynamodb.region")
    String region;

    @Produces
    @Singleton
    @DefaultBean
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(region))
                .httpClientBuilder(ApacheHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(5))
                        .socketTimeout(Duration.ofSeconds(10)));

        if (endpointOverride.isPresent() && !endpointOverride.get().isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride.get()))
                    .credentialsProvider(localDevCredentials());
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }

    private AwsCredentialsProvider localDevCredentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local"));
    }

    @Produces
    @Singleton
    @DefaultBean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }
}
