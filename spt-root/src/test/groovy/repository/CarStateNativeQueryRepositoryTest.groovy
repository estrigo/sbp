package repository

import kz.spt.app.SptApplication
import kz.spt.app.repository.CarStateNativeQueryRepository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.web.WebAppConfiguration
import spock.lang.Specification

@SpringBootTest(classes = [SptApplication.class])
@WebAppConfiguration
class CarStateNativeQueryRepositoryTest extends Specification {

    @Autowired
    private CarStateNativeQueryRepository repository

    def "test repo.findAllWithFiltersForExcelReport does not fails"() {
        when:
        def all = repository.findAllWithFiltersForExcelReport(
                "dummy",
                "2021-01-01 00:00:00",
                "2025-01-01 00:00:00",
                "",
                "",
                "1200")

        then:
        all.empty
    }
}
