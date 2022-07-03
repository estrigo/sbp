package kz.spt.prkstatusplugin.model.specs;

import kz.spt.prkstatusplugin.enums.SoftwareType;
import kz.spt.prkstatusplugin.model.ParkomatUpdate;
import kz.spt.prkstatusplugin.model.ParkomatUpdate_;
import org.springframework.data.jpa.domain.Specification;

public class ParkomatUpdateSpec {

    public static Specification<ParkomatUpdate> updateType(SoftwareType type) {
        return (root, query, builder) -> builder.equal(root.get(ParkomatUpdate_.TYPE), type);
    }
}
