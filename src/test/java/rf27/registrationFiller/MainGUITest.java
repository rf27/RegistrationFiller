package rf27.registrationFiller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MainGUITest {
	
	private MainGUI mainGUI;
	
	@BeforeEach
	public void setUp() {
		mainGUI = new MainGUI("Test Registration Filler v1.1.0");
	}
	
	@SuppressWarnings("static-access")
	@Test
	public void testValidateAsNumber() {
		String good = "1234567890";
		String bad = "123456abc";
		try {
			Assertions.assertTrue(mainGUI.validateAsNumber(good));
			Assertions.assertFalse(mainGUI.validateAsNumber(bad));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
