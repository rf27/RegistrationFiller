/*
 * Description for encounterForm.java
 * - given an Excel spreadsheet exported from the Microsoft Form
 * 		"Shepherd's Hope Online Patient Encounter Form", 
 * - output the following PDFs for each patient entry line:
 * 		- Encounter Form
 * 		- 1032
 * 		- Hepatitis C Intake
 * 
 * JAR Dependencies:
 * - Apache Commons Collections 4.1
 * - Apache POI 3.17
 * - Apache POI Common 3.17
 * - Apache POI API Based on OPC and OOXML Schemas 3.17
 * - XmlBeans 2.6.0
 * - language-detection by shuyo, Copyright 2010-2014 Cybozu Labs, Inc. All rights reserved.
 * 		under Apache License Version 2.0: http://www.apache.org/licenses/LICENSE-2.0.txt
 * - PDFJet for Java
 * 		under evaluation license: https://pdfjet.com/java/download.html 
 */

package rf27.registrationFiller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.cybozu.labs.langdetect.Language;
import com.pdfjet.Color;
import com.pdfjet.CoreFont;
import com.pdfjet.Font;
import com.pdfjet.PDF;
import com.pdfjet.PDFobj;
import com.pdfjet.Page;

public class FormFiller implements Runnable {

	private static String fileName;
	private static Detector detector;
	public static int numPatients = 0;
	private static String logText = "<html>";
	private static boolean printedPDF;
	private static String[] addressLine = new String[3]; // will contain mailing address from data/config.txt and put in 1032 page
	
	private String[] patient; // iterating variable
	private int iterator; // iterating variable
	
	private static String outputFileName; // output variable
	
	private static ArrayList<String> outputFiles = new ArrayList<String>();

	public FormFiller(String inputFile) throws Exception {
		logText = "<html>";
		if (!MainGUI.initializedDetector) {
			initialize();
		}
		fileName = inputFile;
		numPatients = getNumLines(fileName)-1;
		for (int i = 0; i < numPatients; i++) {
			patient = readLine(fileName, i+1);
			iterator = i;
			this.run();
		}
	}

	public void initialize() throws LangDetectException, IOException {
		DetectorFactory.loadProfile("data/profiles.sm");
		MainGUI.initializedDetector = true;
		initConfig();
	}

