package indi.bookmarkx.service.relocation;

import java.util.Optional;

public interface BookmarkRelocationStrategy {

    String getName();

    Optional<RelocationResult> relocate(RelocationContext context);
}
