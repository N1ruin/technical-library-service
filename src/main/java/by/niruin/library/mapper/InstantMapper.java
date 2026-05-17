package by.niruin.library.mapper;

import org.mapstruct.Mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface InstantMapper {
    default Instant map(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return dateTime.toInstant(ZoneOffset.UTC);
    }
}