	private static void generateForm(String[] patient, int i) throws Exception {
		int j;
		int startFile = MainGUI.getStartFile();
		int endFile = MainGUI.getEndFile();
		Font font, fontBold;

		if (i+1 < startFile || (endFile != -1 && i+1 > endFile)) {
			printedPDF = false;
			return;
		}
		
		logText += "Generating File: ";
		printedPDF = true;
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd");
		LocalDateTime now = LocalDateTime.now();
		String today = dtf.format(now);
		String[] firstNameParts = patient[1].split(" ");
		String firstName = "";
		for (j = 0; j < firstNameParts.length; j++) {
			if (firstName.length() + firstNameParts[j].length() >= 25) {
				break;
			}
			if (firstNameParts[j].length() > 0) {
				firstName += firstNameParts[j];	
			}
		}
		String[] lastNameParts = patient[0].split(" ");
		String lastName = "";
		for (j = 0; j < lastNameParts.length; j++) {
			if (lastName.length() + lastNameParts[j].length() >= 25) {
				break;
			}
			if (lastNameParts[j].length() > 0) {
				lastName += lastNameParts[j];
			}
		}
		outputFileName = today + "_pt" + (i+1) + "_" + lastName + "_" +  firstName + ".pdf"; // e.g. 2022_01_01_pt1_Loyd_Michael.pdf
		outputFiles.add(outputFileName);
		String fileDirectory = new File(new File(fileName).getAbsolutePath()).getParent();
		String[] date = today.split("_");

		PDF pdf = new PDF(new BufferedOutputStream(new FileOutputStream(fileDirectory + "/" + outputFileName)));
		BufferedInputStream bisEncounter = new BufferedInputStream(new FileInputStream("data/encounter.pdf"));
		if (patient[19].equals("Yes")) {
			bisEncounter.close();
			bisEncounter = new BufferedInputStream(new FileInputStream("data/encounter_hep.pdf"));
		}
		Map<Integer, PDFobj> objectsEncounter = pdf.read(bisEncounter);
		bisEncounter.close();

		List<PDFobj> pages = pdf.getPageObjects(objectsEncounter);
		Page pageEncounter = new Page(pdf, objectsEncounter, pages.get(0));
		font = pageEncounter.addFontResource(CoreFont.HELVETICA);
		fontBold = pageEncounter.addFontResource(CoreFont.HELVETICA_BOLD);
		pageEncounter.setBrushColor(Color.darkslategray);

		// date of visit
		fontBold.setSize(14f);
		pageEncounter.drawString(fontBold, date[1], 153f, 183f); // month
		pageEncounter.drawString(fontBold, date[2], 190f, 183f); // day
		pageEncounter.drawString(fontBold, date[0], 225f, 183f); //year

		font.setSize(resizeFont(patient[0].length(), 33, 12f)); // last name max 33 char.
		pageEncounter.drawString(font, patient[0], 93f, 223f); 		// last name
		font.setSize(resizeFont(patient[1].length(), 30, 12f)); // first name max 30 char.
		pageEncounter.drawString(font, patient[1], 326f, 223f); 	// first name
		font.setSize(12f);
		String[] middleNames = patient[2].split(" ");
		String middleInitials = "";
		for (j = 0; j < ((middleNames.length > 5) ? 5 : middleNames.length); j++) { // middle initials max 5 char.
			if (middleNames[j].length() > 0)
				middleInitials += "" + middleNames[j].charAt(0);
		}
		pageEncounter.drawString(font, middleInitials, 504f, 223f); 		// middle initial(s)
		font.setSize(12f);

		// sex
		switch (patient[3]) {
		case "Male/Masculino":
			checkBox(pageEncounter, 84.5f, 240f, 7f);
			break;
		case "Female/Femenino":
			checkBox(pageEncounter, 111f, 240f, 7f);
			break;
		}

		// DOB
		Calendar dob = new Calendar.Builder().setCalendarType("gregory").setDate(1900, Calendar.JANUARY, 1).setLenient(false).build();
		dob.add(Calendar.DATE, (int) Double.parseDouble(patient[4]) - 2);
		pageEncounter.drawString(font, "" + (dob.get(Calendar.MONTH)+1), 344f, 242f); // DOB month
		pageEncounter.drawString(font, "" + (dob.get(Calendar.DAY_OF_MONTH)), 420f, 242f); // DOB day
		pageEncounter.drawString(font, "" + dob.get(Calendar.YEAR), 500f, 242f); // DOB year

		// language
		String[] langsParts = patient[5].split(";");
		for (j = 0; j < langsParts.length; j++) {
			switch (langsParts[j]) {
			case "English/Inglés":
				checkBox(pageEncounter, 109f, 260.5f, 7f);
				break;
			case "Spanish/Español":
				checkBox(pageEncounter, 177f, 260.5f, 7f);
				break;
			case "Portuguese/Portugués":
				checkBox(pageEncounter, 235f, 260.5f, 7f);
				break;
			case "Creole":
				checkBox(pageEncounter, 306f, 260.5f, 7f);
				break;
			case "Vietnamese/Vietnamita":
				checkBox(pageEncounter, 353f, 260.5f, 7f);
				break;
			default:
				checkBox(pageEncounter, 437f, 260.5f, 7f);
				font.setSize(resizeFont(langsParts[j].length(), 19, 12f));
				pageEncounter.drawString(font, langsParts[j], 476f, 267f);
				break;
			}
		}

		// race
		String[] racesParts = patient[6].split(";");
		String otherRace = "";
		boolean hasOtherRace = false;
		for (j = 0; j < racesParts.length; j++) {
			switch(racesParts[j]) {
			case "White/Blanco":
				checkBox(pageEncounter, 109f, 281f, 7f);
				break;
			case "African American / Afro-Americano":
				checkBox(pageEncounter, 178f, 281f, 7f);
				break;
			case "Asian/Asiático":
				checkBox(pageEncounter, 306f, 281f, 7f);
				break;
			case "American Indian or Alaskan Native / Indio Americano o Nativo de Alaska":
				otherRace += (hasOtherRace) ? "; American Indian/Alaskan Native" : "American Indian/Alaskan Native";
				hasOtherRace = true;
				break;
			default:
				otherRace += (hasOtherRace) ? "; " + racesParts[j] : racesParts[j];
				hasOtherRace = true;
				break;
			}
		}
		if (!otherRace.equals("")) {
			font.setSize(resizeFont(otherRace.length(), 33, 12f));
			checkBox(pageEncounter, 352f, 281f, 7f);
			pageEncounter.drawString(font, otherRace, 393f, 288f);
		}
		font.setSize(12f);

		// ethnicity
		if (patient[7].equals("Hispanic or Latino / Hispano o Latino")) {
			checkBox(pageEncounter, 109f, 302f, 7f);
		} else {
			checkBox(pageEncounter, 237f, 302f, 7f);
		}

		font.setSize(resizeFont(patient[8].length(), 90, 12f));		// street address max length 90 char.
		pageEncounter.drawString(font, patient[8], 82f, 340f); 		// street address
		font.setSize(resizeFont(patient[9].length(), 27, 12f));		// city max length 27 char.
		pageEncounter.drawString(font, patient[9], 64f, 371f);		// city
		font.setSize(12f);
		pageEncounter.drawString(font, patient[10], 262.5f, 371f);	// state
		pageEncounter.drawString(font, patient[11], 444f, 371f);		// ZIP code

		// primary phone number
		String phoneFormatted = phoneNumberFormatter(patient[12]);
		pageEncounter.drawString(font, phoneFormatted, 114f, 401f);

		font.setSize(resizeFont(patient[13].length(), 43, 12f));
		pageEncounter.drawString(font, patient[13], 71f, 432f); // email

		// alternative phone number
		String altPhoneFormatted = phoneNumberFormatter(patient[14]);
		pageEncounter.drawString(font, altPhoneFormatted, 385f, 432f);

		font.setSize(resizeFont(patient[15].length(), 37, 12f)); // contact name max length 37 char.
		pageEncounter.drawString(font, patient[15], 372f, 401f); // name of contact

		// referred by
		String[] referredParts = patient[16].split(";");
		for (j = 0; j < referredParts.length; j++) {
			switch(referredParts[j]) {
			case "AdventHealth":
				checkBox(pageEncounter, 115f, 457.5f, 7f);
				break;
			case "Orlando Health":
				checkBox(pageEncounter, 194.5f, 457.5f, 7f);
				break;
			case "Doctor's Office/Oficina Médica":
				checkBox(pageEncounter, 279f, 457.5f, 7f);
				break;
			case "Health Dept. / Dept. de Salud":
				checkBox(pageEncounter, 339f, 457.5f, 7f);
				break;
			case "Community Clinic / Clinica de la Comunidad (PCAN)":
				checkBox(pageEncounter, 404f, 457.5f, 7f);
				break;
			case "Church/Iglesia":
				checkBox(pageEncounter, 524f, 457.5f, 7f);
				break;
			case "Central Florida Regional":
				checkBox(pageEncounter, 44f, 478f, 7f);
				break;
			case "Nemours":
				checkBox(pageEncounter, 147f, 478f, 7f);
				break;
			case "Shepherd's Hope staff/website / Personal o página de Internet de Shepherd's Hope":
				checkBox(pageEncounter, 202f, 478f, 7f);
				break;
			case "Friend/Amigo":
				checkBox(pageEncounter, 354.5f, 478f, 7f);
				break;
			default:
				checkBox(pageEncounter, 408f, 478f, 7f);
				pageEncounter.drawString(font, referredParts[j], 444f, 484f);
				break;
			}
		}

		font.setSize(resizeFont(patient[17].length(), 72, 14f));
		pageEncounter.drawString(font, patient[17], 142f, 508f); // reason for visit

		font.setSize(12f);
		String reasonLang = detectLanguage(patient[17]);
		if (!reasonLang.equals("en")) {
			String lang = remapLanguage(reasonLang);
			String reasonTrans = translate(reasonLang, "en", patient[17]);
			if (!reasonTrans.toLowerCase().equals(patient[17].toLowerCase())) {
				pageEncounter.drawString(font, "Visit Reason, Auto Translated from " + lang + ":", 38.5f, 741f);
				font.setSize(resizeFont(reasonTrans.length(), 100, 12f));
				pageEncounter.drawString(font, reasonTrans, 38.5f, 753f);
			}
		}

		font.setSize(12f); // reset font size

		String[] employedParts = patient[18].split(";");
		for (j = 0; j < employedParts.length; j++) {
			switch(employedParts[j]) {
			case "Employed/Empleado (please provide employed in \"Other\") / Por favor proporcione al empleador en \"otro\"":
				checkBox(pageEncounter, 141f, 533f, 7f);
				break;
			case "Unemployed/Desempleado":
				checkBox(pageEncounter, 285.5f, 533f, 7f);
				break;
			case "Child/Niño":
				checkBox(pageEncounter, 360f, 533f, 7f);
				break;
			case "Student/Estudiante":
				checkBox(pageEncounter, 399.5f, 533f, 7f);
				break;
			case "Retired/Retirado":
				checkBox(pageEncounter, 451f, 533f, 7f);
				break;
			case "Homeless/Desamparado":
				checkBox(pageEncounter, 505.5f, 533f, 7f);
				break;
			default:
				font.setSize(resizeFont(employedParts[j].length(), 16, 12f));
				pageEncounter.drawString(font, employedParts[j], 196f, 540.5f);
				break;
			}
		}
		font.setSize(12f);
		pageEncounter.complete(); // finish page 1 (encounter)

		/*
		 * START PRINTING 1032 (Page 2)
		 */
		Page page1032 = new Page(pdf, objectsEncounter, pages.get(1));
		page1032.setBrushColor(Color.darkslategray);

//		boolean nameDoubleOffset = false;
//		if (patient[i][0].length() + patient[i][1].length() + patient[i][2].length() > 46) {
//			nameDoubleOffset = true;
//			String firstLineName = "";
//			//TODO
//		}
		String fullName = cleanupText(patient[1] + " " + patient[2] + " " + patient[0]);
		page1032.drawString(font, fullName, 100f, 333f); // First Name Last Name
		// TODO address length fixing
		page1032.drawString(font, capitalize(patient[8]), 77f, 351f); // Address Line 1
		page1032.drawString(font, capitalize(patient[9]) + ", " + patient[10] + " " + patient[11], 77f, 364f); // Address Line 2
		page1032.drawString(font, phoneNumberFormatter(patient[12]), 62f, 405f); // phone number

		String dobText = "" + (dob.get(Calendar.MONTH)+1) + "/" + (dob.get(Calendar.DAY_OF_MONTH)) + "/" + dob.get(Calendar.YEAR);

		page1032.drawString(font, dobText, 440f, 333f); // DOB

		switch (patient[3]) { // Sex
		case "Male/Masculino":
			page1032.drawEllipse(424f, 350f, 20f, 10f);
			break;
		case "Female/Femenino":
			page1032.drawEllipse(480f, 350f, 20f, 10f);
			break;
		}

		for (j = 0; j < racesParts.length; j++) { // Race(s)
			switch(racesParts[j]) {
			case "White/Blanco":
				page1032.drawEllipse(424f, 366f, 20f, 10f);
				break;
			case "African American / Afro-Americano":
				page1032.drawEllipse(482f, 366f, 20f, 10f);
				break;
			case "Asian/Asiático":
				page1032.drawEllipse(549f, 366f, 26f, 10f);
				break;
			case "American Indian or Alaskan Native / Indio Americano o Nativo de Alaska":
				page1032.drawEllipse(464f, 383f, 58f, 10f);
				break;
			default:
				font.setSize(12f);
				page1032.drawString(font, "OTHER", 532f, 387f);
				page1032.drawEllipse(551f, 383f, 25f, 10f);
				break;
			}
		}

		switch(patient[7]) { // Ethnicity
		case "Hispanic or Latino / Hispano o Latino":
			page1032.drawEllipse(448f, 400f, 19f, 10f);
			break;
		case "NOT Hispanic or Latino / NO Hispano o Latino":
			page1032.drawEllipse(528f, 400f, 30f, 10f);
			break;
		}

		page1032.drawEllipse(330f, 418f, 44f, 10f); // 200% Poverty or Less
		page1032.drawEllipse(198f, 438f, 30f, 10f); // Medical Care 

		// Referred to Address
		font.setSize(11f);
		page1032.drawString(font, addressLine[0], 98f, 542f);
		page1032.drawString(font, addressLine[1], 98f, 554f);
		page1032.drawString(font, addressLine[2], 98f, 566f);

		checkBox(page1032, 308.3f, 709.95f, 6.2f); // "In lieu of signature..." check box

		page1032.complete();

		/*
		 * Start printing hepatitis C intake form
		 * Only printed if patient selects "Yes" for Q20
		 */
		if (patient[19].equals("Yes")) {
			Page pageHepC = new Page(pdf, objectsEncounter, pages.get(2));
			pageHepC.setBrushColor(Color.darkslategray);
			font.setSize(14f);

			// first name and last name are starred except for initials
			String firstStarred = ("" + removeExtraWhitespace(patient[1]).charAt(0)).toUpperCase();
			for (j = 0; j < patient[1].length()-1; j++) {
				firstStarred += "*";
			}
			String lastStarred = ("" + removeExtraWhitespace(patient[0]).charAt(0)).toUpperCase();
			for (j = 0; j < patient[0].length()-1; j++) {
				lastStarred += "*";
			}
			pageHepC.drawString(font, firstStarred + " " + lastStarred, 143.5f, 240f);

			// DOB
			pageHepC.drawString(font, "" + (dob.get(Calendar.MONTH)+1), 448f, 240f);
			pageHepC.drawString(font, "" + dob.get(Calendar.DATE), 486f, 240f);
			pageHepC.drawString(font, "" + dob.get(Calendar.YEAR), 510f, 240f);

			// symptoms
			String[] symptoms = patient[20].split(";");
			for (j = 0; j < symptoms.length; j++) {
				switch (symptoms[j].substring(0, 2)) {
				case "Ab": // Abdominal Pain / Dolor Abdominal
					checkBox(pageHepC, 73.4f, 301.5f, 6.9f);
					break;
				case "Vo": // Vomiting / Vómitos
					checkBox(pageHepC, 165.9f, 301.5f, 6.9f);
					break;
				case "Ja": // Jaundice / Ictericia (la piel o lo claro del ojo se le a puesto Amarillo)
					checkBox(pageHepC, 253.4f, 301.5f, 6.9f);
					break;
				case "Lo": // Loss of Appetite / Pérdida de Apetito
					checkBox(pageHepC, 333.1f, 301.5f, 6.9f);
					break;
				case "Fe": // Fever / Fiebre
					checkBox(pageHepC, 73.4f, 325.5f, 6.9f);
					break;
				case "Na": // Nausea / Náuseas
					checkBox(pageHepC, 165.4f, 325.5f, 6.9f);
					break;
				case "He": // Headache / Dolor de Cabeza
					checkBox(pageHepC, 253.4f, 325.5f, 6.9f);
					break;
				case "Di": // Diarrhea / Diarrea
					checkBox(pageHepC, 333.1f, 325.5f, 6.9f);
					break;
				case "No": // None of the Above / Ninguna de las anteriores
					checkBox(pageHepC, 426.8f, 301.5f, 6.9f);
					break;
				}
			}

			// hepatitis vaccination status
			String[] vacc = patient[21].split(";");
			for (j = 0; j < vacc.length; j++) {
				switch (vacc[j]) {
				case "Hepatitis A":
					checkBox(pageHepC, 239.5f, 394.5f, 6.9f);
					break;
				case "Hepatitis B":
					checkBox(pageHepC, 312.5f, 394.5f, 6.9f);
					break;
				case "Neither / Ninguna":
					checkBox(pageHepC, 385.5f, 394.5f, 6.9f);
					break;
				case "Unsure / Desconozco":
					checkBox(pageHepC, 441.4f, 394.5f, 6.9f);
					break;
				}
			}

			// hepatitis diagnosis history
			String[] cases = patient[22].split(";");
			for (j = 0; j < cases.length; j++) {
				switch (cases[j]) {
				case "Hepatitis A":
					checkBox(pageHepC, 193f, 418.5f, 6.9f);
					break;
				case "Hepatitis B":
					checkBox(pageHepC, 261.5f, 418.5f, 6.9f);
					break;
				case "Hepatitis C":
					checkBox(pageHepC, 329.7f, 418.5f, 6.9f);
					break;
				case "None of the Above / Ninguna de las anteriores":
					font.setSize(12f);
					pageHepC.drawString(font, "None", 474f, 425.5f);
					checkBox(pageHepC, 462f, 418f, 6.9f);
					break;
				case "Unsure / Desconozco":
					checkBox(pageHepC, 397.4f, 418.5f, 6.9f);
					break;
				}
			}

			// patient received blood transfusion pre-7/1982
			switch (patient[23].charAt(0)) {
			case 'Y':
				checkBox(pageHepC, 333f, 442.3f, 7f);
				break;
			case 'N':
				checkBox(pageHepC, 370.5f, 442.3f, 7f);
				break;
			case 'U':
				checkBox(pageHepC, 405.3f, 442.3f, 7f);
				break;
			}

			// patient works in healthcare setting with blood contact
			switch(patient[24].charAt(0)) {
			case 'Y':
				checkBox(pageHepC, 409.2f, 466.3f, 7f);
				break;
			case 'N':
				checkBox(pageHepC, 446.2f, 466.3f, 7f);
			}

			// hepatitis risk factors
			String[] risks = patient[25].split(";");
			for (j = 0; j < risks.length; j++) {
				switch(risks[j].substring(0, 2)) {
				case "IV": // IV Drug Use / Drogas inyectadas
					checkBox(pageHepC, 73.5f, 535.5f, 6.9f);
					break;
				case "In": // Incarceration in prison/jail / Encarcelamiento en prisión/cárcel
					checkBox(pageHepC, 73.5f, 559.5f, 6.9f);
					break;
				case "Ho": // Household contact of a person with hepatitis C / Contacto doméstico de una persona con Hepatitis C
					checkBox(pageHepC, 73.5f, 583.6f, 6.9f);
					break;
				case "Ta": // Tattoos / Tatuajes
					checkBox(pageHepC, 73.5f, 607.5f, 6.9f);
					break;
				case "Se": // Sex partner with known hepatitis C / Pareja sexual a largo plazo con Hepatitis C
					checkBox(pageHepC, 73.5f, 631.5f, 6.9f);
					break;
				case "Sh": // Shared needles for any reason /Agujas compartidas por cualquier motivo
					checkBox(pageHepC, 361.5f, 535.5f, 6.9f);
					break;
				case "Op": // Opioid use / Uso de opioids
					checkBox(pageHepC, 361.5f, 559.5f, 6.9f);
					break;
				case "HI": // HIV/AIDS coinfection / Coinfección por VIH/SIDA
					checkBox(pageHepC, 361.5f, 583.6f, 6.9f);
					break;
				case "Bo": // Body piercing in the last year /perforación del cuerpo en el último año
					checkBox(pageHepC, 363f, 607.5f, 6.9f);
					break;
				case "No": // None of the Above / Ninguna de las anteriores
					checkBox(pageHepC, 362.5f, 631.5f, 6.9f);
					break;
				}
			}

			pageHepC.complete();	
		}

		pdf.addObjects(objectsEncounter);
		pdf.close();
	}

