import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;


public class TestMDC {

	public static void main(String[] args) {
		TestMDC testMDC = new TestMDC();
		for (int i=0;i<99;i++) {
			testMDC.log("bNet");
			testMDC.log("iPortal");
		}
	}
	
	public void log(String applicationName) {
		Logger logger = LoggerFactory.getLogger(TestMDC.class);
		MDC.put("application", applicationName); // do this right at the start, when request is received
		logger.info("logging for application {}", applicationName);
		MDC.remove("application"); //has to be done; do it in finally
	}
}
