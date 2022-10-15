/*
 * Description for fillerGUI.java
 * - opens an interface where the user is able to select a
 * 		.xls or .xlsx file from a file directory and 
 * 		generate filled PDF(s) using the FormFiller class
 * 
 * JAR Dependencies:
 * - Apache Commons Collections 4.1
 * - Apache POI 3.17
 * - Apache POI Common 3.17
 * - Apache POI API Based on OPC and OOXML Schemas 3.17
 * - XmlBeans 2.6.0
 * 		under Apache License Version 2.0: http://www.apache.org/licenses/LICENSE-2.0.txt
 * - PDFJet for Java
 * 		under evaluation license: https://pdfjet.com/java/download.html 
 */

package rf27.registrationFiller;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.BadLocationException;

public class MainGUI extends JFrame implements ActionListener {

	private static final long serialVersionUID = -6099263842497010934L;
	private static JPanel log;
	private static JLabel dataFile;
	public static JLabel fileLog;
	private static JLabel startAtLine;
	private static JButton generateFile;
	private static String fileName = ""; // track .xls/.xlsx file
	private static String directory = ""; // track folder containing .xls/.xlsx file <-- where PDFs are printed
	public static boolean initializedDetector = false; // enabled after first generation
	private static String fileNumberStartText = "", fileNumberEndText = ""; // user input in text boxes
	private static int fileNumberInt = 1; // track next line to print
	private static int logClearCount = 0; // for easter egg
	
	private static String easterEggA = "¸.·´¯`·.´¯`·.¸¸.·´¯`·.¸¸.·´¯`·.´¯`·.¸¸.·´¯`·.¸><(((º>";
	private static String easterEggB = "¸.·´¯`·.¸¸.·´¯`·.´¯`·.¸¸.·´¯`·.¸><(((º>¸.·´¯`·.´¯`·.¸";
	private static String easterEggC = "[Log cleared!]¸.·´¯`·.¸><(((º>¸.·´¯`·.´¯`·.¸¸.·´¯`·.¸";
	private static int eeB = 0, eeC = 0;
	private static boolean firstE = true;
	private static int fish = 0;
	
	static PrintWindow printWindow;
	
	private static String openFileToolText = "<html>(Hotkey CTRL + O)<br>"
			+ "Open this system's directory to find a Microsoft Excel file in the format<br>"
			+ "\"Shepherd's Hope Online Patient Encounter Form(1-x)\", where 'x + 1' refers to the number of patients in the spreadsheet.<html>";
	private static String generateFilesToolText = "<html>(Hotkey CTRL + G)<br>"
			+ "Generate PDFs from the specified Excel spreadsheet using the ranges specified in the top line.</html>";
	private static String clearLogToolText = "<html>(Hotkey CTRL + C)<br>"
			+ "Clears the text log in the middle of the window.</html>";
	private static String resetCounterToolText = "<html>(Hotkey CTRL + R)<br>"
			+ "Reset the line counter to 1. Use this only after clearing the Microsoft Form responses.</html>";
	
	public MainGUI(String name) {
		super(name);
		setFocusable(true);
	}
	
	private static MainGUI frame; // allow for reference by formulas