	// specify x-y coordinate and size (diagonal)
	private static void checkBox(Page page, float x, float y, float diagonal) throws Exception {
		page.setPenColor(Color.black);
		page.setPenWidth(diagonal/5);
		page.moveTo(x, y);
		page.lineTo(x+diagonal, y+diagonal);
		page.moveTo(x, y + diagonal);
		page.lineTo(x + diagonal, y);
		page.strokePath();
	}

	// format numerical String to (###) ###-####
	public static String phoneNumberFormatter(String inputText) {
		int i;
		String phoneNumber = "";
		for (i = 0; i < inputText.length(); i++) { // remove non-numerical chars
			if (inputText.charAt(i) >= '0' && inputText.charAt(i) <= '9') {
				phoneNumber += inputText.charAt(i);
			}
		}
		String phoneFormatted;
		if (phoneNumber.length() == 10) {
			phoneFormatted = "(" + phoneNumber.substring(0, 3) + ") " + 
					phoneNumber.substring(3, 6) + "-" + 
					phoneNumber.substring(6, 10);
		} else if (phoneNumber.length() > 10) {
			phoneFormatted = "(" + phoneNumber.substring(0, 3) + ") " + 
					phoneNumber.substring(3, 6) + "-" + 
					phoneNumber.substring(6, 10) + "x" +
					phoneNumber.substring(10);
		} else {
			phoneFormatted = phoneNumber;
		}
		return phoneFormatted;
	}

