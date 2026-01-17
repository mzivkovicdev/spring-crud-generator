import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;

@Configuration
public class GraphQlDateTimeScalarConfig {

    private static final ZoneOffset API_ZONE = ZoneOffset.UTC;

    @Bean
    GraphQLScalarType dateTimeScalar() {
        return GraphQLScalarType.newScalar()
                .name("DateTime")
                .description("ISO-8601 date/time. Serializes Instant, LocalDate, LocalDateTime as String.")
                .coercing(new Coercing<Object, String>() {

                    @Override
                    public String serialize(final Object dataFetcherResult) {
                        if (Objects.isNull(dataFetcherResult))
                            return null;

                        if (dataFetcherResult instanceof Instant i) {
                            return i.toString();
                        }
                        if (dataFetcherResult instanceof OffsetDateTime odt) {
                            return odt.toInstant().toString();
                        }
                        if (dataFetcherResult instanceof ZonedDateTime zdt) {
                            return zdt.toInstant().toString();
                        }
                        if (dataFetcherResult instanceof LocalDate ld) {
                            return ld.toString();
                        }
                        if (dataFetcherResult instanceof LocalDateTime ldt) {
                            return ldt.atOffset(API_ZONE).toInstant().toString();
                        }

                        throw new IllegalArgumentException(String.format(
                                "DateTime scalar expected Instant/LocalDate/LocalDateTime but was %s",
                                dataFetcherResult.getClass()
                        ));
                    }

                    @Override
                    public Object parseValue(final Object input) {
                        if (Objects.isNull(input))
                            return null;
                        return parseFromString(input.toString());
                    }

                    @Override
                    public Object parseLiteral(final Object input) {
                        if (input instanceof StringValue sv) {
                            return parseFromString(sv.getValue());
                        }
                        throw new IllegalArgumentException("DateTime must be a String");
                    }

                    private Object parseFromString(final String s) {
                        try {
                            return Instant.parse(s);
                        } catch (final DateTimeParseException ignored) {
                        }
                        try {
                            return OffsetDateTime.parse(s).toInstant();
                        } catch (final DateTimeParseException ignored) {
                        }
                        try {
                            return LocalDate.parse(s);
                        } catch (final DateTimeParseException ignored) {
                        }
                        try {
                            return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        } catch (final DateTimeParseException ignored) {
                        }

                        throw new IllegalArgumentException(String.format("Invalid DateTime value: %s", s));
                    }
                })
                .build();
    }
}
