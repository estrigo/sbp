import kz.spt.app.SptApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.web.WebAppConfiguration
import spock.lang.Specification

@SpringBootTest(classes = [ SptApplication.class ])
@WebAppConfiguration
class ApplicationStartTest extends Specification {

    @Autowired
    private ApplicationContext context;

    def "Verify context loads"() {
        expect:
        Objects.nonNull(context)
    }
}