	/*
	 * combine the following formulas to make text more readable
	 * removeExtraWhitespace(String inputText)
	 * capitalize(String inputText)
	 */
	public static String cleanupText(String inputText) {
		return removeExtraWhitespace(capitalize(inputText));
	}

	// make first character in alphabetic String capital
	private static String capitalize(String inputText) {
		String[] temp = inputText.split(" ");
		String out = "";
		for (int i = 0; i < temp.length; i++) {
			if (temp[i].length() > 0) {
				if (temp[i].charAt(0) <= 'z' && temp[i].charAt(0) >= 'a') {
					temp[i] = ("" + temp[i].charAt(0)).toUpperCase() + temp[i].substring(1);
				}
			}
			out += temp[i];
			if (i != temp.length-1) {
				out += " ";
			}
		}
		return out;
	}

	// remove leading, trailing, and duplicate spaces in text
	private static String removeExtraWhitespace(String inputText) {
		String[] temp = inputText.split(" ");
		String out = "";
		for (int i = 0; i < temp.length; i++) {
			if (temp[i].length() > 0) {
				out += temp[i];
				if (i != temp.length-1) {
					out += " ";
				}
			}
		}
		return out;
	}

	// dynamically resize font to fit desired text length
	public static float resizeFont(int currentTextLength, int desiredTextLength, float currentTextSize) {
		if (currentTextLength >= 2 * desiredTextLength) {
			return currentTextSize/2;
		}
		else if (currentTextLength > desiredTextLength) {
			return (float) (1-(currentTextLength-desiredTextLength)/(double) desiredTextLength)*currentTextSize;
		} else {
			return currentTextSize;
		}
	}

