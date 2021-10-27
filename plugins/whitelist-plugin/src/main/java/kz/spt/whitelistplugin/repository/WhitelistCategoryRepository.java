package kz.spt.whitelistplugin.repository;

import kz.spt.whitelistplugin.model.WhitelistCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface WhitelistCategoryRepository extends JpaRepository<WhitelistCategory, Long> {

    @Query("from WhitelistCategory wc where wc.id = ?1")
    WhitelistCategory getById(Long categoryiId);
}