	public static void displayGUI() throws IOException {
//		String text = "9833 East Colonial Drive Orlando, Florida";
//		byte[] textBytes = text.getBytes();
//		String htmlText = "";
//		for (byte b : textBytes) {
//			htmlText += "%" + Integer.toHexString(b).toUpperCase();
//		}
//		System.out.println(htmlText);
//		String output = FormFiller.predictAddress(htmlText); // TODO remove after figuring out Geoapify JSON returns
		
		frame = new MainGUI("Registration Filler v1.1.0 by Michael Loyd");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(600, 300);
		frame.setMinimumSize(new Dimension(500, 300));
		frame.setResizable(true);
		frame.setLocationRelativeTo(null); // open in center of screen
		frame.setIconImage(new ImageIcon("data/donatehere-btn.png").getImage());
		
		JMenuBar mb = new JMenuBar();
		JMenu file = new JMenu("File");
		JMenuItem open = new JMenuItem("Open");
		open.setToolTipText(openFileToolText);
		JMenuItem exit = new JMenuItem("Exit");
		exit.setToolTipText("<html>(Hotkey CTRL + W)<br>"
				+ "Close the program.</html>");
		open.addActionListener(frame);
		exit.addActionListener(frame);
		file.add(open);
		file.add(exit);
		mb.add(file);
		
		JMenu task = new JMenu("Task");
		JMenuItem resetCounter = new JMenuItem("Reset Counter");
		resetCounter.setToolTipText(resetCounterToolText);
		JMenuItem resetLog = new JMenuItem("Clear Log");
		resetLog.setToolTipText(clearLogToolText);
		JMenuItem menuGenerateFiles = new JMenuItem("Generate File(s)");
		menuGenerateFiles.setToolTipText(generateFilesToolText);
		JMenuItem printFiles = new JMenuItem("Print File(s)");
		printFiles.setToolTipText("<html>(Hotkey CTRL + P)<br>"
				+ "Open the print window to select PDFs to print.</html>");
		resetCounter.addActionListener(frame);
		resetLog.addActionListener(frame);
		menuGenerateFiles.addActionListener(frame);
		printFiles.addActionListener(frame);
		task.add(resetCounter);
		task.add(resetLog);
		task.add(menuGenerateFiles);
		task.add(printFiles);
		mb.add(task);
		
		JMenu help = new JMenu("Help");
		JMenuItem changelog = new JMenuItem("Open Change Log");
		changelog.setToolTipText("<html>(HotKey CTRL + L)<br>"
				+ "Open the change log text file.");
		changelog.addActionListener(frame);
		help.add(changelog);
		mb.add(help);
		
		JPanel northPanel = new JPanel();
		generateFile = new JButton("Generate File(s)");
		generateFile.setToolTipText(generateFilesToolText);
		JButton clearLog = new JButton("Clear Log");
		clearLog.setToolTipText(clearLogToolText);
		startAtLine = new JLabel("Start from line: " + fileNumberInt + " to end; or from ");
		JTextArea fileNumberStart = new JTextArea(1, 5);
		fileNumberStart.setToolTipText("Specify a starting line number (>= 1).");
		JScrollPane jsaStart = new JScrollPane(fileNumberStart); // prevent resizing text box
		jsaStart.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		jsaStart.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		JLabel fileNumberSpacer = new JLabel(" to ");
		JTextArea fileNumberEnd = new JTextArea(1, 5);
		fileNumberEnd.setToolTipText("Specify an ending line number (1 <= X <= [last line])");
		JScrollPane jsaEnd = new JScrollPane(fileNumberEnd); // prevent resizing text box
		jsaEnd.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		jsaEnd.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		fileNumberStart.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				try {
					fileNumberStartText = e.getDocument().getText(0, e.getDocument().getLength());
				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}
			}
			public void removeUpdate(DocumentEvent e) {
				try {
					fileNumberStartText = e.getDocument().getText(0, e.getDocument().getLength());
				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}
			}
			public void changedUpdate(DocumentEvent e) {}
		});
		fileNumberEnd.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				try {
					fileNumberEndText = e.getDocument().getText(0, e.getDocument().getLength());
				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}
			}
			public void removeUpdate(DocumentEvent e) {
				try {
					fileNumberEndText = e.getDocument().getText(0, e.getDocument().getLength());
				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}
			}
			public void changedUpdate(DocumentEvent e) {}
		});
		
		fileNumberStart.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {}
			public void keyPressed(KeyEvent e) {
				if (!(e.getKeyCode() >= KeyEvent.VK_0 && e.getKeyCode() <= KeyEvent.VK_9) &&
						!(e.getKeyCode() >= KeyEvent.VK_NUMPAD0 && e.getKeyCode() <= KeyEvent.VK_NUMPAD9) &&
						e.getKeyCode() != KeyEvent.VK_BACK_SPACE) {
					e.consume();
				}
				if (e.getKeyCode() == KeyEvent.VK_TAB) {
					e.consume();
					KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
				}
				if (e.getKeyCode() == KeyEvent.VK_TAB && e.isShiftDown()) {
					e.consume();
					KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent();
				}
			}
			public void keyReleased(KeyEvent e) {}
		});
		
		fileNumberEnd.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {}
			public void keyPressed(KeyEvent e) {
				if (!(e.getKeyCode() >= KeyEvent.VK_0 && e.getKeyCode() <= KeyEvent.VK_9) &&
						!(e.getKeyCode() >= KeyEvent.VK_NUMPAD0 && e.getKeyCode() <= KeyEvent.VK_NUMPAD9) &&
						e.getKeyCode() != KeyEvent.VK_BACK_SPACE) {
					e.consume();
				}
				if (e.getKeyCode() == KeyEvent.VK_TAB) {
					e.consume();
					KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
				}
				if (e.getKeyCode() == KeyEvent.VK_TAB && e.isShiftDown()) {
					e.consume();
					KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent();
				}
			}
			public void keyReleased(KeyEvent e) {}
		});
		generateFile.addActionListener(frame);
		clearLog.addActionListener(frame);
		northPanel.add(generateFile);
		northPanel.add(clearLog);
		northPanel.add(startAtLine);
		northPanel.add(jsaStart);
		northPanel.add(fileNumberSpacer);
		northPanel.add(jsaEnd);

		JPanel southPanel = new JPanel();
		JLabel fileDirectory = new JLabel("File Directory");
		JButton browse = new JButton("Browse...");
		browse.setToolTipText(openFileToolText);
		browse.addActionListener(frame);
		dataFile = new JLabel("No File Selected");

		southPanel.add(fileDirectory);
		southPanel.add(browse);
		southPanel.add(dataFile);

		log = new JPanel();
		fileLog = new JLabel("<html>Click the \"Browse...\" button or File > Open to begin.<br>"
				+ "The program will make PDFs for the following patient entries:<br>"
				+ "- if both text boxes are blank, all entries in the spreadsheet.<br>"
				+ "- if only the left box is filled, from that number (if valid) to the last entry.<br>"
				+ "- if only the right box is filled, from the first entry to that number (if valid).<br>"
				+ "- if both boxes are filled, between those two entries (if both valid).</html>");
		log.add(fileLog);
		
		frame.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {}
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_W && e.isControlDown()) { // CTRL+W
					frame.dispose();
				}
				if (e.getKeyCode() == KeyEvent.VK_O && e.isControlDown()) { // CTRL+O
					openFile();
				}
				if (e.getKeyCode() == KeyEvent.VK_G && e.isControlDown()) { // CTRL+G
					generateFiles();
				}
				if (e.getKeyCode() == KeyEvent.VK_P && e.isControlDown()) { // CTRL+P
//					printFiles();
				}
				if (e.getKeyCode() == KeyEvent.VK_C && e.isControlDown()) { // CTRL+C
					clearLog();
				}
				if (e.getKeyCode() == KeyEvent.VK_L && e.isControlDown()) {
					openChangeLog();
				}
				
				// fish controller
				if ((e.getKeyCode() == KeyEvent.VK_F && fish == 0) ||
					(e.getKeyCode() == KeyEvent.VK_I && fish == 1) ||
					(e.getKeyCode() == KeyEvent.VK_S && fish == 2)) {
					fish++;
				}
				if (e.getKeyCode() == KeyEvent.VK_H && fish == 3) {
					fish++;
					fileLog.setText(easterEggUpdate());
				}
				if (e.getKeyCode() == KeyEvent.VK_TAB || e.getKeyCode() == KeyEvent.VK_SPACE) {
					fileLog.setText(easterEggUpdate());
				}
			}
			public void keyReleased(KeyEvent e) {}
			
		});
		
		frame.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {
				frame.requestFocus(); // allow KeyListener to continue, even after leaving this window
			}
			public void mouseExited(MouseEvent e) {}
		});

		frame.setJMenuBar(mb);
		frame.getContentPane().add(BorderLayout.CENTER, log);
		frame.getContentPane().add(BorderLayout.SOUTH, southPanel);
		frame.getContentPane().add(BorderLayout.NORTH, northPanel);
		frame.pack();
		frame.setVisible(true);	
	}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		
		if (command.equals("Open") || command.equals("Browse...")) {
			logClearCount = 0;
			fish = 0;
			openFile();
		}
		
		if (command.equals("Exit")) {
			this.dispose();
		}
		
		if (command.equals("Reset Counter")) {
			fileNumberInt = 1;
			startAtLine.setText("Start from line: " + fileNumberInt + " or ");
		}
		
		if (command.equals("Generate File(s)")) {
			fish = 0;
			generateFiles();
		}
		
		if (command.equals("Clear Log")) {
			fish = 0;
			clearLog();
		}
		if (command.equals("Print File(s)")) {
			fish = 0;
//			printFiles();
		}
		if (command.equals("Open Change Log")) {
			fish = 0;
			openChangeLog();
		}
	}
	
	private static void clearLog() {
		if (logClearCount <= 5) {
			logClearCount++;	
		}
		if (logClearCount > 5) {
			fileLog.setText(easterEggUpdate());
		} else {
			fileLog.setText("Log Cleared!");
		}
	}

	private static void openFile() {
		JFileChooser j = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
		FileFilter ff = new FileNameExtensionFilter("Microsoft Excel files (.xlsx, .xls)", "xlsx", "xls");
		j.setFileFilter(ff);
		j.setAcceptAllFileFilterUsed(false);
		int ret = j.showOpenDialog(null);
		if (ret == JFileChooser.APPROVE_OPTION) {
			dataFile.setText(j.getSelectedFile().getName());
			directory = j.getSelectedFile().getParent();
			fileName = j.getSelectedFile().getAbsolutePath();
		} else {
			dataFile.setText("No File Selected");
		}
	}
	
	private static void openChangeLog() {
		Desktop desktop = Desktop.getDesktop();
		File changeLogFile = new File("data/changelog.txt");
		if (changeLogFile.exists()) {
			try {
				desktop.open(changeLogFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void generateFiles() {
//		System.out.println(directory);
		new Thread(new Runnable() {
			public void run() {
				generateFile.setText("Running...");
				logClearCount = 0;
				if (validateAsNumber((fileNumberStartText.equals("")) ? "" + fileNumberInt : fileNumberStartText)) { // check if typed start number is valid; otherwise use default start
					if (!fileName.equals("")) {
						try {
							int numLines = FormFiller.getNumLines(fileName);
							int fileNumberStart = (fileNumberStartText.equals("") ? fileNumberInt : Integer.parseInt(fileNumberStartText));
							int fileNumberEnd = (fileNumberEndText.equals("") ? numLines-1 : Integer.parseInt(fileNumberEndText));
							if (fileNumberStart >= numLines) { // start number too high
								fileLog.setText("ERROR: Line start number (" + (fileNumberStartText.equals("") ? "" + fileNumberInt : fileNumberStartText) + ") exceeds patient count (" + numLines + ")");
							} else if (fileNumberStart < 1) { // start number too low
								fileLog.setText("ERROR: Line start number must be greater than 0.");
							} else if(fileNumberEnd >= numLines) { // end number too high
								fileLog.setText("ERROR: Line end number (" + fileNumberEndText + ") exceeds patient count (" + numLines + ")");
							} else if (fileNumberStart > fileNumberEnd) { // start number larger than end number
								fileLog.setText("ERROR: Line start number (" + fileNumberStartText + ") exceeds line end number (" + fileNumberEndText + ")");
							} else {
								new FormFiller(fileName);
								fileNumberInt = numLines;
								startAtLine.setText("Start from line: " + fileNumberInt + " to end; or from ");
								generateFile.setText("Finished!");
								Thread.sleep(2000);
							}
						} catch (Exception e1) {
							e1.printStackTrace();
						}	
					} else {
						fileLog.setText("ERROR: No target file selected!");
					}	
				} else {
					fileLog.setText("ERROR: Invalid line start number: " + fileNumberStartText);
				}
				generateFile.setText("Generate File(s)");
			}
		}).start();
	}
	
	@SuppressWarnings("unused")
	private static void printFiles() {
		if (printWindow == null) {
			if (fileName.equals("")) {
				fileLog.setText("ERROR: No target file selected!");
				return;
			}
			printWindow = new PrintWindow(frame, "Print");
			printWindow.addWindowListener(new WindowListener() {
				public void windowOpened(WindowEvent e) {
					System.out.println("Print Window Opened.");
				}
				
				public void windowClosing(WindowEvent e) {
					System.out.println("Print Window Closing.");
					printWindow = null;
					frame.toFront();
					frame.requestFocus();
				}
				
				public void windowClosed(WindowEvent e) {
					System.out.println("Print Window Closed.");
					printWindow = null;
					frame.toFront();
					frame.requestFocus();
				}

				public void windowIconified(WindowEvent e) {}

				public void windowDeiconified(WindowEvent e) {}

				public void windowActivated(WindowEvent e) {}

				public void windowDeactivated(WindowEvent e) {}
				
			});
		} else {
			fileLog.setText("ERROR: Print prompt is already open!");
		}
	}
	
	// only accessible when fileNumberStartText is a valid number (i.e. within range of list); see validatedAsText(String input)
	public static int getStartFile() {
		if (fileNumberStartText.equals("")) {
			return fileNumberInt;
		}
		return Integer.parseInt(fileNumberStartText);
	}
	
	// only accessible when fileNumberEndText is a valid number (i.e. within range of list); see validatedAsText(String input)
	public static int getEndFile() {
		if (fileNumberEndText.equals("")) {
			return -1; // -1 flags no end number specified
		}
		return Integer.parseInt(fileNumberEndText);
	}
	
	// return XLS/XLSX file
	public static String getFileName() {
		return fileName;
	}
	
	// return directory to XLS/XLSX file
	public static String getDirectory() {
		return directory;
	}
	
	public static boolean validateAsNumber(String inputText) {
		for (int i = 0; i < inputText.length(); i++) {
			if (inputText.charAt(i) < '0' || inputText.charAt(i) > '9') {
				return false;
			}
		}
		return true;
	}
	
	private static String easterEggUpdate() {
		easterEggA = easterEggA.charAt(easterEggA.length()-1) + easterEggA.substring(0, easterEggA.length()-1);
		if (eeB == 0) {
			easterEggB = easterEggB.charAt(easterEggB.length()-1) + easterEggB.substring(0, easterEggB.length()-1);
			eeB = (int) Math.ceil(Math.random() * 3);
		} else {
			eeB--;
		}
		if (eeC == 0) {
			eeC = (int) Math.ceil(Math.random() * 3);
		} else { // on bottom line, print "[Line cleared!]" and remove after panning off
			switch(easterEggC.charAt(easterEggC.length()-1)) {
			case '[':
			case ']':
				easterEggC = '¸' + easterEggC.substring(0, easterEggC.length()-1);
				break;
			case 'L':
			case '!':
				easterEggC = '.' + easterEggC.substring(0, easterEggC.length()-1);
				break;
			case 'o':
			case 'l':
			case 'd':
				easterEggC = '·' + easterEggC.substring(0, easterEggC.length()-1);
				break;
			case 'g':
			case 'a':
				easterEggC = '´' + easterEggC.substring(0, easterEggC.length()-1);
				break;
			case ' ':
			case 'r':
				easterEggC = '¯' + easterEggC.substring(0, easterEggC.length()-1);
				break;
			case 'c':
				easterEggC = '`' + easterEggC.substring(0, easterEggC.length()-1);
				break;
			case 'e':
				if (firstE) {
					easterEggC = '`' + easterEggC.substring(0, easterEggC.length()-1);
					firstE = false;
				} else {
					easterEggC = '.' + easterEggC.substring(0, easterEggC.length()-1);
				}
				break;
			default:
				easterEggC = easterEggC.charAt(easterEggC.length()-1) + easterEggC.substring(0, easterEggC.length()-1);
			}
			eeC--;
		}
		return "<html>" + easterEggA + "<br>" + easterEggB + "<br>" + easterEggC + "</html>";
	}
	
	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					displayGUI();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