	// detect language of text
	public static String detectLanguage(String inputText) throws LangDetectException {
		detector = DetectorFactory.create();
		detector.append(inputText);
		return detector.detect();
	}
	
	// detect x number of possible languages for a given text
	@SuppressWarnings("unused")
	// TODO will replace previous construct String detectLanguage()
	private static ArrayList<Language> detectLanguage(String inputText, int numLangs) throws LangDetectException {
		detector = DetectorFactory.create();
		detector.append(inputText);
		ArrayList<Language> langList = detector.getProbabilities();
		for (int i = numLangs; i < langList.size(); i++) {
			langList.remove(i);
		}
		for (int i = 0; i < langList.size(); i++) {
			System.out.println(langList.get(i).lang);
		}
		return langList;
	}

	// for text output purposes, change language token (e.g. "en") to plain text language (e.g. "English")
	public static String remapLanguage(String inputText) {
		switch(inputText) {
		case "af":
			return "Afrikaans";
		case "ar":
			return "Arabic";
		case "bg":
			return "Bulgarian";
		case "bn":
			return "Bengali";
		case "cs":
			return "Czech";
		case "da":
			return "Danish";
		case "de":
			return "German";
		case "el":
			return "Greek";
		case "en":
			return "English";
		case "es":
			return "Spanish";
		case "fa":
			return "Persian";
		case "fi":
			return "Finnish";
		case "fr":
			return "French";
		case "gu":
			return "Gujarati";
		case "he":
			return "Hebrew";
		case "hi":
			return "Hindi";
		case "hr":
			return "Croatian";
		case "hu":
			return "Hungarian";
		case "id":
			return "Indonesian";
		case "it":
			return "Italian";
		case "ja":
			return "Japanese";
		case "kn":
			return "Kannada";
		case "ko":
			return "Korean";
		case "mk":
			return "Macedonian";
		case "ml":
			return "Malayalam";
		case "mr":
			return "Marathi";
		case "ne":
			return "Nepali";
		case "nl":
			return "Dutch";
		case "no":
			return "Norwegian";
		case "pa":
			return "Punjabi";
		case "pl":
			return "Polish";
		case "pt":
			return "Portuguese";
		case "ro":
			return "Romanian";
		case "ru":
			return "Russian";
		case "sk":
			return "Slovak";
		case "so":
			return "Somali";
		case "sq":
			return "Albanian";
		case "sv":
			return "Swedish";
		case "sw":
			return "Swahili";
		case "ta":
			return "Tamil";
		case "te":
			return "Telugu";
		case "th":
			return "Thai";
		case "tl":
			return "Tagalog";
		case "tr":
			return "Turkish";
		case "uk":
			return "Ukranian";
		case "ur":
			return "Urdu";
		case "vi":
			return "Vietnamese";
		case "zh-cn":
		case "zh-tw":
			return "Chinese";
		default:
			return inputText + "?";
		}
	}

