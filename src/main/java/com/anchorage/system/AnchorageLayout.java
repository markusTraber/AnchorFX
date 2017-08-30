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

import com.anchorage.docks.containers.common.DockCommons;
import com.anchorage.docks.containers.subcontainers.DockSplitterContainer;
import com.anchorage.docks.containers.subcontainers.DockTabberContainer;
import com.anchorage.docks.node.DockNode;
import com.anchorage.docks.node.DockNode.DockPosition;
import com.anchorage.docks.node.ui.DockUIPanel;
import com.anchorage.docks.stations.DockStation;
import com.anchorage.docks.stations.DockSubStation;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.geometry.Orientation;

public class AnchorageLayout {
	
	
	/////////////////////////////
	// Save Layout
	////////////////////////////

	/**
	 * Saves layout of the DockStation. <br/>
	 * <b>Warning:</b> Does not work with multiple instances of one UI class.
	 * 
	 * @param rootDockStation Root dock station
	 * @param filePath file path where to save layout.
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

	private static void parseStationTree(final Parent parent, final Document doc, final Element element, final int index) {
		if (!parent.getChildrenUnmodifiable().isEmpty()) {
			for (Node child : parent.getChildrenUnmodifiable()) {

				final boolean allowedClasses = child instanceof DockSplitterContainer || child instanceof DockNode || child instanceof DockTabberContainer || child instanceof DockSubStation;
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
					Element uiElement = doc.createElement(((DockUIPanel) child).getNodeContent().getClass().getSimpleName().trim().replaceAll("/[^A-Za-z]/", ""));
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

			// Get all already docked DockNodes and remove all already docked nodes
			final List<DockNode> dockNodeList = new ArrayList<DockNode>(dockStation.getDockNodes());
			for (DockNode dockNode : dockNodeList) {
				dockNode.undock();
			}

			List<org.w3c.dom.Node> firstChild = removeTextNodes(doc.getDocumentElement().getChildNodes());
			if (firstChild.isEmpty()) {
				return false; // no child elements
			}
			
			org.w3c.dom.Node startNode = firstChild.get(0);
			
			Node stationNode = handleNode(startNode, dockNodeList);
			dockStation.getChildren().add(stationNode);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	
	private static Node handleNode(final org.w3c.dom.Node node, final List<DockNode> dockNodeList) {
		switch (node.getNodeName()) {
		case "DockSplitterContainer":
			return handleDockSplitterContainer(node, dockNodeList);
		case "DockTabberContainer":
			return handleDockTabberContainer(node, dockNodeList);
		case "DockSubStation":
			// TODO: Implement substation and floating.
			break;
		case "DockNode":
			for (DockNode dockNode : dockNodeList) {
				if (node.getAttributes().getNamedItem("name") != null && dockNode.getContent().titleProperty().getValue().equals(node.getAttributes().getNamedItem("name").getNodeValue())) {
					return dockNode;
				} 
			}
		default:
			break;
		}
		return null;
	}
	
	private static Node handleDockSplitterContainer(final org.w3c.dom.Node node, final List<DockNode> dockNodeList) {
		final double dividerPosition = Double.parseDouble(node.getAttributes().getNamedItem("dividerPositions").getNodeValue());
		final List<org.w3c.dom.Node> childNodes = removeTextNodes(node.getChildNodes());
		final Orientation orientation;
		
		if (node.getAttributes().getNamedItem("orientation").getNodeValue().equals("VERTICAL")) {
			orientation = Orientation.VERTICAL;
		} else {
			orientation = Orientation.HORIZONTAL;
		}

		Node firstNode = handleNode(childNodes.get(0), dockNodeList);
		Node secondNode = handleNode(childNodes.get(1), dockNodeList);;
		
		return DockCommons.createSplitter(firstNode, secondNode, orientation, dividerPosition);
	}
	
	private static Node handleDockTabberContainer(final org.w3c.dom.Node node, final List<DockNode> dockNodeList) {
		final List<org.w3c.dom.Node> childNodes = removeTextNodes(node.getChildNodes());
		
		List<Node> nodeList = new ArrayList<>();
		for (int i = childNodes.size() - 1; i >= 0; i--) {
			nodeList.add(handleNode(childNodes.get(i), dockNodeList));
		}
		
		return DockCommons.createTabber(nodeList);
	}

	private static List<org.w3c.dom.Node> removeTextNodes(final NodeList list) {
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
