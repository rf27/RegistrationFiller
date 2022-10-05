package registration;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterAbortException;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.ArrayList;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.DialogTypeSelection;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.Sides;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;

public class PrintWindow extends JDialog {
	private static final long serialVersionUID = -6220062788679088551L;
	
	private JFrame parentFrame;
	private static boolean deleteFilesSelected = true;
	private static JLabel printingFiles;
	
	private static ArrayList<JCheckBox> fileList;
	private static JComboBox<PrintService> jcbPrinters;
	private static ArrayList<String> outputFiles;
	
	private static String directory, xlsFileName;
	
	public PrintWindow(JFrame frame, String name) {
		super(frame, name);
		parentFrame = frame;
		setFocusable(true);
		outputFiles = new ArrayList<String>();
		directory = MainGUI.getDirectory();
		xlsFileName = MainGUI.getFileName();
		displayGUI();
	}
	
	private void displayGUI() {		
		if (xlsFileName.equals("")) {
			super.dispose();
			return;
		}
		
		JPanel northPanel = new JPanel();
		JLabel selectedPrinter = new JLabel("Warning: No default printer found!");
		PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
		jcbPrinters = new JComboBox<PrintService>();
		for (PrintService ps : printServices) {
			jcbPrinters.addItem(ps);
		}

		PrintService defaultPrintService = PrintServiceLookup.lookupDefaultPrintService();
		if (defaultPrintService != null) {
			selectedPrinter.setText("Selected Printer: ");
			jcbPrinters.setSelectedItem(defaultPrintService);
		}
		northPanel.add(selectedPrinter);
		northPanel.add(jcbPrinters);
		getContentPane().add(BorderLayout.NORTH, northPanel);
				
		fileList = new ArrayList<JCheckBox>();
		boolean hasXLSXFile = false;
		boolean hasPDFFile = false;
		if (!xlsFileName.equals("")) {
			File folder = new File(directory);
			File[] directoryFileList = folder.listFiles();
			for (int i = 0; i < directoryFileList.length; i++) {
				String directoryFileExtension = null;
				try {
					directoryFileExtension = FilenameUtils.getExtension(directoryFileList[i].getName().toString());
				} catch (IllegalArgumentException e1) {
					e1.printStackTrace();
				}
				if (directoryFileExtension.equals("pdf")) {
					fileList.add(new JCheckBox(directoryFileList[i].getName(), false));
					outputFiles.add(directoryFileList[i].getName());
					hasPDFFile = true;
				} else if (directoryFileExtension.equals("xls") ||
						directoryFileExtension.equals("xlsx")) {
					hasXLSXFile = true;
				}
			}
		}
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));
		if (!hasXLSXFile) {
			centerPanel.add(new JLabel("ERROR: Directory does not contain an Excel file."));
		} else if (!hasPDFFile) {
			centerPanel.add(new JLabel("ERROR: No PDF files generated!"));
		} else {
			centerPanel.add(new JLabel("Select which file(s) to print:"));
			for (JCheckBox box : fileList) {
				centerPanel.add(box);
			}
		}
		printingFiles = new JLabel("");
		centerPanel.add(printingFiles);
		getContentPane().add(BorderLayout.CENTER, centerPanel);
		
		Dimension printWindowSize = new Dimension(500, 130 + 27 * (fileList.size()));
		setMinimumSize(printWindowSize);
		setPreferredSize(printWindowSize);
		
		JPanel southPanel = new JPanel();
		JButton selectAll = new JButton("Select All");
		selectAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (selectAll.getActionCommand().equals("Deselect All")) {
					for (JCheckBox box : fileList) {
						box.setSelected(false);
					}
					selectAll.setActionCommand("Select All");
					selectAll.setText("Select All");
				} else {
					for (JCheckBox box : fileList) {
						box.setSelected(true);
					}
					selectAll.setActionCommand("Deselect All");
					selectAll.setText("Deselect All");
				}
			}
		});
		JCheckBox deleteFiles = new JCheckBox("Delete file(s) after printing", true);
		deleteFiles.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				deleteFilesSelected = deleteFiles.isSelected();
			}
		});
		JButton printSelected = new JButton("Print selected file(s)");
		printSelected.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				print();
			}
		});
		southPanel.add(selectAll);
		southPanel.add(deleteFiles);
		southPanel.add(printSelected);
		getContentPane().add(BorderLayout.SOUTH, southPanel);
		
		pack();
		setLocationRelativeTo(parentFrame);
		setVisible(true);
		
		addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {}
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					dispose();
				}
			}
			public void keyReleased(KeyEvent e) {}
		});
	}
	
	private void print() {
		PrinterJob pj = PrinterJob.getPrinterJob();
		try {
			pj.setPrintService((PrintService) jcbPrinters.getSelectedItem());
		} catch (PrinterException e1) {}
		PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
		pras.add(MediaSizeName.NA_LETTER);
		pras.add(new Copies(1));
		pras.add(Sides.ONE_SIDED);
		pras.add(OrientationRequested.PORTRAIT);
		pras.add(Chromaticity.MONOCHROME);
		pras.add(new MediaPrintableArea(0f, 0f, 8.5f, 11f, MediaPrintableArea.INCH));
		pras.add(DialogTypeSelection.NATIVE);
		pras.add(PrintQuality.HIGH);
		PageFormat pf = new PageFormat();
		Paper paper = new Paper();
		paper.setSize(8.5f, 11f);
		paper.setImageableArea(0f, 0f, 8.5f, 11f);
		pf.setPaper(paper);
		pj.setCopies(1);
		for (JCheckBox box : fileList) {
			if (!box.isSelected()) {
				continue; // skip over unchecked boxes
			}		
			try {
				String fileName = box.getActionCommand();
				
				File file = new File(directory + "/" + fileName);
				if (!file.exists()) {
					System.out.println("File not found: " + fileName);
					printingFiles.setText("File not found: " + fileName);
					continue;
				} else {
					System.out.println("Printing file: " + fileName);
					printingFiles.setText("Printing file: " + fileName);
				}
				PDDocument pdFile = PDDocument.load(file);
				PDFPrintable pdfPrintable = new PDFPrintable(pdFile, Scaling.SCALE_TO_FIT);
				// TODO remove font warnings
				
				pras.add(new JobName(box.getActionCommand(), null));
				pj.setJobName(box.getActionCommand());
				pj.pageDialog(pf);
				pj.printDialog(pras);
				pj.setPrintable(pdfPrintable);
				pj.print();
				System.out.println("Printed file: " + box.getActionCommand());
				pdFile.close();
				if (deleteFilesSelected) {
					file.delete(); // delete PDF file after printing if indicated
				}
				printingFiles.setText("Finished!");
			} catch (PrinterAbortException e) {
				pj.cancel();
				System.out.println("Print prompt cancelled.");
			} catch (PrinterException | IOException e) {
				e.printStackTrace();
			}
		}
		if (deleteFilesSelected) { // after finish printing files, delete Excel file if indicated
			Path path = FileSystems.getDefault().getPath("", xlsFileName);
			try {
				Files.delete(path);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.dispose(); // close print window
	}
}