	// use translateBot (Google Script API) to translate text from input to target language
	public static String translate(String inputLang, String targetLang, String text) throws IOException {
		String urlStr = "https://script.google.com/macros/s/AKfycbyB5xzGG5yxwc05ZWXhTx9W_nuXGsogwI6pNm9wnlI16_JZX4w4fcaEnXiGUcLPGLSCIw/exec" + 
				"?q=" + URLEncoder.encode(text, "UTF-8") + 
				"&target=" + targetLang + 
				"&source=" + inputLang;
		URL url = new URL(urlStr);
		StringBuilder response = new StringBuilder();
		HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
		urlConn.setRequestProperty("User-Agent", "Mozilla/5.0");
		BufferedReader br = new BufferedReader (new InputStreamReader(urlConn.getInputStream()));
		String inputLine;
		while ((inputLine = br.readLine()) != null) {
			response.append(inputLine);
		}
		br.close();
		return response.toString();
	}
	
	/*
	 *  using a street address converted to String of Hex values (e.g. %20%21%2A...),
	 *  use the Geoapify API to retrieve a predicted address
	 *  TODO implement with patient input addresses
	 */
	public static String predictAddress(String address) throws IOException {
		URL url = new URL("https://api.geoapify.com/v1/geocode/autocomplete?text=" + address + "&apiKey=d04228ce732e4a2f9466832b4ca3c184");
		HttpURLConnection http = (HttpURLConnection) url.openConnection();
		http.setRequestProperty("Accept", "application/json");
		BufferedReader br = new BufferedReader(new InputStreamReader(http.getInputStream()));
		String returnMessage = "";
		String line;
		while((line = br.readLine()) != null) {
			returnMessage += line + "\n";
		}
		http.disconnect();
		return returnMessage;
	}

