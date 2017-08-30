package com.anchorage.system;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.anchorage.docks.containers.subcontainers.DockSplitterContainer;
import com.anchorage.docks.containers.subcontainers.DockTabberContainer;
import com.anchorage.docks.node.DockNode;
import com.anchorage.docks.node.DockNode.DockPosition;
import com.anchorage.docks.node.ui.DockUIPanel;
import com.anchorage.docks.stations.DockStation;
import com.anchorage.docks.stations.DockSubStation;

import javafx.scene.Node;
import javafx.scene.Parent;

public class AnchorageLayout {
	
	
	/////////////////////////////
	// Save Layout
	////////////////////////////

	/**
	 * Saves layout of the DockStation. <br/>
	 * <b>Warning:</b> Does not work with multiple instances of one UI class.
	 * 
	 * @param rootDockStation
	 *            Root dock station
	 * @param filePath
	 *            file path where to save layout.
	 * @return boolean if saving layout has been successful.
	 */
	public static boolean saveLayout(DockStation rootDockStation, final String filePath) {
		if (rootDockStation.getChildren().isEmpty()) {
			return false;
		}

		final Parent rootNode = rootDockStation;

		DocumentBuilderFactory icFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder icBuilder;

		try {
			icBuilder = icFactory.newDocumentBuilder();
			Document doc = icBuilder.newDocument();
			Element mainRootElement = doc.createElement(rootNode.getClass().getSimpleName().trim());
			doc.appendChild(mainRootElement);
			parseStationTree(rootNode, doc, mainRootElement, 0);

			System.out.println("XML DOM created successfully");

			// DOM XML output
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(filePath));
			transformer.transform(source, result);

			// console output
			// StreamResult console = new StreamResult(System.out);
			// transformer.transform(source, console);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private static void parseStationTree(final Parent parent, final Document doc, final Element element,
			final int index) {
		if (!parent.getChildrenUnmodifiable().isEmpty()) {
			for (Node child : parent.getChildrenUnmodifiable()) {

				final boolean allowedClasses = child instanceof DockSplitterContainer || child instanceof DockNode
						|| child instanceof DockTabberContainer
						|| /* child instanceof DockStation || */ child instanceof DockSubStation;
				Element childElement;

				// Omit elements, that are not necessary.
				if (allowedClasses) {

					// create and append child element
					final String elementName = child.getClass().getSimpleName().trim().replaceAll("/[^A-Za-z]/", "");
					childElement = doc.createElement(elementName);

					switch (elementName) {
					case "DockSplitterContainer":
						DockSplitterContainer container = (DockSplitterContainer) child;
						String dividerpositions = "";
						for (double position : container.getDividerPositions()) {
							dividerpositions += position + " ";
						}
						childElement.setAttribute("dividerPositions", dividerpositions);
						childElement.setAttribute("orientation", container.getOrientation().toString());
						break;
					case "DockNode":
						DockUIPanel dockUIPanel = ((DockNode) child).getContent();
						childElement.setAttribute("name", dockUIPanel.titleProperty().get());
						childElement.setAttribute("index", Integer.toString(index));
						break;
					default:
						break;
					}
					element.appendChild(childElement);
				} else {
					childElement = element;
				}

				// Append UI element or dig deeper
				if (child instanceof DockUIPanel && !(parent instanceof DockSubStation)) {
					Element uiElement = doc.createElement(((DockUIPanel) child).getNodeContent().getClass()
							.getSimpleName().trim().replaceAll("/[^A-Za-z]/", ""));
					childElement.appendChild(uiElement);
				} else if (child instanceof Parent) {
					final int newLevel = index + 1;
					parseStationTree((Parent) child, doc, childElement, newLevel);
				}

			}
		}
	}

	/////////////////////////////
	// Restore Layout
	////////////////////////////

	/**
	 * Restores layout of already docked nodes.
	 * 
	 * @param dockStation DockStation with all docked nodes.
	 * @param filePath Path to file, where layout is saved.
	 * @return boolean if restoring layout has been successful.
	 */
	public static boolean restoreLayout(final DockStation dockStation, final String filePath) {
		try {
			File inputFile = new File(filePath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(inputFile);

			NodeList list = doc.getElementsByTagName("DockNode");

			// Return if no elements in station
			if (list.getLength() == 0) {
				return false;
			}

			org.w3c.dom.Node startNode = list.item(0);

			for (int i = 1; i < list.getLength(); i++) {
				int currentIndex = Integer.parseInt(startNode.getAttributes().getNamedItem("index").getNodeValue());
				int newIndex = Integer.parseInt(list.item(i).getAttributes().getNamedItem("index").getNodeValue());

				if (newIndex > currentIndex) {
					startNode = list.item(i);
				}
			}
			System.out.println("############ StartNode");
			System.out.println(startNode.getNodeName());
			System.out.println(startNode.getAttributes().getNamedItem("index").getNodeValue());
			System.out.println(startNode.getAttributes().getNamedItem("name").getNodeValue());
			System.out.println("############ StartNode End");
			System.out.println();

			// Get all already docked DockNodes and remove all already docked nodes
			final List<DockNode> dockNodeList = new ArrayList<DockNode>(dockStation.getDockNodes());
			for (DockNode dockNode : dockNodeList) {
				dockNode.undock();
			}

			handleNode(dockStation, dockNodeList, startNode.getParentNode(), null, null);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	
	private static void handleNode(final DockStation station, final List<DockNode> dockNodeList,	final org.w3c.dom.Node parentNode, DockPosition firstDockPosition, DockNode dockFirstToThis) {
		System.out.println("handleNode() - " + parentNode.getNodeName());

		switch (parentNode.getNodeName()) {
		case "DockSplitterContainer":
			
			break;
		case "DockTabberContainer":

			break;
		case "DockSubStation":
			
			break;
		default:
			System.out.println("default");
			break;
		}
	}

	private static List<org.w3c.dom.Node> getChildElements(final NodeList list) {
		List<org.w3c.dom.Node> returnList = new ArrayList<>();
		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i).getNodeName().equals("#text")) {
				continue;
			}
			returnList.add(list.item(i));
		}
		return returnList;
	}
}
