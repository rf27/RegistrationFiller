package rf27.registrationFiller;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cybozu.labs.langdetect.LangDetectException;

/**
 * Unit test for FormFiller.java.
 */
public class FormFillerTest {
	
	private FormFiller formFiller;
	
    @BeforeEach
    void setUp() throws Exception {
    	Assertions.assertDoesNotThrow(() -> {
    		formFiller = new FormFiller("Shepherd's Hope Online Patient Encounter Form(1-2).xlsx");
    	}); //  ensure that downloading an .xlsx file will not throw
    	formFiller = new FormFiller("Shepherd's Hope Online Patient Encounter Form(1-2)");
    }
    
    @SuppressWarnings("static-access")
	@Test
    void testInitialize() {
    	Assertions.assertDoesNotThrow(() -> {
    		formFiller.initialize();
    	});
    	try {
			formFiller.initialize(); // assume no throw for initialize
			Assertions.assertEquals("sp", formFiller.detectLanguage("dolor de cabeza"));
		} catch (LangDetectException e) {} catch (IOException e) {}
    	
    }
    
    @SuppressWarnings("static-access")
	@Test
    void testPhoneNumberFormatter() {    	
    	Assertions.assertSame("(123) 456-7890", formFiller.phoneNumberFormatter("abd1234567890"));
    	Assertions.assertSame("(123) 456-7890x123", formFiller.phoneNumberFormatter("1234567890123"));
    	Assertions.assertSame("12345", formFiller.phoneNumberFormatter("12345"));
    }
    
    @SuppressWarnings("static-access")
    @Test
    void testCleanupText() {
    	String expected = "Hello World";
    	
    	Assertions.assertSame(expected, formFiller.cleanupText("  hello World  "));
    	Assertions.assertSame(expected, formFiller.cleanupText("hello    world"));
    	Assertions.assertSame(expected, formFiller.cleanupText("Hello  world   "));
    }
    
    @SuppressWarnings("static-access")
    @Test
    void testResizeFont() {
    	Assertions.assertEquals(0.25f, formFiller.resizeFont(14, 7, 0.5f)); // ctl = 2*dtl
    	Assertions.assertEquals(0.25f, formFiller.resizeFont(16, 7, 0.5f)); // ctl > 2*dtl
    	Assertions.assertEquals(0.1f, formFiller.resizeFont(9, 5, 0.5f)); // dtl < ctl < 2*dtl
    	Assertions.assertEquals(0.5f, formFiller.resizeFont(7, 7, 0.5f)); // ctl = dtl
    	Assertions.assertEquals(0.5f, formFiller.resizeFont(2, 5, 0.5f)); // ctl < dtl
    }
    
    @SuppressWarnings("static-access")
    @Test
    void testRemapLanguage() {
    	Assertions.assertEquals("Spanish", formFiller.remapLanguage("sp"));
    	Assertions.assertEquals("Italian", formFiller.remapLanguage("it"));
    }
    
    @SuppressWarnings("static-access")
    @Test
    void testTranslate() {
    	try {
			Assertions.assertEquals("dolor de cabeza", formFiller.translate("sp", null, null));
		} catch (IOException e) {}
    }
}