	public static int getNumLines(String fileName) {
		int row = 1;
		try {
			while (!readCellData(fileName, row, 0).equals("")) {
				numPatients = row;
				row++;
			}
		} catch (NullPointerException e) {} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return row;
	}

	public static ArrayList<String> getFileNames() {
		return outputFiles;
	}

	private static String[] readLine(String fileName, int line) throws Exception {
		int i;
		String[] out = new String[26];
		/*
		 * cell data:
		 * [0]: last name
		 * [1]: first name
		 * [2]: middle name
		 * [3]: sex
		 * [4]: DOB
		 * [5]: language
		 * [6]: race
		 * [7]: ethnicity
		 * [8]: street address
		 * [9]: city
		 * [10]: state
		 * [11]: ZIP
		 * [12]: phone number
		 * [13]: email
		 * [14]: alt. phone number
		 * [15]: alt. contact name
		 * [16]: referred by
		 * [17]: reason for visit
		 * [18]: financial status
		 * [19]: consent for hepatitis C intake
		 * [20]: current symptoms
		 * [21]: hepatitis vaccinations
		 * [22]: hepatitis diagnosis
		 * [23]: blood transfusion pre-1992
		 * [24]: work with blood contact
		 * [25]: hepatitis C risks
		 */
		for (i = 0; i < 26; i++) {
			out[i] = readCellData(fileName, line, i+6);
		}
		return out;
	}
	
