package com.janescript;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;

import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLConverter;
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLOptions;
import fr.opensagres.xdocreport.core.XDocReportException;
import fr.opensagres.xdocreport.core.io.internal.ByteArrayOutputStream;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class App extends Application {
	private ImageView imageView;
	private WebView webView;

	@Override
	public void start(Stage primaryStage) {
		Preferences prefs = Preferences.userNodeForPackage(App.class);

		Button chooseFile = new Button("Select File");

		imageView = new ImageView();
		webView = new WebView();

		chooseFile.setOnAction(event -> {
			// Open file directory window with settings set below
			String lastOpenedDirectory = prefs.get("lastOpenedDirectory", System.getProperty("user.dir")); // Get from
																											// preferences
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Select File");
			fileChooser.setInitialDirectory(new File(lastOpenedDirectory));
			fileChooser.getExtensionFilters()
					.addAll(new FileChooser.ExtensionFilter("Files", "*.doc", "*.docx", "*.xls", "*.xlsx", "*.pdf"));

			File selectedFile = fileChooser.showOpenDialog(primaryStage);
			if (selectedFile != null) {
				// Update last opened directory for quicker access
				prefs.put("lastOpenedDirectory", selectedFile.getParent());
				// Process file depending on type
				try {
					if (selectedFile.getName().toLowerCase().endsWith(".pdf")) {
						System.out.println("Selected PDF: " + selectedFile.getAbsolutePath());
						OpenPDF(selectedFile);
					}

					else if (selectedFile.getName().toLowerCase().endsWith(".docx")
							|| selectedFile.getName().toLowerCase().endsWith(".doc")) {
						ProcessWord(selectedFile);
						System.out.println("Selected Word Document: " + selectedFile.getAbsolutePath());
					} else if (selectedFile.getName().toLowerCase().endsWith(".xlsx")
							|| selectedFile.getName().toLowerCase().endsWith(".xls")) {
						ProcessExcel(selectedFile);
						System.out.println("Selected Excel Document: " + selectedFile.getAbsolutePath());
					} else {
						throw new UnsupportedFileTypeException();
					}
				} catch (Exception ex) {
					ShowAlert(ex.getMessage(), AlertType.ERROR);
				}
			}

		});
		VBox vbox = new VBox(chooseFile, imageView, webView);
		Scene scene = new Scene(vbox, 300, 200);
		primaryStage.setScene(scene);
		primaryStage.setTitle("My JavaFX App");
		primaryStage.show();
	}

	public void OpenPDF(File file) {
		// rendering PDF as image (not editable)
		try {
			PDDocument doc = PDDocument.load(file);
			PDFRenderer pdfr = new PDFRenderer(doc);
			BufferedImage bufferedImage = pdfr.renderImageWithDPI(0, 50); // Render first page
			Image image = SwingFXUtils.toFXImage(bufferedImage, null);
			imageView.setImage(image);
			doc.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void ProcessExcel(File file) throws IOException {

	}

	private String ConvertDocxToHtml(File file) throws IOException, XDocReportException {
		try (InputStream inputStream = new FileInputStream(file);
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

			XWPFDocument document = new XWPFDocument(inputStream); // Load XWPFDocument

			XHTMLOptions xhtmlOptions = XHTMLOptions.create();
			xhtmlOptions.setIgnoreStylesIfUnused(false);

			XHTMLConverter.getInstance().convert(document, outputStream, xhtmlOptions); // Convert docx to html

			String htmlContent = outputStream.toString("UTF-8");

			// Extract and replace image URIs
			for (XWPFPictureData pictureData : document.getAllPictures()) {
				String imageName = pictureData.getFileName();
				byte[] imageData = pictureData.getData();
				String dataUri = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageData);
				htmlContent = htmlContent.replace("word/media/" + imageName, dataUri); //
			}

			try {
				// Open the DOCX package
				OPCPackage opcPackage = OPCPackage.open(file);

				// Get the InkML file
				PackagePartName partName = PackagingURIHelper.createPartName("/word/ink/ink1.xml");
				PackagePart inkmlPart = opcPackage.getPart(partName);
				InputStream inkmlStream = inkmlPart.getInputStream();

				// Parse InkML using JAXB
				JAXBContext jaxbContext = JAXBContext.newInstance(Inkml.class, Trace.class); // Pass both classes
				Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
				Inkml inkml = (Inkml) unmarshaller.unmarshal(inkmlStream);

				// Access the <inkml:trace> elements
				List<Trace> traces = inkml.getTraces();
				if (traces != null) {
					for (Trace trace : traces) {
						//System.out.println("Trace value: " + trace.getValue());
					}
				} else {
					System.out.println("No traces found.");
				}

				// ... process traces ...
				for (Trace trace : traces) {
					String traceData = trace.getValue();

					List<Point> points = parseCoordinates(traceData);
					for (Point point : points) {
						//System.out.println(point);
					}
					StringBuilder pathData = new StringBuilder();
					for (int i = 0; i < points.size(); i++) {
						Point point = points.get(i);
						if (i == 0) {
							pathData.append("M ").append((point).getX()).append(" ").append((point).getY()).append(" ");
						} else {
							pathData.append("L ").append((point).getX()).append(" ").append((point).getY()).append(" ");
						}
					}
					// Create SVG element
					String svgMarkup = "<svg width=\"500\" height=\"300\">" + "<path d=\"" + pathData.toString()
							+ "\" stroke=\"black\" />" + "</svg>";
					htmlContent = svgMarkup + htmlContent;
				}

				// Close the package
				opcPackage.close();

			} catch (IOException | InvalidFormatException | JAXBException e) {
				e.printStackTrace();
			}
			return htmlContent;

		}
	}

	private List<Point> parseCoordinates(String traceData) {
		List<Point> coordinates = new ArrayList<Point>();
		String data = traceData;
		String regex = "([-+]?\\d+)[\\s',\"]*([-+]?\\d+)";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(data);

		while (matcher.find()) {
			try {
				double x = Double.parseDouble(matcher.group(1));
				double y = Double.parseDouble(matcher.group(2));
				coordinates.add(new Point(x, y));

			} catch (NumberFormatException e) {
				// Handle parsing errors (e.g., log the error, skip the point)
				System.err.println("Error parsing coordinates: " + matcher.group());
			}
		}
		

		return coordinates;
	}

	private void ProcessWord(File file) throws IOException {
		try {
			String htmlContent = ConvertDocxToHtml(file);
			// System.out.println(htmlContent);
			webView.getEngine().loadContent(htmlContent);
		} catch (XDocReportException e) {
			e.printStackTrace();
		}
	}

	public static void ShowAlert(String message, AlertType alertType) {
		Alert alert = new Alert(alertType);
		alert.setTitle("Alert");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

	public class UnsupportedFileTypeException extends Exception {
		String text = "The file type you have selected is not supported";

		public String getMessage() {
			return text;
		}
	}

	public static void main(String[] args) {
		launch(args);

	}
}
