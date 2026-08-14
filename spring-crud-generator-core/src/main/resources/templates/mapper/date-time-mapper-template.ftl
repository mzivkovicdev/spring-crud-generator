import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.mapstruct.Mapper;

@Mapper
public interface DateTimeMapper {

    default Instant mapToInstant(final OffsetDateTime odt) {
        return odt == null ? null : odt.toInstant();
    }

    default OffsetDateTime map(final Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    default OffsetDateTime map(final LocalDateTime ldt) {
        return ldt == null ? null : ldt.atOffset(ZoneOffset.UTC);
    }

    default LocalDateTime mapToLocalDateTime(final OffsetDateTime odt) {
        return odt == null ? null : odt.toLocalDateTime();
    }
}