	private static void initConfig() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader("data/config.txt"));
		for (int i = 0; i < addressLine.length; i++) {
			addressLine[i] = br.readLine();
		}
		br.close();
	}

	@SuppressWarnings("deprecation")
	private static String readCellData(String fileName, int cellRow, int cellCol) {
		String out = null;
		XSSFWorkbook wb = null;
		try {
			FileInputStream fis = new FileInputStream(fileName);
			wb = new XSSFWorkbook(fis);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException e1) {
			e1.printStackTrace();
		}
		Sheet sheet = wb.getSheetAt(0);
		Row row = sheet.getRow(cellRow);
		Cell cell = row.getCell(cellCol);
		switch(cell.getCellType()) {
		case Cell.CELL_TYPE_STRING:
			out = cell.getStringCellValue();
			break;
		case Cell.CELL_TYPE_NUMERIC:
			out = "" + cell.getNumericCellValue();
			break;
		case Cell.CELL_TYPE_BLANK:
			out = "";
		}
		return out;
	}

	public void run() {
		try {
			generateForm(patient, iterator);
			Thread.sleep(100);
			if (printedPDF) {
				logText += outputFileName;
				if (iterator != numPatients-1) {
					logText += "<br>";
				} else {
					logText += "</html>";
				}
				MainGUI.fileLog.setText(logText);
				Thread.sleep(100);
			} else if (iterator == numPatients-1) {
				logText += "</html>";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
