package kz.spt.lib.model;

import org.springframework.data.jpa.domain.Specification;

public class BlacklistSpecification {

    public static Specification<Blacklist> likeNumber(String plateNumber) {
        return (root, query, builder) -> builder.like(root.get(Blacklist_.PLATE_NUMBER), "%" + plateNumber.toUpperCase() + "%");
    }
}
