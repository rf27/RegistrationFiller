# RegistrationFiller
Fills a PDF (specifically, the Shepherd's Hope patient encounter form) from an .xls/.xlsx file generated from Microsoft Forms. Interfaced with Java Swing.

## Copying and Set-Up
1. Fork this repository.
2. Download and unzip the RegistrationFiller folder.
	* In the data folder, open the `config.txt` file and edit the mailing address for your clinic.
	* Also in the data folder, replace the `encounter.pdf` and `encounter_hep.pdf` files with your clinic's PDF files.
3. Download a copy of [Git Bash](https://git-scm.com/download/win). 
4. Download and unzip the [Java Development Kit 18.0.1](https://download.oracle.com/java/18/archive/jdk-18.0.1_windows-x64_bin.zip) into the same directory as the .jar file.
5. Make a copy of the [Shepherd's Hope Online Patient Encounter Form](https://forms.office.com/Pages/ShareFormPage.aspx?id=IeHPzXcA5Eiujcmx7lkqQsm3CQBsuKlBoG0NwCaEnbtUNDlSNzEzV001STlLT1lRR0xDMTA4NUNESS4u&sharetoken=mCK0cH8IpOg6vmNBElw3).
6. Make a copy of Apps Script program [translateBot](https://script.google.com/d/1xwbrYbrQBNShJxdu7bT-wuJEdSXYhJ8A4dlHF6_HMbmemDgP9wXWayiM/edit?usp=sharing) and insert the web app URL in line 829 in `FormFiller.java`

## Using the Program
1. In the RegistrationFiller folder, open the `RegistrationFillerRunner.sh` file.
2. Follow the instructions included on the home page of the Java program.
3. PDFs will be printed to the folder containing the .xls/.xlsx file. By default, this will be the Downloads folder.

## License
This package is distributable under the Apache License 2.0 (see LICENSE file for more information).

## Dependent Packages
* [commons-collections4-4.1](https://commons.apache.org/proper/commons-collections/index.html)
* [commons-io-2.11.0](https://commons.apache.org/proper/commons-io/)
* jsonic-1.2.0
* [json-simple-4.0.1](https://code.google.com/archive/p/json-simple/)
* [langdetect](https://github.com/shuyo/language-detection)
* [pdfbox-app-2.0.26](https://pdfbox.apache.org/)
* [PDFjet for Java](https://pdfjet.com/java/index.html)
* poi-3.17
* poi-ooxml-3.17
* poi-ooxml-schemas-3.17
* [xmlbeans-2.6.0](https://xmlbeans.apache.org/)

## Authors
Michael Loyd ([michael.loyd@knights.ucf.edu](mailto:michael.loyd@knights.ucf.edu))
