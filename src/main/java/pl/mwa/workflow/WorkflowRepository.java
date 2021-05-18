package pl.mwa.workflow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long>{

}
