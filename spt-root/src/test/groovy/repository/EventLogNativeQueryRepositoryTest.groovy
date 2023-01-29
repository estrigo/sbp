package repository

import kz.spt.app.SptApplication
import kz.spt.app.repository.EventLogNativeQueryRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.web.WebAppConfiguration
import spock.lang.Specification

@SpringBootTest(classes = [SptApplication.class])
@WebAppConfiguration
class EventLogNativeQueryRepositoryTest extends Specification {

    @Autowired
    private EventLogNativeQueryRepository repository

    def "test repo.findAllWithFiltersForExcelReport does not fails"() {
        when:
        def all = repository.findAllWithFiltersForExcelReport(
                "dummy",
                "2021-01-01 00:00:00",
                "2025-01-01 00:00:00",
                "")

        then:
        all.empty
    }
}
