package me.reidj.forest.protocol;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import ru.cristalix.core.network.CorePackage;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
public class BulkSaveUserPackage extends CorePackage {

    // request
    private final List<SaveUserPackage> packages;

    // no response
}
